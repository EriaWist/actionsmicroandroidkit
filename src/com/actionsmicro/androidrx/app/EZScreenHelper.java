package com.actionsmicro.androidrx.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionsmicro.BuildConfig;
import com.actionsmicro.airplay.AirPlayServer;
import com.actionsmicro.airplay.clock.PlaybackClock;
import com.actionsmicro.airplay.clock.SimplePlaybackClock;
import com.actionsmicro.airplay.mirror.MirrorClock;
import com.actionsmicro.androidrx.EzScreenServer;
import com.actionsmicro.androidrx.app.MediaPlayerHelper.PlayerListener;
import com.actionsmicro.androidrx.app.state.IdleState;
import com.actionsmicro.androidrx.app.state.StateContext;
import com.actionsmicro.ezcom.jsonrpc.JSONRPC2Session;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;
import com.actionsmicro.web.SimpleMotionJpegOverHttpClient;
import com.actionsmicro.web.SimpleMotionJpegOverHttpClient.ConnectionCallback;
import com.actionsmicro.web.SimpleMotionJpegOverHttpClient.JpegCallback;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.yutel.silver.vo.AirplayState;

public class EZScreenHelper implements PlayerListener {
	public interface ConnectionListener {
		public void onConnected();
		public void onDisconnected();
	}
	public interface DisplayImageInterface {
		public String getOnConnectedDisplayImage();
		public String getOnStopDisplayImage();
	}
	public interface PlaybackDelegate {
		public void onError(int code);
	}
	public interface InitializationListener {

		void onInitializationFailed(int service, Exception e);

