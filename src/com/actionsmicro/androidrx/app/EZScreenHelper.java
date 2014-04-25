package com.actionsmicro.androidrx.app;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

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
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.util.Log;
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

import com.actionsmicro.BuildConfig;
import com.actionsmicro.airplay.AirPlayServer;
import com.actionsmicro.androidrx.EzScreenServer;
import com.actionsmicro.web.SimpleMotionJpegOverHttpClient;
import com.actionsmicro.web.SimpleMotionJpegOverHttpClient.ConnectionCallback;
import com.actionsmicro.web.SimpleMotionJpegOverHttpClient.JpegCallback;
import com.yutel.silver.vo.AirplayState;

public class EZScreenHelper {
	public interface ConnectionListener {
		public void onConnected();
		public void onDisconnected();
	}
	private static final String TAG = "EZScreenHelper";
	private WebView webView;
	private TextureView mjpegView;
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
	private ConnectionListener connectionListener;
	protected Surface mirrorSurface;
	protected int surfaceWidth;
	protected int surfaceHeight;
	private int servers;
	protected SurfaceTexture mirrorSurfaceTexture;
	private ViewGroup container;
	private TextureView mirrorView;
	private String getServiceName() {
		return serviceName;
	}
	public static final int SERVER_EZSCREEN = 0x01<<0; 
	public static final int SERVER_AIRPLAY = 0x01<<1; 
	public EZScreenHelper(Context context, String serviceName, ViewGroup frame, WebView webView, TextureView textureView, int servers) {
		this.context = context;
		this.webView = webView;
		this.mjpegView = textureView;
		this.serviceName = serviceName;
		this.servers = servers;
		this.container = frame;
		audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		initWebView();
		initMjpegView();
		initMirrorView();
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
		showWebView();
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

	private void hideMjpegView() {
		final int invisible = View.INVISIBLE;
		setViewVisibility(mjpegView, invisible);
		setViewVisibility(webView, View.VISIBLE);
	}

	private void setViewVisibility(final View view, final int visibility) {
		this.getMainHandler().post(new Runnable() {

			@Override
			public void run() {
				view.setVisibility(visibility);				
			}
			
		});
	}

	private void stopDisplay() {
		stopMJpegClient();
		showWebView();
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
			showWebView();
		} else {
			if (url.startsWith("http")) {
				displayMotionJpeg(url);
			} else {
				invokeJavascript("javascript:updateDisplay('"+url+"');");
				showWebView();
			}
		}
	}

	private void showWebView() {
		setViewVisibility(webView, View.VISIBLE);
		setViewVisibility(mirrorView, View.INVISIBLE);
		setViewVisibility(mjpegView, View.INVISIBLE);
	}

