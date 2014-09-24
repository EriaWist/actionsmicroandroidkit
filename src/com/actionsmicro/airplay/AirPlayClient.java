package com.actionsmicro.airplay;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.YuvImage;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;

import com.actionsmicro.airplay.http.PlistBody;
import com.actionsmicro.airplay.mirror.TsStreamer;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;
import com.actionsmicro.web.SimpleContentUriHttpFileServer;
import com.dd.plist.BinaryPropertyListWriter;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.XMLPropertyListParser;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.StreamBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

public class AirPlayClient {
	public interface VideoStateListener {

		void onVideoResumed();

		void onVideoPaused();

		void onVideoStopped();

		void onVideoPlayed();

		void onDurationChanged(float duration);

		void onTimeChanged(float position);

		void onVideoError(int errorCode);

	}
	public interface ConnectionManager {

		void onConnectionFailed(Exception e);
		
	}
	private static final String USER_AGENT_STRING = "MediaControl/1.0";
	private static final String TAG = "AirPlayClient";
	private InetAddress serverAddress;
	private AsyncServer reverseConnectionForEvent = new AsyncServer();
	private AsyncHttpServer eventServer = new AsyncHttpServer() {
		@Override
		protected void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getHeaders().getHeaders().getStatusLine());
		}
		@Override
		protected AsyncHttpRequestBody<String> onUnknownBody(RawHeaders headers) {
			return new PlistBody();
		}
	};
	private AsyncServer reverseConnectionForSlideshow = new AsyncServer();
	private AsyncHttpServer slideshowServer = new AsyncHttpServer() {
		@Override
		protected void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getHeaders().getHeaders().getStatusLine());
		}
		@Override
		protected AsyncHttpRequestBody<String> onUnknownBody(RawHeaders headers) {
			return new PlistBody();
		}
	};
	private AsyncHttpServer m3u8Server = new AsyncHttpServer() {
		@Override
		protected void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getHeaders().getHeaders().getStatusLine());
		}
	};
	private HandlerThread timerThread;
	private Handler timerHandler;
	private VideoStateListener videoStateListener;
	private String sessionId = UUID.randomUUID().toString();
	private AsyncHttpClient asyncHttpClient;
	
	private SimpleContentUriHttpFileServer simpleHttpFileServer;
	private Context context;
	private List<ConnectionManager> managers = new ArrayList<ConnectionManager>();
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
					manager.onConnectionFailed(e);
				}						
			}
		}
	}
	public AirPlayClient(Context context, InetAddress inetAddress) {
		this.context = context;
		this.serverAddress = inetAddress;
		reverseConnectionForEvent.run(true, true);
		inqueryServerInfo();
		prepareEventServer();
		establishReverseHttpConnectionForEvent();
		prepareSlideshowServer();
		establishReverseHttpConnectionForSlideshow();		
	}
	
	private void inqueryServerInfo() {
		RawHeaders headers = new RawHeaders();
		headers.add("Content-Lengthe", "0");
		headers.add("User-Agent", USER_AGENT_STRING);
		headers.add("X-Apple-Session-ID", getSessionId());
		try {
			AsyncHttpGet serverInfo = new AsyncHttpGet(getServerUri("/server-info"), headers);
			getHttpClient().executeString(serverInfo, new StringCallback() {

				@Override
				public void onCompleted(Exception e, AsyncHttpResponse source,
						String result) {
					if (e != null) {
			            e.printStackTrace();
			            return;
			        }
					Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
					try {
						NSDictionary serverInfo = (NSDictionary)XMLPropertyListParser.parse(result.getBytes());
					} catch (ParserConfigurationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (ParseException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (SAXException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (PropertyListFormatException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				
			});
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private AsyncHttpClient getHttpClient() {
		if (asyncHttpClient == null) {
			asyncHttpClient = new AsyncHttpClient(new AsyncServer());
			asyncHttpClient.getSocketMiddleware().setMaxConnectionCount(1);
		}
		return asyncHttpClient;
	}
	private void prepareEventServer() {
		eventServer.post("/event", new HttpServerRequestCallback() {
			@Override
			public void onRequest(final AsyncHttpServerRequest request,
					final AsyncHttpServerResponse response) {
				try {
					NSDictionary event = (NSDictionary)XMLPropertyListParser.parse(request.getBody().get().toString().getBytes());
					Log.d(TAG, "Event:\n"+event.toXMLPropertyList());
					if (event.containsKey("category")) {
						if (event.get("category").toString().equals("video")) {
							if (event.containsKey("state")) {
								String state = event.get("state").toString();
								Log.d(TAG, "Event state:"+state);
								// maintain a state machine to distinguish play and resume
								if (state.equals("playing")) {
									if (videoStateListener != null) {
										videoStateListener.onVideoResumed();
									}
								} else if (state.equals("paused")) {
									if (videoStateListener != null) {
										videoStateListener.onVideoPaused();
									}
								} else if (state.equals("stopped")) {
									if (videoStateListener != null) {
										videoStateListener.onVideoStopped();
									}
								}
							}
							if (event.containsKey("error")) {
								NSDictionary error = (NSDictionary)event.get("error");
								if (error.containsKey("code")) {
									int errorCode = ((NSNumber)error.get("code")).intValue();
									if (errorCode != 361) { // not a general error
										if (videoStateListener != null) {
											videoStateListener.onVideoError(errorCode);
										}
									}
								}
							}
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (PropertyListFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				response.responseCode(200);
				response.end();
			}			
		});
	}
	private void establishReverseHttpConnectionForEvent() {
		try {			
			RawHeaders headers = new RawHeaders();
			headers.add("Upgrade", "PTTH/1.0");
			headers.add("Connection", "Upgrade");
			headers.add("X-Apple-Purpose", "event");
			headers.add("Content-Lengthe", "0");
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost reverse = new AsyncHttpPost(getServerUri("/reverse"), headers);
//			reverse.setTimeout(0);
			AsyncHttpClient httpClient = new AsyncHttpClient(reverseConnectionForEvent);
			httpClient.executeString(reverse, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
			            handleNetworkException(e);
			            return;
			        }
			        AsyncSocket socket = source.detachSocket();
			        Log.d(TAG, "Server says: " + source.getHeaders().getHeaders().getStatusLine());
			        
			        eventServer.establishConnection(socket);
			        socket.setClosedCallback(new CompletedCallback() {

						@Override
						public void onCompleted(Exception e) {
							if (e != null) {
								notifyConnectionManagerDidFailed(e);
							}
						}
			        	
			        });
			    }
			});
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	private int slideshowAssetsId = 1;
	private void prepareSlideshowServer() {
		slideshowServer.get("/slideshows/1/assets/1", new HttpServerRequestCallback() {

			@Override
			public void onRequest(AsyncHttpServerRequest request,
					AsyncHttpServerResponse response) {
				synchronized (jpegBuffer) {
					if (jpegBuffer.size() > 0) {
						replySlideshowAsset(response);
					} else {
						try {
							jpegBuffer.wait();
							replySlideshowAsset(response);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				}
			}
			
		});
	}
	private void establishReverseHttpConnectionForSlideshow() {
		try {			
			RawHeaders headers = new RawHeaders();
			headers.add("Upgrade", "PTTH/1.0");
			headers.add("Connection", "Upgrade");
			headers.add("X-Apple-Purpose", "slideshow");
			headers.add("Content-Lengthe", "0");
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost reverse = new AsyncHttpPost(getServerUri("/reverse"), headers);
//			reverse.setTimeout(0);
			AsyncHttpClient httpClient = new AsyncHttpClient(reverseConnectionForSlideshow);
			httpClient.executeString(reverse, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
			            handleNetworkException(e);
			            return;
			        }
			        AsyncSocket socket = source.detachSocket();
			        Log.d(TAG, "Server says: " + source.getHeaders().getHeaders().getStatusLine());
			        
			        slideshowServer.establishConnection(socket);
			        socket.setClosedCallback(new CompletedCallback() {

						@Override
						public void onCompleted(Exception e) {
							if (e != null) {
								notifyConnectionManagerDidFailed(e);
							}
						}
			        	
			        });
			        synchronized (slideshowServer) {
			        	slideshowServer.notify();
			        }
			    }
			});
			synchronized (slideshowServer) {
				try {
					slideshowServer.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	private String getSessionId() {
		return sessionId;//"368e90a4-5de6-4196-9e58-9917bdd4ffd7";
	}
	public void playVideo(String url, VideoStateListener videoStateListener) {
		stopHttpFileServer();		
		
		duration = -1;
		position = 0;
		rate = 0;
		
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
		}
		this.videoStateListener = videoStateListener;
		try {
			RawHeaders headers = new RawHeaders();
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			headers.add("Content-Type", "application/x-apple-binary-plist");
			AsyncHttpPost playVideo = new AsyncHttpPost(getServerUri("/play"), headers);
			NSDictionary playbackInfo = new NSDictionary();
			playbackInfo.put("Content-Location", mediaUriString);
			playbackInfo.put("Start-Position", 0.0);
			ByteArrayOutputStream binaryPlist = new ByteArrayOutputStream();
			BinaryPropertyListWriter.write(binaryPlist, playbackInfo);
			StreamBody body = new StreamBody(new ByteArrayInputStream(binaryPlist.toByteArray()), binaryPlist.size());
			playVideo.setBody(body);
			getHttpClient().executeString(playVideo, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
			            handleNetworkException(e);
			            return;
			        }
			        Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
			        if (AirPlayClient.this.videoStateListener != null) {
			        	AirPlayClient.this.videoStateListener.onVideoPlayed();
			        }
			        startPeriodicalPoller();
			    }
			});
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	private URI getServerUri(String path) throws URISyntaxException {
		return getServerUri(path, 7000, null);
	}
	private URI getServerUri(String path, int port) throws URISyntaxException {
		return getServerUri(path, port, null);
	}
	private URI getServerUri(String path, int port, String query) throws URISyntaxException {
		return new URI("http", null, serverAddress.getHostAddress(), port, path, query, null);
	}
	public void close() {
		if (reverseConnectionForEvent != null) {
			reverseConnectionForEvent.stop();
		}
		stopPeriodicalPoller();
		stopHttpFileServer();
		if (asyncHttpClient != null) {
			asyncHttpClient.getServer().stop();
		}
		releaseTsStreamer();
		stopM3u8Server();
		m3u8Server = null;
	}
	private void stopM3u8Server() {
		if (m3u8Server != null) {
			m3u8Server.stop();
		}
	}
	private void releaseTsStreamer() {
		if (tsStreamer != null) {
			tsStreamer.release();
			tsStreamer = null;
		}
	}
	
	private void stopPeriodicalPoller() {
		if (timerThread != null) {
			timerThread.quit();
			timerThread = null;
		}
	}
	public void scrubVideo(float position) {
		RawHeaders headers = new RawHeaders();
		headers.add("User-Agent", USER_AGENT_STRING);
		headers.add("Content-Lengthe", "0");
		try {
			AsyncHttpPost scrub = new AsyncHttpPost(getServerUri("/scrub", 7000, "position="+position));
			getHttpClient().executeString(scrub, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
			            handleNetworkException(e);
			            return;
			        }
			        Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
			        
			    }
			});
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public void stopVideo() {
		RawHeaders headers = new RawHeaders();
		headers.add("User-Agent", USER_AGENT_STRING);
		headers.add("Content-Lengthe", "0");
		final SimpleContentUriHttpFileServer detachedFileServer = simpleHttpFileServer; // since it's asynchronous, we need to detach the file server first to prevent stop wrong file server.
		simpleHttpFileServer = null;
		try {
			AsyncHttpPost stop = new AsyncHttpPost(getServerUri("/stop"));
			getHttpClient().executeString(stop, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
			            handleNetworkException(e);
			            return;
			        }
			        Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
			        if (detachedFileServer != null) {
			        	detachedFileServer.stop();			        	
			        }
			        if (videoStateListener != null) {
			        	videoStateListener.onVideoStopped();
			        }
			    }
			});
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		stopPeriodicalPoller();
	}
	public void resumeVideo() {
		setVideoRate(1, new StringCallback() {
		    @Override
		    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
		        if (e != null) {
		            e.printStackTrace();
		            handleNetworkException(e);
		            return;
		        }
		        Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
		        if (videoStateListener != null) {
		        	videoStateListener.onVideoResumed();
		        }
		    }
		});
	}
	public void pauseVideo() {
		setVideoRate(0, new StringCallback() {
		    @Override
		    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
		        if (e != null) {
		            e.printStackTrace();
		            handleNetworkException(e);
		            return;
		        }
		        Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
		        if (videoStateListener != null) {
		        	videoStateListener.onVideoPaused();
		        }
		    }
		});
	}
	private void setVideoRate(float rate, StringCallback callback) {
		RawHeaders headers = new RawHeaders();
		headers.add("User-Agent", USER_AGENT_STRING);
		headers.add("Content-Lengthe", "0");
		try {
			AsyncHttpPost scrub = new AsyncHttpPost(getServerUri("/rate", 7000, "value="+rate));
			getHttpClient().executeString(scrub, callback);
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	private float duration = -1;
	private float position;
	private float rate;
	private void startPeriodicalPoller() {
		timerThread = new HandlerThread("AirPlayTimerThread");
		timerThread.start();
		timerHandler = new Handler(timerThread.getLooper());
		schedulePlaybackInfoPoller();
	}
	private void schedulePlaybackInfoPoller() {
		timerHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				inqueryPlaybackInfo();
			}
			
		}, 1000);
	}
	private void setRate(float rate) {
		if (this.rate != rate) {
			this.rate = rate;
			if (rate == 1) {
				if (videoStateListener != null) {
					videoStateListener.onVideoPlayed();
				}
			} else {
				if (videoStateListener != null) {
					videoStateListener.onVideoPaused();
				}
			}
		}
	}
	private void setDuration(final float duration) {
		if (AirPlayClient.this.duration != duration) {
			AirPlayClient.this.duration = duration;
			if (videoStateListener != null) {
				videoStateListener.onDurationChanged(duration);
			}									
		}
	}
	private void setPosition(final float position) {
		if (AirPlayClient.this.position != position) {
			AirPlayClient.this.position = position;
			if (videoStateListener != null) {
				videoStateListener.onTimeChanged(position);
			}	
		}
	}
	private void inqueryPlaybackInfo() {
		try {
			RawHeaders headers = new RawHeaders();
			headers.add("Content-Lengthe", "0");
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpGet playbackInfo = new AsyncHttpGet(getServerUri("/playback-info"), headers);
			getHttpClient().executeString(playbackInfo, new StringCallback() {

				@Override
				public void onCompleted(Exception e,
						AsyncHttpResponse source, String result) {
					if (e != null) {
			            e.printStackTrace();
			            handleNetworkException(e);
			            return;
			        }
					schedulePlaybackInfoPoller();
					Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
					try {
						NSDictionary playbackInfo = (NSDictionary)XMLPropertyListParser.parse(result.getBytes());
						if (playbackInfo.containsKey("duration")) {
							final float duration = ((NSNumber)playbackInfo.get("duration")).floatValue();
							setDuration(duration);
						}
						if (playbackInfo.containsKey("position")) {
							final float position = ((NSNumber)playbackInfo.get("position")).floatValue();
							setPosition(position);
						}
						if (playbackInfo.containsKey("rate")) {
							final float rate = ((NSNumber)playbackInfo.get("rate")).floatValue();
							setRate(rate);
						}
						boolean readyToPlay = false;
						if (playbackInfo.containsKey("readyToPlay")) {
							readyToPlay = ((NSNumber)playbackInfo.get("readyToPlay")).boolValue();
						}
						Log.d(TAG, "playback-info: readyToPlay:" +(readyToPlay?"true":"false")+", duration:"+duration+", position:"+position+", rate:"+rate);
						
					} catch (ParserConfigurationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (ParseException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (SAXException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (PropertyListFormatException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				}

				
			});
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void stopHttpFileServer() {
		if (simpleHttpFileServer != null) {
			simpleHttpFileServer.stop();
			simpleHttpFileServer = null;
		}
	}
	protected void handleNetworkException(Exception e) {
		if (videoStateListener != null) {
			videoStateListener.onVideoError(-1004); //TODO refactor the error code with domain for client to distinguish.
		}
		if (e != null) {
			notifyConnectionManagerDidFailed(e);
		}
	}
	private ByteArrayOutputStream jpegBuffer = new ByteArrayOutputStream(500*1024);
	private Semaphore semaphore = new Semaphore(1);
	public void startSlideshow() {
		
		try {
			RawHeaders headers = new RawHeaders();
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			headers.add("Content-Type", "text/x-apple-plist+xml");
			AsyncHttpRequest putSlideshow = new AsyncHttpRequest(getServerUri("/slideshows/1"), "PUT", headers);
			NSDictionary slideshow = new NSDictionary();
			NSDictionary settings = new NSDictionary();
			settings.put("slideDuration", 0);
			settings.put("theme", "Classic");
			slideshow.put("settings", settings);
			slideshow.put("state", "playing");
			StringBody body = new PlistBody(slideshow.toXMLPropertyList());
			putSlideshow.setBody(body);
			getHttpClient().executeString(putSlideshow, new StringCallback() {

				@Override
				public void onCompleted(Exception e, AsyncHttpResponse source,
						String result) {
					// TODO Auto-generated method stub
					
				}
				
			});
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void displayPhoto(InputStream jpegStream, long length) {
		
		if (semaphore.tryAcquire()) {
			Log.d(TAG, "clone jpeg buffer");
			synchronized (jpegBuffer) {
				jpegBuffer.reset();
				try {
					Utils.dump(jpegStream, jpegBuffer);
					jpegBuffer.notify();
				} catch (IOException e1) {
					e1.printStackTrace();
					jpegBuffer.reset();
				}
			}
			Log.d(TAG, "send /photo request");
			RawHeaders headers = new RawHeaders();
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-AssetAction", "cacheOnly");
			headers.add("X-Apple-Session-ID", getSessionId());
			final String photoId = UUID.randomUUID().toString();
			headers.add("X-Apple-AssetKey", photoId);
			headers.add("Content-Length", String.valueOf(jpegBuffer.size()));
			try {
				AsyncHttpRequest putPhoto = new AsyncHttpRequest(getServerUri("/photo"), "PUT", headers);
				StreamBody jpegDataBody = new StreamBody(new ByteArrayInputStream(jpegBuffer.toByteArray()), jpegBuffer.size());
				putPhoto.setBody(jpegDataBody);
				getHttpClient().executeString(putPhoto, new StringCallback() {

					@Override
					public void onCompleted(Exception e, AsyncHttpResponse source,
							String result) {
						Log.d(TAG, "send /photo request - cache complete\n"+source.getHeaders().getHeaders().getResponseMessage());
						semaphore.release();									
						
						RawHeaders headers = new RawHeaders();
						headers.add("User-Agent", USER_AGENT_STRING);
						headers.add("X-Apple-AssetAction", "displayCached");
						headers.add("X-Apple-Session-ID", getSessionId());
						headers.add("X-Apple-AssetKey", photoId);
						headers.add("Content-Length", "0");
						headers.add("X-Apple-Transition", "None");
						
						try {
							AsyncHttpRequest putPhoto = new AsyncHttpRequest(getServerUri("/photo"), "PUT", headers);
							getHttpClient().executeString(putPhoto, new StringCallback() {

								@Override
								public void onCompleted(Exception e,
										AsyncHttpResponse source, String result) {
									Log.d(TAG, "send /photo request - display cached complete\n"+source.getHeaders().getHeaders().getResponseMessage());
								}
								
							});
						
						} catch (URISyntaxException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
					}

				});
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Log.d(TAG, "send /photo busy");
		}
	}
	private void replySlideshowAsset(AsyncHttpServerResponse response) {
		Log.d(TAG, "replySlideshowAsset");
		if (jpegBuffer.size() > 0) {
			try {
				NSDictionary slideshowAsset = new NSDictionary();
				slideshowAsset.put("data", new NSData(jpegBuffer.toByteArray()));
				NSDictionary info = new NSDictionary();
				info.put("id", slideshowAssetsId);
				info.put("key", 1);
				slideshowAsset.put("info", info);
				response.getHeaders().getHeaders().add("Content-Type", "application/x-apple-binary-plist");
				ByteArrayOutputStream binaryPlist = new ByteArrayOutputStream();
				BinaryPropertyListWriter.write(binaryPlist, slideshowAsset);
				response.sendStream(new ByteArrayInputStream(binaryPlist.toByteArray()), binaryPlist.size());
				response.responseCode(200);
				response.end();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			jpegBuffer.reset();
		}
	}
	private TsStreamer tsStreamer = null;
	private AsyncServerSocket m3u8ServerSocket;
	public void displayYuvImage(YuvImage yuvImage) {
		requestRemoteToPlayTsStreamer();
		displayYuvImageViaTsStreamer(yuvImage);
	}
	private void requestRemoteToPlayTsStreamer() {
		if (tsStreamer == null) {
			tsStreamer = new TsStreamer();
			tsStreamer.setDelegate(new TsStreamer.Delegate() {
				
				@Override
				public void onSizeChanged() {
					stopVideo();
					releaseTsStreamer();
					stopM3u8Server();
					requestRemoteToPlayTsStreamer();
				}
			});
			tsStreamer.start();
			Log.d(TAG, "tsStreamer running at:" + getTsServerUrl());
			initM3u8ServerForTsStreamer();
			m3u8ServerSocket = m3u8Server.listen(0);
			Log.d(TAG, "m3u8Server running at:" + getM3u8ServerUrl());
			playVideo(getM3u8ServerUrl(), null);
		}
	}
	private void initM3u8ServerForTsStreamer() {
		m3u8Server.get("/", new HttpServerRequestCallback() {

			@Override
			public void onRequest(AsyncHttpServerRequest request,
					AsyncHttpServerResponse response) {
				response.setContentType("application/x-mpegURL");
				StringBuilder sb = new StringBuilder();
				sb.append("#EXTM3U\n" +
						"#EXT-X-TARGETDURATION:1\n" +
						"#EXT-X-VERSION:3\n" +
						"#EXT-X-MEDIA-SEQUENCE:1\n" +
						"#EXTINF:0.3,\n" +
						getTsServerUrl());
				response.send(sb.toString());
			}
			
		});
	}
	private void displayYuvImageViaTsStreamer(YuvImage yuvImage) {
		if (tsStreamer != null) {
			tsStreamer.displayYuvImage(yuvImage);
		}
	}
	
	private static NSDictionary disctionaryWithKeyAndValue(String key, String value) {
		NSDictionary dict = new NSDictionary();
		dict.put(key, value);
		return dict;
	}
	private String getM3u8ServerUrl() {
		if (m3u8ServerSocket != null) {
			try {
				return new URL("http", getIPAddress(true), m3u8ServerSocket.getLocalPort(), "").toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	private String getTsServerUrl() {
		if (tsStreamer != null) {
			try {
				return new URL("http", getIPAddress(true), tsStreamer.getListeningPort(), "").toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	private String getIPAddress(boolean useIPv4) { //TODO  DRY
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();

		String ipString = String.format(
				"%d.%d.%d.%d",
				(ip & 0xff),
				(ip >> 8 & 0xff),
				(ip >> 16 & 0xff),
				(ip >> 24 & 0xff));

		return ipString;
    }
}