		void onInitalizationFinished();
		
	}
	private static final String TAG = "EZScreenHelper";
	private PlaybackDelegate playbackDelegate;
	private WebView webView;
	private TextureView mjpegView;
	private Handler mainHandler = new Handler(Looper.getMainLooper());
	private AirPlayServer airplayService;
	private MulticastLock lock;
	private float pendingStartingPosition = -1;
	private EzScreenServer ezScreenServer;
	private SimpleMotionJpegOverHttpClient mJpegClient;
	private int duration;
	private int state = AirplayState.STOPPED;
	private int currentTime;
	private boolean metadataLoaded;
	private AudioManager audio;
	private Context context;
	private String serviceName;
	private ConnectionListener connectionListener;
	private DisplayImageInterface displayImageInterface;
	protected Surface mirrorSurface;
	protected int surfaceWidth;
	protected int surfaceHeight;
	private int servers;
	protected SurfaceTexture mirrorSurfaceTexture;
	private ViewGroup container;
	private TextureView mirrorView;
	private InitializationListener initializationListener;
	private AndroidRxSchemaServer androidRxSchemaServer;
	private ViewGroup musicView;
	private Tracker gaTracker;
	public Tracker getGaTracker() {
		return gaTracker;
	}
	public void setGaTracker(Tracker tracker) {
		this.gaTracker = tracker;
	}
	private String getServiceName() {
		return serviceName;
	}
	public static final int SERVER_EZSCREEN = 0x01<<0; 
	public static final int SERVER_AIRPLAY = 0x01<<1;
	private List<View> allViews = new ArrayList<View>();
	private ImageView photoView;
	public EZScreenHelper(Context context, String serviceName, ViewGroup frame, WebView webView, TextureView textureView, ViewGroup musicView, int servers) {
		this.context = context;
		this.musicView = musicView;
		this.webView = webView;
		this.mjpegView = textureView;
		this.serviceName = serviceName;
		updateServerBits(servers);
		this.container = frame;
		audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		addView(musicView);
		if (webView != null) {
			initWebView();
			addView(webView);
		}
		if (mjpegView != null) {
			initMjpegView();
			addView(mjpegView);
		}
		if (needToLoadAirPlay()) {
			initMirrorView();
			addView(mirrorView);
			photoView = new ImageView(context);
			container.addView(photoView);
			hidePhotoView();
			addView(photoView);
		}
		androidRxSchemaServer = new AndroidRxSchemaServer(context);
//		try {
//			androidRxSchemaServer.start();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
	}
	private void createNetworkThread() {
		if (networkThread == null) {
			networkThread = new LooperThread();
			networkThread.start();
		}
		try {
			synchronized(networkThread) {
				networkThread.wait();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void hidePhotoView() {
		setViewVisibility(photoView, View.INVISIBLE);
	}
	private void addView(View view) {
		if (view != null && !allViews.contains(view)) {
			allViews.add(view);
		}
	}
	private boolean needToLoadAirPlay() {
		return (servers & SERVER_AIRPLAY) != 0;
	}
	private boolean needToLoadEzScreen() {
		return (servers & SERVER_EZSCREEN) != 0;
	}

	public WebView getWebView() {
		return webView;
	}

	public TextureView getMjpegView() {
		return mjpegView;
	}

	public Handler getMainHandler() {
		return mainHandler;
	}

	public AirPlayServer getAirplayService() {
		return airplayService;
	}

	public void setAirplayService(AirPlayServer airplayService) {
		this.airplayService = airplayService;
	}

	public MulticastLock getLock() {
		return lock;
	}

	public void setLock(MulticastLock lock) {
		this.lock = lock;
	}

	public float getPendingStartingPosition() {
		return pendingStartingPosition;
	}

	public void setPendingStartingPosition(float pendingStartingPosition) {
		this.pendingStartingPosition = pendingStartingPosition;
	}

	public EzScreenServer getEzScreenServer() {
		return ezScreenServer;
	}

	public void setEzScreenServer(EzScreenServer ezScreenServer) {
		this.ezScreenServer = ezScreenServer;
	}

	public SimpleMotionJpegOverHttpClient getmJpegClient() {
		return mJpegClient;
	}

	public void setmJpegClient(SimpleMotionJpegOverHttpClient mJpegClient) {
		this.mJpegClient = mJpegClient;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(int currentTime) {
		this.currentTime = currentTime;
	}

	public boolean isMetadataLoaded() {
		return metadataLoaded;
	}

	public void setMetadataLoaded(boolean metadataLoaded) {
		this.metadataLoaded = metadataLoaded;
	}
	
	private void resetToStandby() {
		stopVideo();
		showWebView();
		displayUrl("images/standby.jpg");
		trackScreenHit("home");
	}
	private void stopVideo() {
		stopMediaPlayer();
		this.setState(AirplayState.STOPPED);		
		sendCallbackNotification("ezcastplayer.onended", null);
		resetPlaybackStates();
		if (this.getAirplayService() != null) {
			this.getAirplayService().sendEvent();
		}	
	}
	private void pauseVideo() {
		if (mediaPlayerHelper != null) {
			mediaPlayerHelper.pause();
		}
	}
	private void resumeVideo() {
		if (mediaPlayerHelper != null) {
			mediaPlayerHelper.resume();
		}
	}
	private void setVolume(float volume) {
		if (mediaPlayerHelper != null) {
			mediaPlayerHelper.setVolume(volume);
		}
	}
	private void seek(long time) {
		if (mediaPlayerHelper != null) {
			mediaPlayerHelper.seek((int)time*1000);
		}
	}

	private void invokeJavascript(final String javascript) {
		if (webView != null) {
			webView.post(new Runnable() {

				@Override
				public void run() {
					webView.loadUrl(javascript);
				}						
			});
		}
	}

	private void resetPlaybackStates() {
		this.setState(AirplayState.STOPPED);
		this.setDuration(0);
		this.setCurrentTime(0);
		this.setPendingStartingPosition(-1);
		this.setMetadataLoaded(false);
	}
	
	@JavascriptInterface
	public void onDurationChange(int seconds) {
		Log.d(TAG, "onDurationChange:"+seconds);
		Map<String, Object> namedParams = new HashMap<String, Object>();
		namedParams.put("duration", Float.valueOf(seconds));
		sendCallbackNotification("ezcastplayer.ondurationchange", namedParams);
		if (this.isMetadataLoaded()) {
			if (getAirplayService() != null) {
				this.getAirplayService().sendEvent();
			}
			this.setDuration(seconds);
			if (this.getPendingStartingPosition() != -1 && this.getPendingStartingPosition() != 0) {
				long seekTo = (long)(this.getDuration()*this.getPendingStartingPosition());
				seek(seekTo);
				Log.d(TAG, "seekTo:"+seekTo);
			}
		}
	}
	private void sendCallbackNotification(final String notificationName,
			final Map<String, Object> namedParams) {
		
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				try {
					if (mediaCallbackRpc != null) {
						JSONRPC2Notification notification = new JSONRPC2Notification(notificationName);
						if (namedParams != null) {
							notification.setNamedParams(namedParams);
						}
						mediaCallbackRpc.send(notification);
					}
				} catch (JSONRPC2SessionException e) {
					e.printStackTrace();
				} finally {

				}
			}
		};
			
		performOnNetworkThread(runnable);		
		
	}
	@JavascriptInterface
	public void onLoadStart() {
		Log.d(TAG, "onLoadStart:");
		sendCallbackNotification("ezcastplayer.onloadstart", null);
		
//		state = AirplayState.CACHING;
//		airplayService.sendEvent();
	}
	@JavascriptInterface
	public void onPlay() {
		Log.d(TAG, "onPlay:");
		sendCallbackNotification("ezcastplayer.onplay", null);
		this.setState(AirplayState.PLAYING);		
	}
	@JavascriptInterface
	public void onTimeUpdate(int currentTime) {
		Log.d(TAG, "onTimeUpdate:"+currentTime);
		Map<String, Object> namedParams = new HashMap<String, Object>();
		namedParams.put("time", Float.valueOf(currentTime));
		sendCallbackNotification("ezcastplayer.ontimeupdate", namedParams);
		this.setCurrentTime(currentTime);
	}
	@JavascriptInterface
	public void onError(int error) {
		Log.d(TAG, "onError:"+error);
		Map<String, Object> namedParams = new HashMap<String, Object>();
		namedParams.put("error", Float.valueOf(error));
		sendCallbackNotification("ezcastplayer.onerror", namedParams);
		this.setState(AirplayState.ERROR);
		if (getAirplayService() != null) {
			this.getAirplayService().sendEvent();
		}
		if (playbackDelegate != null) {
			playbackDelegate.onError(error);
		}
	}
	@JavascriptInterface
	public void onEnded() {
		Log.d(TAG, "onEnded:");
		stopMediaPlayer();
		this.setState(AirplayState.STOPPED);		
		sendCallbackNotification("ezcastplayer.onended", null);
		resetPlaybackStates();
		if (this.getAirplayService() != null) {
			this.getAirplayService().sendEvent();
		}
	}

	@JavascriptInterface
	public void onPaused() {
		Log.d(TAG, "onPaused:");
		sendCallbackNotification("ezcastplayer.onpause", null);
		this.setState(AirplayState.PAUSING);
		if (getAirplayService() != null) {
			this.getAirplayService().sendEvent();
		}
//		if (!initializing) {
//			if (state != AirplayState.STOPPED) {
//				state = AirplayState.PAUSING;
//				airplayService.sendEvent();
//			}
//		} else {
//			initializing = false;
//		}
	}
	@JavascriptInterface
	public void onWaiting() {
		Log.d(TAG, "onWaiting:");
		sendCallbackNotification("ezcastplayer.onwaiting", null);
		this.setState(AirplayState.CACHING);
		if (getAirplayService() != null) {
			this.getAirplayService().sendEvent();
		}
	}
	@JavascriptInterface
	public void onSeeked() {
		Log.d(TAG, "onSeeked:");
		sendCallbackNotification("ezcastplayer.onseeked", null);
		this.setPendingStartingPosition(-1);
//		resumeVideo();
//		state = AirplayState.PLAYING;
//		airplayService.sendEvent();
	}
	/**
	 * Fires when the audio/video is ready to play after having been paused or stopped for buffering
	 */
	@JavascriptInterface
	public void onPlaying() {
		Log.d(TAG, "onPlaying:");
		sendCallbackNotification("ezcastplayer.onplaying", null);

//		if (pendingStartingPosition == -1 || pendingStartingPosition == 0) {
//			state = AirplayState.PLAYING;
//			airplayService.sendEvent();
//		}
		this.setState(AirplayState.PLAYING);
		if (this.getAirplayService() != null) {
			this.getAirplayService().sendEvent();
		}
	}
	@JavascriptInterface
	public void onLoadedMetadata() {
		Log.d(TAG, "onLoadedMetadata:");
		sendCallbackNotification("ezcastplayer.onloadedmetadata", null);
		this.setState(AirplayState.CACHING);
		this.setMetadataLoaded(true);		
	}

	private void hideMjpegView() {
		final int invisible = View.INVISIBLE;
		if (mjpegView != null) {
			setViewVisibility(mjpegView, invisible);			
		}
		if (webView != null) {
			setViewVisibility(webView, View.VISIBLE);
		}
	}

	private void setViewVisibility(final View view, final int visibility) {
		if (view != null) {
			
			this.getMainHandler().post(new Runnable() {

				@Override
				public void run() {
					if (view == mjpegView && 
							visibility != View.VISIBLE && 
							mjpegView.getVisibility() != visibility) {
						clearMjpegView();
					}
					view.setVisibility(visibility);				
				}

			});
		}
	}
	private void clearMjpegView() {
		Log.d(TAG, "clearMjpegView");
		Canvas canvas = mjpegView.lockCanvas();
		if (canvas != null) {
			canvas.drawARGB(0xff, 0, 0, 0);
			mjpegView.unlockCanvasAndPost(canvas);
			Log.d(TAG, "clearMjpegView done");
		}
	}

	private void stopDisplay(String stopImage) {
		stopMJpegClient();
		showWebView();
		displayUrl(stopImage);
	}
	private void cleanUpServers() {
		if (this.getEzScreenServer() != null) {
			this.getEzScreenServer().stop();
		}
		if (this.getAirplayService() != null) {
			this.getAirplayService().stop();
			this.setAirplayService(null);
		}
	}
	private void displayUrl(final String url) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			invokeJavascript("javascript:updateDisplay(\""+url+"\");");
			showWebView();
		} else {
			if (url.startsWith("http")) {
				displayMotionJpeg(url);
			} else {
				invokeJavascript("javascript:updateDisplay(\""+url+"\");");
				showWebView();
			}
		}
	}

