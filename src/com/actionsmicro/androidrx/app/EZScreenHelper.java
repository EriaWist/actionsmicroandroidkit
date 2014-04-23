package com.actionsmicro.androidrx.app;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionsmicro.BuildConfig;
import com.actionsmicro.airplay.AirPlayServer;
import com.actionsmicro.androidrx.EzScreenServer;
import com.actionsmicro.web.SimpleMotionJpegOverHttpClient;
import com.actionsmicro.web.SimpleMotionJpegOverHttpClient.ConnectionCallback;
import com.actionsmicro.web.SimpleMotionJpegOverHttpClient.JpegCallback;
import com.yutel.silver.vo.AirplayState;

public class EZScreenHelper {
	protected static final String TAG = "EZScreenHelper";
	private WebView webView;
	private TextureView textureView;
	private Handler mainHandler = new Handler(Looper.getMainLooper());
	private Runnable resetToStandby = new Runnable() {

		@Override
		public void run() {
			resetToStandby();
		}
		
	};
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

	public String getServiceName() {
		return serviceName;
	}

	public EZScreenHelper(Context context, String serviceName, WebView webView, TextureView textureView) {
		this.context = context;
		this.webView = webView;
		this.textureView = textureView;
		this.serviceName = serviceName;
		audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		initWebView();
		initTextureView();
	}

	public WebView getWebView() {
		return webView;
	}

	public TextureView getTextureView() {
		return textureView;
	}

	public Handler getMainHandler() {
		return mainHandler;
	}


	public Runnable getResetToStandby() {
		return resetToStandby;
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
		hideTextureView();
		displayUrl("images/standby.jpg");
	}
	private void stopVideo() {
		invokeJavascript("javascript:stopVideo();");
	}
	private void pauseVideo() {
		invokeJavascript("javascript:pauseVideo();");
	}
	private void resumeVideo() {
		invokeJavascript("javascript:resumeVideo();");
	}
	private void setVolume(float volume) {
		invokeJavascript("javascript:setVolume("+volume+");");
	}
	private void seek(long time) {
		invokeJavascript("javascript:seek("+time+");");
	}
	
	
	private void invokeJavascript(final String javascript) {
		this.getWebView().post(new Runnable() {

			@Override
			public void run() {
				EZScreenHelper.this.getWebView().loadUrl(javascript);
			}						
		});
	}

	private void resetStates() {
		this.setState(AirplayState.STOPPED);
		this.setDuration(0);
		this.setCurrentTime(0);
		this.setPendingStartingPosition(-1);
		this.setMetadataLoaded(false);
	}
	
	@JavascriptInterface
	public void onDurationChange(int seconds) {
		Log.d(TAG, "onDurationChange:"+seconds);
		if (this.isMetadataLoaded()) {
			this.getAirplayService().sendEvent();

			this.setDuration(seconds);
			if (this.getPendingStartingPosition() != -1 && this.getPendingStartingPosition() != 0) {
				long seekTo = (long)(this.getDuration()*this.getPendingStartingPosition());
				seek(seekTo);
				Log.d(TAG, "seekTo:"+seekTo);
			}
		}
	}
	@JavascriptInterface
	public void onLoadStart() {
		Log.d(TAG, "onLoadStart:");
//		state = AirplayState.CACHING;
//		airplayService.sendEvent();
	}
	@JavascriptInterface
	public void onPlay() {
		Log.d(TAG, "onPlay:");
		
	}
	@JavascriptInterface
	public void onTimeUpdate(int currentTime) {
		Log.d(TAG, "onTimeUpdate:"+currentTime);
		this.setCurrentTime(currentTime);
	}
	@JavascriptInterface
	public void onError(int error) {
		Log.d(TAG, "onError:"+error);
		this.setState(AirplayState.ERROR);
		this.getAirplayService().sendEvent();
	}
	@JavascriptInterface
	public void onEnded() {
		Log.d(TAG, "onEnded:");
		resetStates();
		if (this.getAirplayService() != null) {
			this.getAirplayService().sendEvent();
		}
	}

	@JavascriptInterface
	public void onPaused() {
		Log.d(TAG, "onPaused:");
		this.setState(AirplayState.PAUSING);
		this.getAirplayService().sendEvent();
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
		this.setState(AirplayState.CACHING);
		this.getAirplayService().sendEvent();
	}
	@JavascriptInterface
	public void onSeeked() {
		Log.d(TAG, "onSeeked:");
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
//		if (pendingStartingPosition == -1 || pendingStartingPosition == 0) {
//			state = AirplayState.PLAYING;
//			airplayService.sendEvent();
//		}
		this.setState(AirplayState.PLAYING);
		this.getAirplayService().sendEvent();
	}
	@JavascriptInterface
	public void onLoadedMetadata() {
		Log.d(TAG, "onLoadedMetadata:");
		this.setState(AirplayState.CACHING);
		this.setMetadataLoaded(true);		
	}

