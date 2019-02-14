package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Display;
import android.view.TextureView;
import android.view.WindowManager;

import com.actionsmicro.BuildConfig;
import com.actionsmicro.androidkit.ezcast.ConnectionManager;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.TrackableApi;
import com.actionsmicro.androidkit.ezcast.imp.googlecast.ScreenPresentation.BackPressedHandler;
import com.actionsmicro.graphics.YuvImageToJpegHelper;
import com.actionsmicro.utils.Log;
import com.actionsmicro.web.SimpleContentUriHttpFileServer;
import com.actionsmicro.web.SimpleMotionJpegHttpServer;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.Cast.Listener;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastRemoteDisplay;
import com.google.android.gms.cast.LaunchOptions;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class EZCastOverGoogleCast implements DisplayApi, MediaPlayerApi {
	private static final double VOLUME_INCREMENT = 0.1;
	private final Bitmap mAdvertiseImg;
	private EZCastChannel ezcastChannel;
	private SimpleMotionJpegHttpServer simpleMotionJpegHttpServer;
	private String TAG = "EZCastOverGoogleCast";
	private GoogleCastApp currentApplication;
	private ByteArrayOutputStream compressionBuffer;
	private Context context;
	private CastDevice castDevice;
	private final DeviceInfo mDeviceInfo;
	private TrackableApi trackableApi;
	private static Map<CastDevice, EZCastOverGoogleCast> reg = new HashMap<CastDevice, EZCastOverGoogleCast>();
	private static HashMap<EZCastOverGoogleCast, Integer> referenceCount = new HashMap<EZCastOverGoogleCast, Integer>();
    private ScreenPresentation mPresentation;
    private TextureView mSurfaceTextureView;
	private RtspDecoder mPlayer;
    private boolean isSurfaceReady = false;
    private BackPressedHandler mBackPressedHandler;

    public EZCastOverGoogleCast(Context context,DeviceInfo deviceInfo , TrackableApi trackableApi, Bitmap advertiseImg, BackPressedHandler backPressedHandler) {
		if (Build.VERSION.SDK_INT >= 24) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
		this.context = context;
		mDeviceInfo = deviceInfo;
		castDevice = ((GoogleCastDeviceInfo)deviceInfo).getCastDevice();
		mAdvertiseImg = advertiseImg;
		mBackPressedHandler = backPressedHandler;
		this.trackableApi = trackableApi;
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
						Log.d(TAG,   ": onApplicationDisconnected");
//						notifyConnectionManagerDidFailed(new Exception("Google Cast API: onApplicationDisconnected : " + statusCode)); //TODO create custom exception
//						teardown();
					}
					@Override
					public void onApplicationStatusChanged () {
						if (googleCastApiClient != null) {
							try {
								Log.d(TAG, ": onApplicationStatusChanged: "
										+ Cast.CastApi.getApplicationStatus(googleCastApiClient));
							} catch (IllegalStateException e) {
							}
						}
					}
					@Override
					public void onVolumeChanged () {
						Log.d(TAG,   ": onVolumeChanged");
					}
				});

        CastRemoteDisplay.CastRemoteDisplayOptions.Builder remoteDisplayBuilder = new
                CastRemoteDisplay.CastRemoteDisplayOptions.Builder(castDevice, new CastRemoteDisplay.CastRemoteDisplaySessionCallbacks() {
            @Override
            public void onRemoteDisplayEnded(Status status) {
                Log.d(TAG, "Stop Casting because Remote Display session ended");
            }
        });

        googleCastApiClient = new GoogleApiClient.Builder(context)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addApi(CastRemoteDisplay.API, remoteDisplayBuilder.build())
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
				Log.d(TAG ,   ": onConnectionSuspended:");
				waitingForReconnect = true;

			}

		})
		.addOnConnectionFailedListener(new OnConnectionFailedListener() {

			@Override
			public void onConnectionFailed(ConnectionResult arg0) {
				Log.d(TAG,   ": onConnectionFailed:");
				notifyConnectionManagerDidFailed(new Exception("Google Cast API: onConnectionFailed : " + arg0)); //TODO create custom exception
				teardown();
			}

		})
		.build();
		Log.d(TAG,   ": GoogleApiClient.connect");
		googleCastApiClient.connect();
	}

	public boolean isReadyToDisplay() {
		if (currentApplication != null) {
			return currentApplication.isApplicationStarted();
		}
		return false;
	}
	public synchronized void sendJpegEncodedScreenData(InputStream input, long length) {
//		Log.d(TAG,   ": try to sendJpegEncodedScreenData");

		if(mPresentation !=null){
			mPresentation.setImageView(input);
			return;
		}


		if ((getState() == State.PLAYING && mIsStopping == false) || !isReadyToDisplay()) {
			return;
		}
		if (simpleMotionJpegHttpServer != null) {
//			Log.d(TAG,   ": sendJpegEncodedScreenData");
			simpleMotionJpegHttpServer.sendJpegStream(input, length);
		}
	}

	@Override
	public void disconnect() {
		teardown();
	}

	@Override
	public void startDisplaying() {
		if ((getState() == State.PLAYING && mIsStopping == false)) {
			return;
		}

		if (!isDisplaying) {
			if (trackableApi != null) {
				trackableApi.startTrackingWifiDisplay();
			}
		}
		isDisplaying = true;
		launcheEZCastApp(true);
	}
	private void startDisplayingImp(ResultCallback<Status> resultCallback) {
		Log.d(TAG,   ": startDisplayingImp");
		if(!isSupportH264Streaming()){
			if (simpleMotionJpegHttpServer == null || ezcastChannel == null) {
				connectEzCastChannel();
				createMjpegServer();
				// { "method": "echo", "params": ["Hello JSON-RPC"], "id": 1}
				sendMessage("{ \"method\": \"display\", \"params\": {\"url\" : \""+simpleMotionJpegHttpServer.getServerUrl()+"\"}, \"id\": null}", resultCallback);
			}
		}
	}
	private boolean isCurrentApplicationEzCast() {
		return (currentApplication != null && currentApplication.getAppId().equals(getEzCastAppId()) && currentApplication.isApplicationStarted());
	}

	@Override
	public void stopDisplaying() {
		if (isDisplaying && trackableApi != null) {
			trackableApi.stopTrackingWifiDisplay();
		}
		isDisplaying = false;
		stopDisplayingImp(null);
	}

	private boolean isSupportH264Streaming(){
		return mDeviceInfo.supportH264Streaming();
	}

	private void stopDisplayingImp(final ResultCallback<Status> resultCallback) {
		Log.d(TAG,   ": stopDisplayingImp");
		if(!isSupportH264Streaming()){
			sendMessage("{\"jsonrpc\": \"2.0\", \"method\": \"stopDisplay\"}", new ResultCallback<Status>() {

				@Override
				public void onResult(Status result) {
					try {
						disconnectEzCastChannel();
					} catch (IOException e) {
						e.printStackTrace();
					}
					releaserMjpegServer();
					if (resultCallback != null) {
						resultCallback.onResult(result);
					}
				}

			});
		} else {
			dismissPresentation();
			stopApplication(currentApplication, resultCallback);
		}
	}

	@Override
	public void resendLastImage() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public synchronized void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception {
		if (simpleMotionJpegHttpServer != null) {
			YuvImageToJpegHelper helper = YuvImageToJpegHelper.getDefaultHelper();
			synchronized (helper) {
				InputStream inputStream = helper.compressYuvImageToJpegStream(yuvImage, quailty);
				sendJpegEncodedScreenData(inputStream, inputStream.available());
			}
		}
	}

	@Override
	public void sendH264EncodedScreenData(byte[] contents, int width, int height) throws Exception {
		if(mDeviceInfo.supportH264Streaming()){
			if(!isSurfaceReady){
				enqueueH264Data(contents);
			}  else {
				enqueueH264Data(contents);
				dequeueH264Data();
			}
		}
	}

    private ArrayList<byte[]> h264Queue = new ArrayList<>();

	private synchronized void enqueueH264Data(byte[] contents) {
		// clear previous data after got codec data
		if (contents[4] == 0x67) {
			h264Queue.clear();
		}
		h264Queue.add(contents);
	}

	private synchronized void dequeueH264Data() {
		if (h264Queue.size() == 0 || !isSurfaceReady) {
			return;
		}
		while (h264Queue.size() > 0) {
			byte[] contents = h264Queue.remove(0);
			onReceivedData(contents);
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
		Log.d(TAG,   ": teardown");
		stopStreamPositionTimer();
		if (googleCastApiClient != null) {
			stopDisplayingImp(null);
			stopCurrentApplication(new ResultCallback<Status>() {

				@Override
				public void onResult(Status arg0) {
					if(currentApplication != null){
						Log.d(TAG, "stopApplication:" + currentApplication.getAppId());
						currentApplication = null;
					}
				}
				
			});
			
			if (googleCastApiClient.isConnected()) {
				googleCastApiClient.disconnect();
			}
			googleCastApiClient = null;
		}

	}
	private void stopCurrentApplication(final ResultCallback<Status> resultCallback) {
		stopApplication(currentApplication, resultCallback);
		currentApplication = null;
	}
	private List<Runnable> pendingTasks = new ArrayList<Runnable>();
	public void stopApplication(final GoogleCastApp application,
			final ResultCallback<Status> resultCallback) {
		Log.d(TAG, "stopApplication:"+(application!=null?application.getAppId():application));
		if (application != null) {
			final String appId = application.getAppId();
			application.stopApplication(new ResultCallback<Status>() {

				@Override
				public void onResult(Status result) {
					if (result != null && !result.isSuccess()) {
						Log.d(TAG, "stopApplication -> failed onResult:"+result);
					}
					Log.d(TAG, "application " + appId+" stopped");

					if (resultCallback != null) {
						resultCallback.onResult(result);
					}
				}
			});
		} else {
			if (resultCallback != null) {
				resultCallback.onResult(null);
			}
		}
	}
	private void disconnectEzCastChannel() throws IOException {
		if (googleCastApiClient != null) {
			if (ezcastChannel != null) {          
				Log.d(TAG,   ": Remove Custom channel : removeMessageReceivedCallbacks");
				Cast.CastApi.removeMessageReceivedCallbacks(googleCastApiClient, ezcastChannel.getNamespace());
				ezcastChannel = null;
			}	
		}
	}
	private void releaserMjpegServer() {
		if (simpleMotionJpegHttpServer != null) {
			Log.d(TAG,   ": releaserMjpegServer");
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
					Log.d(TAG,   ": Create Custom channel : setMessageReceivedCallbacks");
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
		simpleMotionJpegHttpServer = new SimpleMotionJpegHttpServer(context, 0, new SimpleMotionJpegHttpServer.OnConnectionListener() {
			@Override
			public void onClientConnected(SimpleMotionJpegHttpServer simpleMotionJpegHttpServer) {
			}
		});
		Log.d(TAG,   ": createMjpegServer");
		
	}
	private void sendMessage(final String message, final ResultCallback<Status> resultCallback) {
		if (googleCastApiClient != null && googleCastApiClient.isConnected() && ezcastChannel != null) {
			try {
				Log.d(TAG, ": sendMessage:"+message);
				Cast.CastApi.sendMessage(googleCastApiClient, ezcastChannel.getNamespace(), message)
				.setResultCallback(
						new ResultCallback<Status>() {
							@Override
							public void onResult(Status result) {
								Log.d(TAG, ": sendMessage("+message+").onResult:"+result);
								if (!result.isSuccess()) {
									Log.e(TAG, "Sending message failed");
								}
//								if (resultCallback != null) {
//									resultCallback.onResult(result);
//								}
							}
						});
			} catch (Exception e) {
				Log.e(TAG, "Exception while sending message", e);
			}
		} else {
		}
		if (resultCallback != null) {
			resultCallback.onResult(null);
		}
	}
	class EZCastChannel implements MessageReceivedCallback {
		public String getNamespace() {
			return "urn:x-cast:com.actions-micro.ezcast";
		}

		@Override
		public void onMessageReceived(CastDevice castDevice, String namespace,String message) {
			Log.d(TAG,   ": onMessageReceived: " + message);
		}
	}
	public static synchronized EZCastOverGoogleCast createClient(Context context,
																 DeviceInfo deviceInfo, TrackableApi trackableApi, ConnectionManager connectionManager, Bitmap advertiseImg, BackPressedHandler backPressedHandler) {
		EZCastOverGoogleCast googleCastClient;
		CastDevice castDevice = ((GoogleCastDeviceInfo)deviceInfo).getCastDevice();
		if (reg.containsKey(castDevice)) {
			googleCastClient = reg.get(castDevice);
			googleCastClient.addConnectionManager(connectionManager);
			referenceCount.put(googleCastClient, referenceCount.get(googleCastClient) + 1);
		} else {
			googleCastClient = new EZCastOverGoogleCast(context, deviceInfo, trackableApi, advertiseImg, backPressedHandler);
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
			mRemoteMediaPlayer.pause(googleCastApiClient);
			return true;
		}
		return false;
	}
	@Override
	public boolean resume() {
		if (mRemoteMediaPlayer != null) {
			mRemoteMediaPlayer.play(googleCastApiClient);
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
	private boolean byUser = false;
	private boolean mIsStopping = false;
	@Override
	public boolean stop() {
		if (mRemoteMediaPlayer != null &&
				playerState != State.STOPPED) {
			byUser = true;
			mIsStopping =true;
			Runnable stopPlaying = new Runnable() {

				@Override
				public void run() {
					final Runnable runnable = this;
					if (mRemoteMediaPlayer != null &&
							playerState != State.STOPPED) {
						Log.d(TAG, "going to stop mRemoteMediaPlayer");
						mRemoteMediaPlayer.stop(googleCastApiClient).setResultCallback(new ResultCallback<MediaChannelResult>() {

							@Override
							public void onResult(MediaChannelResult result) {
								
								if (result.getStatus().isSuccess()) {
									Log.d(TAG, "mRemoteMediaPlayer.stop success!");
								} else {
									Log.d(TAG, "mRemoteMediaPlayer.stop failed: code:"+result.getStatus().getStatusCode() + ";" + result.getStatus().getStatus());									
									
								}
								mIsStopping = false;
								playerState = State.STOPPED;
								handleMediaPlayerStop(Cause.USER);
								if (trackableApi != null) {
									trackableApi.commitMediaUsageTracking();
								}						
								finishPendingTask(runnable);
							}
							
						});
					} else {
						Log.d(TAG, "mRemoteMediaPlayer already stopped");
						mIsStopping = false;
						playerState = State.STOPPED;
						finishPendingTask(runnable);
					}
				}

			};
			executeOrSchedule("stopPlaying", stopPlaying);
			return true;
		}
		return false;
	}
	private void executeOrSchedule(String taskName, Runnable pendingTask) {
		synchronized (pendingTasks) {
			pendingTasks.add(pendingTask);
			if (pendingTasks.size() == 1) {
				Log.d(TAG, "run "+taskName+" directly");
				pendingTask.run();
			} else {
				Log.d(TAG, "schedule "+taskName);
			}
		}
	}
	@Override
	public boolean play(final Context context, final String url, final String userAgentString, Long mediaContentLength, final String title) throws Exception {
		Log.d(TAG, "play "+url);
		stopDisplayingImp(new ResultCallback<Status>() {
			@Override
			public void onResult(Status result) {
				dismissPresentation();
				launchMediaPlayer(context, url, userAgentString, title);
			}

		});
		return true;
	}
	private void launchMediaPlayer(final Context context, final String url, final String userAgentString,
			final String title) {
		launcheApplication(getEzCastMediaPlayerId(), new ResultCallback<Cast.ApplicationConnectionResult>() {

			@Override
			public void onResult(Cast.ApplicationConnectionResult result) {
				if (result == null || result.getStatus().isSuccess()) {
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
					} else {
					}
					if (trackableApi != null) {
						trackableApi.beginMediaUsageTracking(context, url, userAgentString, title);
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
									long duration = mRemoteMediaPlayer.getStreamDuration() / 1000;
									if (mediaPlayerStateListener != null) {
										mediaPlayerStateListener.mediaPlayerDurationIsReady(EZCastOverGoogleCast.this, duration);
									}
									if (trackableApi != null) {
										trackableApi.setMediaUsageDuration((int) duration);
									}
								} else {
									Log.e(TAG, "Media loaded media failed: code:" + status.getStatusCode() + ";" + status.getStatus());
									PendingIntent resolution = status.getResolution();
									if (resolution != null) {
										Log.d(TAG, "pending resolution:" + resolution);
									}
									if (mediaPlayerStateListener != null) {
										mediaPlayerStateListener.mediaPlayerDidFailed(EZCastOverGoogleCast.this, AV_RESULT_ERROR_GENERIC); //TODO do code conversion 
									}
									if (trackableApi != null) {
										trackableApi.setMediaUsageResultCode(String.valueOf(status.getStatusCode()), AV_RESULT_ERROR_GENERIC);
										trackableApi.commitMediaUsageTracking();
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

		}, new LaunchOptions.Builder().setRelaunchIfRunning(true).build(), null);
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
							// ignore wierd behavior that still receive playing stat after stopped
							if (mediaStatus != null && currentApplication.getAppId().equals(getEzCastMediaPlayerId())) {
								Log.d(TAG,  ": onStatusUpdated:" + mediaStatus.getPlayerState() +
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
									if (playerState != State.STOPPED && mRemoteMediaPlayer.getStreamDuration() != 0) {
										handleMediaPlayerStop(Cause.REMOTE);
									}
									playerState = State.STOPPED;
									break;
								}
							}
						}
					});

			mRemoteMediaPlayer.setOnMetadataUpdatedListener(
					new RemoteMediaPlayer.OnMetadataUpdatedListener() {
						@Override
						public void onMetadataUpdated() {
							Log.d(TAG,  ": onMetadataUpdated."+
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
	private void launcheApplication(final String castAppId, final ResultCallback<Cast.ApplicationConnectionResult> resultCallback, final LaunchOptions options, final GoogleCastApp.RemoteDisplayListener remoteDisplayListener) {
		Runnable launchApp = new Runnable() {

			@Override
			public void run() {
				final Runnable runnable = this;
				Log.d(TAG, "launcheApplication:"+castAppId+", currentApplication:"+(currentApplication!=null?currentApplication.getAppId():null));
				final GoogleCastApp appToBeLaunched = new GoogleCastApp(googleCastApiClient, castAppId);
				if ((currentApplication == null || !currentApplication.getAppId().equals(castAppId))) {
					GoogleCastApp appToBeStopped = currentApplication;
					currentApplication = null;
					stopApplication(appToBeStopped, new ResultCallback<Status>() {
						@Override
						public void onResult(Status result) {
							appToBeLaunched.launcheApplication(new ResultCallback<Cast.ApplicationConnectionResult>() {

								@Override
								public void onResult(
										ApplicationConnectionResult result) {
									currentApplication = appToBeLaunched;
									if (resultCallback != null) {
										resultCallback.onResult(result);
									}
									finishPendingTask(runnable);
								}
							}, options, remoteDisplayListener);
						}
					});
				} else {
					appToBeLaunched.launcheApplication(new ResultCallback<Cast.ApplicationConnectionResult>() {

						@Override
						public void onResult(
								ApplicationConnectionResult result) {
							currentApplication = appToBeLaunched;
							if (resultCallback != null) {
								resultCallback.onResult(result);
							}
							finishPendingTask(runnable);
						}
					}, options, remoteDisplayListener);
				}
			}
		};
		executeOrSchedule("launchApp", launchApp);		
	}
	private void launcheEZCastApp(final boolean startDisplaying) {
		if(currentApplication != null && currentApplication.getAppId() == getEzCastAppId() && currentApplication.isApplicationStarted()){
			return;
		}
		launcheApplication(getEzCastAppId(), new ResultCallback<Cast.ApplicationConnectionResult>() {

			@Override
			public void onResult(Cast.ApplicationConnectionResult result) {
				if (result == null || result.getStatus().isSuccess()) {
					if (startDisplaying) {
						startDisplayingImp(null);
					}
				} else {
					notifyConnectionManagerDidFailed(new Exception("Google Cast API: launcheApplication: onResult : " + result.getStatus())); //TODO create custom exception
					teardown();
				}
			}

			// https://github.com/ConnectSDK/Connect-SDK-Android-Google-Cast/blob/master/src/com/connectsdk/service/CastService.java
			// use true to avoid Relaunch 2005 issue
		}, new LaunchOptions.Builder().setRelaunchIfRunning(true).build(), new GoogleCastApp.RemoteDisplayListener() {
			@Override
			public void onConnectSuccessed(Display display) {
				createPresentation(display);
				Log.d(TAG, "Created presentation");
			}

			@Override
			public void onConnectFailed(Status status) {
				Log.d(TAG, "launcheEZCastApp onConnectFailed");

			}
		});
	}

    private void createPresentation(Display display) {
        dismissPresentation();
		mPresentation = new ScreenPresentation(context, display, mAdvertiseImg, new ScreenPresentation.BackPressedHandler() {
			@Override
			public void onBackPressed() {
				mBackPressedHandler.onBackPressed();
			}
		});
        try {
            mPresentation.show();
            mSurfaceTextureView = mPresentation.getTextureView();
			mPlayer = new RtspDecoder(mSurfaceTextureView, new TextureView.SurfaceTextureListener() {
				@Override
				public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
					isSurfaceReady = true;
					dequeueH264Data();
				}

				@Override
				public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

				}

				@Override
				public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
					return false;
				}

				@Override
				public void onSurfaceTextureUpdated(SurfaceTexture surface) {

				}
			});
        } catch (WindowManager.InvalidDisplayException ex) {
            Log.e(TAG, "Unable to show presentation, display was removed.", ex);
            dismissPresentation();
        }
    }

	private void dismissPresentation() {
		isSurfaceReady = false;
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer = null;
		}

		if (mPresentation != null) {
			mPresentation.dismiss();
			mPresentation = null;
		}
	}

	private String getEzCastAppId() {
		String castAppId = GoogleCastFinder.CAST_APP_ID;
		if (BuildConfig.DEBUG) {
			castAppId = GoogleCastFinder.CAST_DEV_APP_ID;
		}
		if(mDeviceInfo.supportH264Streaming()){
		    castAppId = GoogleCastFinder.CAST_REMOTEDISPLAY_APPID;
        }
		return castAppId;
	}

	private String getEzCastMediaPlayerId() {
		String castAppId = GoogleCastFinder.CAST_MEDIA_PLAYER_ID;
		return castAppId;
	}
	private void handleMediaPlayerStop(Cause cause) {
		stopHttpFileServer();		
//		launcheEZCastApp(isDisplaying);
		if (mediaPlayerStateListener != null) {
			if (!byUser)
				mediaPlayerStateListener.mediaPlayerDidStop(EZCastOverGoogleCast.this, cause);
			else
				mediaPlayerStateListener.mediaPlayerDidStop(EZCastOverGoogleCast.this, Cause.UNKNOWN);
			byUser = false;
		}
		if (trackableApi != null) {
			trackableApi.commitMediaUsageTracking();
		}
	}
	private void finishPendingTask(Runnable pendingTask) {
		synchronized (pendingTasks) {
			pendingTasks.remove(pendingTask);
			Log.d(TAG, "finishPendingTask pendingTasks left:"+pendingTasks.size());
			if (pendingTasks.size() > 0) {
				pendingTasks.get(0).run();
			}
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

	public void onReceivedData(byte[] videoData) {
		if (videoData[4] == 0x67) {
			try {
				byte[] sps = new byte[videoData.length-4];
				System.arraycopy(videoData, 4, sps, 0, videoData.length - 4);
				mPlayer.initial(sps);
				mPresentation.hideImage();
			} catch (Exception e) {
				Log.e(TAG, "init fail", e);
				return;
			}
		}

		if (mPlayer != null)
			mPlayer.enqueVideoData(videoData);

	}
}