	private void displayMotionJpeg(final String url) {
		stopMJpegClient();
		showMjpegView();
		try {
			this.setmJpegClient(new SimpleMotionJpegOverHttpClient(url, new JpegCallback() {

				@Override
				public void onJpegAvaiable(byte[] jpegData, int size) {
					if (EZScreenHelper.this.getMjpegView() != null) {
						
						Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, size);
						if (bitmap != null) {
//									drawOnSurface(bitmap);
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
		setViewVisibility(mjpegView, View.VISIBLE);
		setViewVisibility(webView, View.INVISIBLE);
		setViewVisibility(mirrorView, View.INVISIBLE);		
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
		if (callback != null) {
			invokeJavascript("javascript:playVideo('"+url+"','"+callback+"');");
		} else {
			invokeJavascript("javascript:playVideo('"+url+"', null);");			
		}
		showWebView();
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
					if (connectionListener != null) {
						connectionListener.onDisconnected();
					}
				}

				@Override
				public void onConnected() {
					EZScreenHelper.this.displayUrl("images/connected.jpg");	
					if (connectionListener != null) {
						connectionListener.onConnected();
					}
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

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void initAirplay() {
		try {
			this.setAirplayService(new AirPlayServer(context, InetAddress.getByName(getIpAddress()), getServiceName(), new AirPlayServer.AirPlayServerDelegate() {
				final static String TAG = EZScreenHelper.TAG+".AirPlayServer";
				private FileOutputStream testFile;
				private final byte[] nalHeader = {0x00, 0x00, 0x00, 0x01};
				private MediaCodec decoder;
				private Thread renderThread;
				private static final boolean DUMP_H264 = true;
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
					if (connectionListener != null) {
						connectionListener.onConnected();
					}
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
				private static final String MIME_VIDEO_AVC = "video/avc";
				private boolean stopRenderer = false;
				private long startMs;
				private ByteBuffer[] inputBuffers;
				private long timestampBase;
				
				@Override
				public void onStartMirroring() {
					Log.d(TAG, "onStartMirroring");
					showMirrorView();
					stopDecoding();
					timestampBase = 0;
					startMs = System.currentTimeMillis();
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
					decoder.configure(MediaFormat.createVideoFormat(MIME_VIDEO_AVC, 100, 100), mirrorSurface, null, 0);
					decoder.start();
					inputBuffers = decoder.getInputBuffers();
					startRenderer();
					
					closeTestFile();
					if (DUMP_H264) {
						try {
							testFile = new FileOutputStream("/sdcard/test"+".h264");
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (connectionListener != null) {
						connectionListener.onConnected();
					}
				}

				private void startRenderer() {
					stopRenderer = false;
					renderThread = new Thread(new Runnable() {

						@Override
						public void run() {
							MediaCodec.BufferInfo bufferInfo= new MediaCodec.BufferInfo();
							while (!stopRenderer) {
								int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 5000);
								if (outputBufferIndex >= 0) {		
									Log.d(TAG, "bufferInfo.presentationTimeUs:"+bufferInfo.presentationTimeUs);
									Log.d(TAG, "bufferInfo.presentationTimeUs-timestampBase/1000:"+((bufferInfo.presentationTimeUs-timestampBase)/1000));
									Log.d(TAG, "bufferInfo.System.currentTimeMillis() - startMs:"+(System.currentTimeMillis() - startMs));
									while (((bufferInfo.presentationTimeUs-timestampBase) / 1000) > (System.currentTimeMillis() - startMs)) {
										Log.d(TAG, "bufferInfo.presentationTimeUs-timestampBase:"+(bufferInfo.presentationTimeUs-timestampBase));
										Log.d(TAG, "bufferInfo.System.currentTimeMillis() - startMs:"+(System.currentTimeMillis() - startMs));
								        try {
								          Thread.sleep(10);
								        } catch (InterruptedException e) {
								          e.printStackTrace();
								          break;
								        }
								      }
									decoder.releaseOutputBuffer(outputBufferIndex, ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)?false:true);
								} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
									decoder.getOutputBuffers();
								} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
									MediaFormat outputFormat = decoder.getOutputFormat();
									int width = outputFormat.getInteger(MediaFormat.KEY_WIDTH);
									int height = outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
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
						}
						
					});
					renderThread.start();
				}

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

				@Override
				public void onSpsAvailable(byte[] sps) {
					decodeByteWithPrefix(nalHeader, sps, 0, sps.length,  0);
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

				private void decodeByteWithPrefix(byte[] prefix, byte[] data, int offset, int length, long timestamp) {
					if (decoder != null) {
						int bufferIndex = decoder.dequeueInputBuffer(10000);
						if (bufferIndex != -1) {
							inputBuffers[bufferIndex].clear();
							if (prefix != null) {
								inputBuffers[bufferIndex].put(prefix);
							}
							inputBuffers[bufferIndex].put(data, offset, length);
							int totalWrite = inputBuffers[bufferIndex].position();
							inputBuffers[bufferIndex].rewind();
							int flags = 0;//(nalCounter<=1)?MediaCodec.BUFFER_FLAG_CODEC_CONFIG:0;
							decoder.queueInputBuffer(bufferIndex, 0, totalWrite, timestamp, flags);							
						}
					}
				}

				@Override
				public void onPpsAvailable(byte[] pps) {
					decodeByteWithPrefix(nalHeader, pps, 0, pps.length, 0);
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
				/**
			     * baseline NTP time if bit-0=0 -> 7-Feb-2036 @ 06:28:16 UTC
			     */
			    protected static final long msb0baseTime = 2085978496000L;

			    /**
			     *  baseline NTP time if bit-0=1 -> 1-Jan-1900 @ 01:00:00 UTC
			     */
			    protected static final long msb1baseTime = -2208988800000L;
				public long getTime(long ntpTimeValue)
			    {
			        long seconds = (ntpTimeValue >>> 32) & 0xffffffffL;     // high-order 32-bits
			        long fraction = ntpTimeValue & 0xffffffffL;             // low-order 32-bits

			        // Use round-off on fractional part to preserve going to lower precision
			        fraction = Math.round(1000D * fraction / 0x100000000L);

			        /*
			         * If the most significant bit (MSB) on the seconds field is set we use
			         * a different time base. The following text is a quote from RFC-2030 (SNTP v4):
			         *
			         *  If bit 0 is set, the UTC time is in the range 1968-2036 and UTC time
			         *  is reckoned from 0h 0m 0s UTC on 1 January 1900. If bit 0 is not set,
			         *  the time is in the range 2036-2104 and UTC time is reckoned from
			         *  6h 28m 16s UTC on 7 February 2036.
			         */
			        long msb = seconds & 0x80000000L;
			        if (msb == 0) {
			            // use base: 7-Feb-2036 @ 06:28:16 UTC
			            return msb0baseTime + (seconds * 1000) + fraction;
			        } else {
			            // use base: 1-Jan-1900 @ 01:00:00 UTC
			            return msb1baseTime + (seconds * 1000) + fraction;
			        }
			    }
				@Override
				public void onH264FrameAvailable(byte[] frame, int offset, int size, long timestamp) {
					long timestampInMicroSec = getTime(timestamp)*1000;
					if (timestampBase == 0) {
						timestampBase = timestampInMicroSec;
						Log.d(TAG, "bufferInfo.timestampBase:"+timestampBase);
					}
					decodeByteWithPrefix(null, frame, offset, size, timestampInMicroSec);
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
					if (connectionListener != null) {
						connectionListener.onDisconnected();
					}
					stopDecoding();
					closeTestFile();
					hideMirrorView();
				}

				private void stopDecoding() {
					if (renderThread != null) {
						stopRenderer = true;
						try {
							renderThread.join();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
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
					inputBuffers = null;
				}
			}));
			this.getAirplayService().start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

	private void playVideo(String url, String callback, boolean autoplay, int startpos) {
		if (callback != null) {
			invokeJavascript("javascript:playVideoImp('"+url+"','"+callback+"'"+ (autoplay?"true, ":"false, ")+ startpos+");");
		} else {
			invokeJavascript("javascript:playVideoImp('"+url+"', null, "+ (autoplay?"true, ":"false, ")+ startpos+");");			
		}
		showWebView();
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
				Log.d(TAG, "onSurfaceTextureUpdated:");
				
			}
			
		});
	}
	private void hideMirrorView() {
		setViewVisibility(mirrorView, View.INVISIBLE);
		setViewVisibility(webView, View.VISIBLE);
	}
	private void showMirrorView() {
		setViewVisibility(mirrorView, View.VISIBLE);
		setViewVisibility(webView, View.INVISIBLE);
		setViewVisibility(mjpegView, View.INVISIBLE);
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
	public void start() {
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

	public ConnectionListener getConnectionListener() {
		return connectionListener;
	}

	public void setConnectionListener(ConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
	}
}