	private void showWebView() {
		if (webView != null) {
			setViewVisibility(webView, View.VISIBLE);
			hideAllViewsExcept(webView);
		}
	}

	private void stopMediaPlayer() {
		if (mediaPlayerHelper != null) {
			mediaPlayerHelper.stop();
			mediaPlayerHelper = null;
		}
	}
	private void displayMotionJpeg(final String url) {
		stopMJpegClient();
		showMjpegView();
		try {
			this.setmJpegClient(new SimpleMotionJpegOverHttpClient(url, new JpegCallback() {

				@Override
				public void onJpegAvaiable(byte[] jpegData, int size) {
					if (EZScreenHelper.this.getMjpegView() != null && jpegData != null) {
						Bitmap bitmap = null;
						try {
							bitmap = BitmapFactory.decodeByteArray(jpegData, 0, size);							
						} catch (OutOfMemoryError oom) {
							try {
								Log.e(TAG, "OOM: jpeg size:"+size);
								BitmapFactory.Options options = new BitmapFactory.Options(); 
								options.inSampleSize = 2; 
								bitmap = BitmapFactory.decodeByteArray(jpegData, 0, size, options);
							} catch (Throwable t) {
								t.printStackTrace();
							}
						}
						if (bitmap != null) {
							Canvas canvas = EZScreenHelper.this.getMjpegView().lockCanvas();
							if (canvas != null) {
								final int savedState = canvas.save();
								try {
									Log.d(TAG, "canvas width:"+canvas.getWidth()+", height:"+canvas.getHeight()+ "; bitmap width:"+bitmap.getWidth()+", height:"+bitmap.getHeight());
									final float scaleFactor = Math.min( (float)canvas.getWidth() / (float)bitmap.getWidth(), (float)canvas.getHeight() / (float)bitmap.getHeight() );
									final float finalWidth = (float)bitmap.getWidth() * scaleFactor;
									final float finalHeight = (float)bitmap.getHeight() * scaleFactor;
									final float leftPadding = ((float)canvas.getWidth() - finalWidth)/2;
									final float topPadding =  ((float)canvas.getHeight() - finalHeight)/2;
									canvas.drawColor(Color.BLACK);
									canvas.translate(leftPadding, topPadding);
									canvas.scale(scaleFactor, scaleFactor);
									canvas.drawBitmap(bitmap, 0, 0, null);
								} finally {
									canvas.restoreToCount(savedState);
									EZScreenHelper.this.getMjpegView().unlockCanvasAndPost(canvas);
								}
							}
						}
					}
				}

			}, new ConnectionCallback() {

				@Override
				public void onConnectionFailed(IOException e) {
					Log.e(TAG, "onConnectionFailed", e);
				}

				@Override
				public void onDisconnected() {
					Log.d(TAG, "onDisconnected");
				}

			}));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void showMjpegView() {
		if (mjpegView != null) {
			setViewVisibility(mjpegView, View.VISIBLE);
			hideAllViewsExcept(mjpegView);
		}
	}

	private void stopMJpegClient() {
		if (this.getmJpegClient() != null) {
			this.getmJpegClient().stop();
			this.setmJpegClient(null);
		}
	}
	private String getIpAddress() {
		WifiManager wim= (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		return Formatter.formatIpAddress(wim.getConnectionInfo().getIpAddress());
	}
	private void playVideo(final String url, String callback) {
		playVideo(url, callback, true, 0);
	}
	private void initEzAndroidRx() {
		try {
			String deviceID;
			deviceID = AndroidRxSchemaServer.getUUID(context);
			
			this.setEzScreenServer(new EzScreenServer(context, InetAddress.getByName(getIpAddress()), getServiceName(), deviceID, new EzScreenServer.EzScreenServerDelegate() {

				@Override
				public void stopVideo() {
					EZScreenHelper.this.stopVideo();
				}

				@Override
				public void stopDisplay() {
					String stopImage = "images/connected.jpg";
					if (displayImageInterface != null) {
						stopImage = displayImageInterface.getOnStopDisplayImage();
					}
					EZScreenHelper.this.stopDisplay(stopImage);
				}

				@Override
				public void seek(int time) {
					EZScreenHelper.this.seek(time);
				}

				@Override
				public void resumeVideo() {
					EZScreenHelper.this.resumeVideo();
				}

				@Override
				public void playVideo(String url, String callback) {
					stateContext.onLoadEzScreenVideo(url, callback);
				}

				@Override
				public void pauseVideo() {
					EZScreenHelper.this.pauseVideo();
				}

				@Override
				public void onDisconnected() {
					stateContext.onEzScreenClientDisconnected();
					androidRxSchemaServer.stopFunction();
				}

				@Override
				public void onConnected() {
					stateContext.onEzScreenClientConnected();
					androidRxSchemaServer.startFunction(AndroidRxSchemaServer.RxFunction.EZCAST);
				}
				
				@Override
				public void displayUrl(String url) {
					trackScreenHit("ezcastrx.mirror");
					stateContext.onDisplayUrl(url);
				}
				
				@Override
				public void increaseVolume() {
					audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
							AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
				}

				@Override
				public void decreaseVolume() {
					audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
			                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
				}

				@Override
				public void onInitializationStart() {
					ezScreenInitialized = false;
				}

				@Override
				public void onInitializationFinished() {
					ezScreenInitialized = true;
					informInitializationListenerOnFinishedIfNeeded();
				}

				@Override
				public void onInitializationFailed(Exception e) {
					if (initializationListener != null) {
						initializationListener.onInitializationFailed(SERVER_EZSCREEN, e);
					}
				}
			}));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.getEzScreenServer().start();
	}
	private MediaCodec decoder;
	private Thread renderThread;
	private boolean stopRenderer = false;
	protected boolean airplayInitialized;
	private boolean ezScreenInitialized;
	protected boolean alreadyFailed;
	private StateContext stateContext;
	private MediaPlayerHelper mediaPlayerHelper;
	private JSONRPC2Session mediaCallbackRpc;
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void initAirplay() {
		try {
			this.setAirplayService(new AirPlayServer(context, InetAddress.getByName(getIpAddress()), getServiceName(), new AirPlayServer.AirPlayServerDelegate() {
				final static String TAG = EZScreenHelper.TAG+".AirPlayServer";
				@Override
				public void stopVideo() {
					Log.d(TAG, "stopVideo");
					stateContext.onStopAirPlayVideo();
					androidRxSchemaServer.stopFunction();
				}
				
				@Override
				public void seek(int position) {
					Log.d(TAG, "seek:"+position);
					EZScreenHelper.this.seek(position);
				}
				
				@Override
				public void resumeVideo() {
					Log.d(TAG, "resumeVideo");
					EZScreenHelper.this.resumeVideo();
//					while (EZScreenHelper.this.getState() != AirplayState.PLAYING) {
//						try {
//							Thread.sleep(500);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					}
				}
				
				@Override
				public void pauseVideo() {
					Log.d(TAG, "pauseVideo");
					EZScreenHelper.this.pauseVideo();
				}
				
				@Override
				public void loadVideo(String url, float rate, float position) {
					Log.d(TAG, "loadVideo:"+url);
					Log.d(TAG, "loadVideo.rate:"+rate);
					Log.d(TAG, "loadVideo.position:"+position);
					try {
						Uri uri = Uri.parse(url);
						trackScreenHit("airplay.video", 1, uri.getHost());
					} catch (Exception e) {
						trackScreenHit("airplay.video");						
					}
					stateContext.onLoadAirPlayVideo(url, rate, position);
					androidRxSchemaServer.startFunction(AndroidRxSchemaServer.RxFunction.EZAIR);
				}
				
				@Override
				public int getVideoStatus() {
					Log.d(TAG, "getVideoStatus:"+EZScreenHelper.this.getState());
					return EZScreenHelper.this.getState();
				}
				
				@Override
				public int getVideoPosition() {
					Log.d(TAG, "getVideoPosition:"+EZScreenHelper.this.getCurrentTime());
					return EZScreenHelper.this.getCurrentTime();
				}
				
				@Override
				public int getVideoDuration() {
					Log.d(TAG, "getVideoDuration:"+EZScreenHelper.this.getDuration());
					return EZScreenHelper.this.getDuration();
				}
				@Override
				public void onStartMirroring(InetAddress remoteAddress) {
					Log.d(TAG, "onStartMirroring");
					trackScreenHit("airplay.mirror");
					stateContext.onStartMirroring(remoteAddress);
					androidRxSchemaServer.startFunction(AndroidRxSchemaServer.RxFunction.EZAIR_MIRROR);
				}

				private void startRenderer() {
					stopRenderer = false;
					renderThread = new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								MediaCodec.BufferInfo bufferInfo= new MediaCodec.BufferInfo();
								while (!stopRenderer) {
									doRender(bufferInfo);
								}
							} catch(Exception e) {
								e.printStackTrace();
								// TODO exception handling
							} finally {
								stopRenderer = true;
							}
						}
						
					});
					renderThread.setName("AirPlay Mirror Decoder");
					renderThread.start();
				}

				

				@Override
				public void onSpsAvailable(byte[] sps) {
					decodeBytesWithPrefix(nalHeader, sps, 0, sps.length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG, -1);
					if (testFile != null) {
						try {
							testFile.write(nalHeader);
							testFile.write(sps);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {

						}
					}					
				}

				private void decodeBytesWithPrefix(byte[] prefix, byte[] data, int offset, int length, long timestamp, int flags, long timeoutUs) {
					if (decoder != null && length > 0) {
						int bufferIndex = -1;
						bufferIndex = decoder.dequeueInputBuffer(timeoutUs);
						if (bufferIndex != -1) {
							Log.d(TAG, "dequeueInputBuffer ("+prefix+"):"+bufferIndex);
							inputBuffers[bufferIndex].clear();
							if (prefix != null) {
								inputBuffers[bufferIndex].put(prefix);
							}
							inputBuffers[bufferIndex].put(data, offset, length);
							int totalWrite = inputBuffers[bufferIndex].position();
							inputBuffers[bufferIndex].rewind();
							decoder.queueInputBuffer(bufferIndex, 0, totalWrite, timestamp, flags);							
						} else {
							Log.w(TAG, "MediaCodec input buffer is not enough.");
						}
					}
				}

				@Override
				public void onPpsAvailable(byte[] pps) {
					decodeBytesWithPrefix(nalHeader, pps, 0, pps.length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG, -1);
					if (testFile != null) {
						try {
							testFile.write(nalHeader);
							testFile.write(pps);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {

						}
					}				
				}				
				@Override
				public void onH264FrameAvailable(byte[] frame, int offset, int size, long timestamp) {
					if (renderThread == null) {
						startRenderer();					
					}
					if (playbackClock == null) {
						playbackClock = new SimplePlaybackClock(timestamp, 1000, TAG);
					}
					decodeBytesWithPrefix(null, frame, offset, size, timestamp, 0, 5000);
					if (testFile != null) {
						try {
							testFile.write(frame, offset, size);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				@Override
				public void onStopMirroring() {
					Log.d(TAG, "onStopMirroring");
					stateContext.onStopMirroring();
					androidRxSchemaServer.stopFunction();
				}

				private void doRender(MediaCodec.BufferInfo bufferInfo) {
					int outputBufferIndex = -1;
					try {
						outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 500000);
					} catch(Exception e) {
						Log.e(TAG, "dequeueOutputBuffer:"+e.getClass());
						stopRenderer = true;
					} finally {
					
					}
					if (outputBufferIndex >= 0) {		
						boolean shouldRender = playbackClock.waitUntilTime(bufferInfo.presentationTimeUs);
						decoder.releaseOutputBuffer(outputBufferIndex, shouldRender);
					} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
						decoder.getOutputBuffers();
					} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
						MediaFormat outputFormat = decoder.getOutputFormat();
						int width = outputFormat.getInteger(MediaFormat.KEY_WIDTH);
						int height = outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
						Log.d(TAG, "outputFormat width:"+width+", height:"+height);
						final Matrix transform = new Matrix();
						transform.setRectToRect(new RectF(0, 0, width, height), new RectF(0, 0, surfaceWidth, surfaceHeight) , Matrix.ScaleToFit.CENTER);
						transform.preScale((float)width/(float)surfaceWidth, (float)height/(float)surfaceHeight);
						mainHandler.post(new Runnable() {

							@Override
							public void run() {
								mirrorView.setTransform(transform);											
							}

						});
					}
				}

				@Override
				public void onInitalizationStart() {
				}

				@Override
				public void onInitalizationFinished() {
					airplayInitialized = true;
					informInitializationListenerOnFinishedIfNeeded();
				}

				@Override
				public void onInitalizationFailed(Exception e) {
					if (!alreadyFailed && initializationListener != null) {
						initializationListener.onInitializationFailed(SERVER_AIRPLAY, e);
					}
					alreadyFailed = true;
				}

				@Override
				public void onStartAirTunes(InetAddress inetAddress) {
					stateContext.onStartAirTunes(inetAddress);
					androidRxSchemaServer.startFunction(AndroidRxSchemaServer.RxFunction.EZAIR);
				}

				@Override
				public void onStopAirTunes() {
					stateContext.onStopAirTunes();
					androidRxSchemaServer.stopFunction();
				}

				@Override
				public void onReceiveAirTunesMetadata(String albumName,
						String artist, String title) {
					trackScreenHit("airplay.music");
					stateContext.onReceiveAirTunesMetadata(albumName, artist, title);
				}

				@Override
				public void onReceiveAirTunesCoverArt(byte[] byteArray) {
					stateContext.onReceiveAirTunesCoverArt(byteArray);
				}

				@Override
				public void onAirPlayStart() {
					Log.d(TAG, "onAirPlayStart");
					stateContext.onAirPlayStart();
				}

				@Override
				public void onAirPlayStop() {
					Log.d(TAG, "onAirPlayStop");
					stateContext.onAirPlayStop();					
				}

				@Override
				public void setVolume(float volume) {
					EZScreenHelper.this.setVolume(volume);
				}

				@Override
				public void displayPhoto(byte[] jpeg, String assetKey, String transition) {
					Log.d(TAG, "displayPhoto:"+assetKey+" size:"+ jpeg.length + " with:"+transition);
					trackScreenHit("airplay.photo");
					stateContext.onDisplayPhoto(jpeg, assetKey, transition);
				}

				@Override
				public boolean displayCached(String assetKey, String transition) {
					Log.d(TAG, "displayCached:"+ assetKey + " with:"+transition);
					trackScreenHit("airplay.photo");
					return stateContext.onDisplayCached(assetKey, transition);
				}

				@Override
				public void cachePhoto(String assetKey, byte[] jpeg) {
					Log.d(TAG, "cachePhoto:" + assetKey + " size:"+ jpeg.length);
					stateContext.onCachePhoto(assetKey, jpeg);
				}
				
			}));
			alreadyFailed = false;
			this.getAirplayService().start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

	protected void updateAirTunesCoverArt(final byte[] byteArray) {
		if (musicView != null) {
			mainHandler.post(new Runnable() {

				@Override
				public void run() {
					int id = context.getResources().getIdentifier("cover", "id", context.getPackageName());
					ImageView imageView = (ImageView) musicView.findViewById(id);
					if (imageView != null) {
						if (byteArray != null) {
							imageView.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
						} else {
							// TODO we need a fallback image
							imageView.setImageResource(context.getResources().getIdentifier("ic_launcher", "drawable", context.getPackageName()));
						}
					}
				}

			});
		}
	}

	protected void showMusicView() {
		if (musicView != null) {
			setViewVisibility(musicView, View.VISIBLE);
			hideAllViewsExcept(musicView);
		}
	}

	protected void updateAirTunesMetadata(final String albumName, final String artist,
			final String title) {
		if (musicView != null) {
			mainHandler.post(new Runnable() {

				@Override
				public void run() {
					updateUiTextById(title, "songTitle");
					updateUiTextById(artist, "artist");
					updateUiTextById(albumName, "album");
				}

			});
		}
	}

	protected void hideMusicView() {
		setViewVisibility(musicView, View.INVISIBLE);
	}

	protected void informInitializationListenerOnFinishedIfNeeded() {
		if ((airplayInitialized || !needToLoadAirPlay()) && 
				(ezScreenInitialized || !needToLoadEzScreen())) {
			if (initializationListener != null) {
				initializationListener.onInitalizationFinished();
			}
		}
		
	}

	private void playVideo(String url, String callback, boolean autoplay, int startpos) {
		closeMediaCallbackRpcSessionIfNeeded();
		createMediaCallbackRpcSession(callback);
		stopMediaPlayer();
		mediaPlayerHelper = new MediaPlayerHelper(context, container, this);
		mediaPlayerHelper.load(url);
		if (autoplay) {
			mediaPlayerHelper.play(startpos);
		}
	}
	private void createMediaCallbackRpcSession(String callback) {
		if (callback != null) {
			try {
				mediaCallbackRpc = new JSONRPC2Session(new URL(callback));
				
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	@SuppressLint({ "NewApi", "SetJavaScriptEnabled" })
	private void initWebView() {
		this.getWebView().addJavascriptInterface(this, "appCallbacks");
		if (BuildConfig.DEBUG) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				WebView.setWebContentsDebuggingEnabled(true);
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			this.getWebView().getSettings().setMediaPlaybackRequiresUserGesture(false);			
		}
		this.getWebView().getSettings().setJavaScriptEnabled(true);
		this.getWebView().setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage (ConsoleMessage consoleMessage) {
				Log.d(TAG, "webview:"+consoleMessage.message());
				return false;
			}
		});
		this.getWebView().setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedError (WebView view, int errorCode, String description, String failingUrl) {
				Log.d(TAG, "onReceivedError:"+errorCode+", description:"+description+", url:"+failingUrl);
			}
		});
		this.getWebView().loadUrl("file:///android_asset/display/ezcast.html");
	}
	private void initMirrorView() {
		mirrorView = new TextureView(context);
		container.addView(mirrorView);
		hideMirrorView();
		mirrorView.setSurfaceTextureListener(new SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
					int width, int height) {
				Log.d(TAG, "onSurfaceTextureAvailable:"+" w:"+width+" h:"+height);
				EZScreenHelper.this.mirrorSurfaceTexture = surfaceTexture;  
				surfaceWidth = width;
				surfaceHeight = height;
			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				Log.d(TAG, "onSurfaceTextureDestroyed:");
				return true;
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
					int width, int height) {
				Log.d(TAG, "onSurfaceTextureSizeChanged:"+" w:"+width+" h:"+height);				
				surfaceWidth = width;
				surfaceHeight = height;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//				Log.d(TAG, "onSurfaceTextureUpdated:");
				
			}
			
		});
	}
	private void hideAllViewsExcept(View exception) {
		stopMediaPlayer();
		for (View view : allViews) {
			if (view != exception) {
				setViewVisibility(view, View.INVISIBLE);
			}
		}
	}
	private void hideMirrorView() {
		if (mirrorView != null) {
			setViewVisibility(mirrorView, View.INVISIBLE);
		}
		if (webView != null) {
			setViewVisibility(webView, View.VISIBLE);
		}
	}
	private void showMirrorView() {
		if (mirrorView != null) {
			setViewVisibility(mirrorView, View.VISIBLE);
			hideAllViewsExcept(mirrorView);
		}
	}

