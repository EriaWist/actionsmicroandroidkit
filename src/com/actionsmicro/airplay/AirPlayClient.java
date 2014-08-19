package com.actionsmicro.airplay;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import com.actionsmicro.airplay.http.PlistBody;
import com.actionsmicro.utils.Log;
import com.actionsmicro.web.SimpleContentUriHttpFileServer;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.XMLPropertyListParser;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
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
	private static final String USER_AGENT_STRING = "MediaControl/1.0";
	private static final String TAG = "AirPlayClient";
	private InetAddress serverAddress;
	private AsyncServer reverseConnection = new AsyncServer();
	private AsyncHttpServer eventServer = new AsyncHttpServer() {
		@Override
		protected void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getHeaders().getHeaders().getStatusLine());
		}
		@Override
		protected AsyncHttpRequestBody<String> onUnknownBody(RawHeaders headers) {
			return new PlistBody();
		}
	};;
	private HandlerThread timerThread;
	private Handler timerHandler;
	private VideoStateListener videoStateListener;
	private String sessionId = UUID.randomUUID().toString();
	private AsyncHttpClient asyncHttpClient;
	
	private SimpleContentUriHttpFileServer simpleHttpFileServer;
	private Context context;
	
	public AirPlayClient(Context context, InetAddress inetAddress) {
		this.context = context;
		this.serverAddress = inetAddress;
		reverseConnection.run(true, true);
		inqueryServerInfo();
		prepareEventServer();
		establishReverseHttpConnection();
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
					Log.d(TAG, "Event\n:"+event.toXMLPropertyList());
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
	private void establishReverseHttpConnection() {
		try {			
			RawHeaders headers = new RawHeaders();
			headers.add("Upgrade", "PTTH/1.0");
			headers.add("Connection", "Upgrade");
			headers.add("X-Apple-Purpose", "event");
			headers.add("Content-Lengthe", "0");
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost reverse = new AsyncHttpPost(getServerUri("/reverse"), headers);
			reverse.setTimeout(0);
			AsyncHttpClient httpClient = new AsyncHttpClient(reverseConnection);
			httpClient.executeString(reverse, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
			            return;
			        }
			        AsyncSocket socket = source.detachSocket();
			        Log.d(TAG, "Server says: " + source.getHeaders().getHeaders().getStatusLine());
			        eventServer.establishConnection(socket);
			    }
			});
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
			AsyncHttpPost playVideo = new AsyncHttpPost(getServerUri("/play"), headers);
			NSDictionary playbackInfo = new NSDictionary();
			playbackInfo.put("Content-Location", mediaUriString);
			playbackInfo.put("Start-Position", 0.0);
			StringBody body = new PlistBody(playbackInfo.toXMLPropertyList());
			playVideo.setBody(body);
			getHttpClient().executeString(playVideo, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
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
		}
	}
	private URI getServerUri(String path) throws URISyntaxException {
		return getServerUri(path, null);
	}
	private URI getServerUri(String path, String query) throws URISyntaxException {
		return new URI("http", null, serverAddress.getHostAddress(), 7000, path, query, null);
	}
	public void close() {
		if (reverseConnection != null) {
			reverseConnection.stop();
		}
		stopPeriodicalPoller();
		stopHttpFileServer();
		if (asyncHttpClient != null) {
			asyncHttpClient.getServer().stop();
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
			AsyncHttpPost scrub = new AsyncHttpPost(getServerUri("/scrub", "position="+position));
			getHttpClient().executeString(scrub, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
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
			AsyncHttpPost scrub = new AsyncHttpPost(getServerUri("/rate", "value="+rate));
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
					schedulePlaybackInfoPoller();
					if (e != null) {
			            e.printStackTrace();
			            return;
			        }
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
}
