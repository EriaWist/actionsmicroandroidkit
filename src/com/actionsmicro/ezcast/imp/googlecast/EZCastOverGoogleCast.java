package com.actionsmicro.ezcast.imp.googlecast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Bundle;

import com.actionsmicro.BuildConfig;
import com.actionsmicro.ezcast.ConnectionManager;
import com.actionsmicro.ezcast.DisplayApi;
import com.actionsmicro.ezcast.MediaPlayerApi;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;
import com.actionsmicro.web.SimpleContentUriHttpFileServer;
import com.actionsmicro.web.SimpleMotionJpegHttpServer;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.Listener;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class EZCastOverGoogleCast implements DisplayApi, MediaPlayerApi {
	private static final double VOLUME_INCREMENT = 0.1;
	private EZCastChannel ezcastChannel;
	private SimpleMotionJpegHttpServer simpleMotionJpegHttpServer;
	private String TAG = "EZCastOverGoogleCast";
	private GoogleCastApp currentApplication;
	private ByteArrayOutputStream compressionBuffer;
	private Context context;
	private CastDevice castDevice;
	private static Map<CastDevice, EZCastOverGoogleCast> reg = new HashMap<CastDevice, EZCastOverGoogleCast>();
	private static HashMap<EZCastOverGoogleCast, Integer> referenceCount = new HashMap<EZCastOverGoogleCast, Integer>(); 

	public EZCastOverGoogleCast(Context context, CastDevice castDevice) {
		this.context = context;
		this.castDevice = castDevice;
	}
	private ArrayList<ConnectionManager> managers = new ArrayList<ConnectionManager>();
	private MediaPlayerStateListener mediaPlayerStateListener;

	private RemoteMediaPlayer mRemoteMediaPlayer;
	private SimpleContentUriHttpFileServer simpleHttpFileServer;
	private GoogleApiClient googleCastApiClient;

	public void addConnectionManager(ConnectionManager listener) {
		synchronized(managers) {
			if (!managers.contains(listener)) {
				managers.add(listener);
			}
		}
	}
	public void removeConnectionManager(ConnectionManager manager) {
		synchronized(managers) {
			managers.remove(manager);
		}
	}
	private void notifyConnectionManagerDidFailed(Exception e) {
		synchronized(managers) {
			Iterator<ConnectionManager> iterator = new CopyOnWriteArrayList<ConnectionManager>(managers).listIterator();
			while (iterator.hasNext()) {
				ConnectionManager manager = iterator.next(); 
				if (manager != null) {
					manager.onConnectionFailed(this, e);
				}						
			}
		}
	}
	public void connect() {
		connectGoogleCastApi();
	}
	private void connectGoogleCastApi() {
		Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
				.builder(castDevice, new Listener() {
					@Override
					public void onApplicationDisconnected (int statusCode) {
						Log.d(TAG, EZCastOverGoogleCast.this + ": onApplicationDisconnected");
						notifyConnectionManagerDidFailed(new Exception("Google Cast API: onApplicationDisconnected : " + statusCode)); //TODO create custom exception
						teardown();
					}
					@Override
					public void onApplicationStatusChanged () {
						if (googleCastApiClient != null) {
							Log.d(TAG, EZCastOverGoogleCast.this + ": onApplicationStatusChanged: "
									+ Cast.CastApi.getApplicationStatus(googleCastApiClient));
						}
					}
					@Override
					public void onVolumeChanged () {
						Log.d(TAG, EZCastOverGoogleCast.this + ": onVolumeChanged");
					}
				});

		googleCastApiClient = new GoogleApiClient.Builder(context)
		.addApi(Cast.API, apiOptionsBuilder.build())
		.addConnectionCallbacks(new ConnectionCallbacks() {

			private boolean waitingForReconnect;

			@Override
			public void onConnected(Bundle arg0) {
				if (googleCastApiClient != null) {
					if (waitingForReconnect) {
						waitingForReconnect = false;
					} else {
						launcheEZCastApp(isDisplaying);
					}
				}
			}

			@Override
			public void onConnectionSuspended(int arg0) {
				Log.d(TAG , EZCastOverGoogleCast.this + ": onConnectionSuspended:");
				waitingForReconnect = true;

			}

		})
		.addOnConnectionFailedListener(new OnConnectionFailedListener() {

			@Override
			public void onConnectionFailed(ConnectionResult arg0) {
				Log.d(TAG, EZCastOverGoogleCast.this + ": onConnectionFailed:");
				notifyConnectionManagerDidFailed(new Exception("Google Cast API: onConnectionFailed : " + arg0)); //TODO create custom exception
			}

		})
		.build();
		Log.d(TAG, EZCastOverGoogleCast.this + ": GoogleApiClient.connect");
		googleCastApiClient.connect();
	}

	public boolean isReadyToDisplay() {
		if (currentApplication != null) {
			return currentApplication.isApplicationStarted();
		}
		return false;
	}
	public synchronized void sendJpegEncodedScreenData(InputStream input, long length) {
		Log.d(TAG, EZCastOverGoogleCast.this + ": try to sendJpegEncodedScreenData");		
		if (simpleMotionJpegHttpServer != null) {
			Log.d(TAG, EZCastOverGoogleCast.this + ": sendJpegEncodedScreenData");
			simpleMotionJpegHttpServer.sendJpegStream(input, length);
		} else {
			ByteArrayOutputStream compressionBuffer = getCompressionBuffer();
			compressionBuffer.reset();
			try {
				Utils.dump(input, compressionBuffer);
			} catch (IOException e) {
				e.printStackTrace();
				compressionBuffer.reset();
			}
		}
	}

	@Override
	public void disconnect() {
		teardown();
	}

	@Override
	public void startDisplaying() {
		isDisplaying = true;
		if (!isCurrentApplicationEzCast()) {
			launcheEZCastApp(true);			
		} else {
			startDisplayingImp();
		}
	}
	private void startDisplayingImp() {
		if (simpleMotionJpegHttpServer == null || ezcastChannel == null) {
			connectEzCastChannel();
			createMjpegServer();	
			// { "method": "echo", "params": ["Hello JSON-RPC"], "id": 1}
			sendMessage("{ \"method\": \"display\", \"params\": {\"url\" : \""+simpleMotionJpegHttpServer.getServerUrl()+"\"}, \"id\": null}");			
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ByteArrayOutputStream compressionBuffer = getCompressionBuffer();
			if (compressionBuffer.size() > 0) {
				sendJpegEncodedScreenData(new ByteArrayInputStream(compressionBuffer.toByteArray()), compressionBuffer.size());
				compressionBuffer.reset();
			}
		}
	}
	private boolean isCurrentApplicationEzCast() {
		return currentApplication != null && currentApplication.getAppId().equals(getEzCastAppId()) && currentApplication.isApplicationStarted();
	}

	@Override
	public void stopDisplaying() {
		isDisplaying = false;
		stopDisplayingImp();
	}
	private void stopDisplayingImp() {
		Log.d(TAG, EZCastOverGoogleCast.this + ": stopDisplayingImp");
		
		sendMessage("{\"jsonrpc\": \"2.0\", \"method\": \"stopDisplay\"}");
		try {
			disconnectEzCastChannel();
		} catch (IOException e) {
			e.printStackTrace();
		}
		releaserMjpegServer();
	}

	@Override
	public void resendLastImage() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public synchronized void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception {
		if (simpleMotionJpegHttpServer != null) {
			final int width = yuvImage.getWidth();
			final int height = yuvImage.getHeight();
			Log.d(TAG, "sentImageToServer width=" + width+",height=" + height);
			ByteArrayOutputStream compressionBuffer = getCompressionBuffer();
			compressionBuffer.reset();
			Log.d(TAG, "Start compress");
			android.graphics.Rect rect = new android.graphics.Rect(0, 0, width, height); 
			yuvImage.compressToJpeg(rect, quailty, compressionBuffer);
			sendJpegEncodedScreenData(new ByteArrayInputStream(compressionBuffer.toByteArray()), compressionBuffer.size());
			compressionBuffer.reset();
		}
	}
	private ByteArrayOutputStream getCompressionBuffer() {
		// for performance reason we keep it as member
		if (compressionBuffer == null) {
			compressionBuffer = new ByteArrayOutputStream(1024*1024);
		}
		return compressionBuffer;
	}
	private void teardown() {
		Log.d(TAG, EZCastOverGoogleCast.this + ": teardown");
		stopStreamPositionTimer();
		if (googleCastApiClient != null) {
			stopDisplayingImp();
			stopCurrentApplication();
			
			if (googleCastApiClient.isConnected()) {
				googleCastApiClient.disconnect();
			}
			googleCastApiClient = null;
		}

		castDevice = null;
	}
	private void stopCurrentApplication() {
		if (currentApplication != null) {
			Log.d(TAG, "stopApplication:" + currentApplication.getAppId());
			currentApplication.stopApplication();
			currentApplication = null;
			try {
				Thread.sleep(500); // Tricky part to wait until app is stopped. no callback design in google Api.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	private void disconnectEzCastChannel() throws IOException {
		if (googleCastApiClient != null) {
			if (ezcastChannel != null) {          
				Log.d(TAG, EZCastOverGoogleCast.this + ": Remove Custom channel : removeMessageReceivedCallbacks");
				Cast.CastApi.removeMessageReceivedCallbacks(googleCastApiClient, ezcastChannel.getNamespace());
				ezcastChannel = null;
			}	
		}
	}
	private void releaserMjpegServer() {
		if (simpleMotionJpegHttpServer != null) {
			Log.d(TAG, EZCastOverGoogleCast.this + ": releaserMjpegServer");
			simpleMotionJpegHttpServer.cleanup();
			simpleMotionJpegHttpServer = null;
		}
	}
	private void connectEzCastChannel() {
		if (ezcastChannel == null && 
				simpleMotionJpegHttpServer == null) {
			ezcastChannel = new EZCastChannel();
			if (ezcastChannel != null && googleCastApiClient != null) {
				try {
					Log.d(TAG, EZCastOverGoogleCast.this + ": Create Custom channel : setMessageReceivedCallbacks");
					Cast.CastApi.setMessageReceivedCallbacks(googleCastApiClient,
							ezcastChannel.getNamespace(),
							ezcastChannel);
				} catch (IOException e) {
					Log.e(TAG, "Exception while creating channel", e);
				}
			}
		}
	}
	private void createMjpegServer() {
		simpleMotionJpegHttpServer = new SimpleMotionJpegHttpServer(0);
		Log.d(TAG, EZCastOverGoogleCast.this + ": createMjpegServer");
		
	}
	private void sendMessage(final String message) {
		if (googleCastApiClient != null && ezcastChannel != null) {
			try {
				Log.d(TAG, EZCastOverGoogleCast.this + ": sendMessage:"+message);
				Cast.CastApi.sendMessage(googleCastApiClient, ezcastChannel.getNamespace(), message)
				.setResultCallback(
						new ResultCallback<Status>() {
							@Override
							public void onResult(Status result) {
								Log.d(TAG, EZCastOverGoogleCast.this + ": sendMessage("+message+").onResult:"+result);
								if (!result.isSuccess()) {
									Log.e(TAG, "Sending message failed");
								}
							}
						});
			} catch (Exception e) {
				Log.e(TAG, "Exception while sending message", e);
			}
		}
	}
	class EZCastChannel implements MessageReceivedCallback {
		public String getNamespace() {
			return "urn:x-cast:com.actions-micro.ezcast";
		}

		@Override
		public void onMessageReceived(CastDevice castDevice, String namespace,String message) {
			Log.d(TAG, EZCastOverGoogleCast.this + ": onMessageReceived: " + message);
		}
	}
	public static synchronized EZCastOverGoogleCast createClient(Context context,
			CastDevice castDevice, ConnectionManager connectionManager) {
		EZCastOverGoogleCast googleCastClient; 
		if (reg.containsKey(castDevice)) {
			googleCastClient = reg.get(castDevice);
			googleCastClient.addConnectionManager(connectionManager);
			referenceCount.put(googleCastClient, referenceCount.get(googleCastClient) + 1);
		} else {
			googleCastClient = new EZCastOverGoogleCast(context, castDevice);
			googleCastClient.addConnectionManager(connectionManager);
			googleCastClient.connect();		
			referenceCount.put(googleCastClient, 1);
			reg.put(castDevice, googleCastClient);
		}
		return googleCastClient;
	}
	public static synchronized void releaseClient(EZCastOverGoogleCast googleCastClient, ConnectionManager connectionManager) {
		googleCastClient.removeConnectionManager(connectionManager);
		if (referenceCount.containsKey(googleCastClient)) {
			int refCount = referenceCount.get(googleCastClient) - 1;
			if (refCount == 0) {
				reg.remove(googleCastClient.castDevice);
				referenceCount.remove(googleCastClient);
				googleCastClient.disconnect();				
			} else {
				referenceCount.put(googleCastClient, refCount);
			}
		}
	}
	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		// TODO throw exception?
	}
	@Override
	public State getState() {
		return playerState;
	}
	@Override
	public boolean pause() {
		if (mRemoteMediaPlayer != null) {
			try {
				mRemoteMediaPlayer.pause(googleCastApiClient);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}
	@Override
	public boolean resume() {
		if (mRemoteMediaPlayer != null) {
			try {
				mRemoteMediaPlayer.play(googleCastApiClient);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}
	@Override
	public boolean increaseVolume() {
		if (googleCastApiClient != null) {
			double currentVolume = Cast.CastApi.getVolume(googleCastApiClient);
			if (currentVolume < 1.0) {
				try {
					Cast.CastApi.setVolume(googleCastApiClient, 
							Math.min(currentVolume + VOLUME_INCREMENT, 1.0));
				} catch (Exception e) {
					Log.e(TAG, "unable to set volume", e);
					return false;
				}
			}
			return true;
		}
		return false;
	}
	@Override
	public boolean decreaseVolume() {
		if (googleCastApiClient != null) {
			double currentVolume = Cast.CastApi.getVolume(googleCastApiClient);
			if (currentVolume > 0.0) {
				try {
					Cast.CastApi.setVolume(googleCastApiClient, 
							Math.max(currentVolume - VOLUME_INCREMENT, 0.0));
				} catch (Exception e) {
					Log.e(TAG, "unable to set volume", e);
					return false;
				}
			}
			return true;
		}
		return false;
	}
	@Override
	public boolean seek(int position) {
		if (mRemoteMediaPlayer != null) {
			mRemoteMediaPlayer.seek(googleCastApiClient, 1000L * position);
			return true;
		}
		return false;
	}
	@Override
	public boolean stop() {
		if (mRemoteMediaPlayer != null &&
				playerState != State.STOPPED) {
			try {
				mRemoteMediaPlayer.stop(googleCastApiClient);
				playerState = State.STOPPED;
				handleMediaPlayerStop();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}
	@Override
	public boolean play(final Context context, final String url, String userAgentString, Long mediaContentLength, final String title) throws Exception {
		stopDisplayingImp();
		launcheApplication("D3D8AEDC", new ResultCallback<Cast.ApplicationConnectionResult>() {

			@Override
			public void onResult(Cast.ApplicationConnectionResult result) {
				Status status = result.getStatus();
				if (status.isSuccess()) {
					stopHttpFileServer();
					String mimeType = "video/mp4";
					Uri mediaUri = null;
					try {
						mediaUri = Uri.parse(url);
						if (mediaUri.getScheme() == null) {
							mediaUri = mediaUri.buildUpon().scheme("file").build();
						}
					} catch (Exception e) {
						e.printStackTrace();
						mediaUri = Uri.fromFile(new File(url));
					}
					String mediaUriString = url;
					if (mediaUri.getScheme().equalsIgnoreCase(ContentResolver.SCHEME_CONTENT) || 
							mediaUri.getScheme().equalsIgnoreCase("file")) {
						simpleHttpFileServer = new SimpleContentUriHttpFileServer(context, mediaUri, 0);
						try {
							simpleHttpFileServer.start();
						} catch (IOException e) {
							e.printStackTrace();
						}
						mediaUriString = simpleHttpFileServer.getServerUrl();
						mimeType = simpleHttpFileServer.getMimeType();
					}
					createMediaPlayerIfNeeded();
					MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
					if (title != null) {
						mediaMetadata.putString(MediaMetadata.KEY_TITLE, title);
					}
					Log.d(TAG, "try to load url:"+mediaUriString);
					MediaInfo mediaInfo = new MediaInfo.Builder(mediaUriString) //
					.setContentType(mimeType)
					.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
					.setMetadata(mediaMetadata)
					.build();
					try {
						mRemoteMediaPlayer.load(googleCastApiClient, mediaInfo, true)
						.setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {

							@Override
							public void onResult(MediaChannelResult result) {
								Status status = result.getStatus();
								if (status.isSuccess()) {
									Log.d(TAG, "Media loaded successfully");
									playerState = State.PLAYING;
									if (mediaPlayerStateListener != null) {
										mediaPlayerStateListener.mediaPlayerDidStart(EZCastOverGoogleCast.this);
									}
									if (mediaPlayerStateListener != null) {
										mediaPlayerStateListener.mediaPlayerDurationIsReady(EZCastOverGoogleCast.this, mRemoteMediaPlayer.getStreamDuration()/1000);
									}
								} else {
									Log.e(TAG, "Media loaded media failed: code:"+status.getStatusCode() + ";" + status.getStatus());
									PendingIntent resolution = status.getResolution();
									if (resolution != null) {
										Log.d(TAG, "pending resolution:"+resolution);
									}
									if (mediaPlayerStateListener != null) {
										mediaPlayerStateListener.mediaPlayerDidFailed(EZCastOverGoogleCast.this, AV_RESULT_ERROR_GENERIC); //TODO do code conversion 
									}
									launcheEZCastApp(isDisplaying);
								}
							}
						});
					} catch (IllegalStateException e) {
						Log.e(TAG, "Problem occurred with media during loading", e);
					} catch (Exception e) {
						Log.e(TAG, "Problem opening media during loading", e);
					}
				} else {
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidFailed(EZCastOverGoogleCast.this, AV_RESULT_ERROR_START_INIT_FAILED);
					}
					launcheEZCastApp(isDisplaying);
				}
			}

		});
		return true;
	}
	private void stopHttpFileServer() {
		if (simpleHttpFileServer != null) {
			simpleHttpFileServer.stop();
			simpleHttpFileServer = null;
		}
	}
	private State playerState = State.STOPPED;
	private Timer mSeekbarTimer;
	private boolean isDisplaying;

	private void createMediaPlayerIfNeeded() {
		if (mRemoteMediaPlayer == null && googleCastApiClient != null) {
			mRemoteMediaPlayer = new RemoteMediaPlayer();
			mRemoteMediaPlayer.setOnStatusUpdatedListener(
					new RemoteMediaPlayer.OnStatusUpdatedListener() {

						
						@Override
						public void onStatusUpdated() {
							MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
							Log.d(TAG, EZCastOverGoogleCast.this +": onStatusUpdated:" + mediaStatus.getPlayerState() +
									" duration:" + mRemoteMediaPlayer.getStreamDuration() + 
									" position:"+ mRemoteMediaPlayer.getApproximateStreamPosition());
							switch (mediaStatus.getPlayerState()) {
							case MediaStatus.PLAYER_STATE_PLAYING:
								playerState = State.PLAYING;
								startStreamPositionTimer();
								break;
							case MediaStatus.PLAYER_STATE_PAUSED:
								playerState = State.PAUSED;
								stopStreamPositionTimer();
								break;								
							case MediaStatus.PLAYER_STATE_BUFFERING:
								break;
							case MediaStatus.PLAYER_STATE_UNKNOWN:
								break;
							case MediaStatus.PLAYER_STATE_IDLE:
								playerState = State.STOPPED;
								if (mRemoteMediaPlayer.getStreamDuration() != 0) {
									handleMediaPlayerStop();
								}
								break;
							}
						}
					});

			mRemoteMediaPlayer.setOnMetadataUpdatedListener(
					new RemoteMediaPlayer.OnMetadataUpdatedListener() {
						@Override
						public void onMetadataUpdated() {
							Log.d(TAG, EZCastOverGoogleCast.this +": onMetadataUpdated."+
									" duration:" + mRemoteMediaPlayer.getStreamDuration() + 
									" position:"+ mRemoteMediaPlayer.getApproximateStreamPosition());
							MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
							if (mediaInfo != null) {
								MediaMetadata metadata = mediaInfo.getMetadata();
							}
						}
					});
			try {
				Cast.CastApi.setMessageReceivedCallbacks(googleCastApiClient, mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public MediaPlayerStateListener getMediaPlayerStateListeners() {
		return mediaPlayerStateListener;
	}
	public void setMediaPlayerStateListeners(MediaPlayerStateListener mediaPlayerStateListener) {
		this.mediaPlayerStateListener = mediaPlayerStateListener;
	}
	private void startStreamPositionTimer() {
		stopStreamPositionTimer();
		mSeekbarTimer = new Timer();
		mSeekbarTimer.scheduleAtFixedRate(new UpdateStreamPositionTask(), 100, 1000);
	}
	private void stopStreamPositionTimer() {
		if (mSeekbarTimer != null) {
			mSeekbarTimer.cancel();
			mSeekbarTimer = null;
		}
	}
	private void launcheApplication(String castAppId, ResultCallback<Cast.ApplicationConnectionResult> resultCallback) {
		if (currentApplication == null || !currentApplication.getAppId().equals(castAppId)) {
			Log.d(TAG, "launcheApplication:" + castAppId);
			
			stopCurrentApplication();
			if (googleCastApiClient != null) {
				currentApplication = new GoogleCastApp(googleCastApiClient, castAppId);
				currentApplication.launcheApplication(resultCallback);
			}
		}
	}
	private void launcheEZCastApp(final boolean startDisplaying) {
		launcheApplication(getEzCastAppId(), new ResultCallback<Cast.ApplicationConnectionResult>() {

			@Override
			public void onResult(Cast.ApplicationConnectionResult result) {
				Status status = result.getStatus();
				if (status.isSuccess()) {
					if (startDisplaying) {
						startDisplayingImp();
					}
				} else {
					notifyConnectionManagerDidFailed(new Exception("Google Cast API: launcheApplication: onResult : " + result.getStatus())); //TODO create custom exception
					teardown();
				}
			}

		});
	}
	private String getEzCastAppId() {
		String castAppId = GoogleCastFinder.CAST_APP_ID;
		if (BuildConfig.DEBUG) {
			castAppId = GoogleCastFinder.CAST_DEV_APP_ID;
		}
		return castAppId;
	}
	private void handleMediaPlayerStop() {
		stopHttpFileServer();		
		launcheEZCastApp(isDisplaying);
		if (mediaPlayerStateListener != null) {
			mediaPlayerStateListener.mediaPlayerDidStop(EZCastOverGoogleCast.this);
		}
	}
	private class UpdateStreamPositionTask extends TimerTask {

        @Override
        public void run() {
        	if (mRemoteMediaPlayer != null && mediaPlayerStateListener != null) {
        		mediaPlayerStateListener.mediaPlayerTimeDidChange(EZCastOverGoogleCast.this, mRemoteMediaPlayer.getApproximateStreamPosition()/1000);
        	}
        }
    }
}