	private void hideTextureView() {
		final int invisible = View.INVISIBLE;
		setTextureViewVisibility(invisible);
	}

	private void setTextureViewVisibility(final int visibility) {
		this.getMainHandler().post(new Runnable() {

			@Override
			public void run() {
				EZScreenHelper.this.getTextureView().setVisibility(visibility);
			}
			
		});
	}

	private void stopDisplay() {
		stopMJpegClient();
		hideTextureView();
		displayUrl("images/connected.jpg");
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
			invokeJavascript("javascript:updateDisplay('"+url+"');");
			hideTextureView();
		} else {
			if (url.startsWith("http")) {
				displayMotionJpeg(url);
			} else {
				invokeJavascript("javascript:updateDisplay('"+url+"');");
				hideTextureView();
			}
		}
	}

	private void displayMotionJpeg(final String url) {
		stopMJpegClient();
		showTextureView();
		try {
			this.setmJpegClient(new SimpleMotionJpegOverHttpClient(url, new JpegCallback() {

				@Override
				public void onJpegAvaiable(byte[] jpegData, int size) {
					if (EZScreenHelper.this.getTextureView() != null) {
						
						Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, size);
						if (bitmap != null) {
//									drawOnSurface(bitmap);
							Canvas canvas = EZScreenHelper.this.getTextureView().lockCanvas();
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
									EZScreenHelper.this.getTextureView().unlockCanvasAndPost(canvas);
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

	private void showTextureView() {
		setTextureViewVisibility(View.VISIBLE);
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
	protected void playVideo(final String url, String callback) {
		if (callback != null) {
			invokeJavascript("javascript:playVideo('"+url+"','"+callback+"');");
		} else {
			invokeJavascript("javascript:playVideo('"+url+"', null);");			
		}
		hideTextureView();
	}
	private void initEzAndroidRx() {
		try {
			this.setEzScreenServer(new EzScreenServer(context, InetAddress.getByName(getIpAddress()), getServiceName(), new EzScreenServer.EzScreenServerDelegate() {

				@Override
				public void stopVideo() {
					EZScreenHelper.this.stopVideo();
				}

				@Override
				public void stopDisplay() {
					EZScreenHelper.this.stopDisplay();
					
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
				public void resetToStandby() {
					EZScreenHelper.this.resetToStandby();
				}

				@Override
				public void playVideo(String url, String callback) {
					EZScreenHelper.this.playVideo(url, callback);
				}

				@Override
				public void pauseVideo() {
					EZScreenHelper.this.pauseVideo();
				}

				@Override
				public void onDisconnected() {
					EZScreenHelper.this.resetToStandby();
				}

				@Override
				public void onConnected() {
					EZScreenHelper.this.displayUrl("images/connected.jpg");	
				}
				
				@Override
				public void displayUrl(String url) {
					EZScreenHelper.this.displayUrl(url);
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
			}));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.getEzScreenServer().start();
	}

	private void initAirplay() {
		try {
			this.setAirplayService(new AirPlayServer(context, InetAddress.getByName(getIpAddress()), getServiceName(), new AirPlayServer.AirPlayServerDelegate() {
				final static String TAG = EZScreenHelper.TAG+".AirPlayServer";
				@Override
				public void stopVideo() {
					Log.d(TAG, "stopVideo");
					EZScreenHelper.this.stopVideo();
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
					while (EZScreenHelper.this.getState() != AirplayState.PLAYING) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
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
					resetStates();
					EZScreenHelper.this.setPendingStartingPosition(position);
					playVideo(url, null, true/*rate!=0*/, 0);
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
			}));
			this.getAirplayService().start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

	protected void playVideo(String url, String callback, boolean autoplay, int startpos) {
		if (callback != null) {
			invokeJavascript("javascript:playVideoImp('"+url+"','"+callback+"'"+ (autoplay?"true, ":"false, ")+ startpos+");");
		} else {
			invokeJavascript("javascript:playVideoImp('"+url+"', null, "+ (autoplay?"true, ":"false, ")+ startpos+");");			
		}
		hideTextureView();
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

	private void initTextureView() {
		hideTextureView();
		this.getTextureView().setSurfaceTextureListener(new SurfaceTextureListener() {

			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface,
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
	public void start() {
		initEzAndroidRx();
		android.net.wifi.WifiManager wifi =
				(android.net.wifi.WifiManager)
				context.getSystemService(android.content.Context.WIFI_SERVICE);
		setLock(wifi.createMulticastLock("EzDnssdLock"));
		getLock().setReferenceCounted(true);
		getLock().acquire();
		new Thread(new Runnable() {

			@Override
			public void run() {
				initAirplay();			
			}
			
		}).start();
	}
	public void stop() {
		stopMJpegClient();
		this.getMainHandler().removeCallbacks(this.getResetToStandby());
		resetToStandby();
		cleanUpServers();
		if (this.getLock() != null) {
			this.getLock().release();
			this.setLock(null);
		}
	}
}