	private void initMjpegView() {
		hideMjpegView();
		this.getMjpegView().setSurfaceTextureListener(new SurfaceTextureListener() {

			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
					int width, int height) {
				Log.d(TAG, "onSurfaceTextureAvailable:"+" w:"+width+" h:"+height);
			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				Log.d(TAG, "onSurfaceTextureDestroyed:");
				stopMJpegClient();
				return true;
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
					int width, int height) {
				Log.d(TAG, "onSurfaceTextureSizeChanged:"+" w:"+width+" h:"+height);				
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
				Log.d(TAG, "onSurfaceTextureUpdated:");
				
			}
			
		});
	}
	public void start(int servers) {
		updateServerBits(servers);
		start();
	}
	private void updateServerBits(int servers) {
		this.servers = servers;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			this.servers &= ~SERVER_AIRPLAY;
		}
	}
	public void start() {
		stateContext = new StateContext(new IdleState()) {
			@Override
			protected void showConnectedIndicator() {
				EZScreenHelper.this.showConnectedIndicator();
			}

			@Override
			protected void informDelegateConnected() {
				EZScreenHelper.this.informDelegateConnected();
			}

			@Override
			protected void resetToStandby() {
				EZScreenHelper.this.resetToStandby();
			}

			@Override
			protected void informDelegateDisconnected() {
				EZScreenHelper.this.informDelegateDisconnected();
			}

			@Override
			protected void loadVideo(String url, float rate, float position) {
				EZScreenHelper.this.loadAirPlayVideo(url, rate, position);
			}

			@Override
			protected void showMusicView() {
				EZScreenHelper.this.showMusicView();
			}

			@Override
			protected void hideMusicView() {
				EZScreenHelper.this.hideMusicView();
			}

			@Override
			protected void updateAirTunesMetadata(String albumName,
					String artist, String title) {
				EZScreenHelper.this.updateAirTunesMetadata(albumName, artist, title);
			}

			@Override
			protected void stopVideo() {
				EZScreenHelper.this.stopVideo();
			}

			@Override
			protected void updateAirTunesCoverArt(byte[] byteArray) {
				EZScreenHelper.this.updateAirTunesCoverArt(byteArray);
			}

			@Override
			protected void doAirPlayMirror(InetAddress remoteAddress) {
				EZScreenHelper.this.doAirPlayMirror(remoteAddress);
			}

			@Override
			protected void stopMirror() {
				EZScreenHelper.this.stopMirror();
			}

			@Override
			protected void stopMusic() {
				// TODO force to disconnect from client
				EZScreenHelper.this.hideMusicView();
			}

			@Override
			protected void showMirrorView() {
				EZScreenHelper.this.showMirrorView();
			}

			@Override
			protected void displayPhoto(final byte[] jpeg, String assetKey, final String transition) {
				showPhotoView();
				cacheImage(assetKey, jpeg);
				displayCached(assetKey, transition);
			}

			@Override
			protected void cacheImage(String assetKey, byte[] jpeg) {
				try {
					final File cacheFile = new File(getAirPlayCacheDir(), assetKey+".jpg");
					FileOutputStream cacheFileOutput = new FileOutputStream(cacheFile);
					cacheFileOutput.write(jpeg);
					cacheFileOutput.close();					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}

			@Override
			protected boolean displayCached(String assetKey, String transition) {
				showPhotoView();
				final File cacheFile = new File(getAirPlayCacheDir(), assetKey+".jpg");
				if (cacheFile.exists()) {
					mainHandler.post(new Runnable() {

						@Override
						public void run() {
							Uri cacheFileUri = Uri.fromFile(cacheFile);
							try {
								photoView.setImageURI(cacheFileUri);
							} catch (OutOfMemoryError oom) {
								try {
									photoView.setImageBitmap(readBitmapInHalfSize(cacheFileUri, context));
								} catch (Throwable t) {
									t.printStackTrace();
								}
							}
						}

					});
					return true;
				} else {
					return false;
				}
			}

			@Override
			protected void hidePhotoView() {
				EZScreenHelper.this.hidePhotoView();
			}

			@Override
			protected void displayUrl(String url) {
				EZScreenHelper.this.displayUrl(url);
				
			}

			@Override
			protected void loadEzScreenVideo(String url, String callback) {
				try {
					Uri uri = Uri.parse(url);
					trackScreenHit("ezcastrx.media", 1, uri.getHost());
				} catch (Exception e) {
					trackScreenHit("ezcastrx.media");						
				}
				EZScreenHelper.this.hideAllViewsExcept(null);
				EZScreenHelper.this.playVideo(url, callback);				
			}

			@Override
			protected void hideMirrorView() {
				EZScreenHelper.this.hideMirrorView();				
			}
		};
		android.net.wifi.WifiManager wifi =
				(android.net.wifi.WifiManager)
				context.getSystemService(android.content.Context.WIFI_SERVICE);
		setLock(wifi.createMulticastLock("EzDnssdLock"));
		getLock().setReferenceCounted(true);
		getLock().acquire();
		if ((servers & SERVER_EZSCREEN) != 0) {
			initEzAndroidRx();
		}
		if (((servers & SERVER_AIRPLAY) != 0) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					initAirplay();			
				}

			}).start();
		}
		createNetworkThread();
	}
	private static Bitmap readBitmapInHalfSize(Uri imageFile, Context context) { 
		Bitmap bitmap = null; 
		InputStream stream = null;
        try {
            stream = context.getContentResolver().openInputStream(imageFile);
            BitmapFactory.Options options = new BitmapFactory.Options(); 
    		options.inSampleSize = 2; 
			bitmap = BitmapFactory.decodeStream(stream, null, options);					
        } catch (Exception e) {
            Log.e(TAG, "Unable to open content: " + imageFile, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
		return bitmap; 
	}
	public void stop() {
		stopMirrorDecoding();
		stopMJpegClient();
		resetToStandby();
		cleanUpServers();
		if (this.getLock() != null) {
			this.getLock().release();
			this.setLock(null);
		}
//		androidRxSchemaServer.stop();
		ezScreenInitialized = false;
		airplayInitialized = false;
		cleanUpAirPlayCache();
		
		stopNetworkThread();
		closeMediaCallbackRpcSessionIfNeeded();
		releasePlaybackClock();
	}
	private void stopNetworkThread() {
		if (networkThread != null) {
			networkThread.stopLooper();
			try {
				networkThread.join(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			networkThread = null;
		}
	}
	private void closeMediaCallbackRpcSessionIfNeeded() {
		if (mediaCallbackRpc != null) {
			mediaCallbackRpc.close();
			mediaCallbackRpc = null;
		}
	}

	private void cleanUpAirPlayCache() {
		Utils.deleteFolder(getAirPlayCacheDir());
	}
	public ConnectionListener getConnectionListener() {
		return connectionListener;
	}

	public void setConnectionListener(ConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
	}
	public void setDisplayImageInterface(DisplayImageInterface displayImageI) {
		this.displayImageInterface = displayImageI;
	}
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void stopMirrorDecoding() {
		if (renderThread != null) {
			stopRenderer = true;
			renderThread.interrupt();
			try {
				renderThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			renderThread = null;
		}
		if (decoder != null) {
			decoder.stop();
			decoder.release();
			decoder = null;
		}
		if (mirrorSurface != null) {
			mirrorSurface.release();
			mirrorSurface = null;
		}
	}

	public InitializationListener getInitializationListener() {
		return initializationListener;
	}

	public void setInitializationListener(InitializationListener initializationListener) {
		this.initializationListener = initializationListener;
	}

	private void updateUiTextById(String text, String idName) {
		int id = context.getResources().getIdentifier(idName, "id", context.getPackageName());
		TextView textView = (TextView) musicView.findViewById(id);
		if (textView != null) {
			textView.setText(text);
		}
	}

	private void showConnectedIndicator() {
		String connectedImage = "images/connected.jpg";
		if (displayImageInterface != null) {
			connectedImage = displayImageInterface.getOnConnectedDisplayImage();
		}
		EZScreenHelper.this.displayUrl(connectedImage);
	}

	private void informDelegateConnected() {
		if (connectionListener != null) {
			connectionListener.onConnected();
		}
	}

	private void informDelegateDisconnected() {
		if (connectionListener != null) {
			connectionListener.onDisconnected();
		}
	}

	private void loadAirPlayVideo(String url, float rate, float position) {
		resetPlaybackStates();
		EZScreenHelper.this.setPendingStartingPosition(position);
		playVideo(url, null, true/*rate!=0*/, 0);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void doAirPlayMirror(InetAddress remoteAddress) {
		showMirrorView();
		stopMirrorDecoding();
		releasePlaybackClock();
		try {
			playbackClock = new MirrorClock(remoteAddress, 7010, 100, 25);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		
		decoder = MediaCodec.createDecoderByType(MIME_VIDEO_AVC);
		if (mirrorSurface == null) {
			while (mirrorSurfaceTexture == null) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mirrorSurface = new Surface(mirrorSurfaceTexture);
		}
		decoder.configure(MediaFormat.createVideoFormat(MIME_VIDEO_AVC, 1920, 1080), mirrorSurface, null, 0);
		decoder.start();
		inputBuffers = decoder.getInputBuffers();
		
		closeTestFile();
		if (DUMP_H264) {
			try {
				testFile = new FileOutputStream("/sdcard/test"+".h264");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private FileOutputStream testFile;
	private static final byte[] nalHeader = {0x00, 0x00, 0x00, 0x01};
	private static final boolean DUMP_H264 = false;
	private static final String MIME_VIDEO_AVC = "video/avc";
	private ByteBuffer[] inputBuffers;
	private PlaybackClock playbackClock;
	
	private void closeTestFile() {
		if (testFile != null) {
			try {
				testFile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			testFile = null;
		}
	}

	private void stopMirror() {
		// TODO to disconnect from client as well
		stopMirrorDecoding();
		inputBuffers = null; 
		closeTestFile();
		hideMirrorView();
		releasePlaybackClock();
	}
	private void releasePlaybackClock() {
		if (playbackClock != null) {
			playbackClock.release();
			playbackClock = null;
		}
	}

	public PlaybackDelegate getPlaybackDelegate() {
		return playbackDelegate;
	}

	public void setPlaybackDelegate(PlaybackDelegate playbackDelegate) {
		this.playbackDelegate = playbackDelegate;
	}
	
	private void trackScreenHit(String screenName) {
		if (gaTracker != null) {
			gaTracker.setScreenName(screenName);
			gaTracker.send(new HitBuilders.AppViewBuilder().build());
		}
	}
	private void trackScreenHit(String screenName, int customDimensionIndex, String dimension) {
		if (gaTracker != null) {
			gaTracker.setScreenName(screenName);
			gaTracker.send(new HitBuilders.AppViewBuilder().setCustomDimension(customDimensionIndex, dimension).build());
		}
	}
	protected void showPhotoView() {
		if (photoView != null) {
			setViewVisibility(photoView, View.VISIBLE);
			hideAllViewsExcept(photoView);
		}
	}
	private File getAirPlayCacheDir() {
		File airPlayCacheDir = new File(context.getCacheDir(), "AirPlay");
		if (!airPlayCacheDir.exists()) {
			airPlayCacheDir.mkdirs();
		}
		return airPlayCacheDir;
	}
	private void performOnNetworkThread(Runnable runnable) {
		Handler handler = getNetworkHandler();
		if (handler != null) {
			handler.post(runnable);			
		}
	}
	private LooperThread networkThread;
	protected Handler getNetworkHandler() {
		if (networkThread != null) {
			return networkThread.getHandler();
		}
		return null;
	}
	private class LooperThread extends Thread {
		private Looper myLooper;
		private Handler handler;
		protected Handler getHandler() {
			return handler;
		}
		@Override
		public void run() {
			Looper.prepare();
			myLooper = Looper.myLooper();
			handler = new Handler();
			synchronized(this) {
				this.notifyAll();
			}
			Looper.loop();
		}
		public void stopLooper() {
			if (myLooper != null) {
				myLooper.quit();
			}
		}
	}
}