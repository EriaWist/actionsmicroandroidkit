package com.actionsmicro.airplay;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import com.actionsmicro.airplay.auth.AirPlayAuth;
import com.actionsmicro.airplay.http.PlistBody;
import com.actionsmicro.airplay.mirror.TsStreamer;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi.Cause;
import com.actionsmicro.utils.Device;
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
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.StreamBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.xml.sax.SAXException;

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

public class AirPlayClient {
	public enum AuthState {
		Init,
		Auth,
		Pin,
		Verify,
		Done,
		Fail
	}

	public interface VideoStateListener {

		void onVideoResumed();

		void onVideoPaused();

		void onVideoStopped(Cause cause);

		void onVideoPlayed();

		void onDurationChanged(float duration);

		void onTimeChanged(float position);

		void onVideoError(int errorCode);

	}
	public interface ConnectionManager {
		void onConnectionFailed(Exception e);
	}

	private static final String USER_AGENT_AUTH_STRING = "AirPlay/320.20";
	private static final String USER_AGENT_STRING = "MediaControl/1.0";
	private static final String TAG = "AirPlayClient";
	private InetAddress serverAddress;
	private AsyncServer reverseConnectionForEvent = new AsyncServer();
	private AsyncHttpServer eventServer = new AsyncHttpServer() {
		@Override
		protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getPath());
			return false;
		}
		@Override
		protected AsyncHttpRequestBody<String> onUnknownBody(Headers headers) {
			return new PlistBody();
		}
	};
	private AsyncServer reverseConnectionForSlideshow = new AsyncServer();
	private AsyncHttpServer slideshowServer = new AsyncHttpServer() {
		@Override
		protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getPath());
			return false;
		}
		@Override
		protected AsyncHttpRequestBody<String> onUnknownBody(Headers headers) {
			return new PlistBody();
		}
	};
	private AsyncHttpServer m3u8Server = new AsyncHttpServer() {
		@Override
		protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getPath());
			return false;
		}
	};

	private DisplayApi m_displayApi;
	private DisplayApi.DisplayListener m_displayListener;
	private AuthState authState;
	private HandlerThread timerThread;
	private Handler timerHandler;
	private VideoStateListener videoStateListener;
	private String sessionId = UUID.randomUUID().toString();
	private AsyncHttpClient asyncHttpClient;
	private AirPlayAuth airplayAuth = new AirPlayAuth();
	private SimpleContentUriHttpFileServer simpleHttpFileServer;
	private Context context;
	private List<ConnectionManager> managers = new ArrayList<ConnectionManager>();
	protected int currentSessionId;
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
		authState = AuthState.Init;
	}

	/*	IOS10.2 airplay Authentication flow
	*  Do Authentication: PairVerify1 -> PairVerify2
	*  If Pair Verify failed => Do PairSetup:
	*  		1. Post to airplay server: "/pair-pin-start"
	*  		2. Get user input PIN code
	*		3. Do pairing: doPairSetupPin1 -> doPairSetupPin2 -> doPairSetupPin3
	*  		4. Success => Do Authentication
	*/

	public void SetPinCode(String code) {
		if(authState != AuthState.Pin)
			return;
		DoPairSetup1(code);
	}

	public AuthState CheckAuthState(DisplayApi.DisplayListener listener, DisplayApi displayApi) {
		m_displayApi = displayApi;
		m_displayListener = listener;
		DoAirplayAuth();
		return authState;
	}

	private void DoAirplayAuth() {
		authState = AuthState.Auth;
		airplayAuth.authenticate();//Initial keys
		try {
			byte[] data = airplayAuth.getPairVerify1();
			Headers headers = new Headers();
			headers.add("Content-Length", String.valueOf(data.length));
			headers.add("Content-Type", "application/octet-stream");
			headers.add("User-Agent", USER_AGENT_AUTH_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost pv1 = new AsyncHttpPost(getServerUri("/pair-verify"), headers);
			pv1.setBody(new StreamBody(new ByteArrayInputStream(data), data.length));
			getHttpClient().executeByteBufferList(pv1, new AsyncHttpClient.DownloadCallback() {
				@Override
				public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, ByteBufferList byteBufferList) {
					authState = AuthState.Pin;
					if (e != null) {
						e.printStackTrace();
						DoPairPinStart();
					}
					else if(asyncHttpResponse.code() != 200){
						Log.d(TAG, "Airplay auth failed. Go to Pin verify.");
						DoPairPinStart();
					}
					else {
						authState = AuthState.Auth;
						DoPairVerify2(byteBufferList.getAllByteArray());
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			authState = AuthState.Fail;
		}
	}

	private void DoPairVerify2(byte[] input) {
		try {
			byte[] data = airplayAuth.getPairVerify2(input);
			Headers headers = new Headers();
			headers.add("Content-Length", String.valueOf(data.length));
			headers.add("Content-Type", "application/octet-stream");
			headers.add("User-Agent", USER_AGENT_AUTH_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost pv2 = new AsyncHttpPost(getServerUri("/pair-verify"), headers);
			pv2.setBody(new StreamBody(new ByteArrayInputStream(data), data.length));
			getHttpClient().executeByteBufferList(pv2, new AsyncHttpClient.DownloadCallback() {
				@Override
				public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, ByteBufferList byteBufferList) {
					if (e != null) {
						e.printStackTrace();
						DoPairPinStart();
					}
					else if(asyncHttpResponse.code() != 200){
						Log.d(TAG, "PairVerify2 failed");
						DoPairPinStart();
					}
					else {
						authState = AuthState.Done;
						Log.d(TAG, "Airplay Authentication Success");
						//TODO: Old flow still work after authentication?
						reverseConnectionForEvent.run(true, true);
						inqueryServerInfo();
						prepareEventServer();
						establishReverseHttpConnectionForEvent();
						prepareSlideshowServer();
						establishReverseHttpConnectionForSlideshow();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			authState = AuthState.Pin;
			DoPairPinStart();
		}
	}

	private void DoPairPinStart() {
		try {
			Headers headers = new Headers();
			headers.add("Content-Length", "0");
			headers.add("User-Agent", USER_AGENT_AUTH_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost pps = new AsyncHttpPost(getServerUri("/pair-pin-start"), headers);
			getHttpClient().executeByteBufferList(pps, new AsyncHttpClient.DownloadCallback() {
				@Override
				public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, ByteBufferList byteBufferList) {
					authState = AuthState.Fail;
					if (e != null) {
						e.printStackTrace();
					}
					else if(asyncHttpResponse.code() != 200){
						Log.d(TAG, "PairPinStart failed");
					}
					else {
						authState = AuthState.Pin;
						m_displayListener.requireInputPin(m_displayApi);
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			authState = AuthState.Fail;
		}
	}

	private void DoPairSetup1(final String pin) {
		try {
			authState = AuthState.Verify;
			byte[] data = airplayAuth.getPairSetupPin1();
			Headers headers = new Headers();
			headers.add("Content-Length", String.valueOf(data.length));
			headers.add("Content-Type", "application/x-apple-binary-plist");
			headers.add("User-Agent", USER_AGENT_AUTH_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost ps1 = new AsyncHttpPost(getServerUri("/pair-setup-pin"), headers);
			ps1.setBody(new StreamBody(new ByteArrayInputStream(data), data.length));
			getHttpClient().executeByteBufferList(ps1, new AsyncHttpClient.DownloadCallback() {
				@Override
				public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, ByteBufferList byteBufferList) {
					if (e != null) {
						e.printStackTrace();
						authState = AuthState.Fail;
					}
					else if(asyncHttpResponse.code() != 200){
						Log.d(TAG, "DoPairSetup1 failed");
						authState = AuthState.Fail;
					}
					else {
						DoPairSetup2(byteBufferList.getAllByteArray(), pin);
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			authState = AuthState.Fail;
		}
	}

	private void DoPairSetup2(byte[] input, final String PIN) {
		try {
			byte[] data = airplayAuth.getPairSetupPin2(input, PIN);
			Headers headers = new Headers();
			headers.add("Content-Length", String.valueOf(data.length));
			headers.add("Content-Type", "application/x-apple-binary-plist");
			headers.add("User-Agent", USER_AGENT_AUTH_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost ps1 = new AsyncHttpPost(getServerUri("/pair-setup-pin"), headers);
			ps1.setBody(new StreamBody(new ByteArrayInputStream(data), data.length));
			getHttpClient().executeByteBufferList(ps1, new AsyncHttpClient.DownloadCallback() {
				@Override
				public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, ByteBufferList byteBufferList) {
					if (e != null) {
						e.printStackTrace();
						authState = AuthState.Fail;
					}
					else if(asyncHttpResponse.code() != 200){
						Log.d(TAG, "DoPairSetup2 failed");
						authState = AuthState.Fail;
					}
					else {
						DoPairSetup3(byteBufferList.getAllByteArray());
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			authState = AuthState.Fail;
		}
	}

	private void DoPairSetup3(byte[] input){
		try {
			byte[] data = airplayAuth.getPairSetupPin3(input);
			Headers headers = new Headers();
			headers.add("Content-Length", String.valueOf(data.length));
			headers.add("Content-Type", "application/x-apple-binary-plist");
			headers.add("User-Agent", USER_AGENT_AUTH_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost ps1 = new AsyncHttpPost(getServerUri("/pair-setup-pin"), headers);
			ps1.setBody(new StreamBody(new ByteArrayInputStream(data), data.length));
			getHttpClient().executeByteBufferList(ps1, new AsyncHttpClient.DownloadCallback() {
				@Override
				public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, ByteBufferList byteBufferList) {
					if (e != null) {
						e.printStackTrace();
						authState = AuthState.Fail;
					}
					else if(asyncHttpResponse.code() != 200){
						Log.d(TAG, "DoPairSetup3 failed");
						authState = AuthState.Fail;
					}
					else {
						DoAirplayAuth();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			authState = AuthState.Fail;
		}
	}

	private void inqueryServerInfo() {
		Headers headers = new Headers();
		headers.add("Content-Length", "0");
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
					Log.v(TAG, "Event:\n"+event.toXMLPropertyList());
					if (event.containsKey("category")) {
						if (event.get("category").toString().equals("video")) {
							if (event.containsKey("state")) {
								String state = event.get("state").toString();
								NSNumber sessionIdObject = (NSNumber)event.get("sessionID");
								int sessionId = sessionIdObject!=null?sessionIdObject.intValue():0;
								Log.d(TAG, "Event state:"+state+ " session:"+sessionId);
								// maintain a state machine to distinguish play and resume
								if (state.equals("playing")) {
									currentSessionId = sessionId;
									if (videoStateListener != null) {
										videoStateListener.onVideoResumed();
									}
								} else if (state.equals("paused")) {
									if (currentSessionId == sessionId) {
										if (videoStateListener != null) {
											videoStateListener.onVideoPaused();
										}
									}
								} else if (state.equals("stopped")) {
									if (currentSessionId == sessionId) {
										currentSessionId = -1;
										isPlayingVideo = false;
										if (videoStateListener != null) {
											videoStateListener.onVideoStopped(Cause.REMOTE);
										}
									}
								} else if (state.equals("loading")) {
									currentSessionId = sessionId;
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
				} catch (Throwable e) {
					e.printStackTrace();
				}
				response.code(200);
				response.end();
			}			
		});
	}
	private void establishReverseHttpConnectionForEvent() {
		try {			
			Headers headers = new Headers();
			headers.add("Upgrade", "PTTH/1.0");
			headers.add("Connection", "Upgrade");
			headers.add("X-Apple-Purpose", "event");
			headers.add("Content-Length", "0");
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost reverse = new AsyncHttpPost(getServerUri("/reverse"), headers);
			reverse.setTimeout(1000);
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
			        Log.d(TAG, "establishReverseHttpConnectionForEvent Server says: " + source.message());
			        
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
	private boolean isPlayingVideo;
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
			Headers headers = new Headers();
			headers.add("Upgrade", "PTTH/1.0");
			headers.add("Connection", "Upgrade");
			headers.add("X-Apple-Purpose", "slideshow");
			headers.add("Content-Length", "0");
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-Session-ID", getSessionId());
			AsyncHttpPost reverse = new AsyncHttpPost(getServerUri("/reverse"), headers);
			reverse.setTimeout(1000);
			AsyncHttpClient httpClient = new AsyncHttpClient(reverseConnectionForSlideshow);
			httpClient.executeString(reverse, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
			            handleNetworkException(e);			            
			        } else {
			        	AsyncSocket socket = source.detachSocket();
			        	Log.d(TAG, "Server says: " + source.message());

			        	slideshowServer.establishConnection(socket);
			        	socket.setClosedCallback(new CompletedCallback() {

			        		@Override
			        		public void onCompleted(Exception e) {
			        			if (e != null) {
			        				notifyConnectionManagerDidFailed(e);
			        			}
			        		}

			        	});
			        }
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
	public void playVideo(final String url, final VideoStateListener videoStateListener) {
		isPlayingVideo = true;
		Runnable playVideo = new Runnable() {

			@Override
			public void run() {
				Log.d(TAG, "playVideo:"+url);
				final Runnable self = this;
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
				AirPlayClient.this.videoStateListener = videoStateListener;
				try {
					Headers headers = new Headers();
					headers.add("User-Agent", USER_AGENT_STRING);
					headers.add("X-Apple-Session-ID", getSessionId());
					headers.add("Content-Type", "application/x-apple-binary-plist");
					final AsyncHttpPost playVideo = new AsyncHttpPost(getServerUri("/play"), headers);
					NSDictionary playbackInfo = new NSDictionary();
					playbackInfo.put("Content-Location", mediaUriString);
					playbackInfo.put("Start-Position", 0.0);
					ByteArrayOutputStream binaryPlist = new ByteArrayOutputStream();
					BinaryPropertyListWriter.write(binaryPlist, playbackInfo);
					StreamBody body = new StreamBody(new ByteArrayInputStream(binaryPlist.toByteArray()), binaryPlist.size());
					playVideo.setBody(body);
					synchronized (playVideo) {
						getHttpClient().executeString(playVideo, new StringCallback() {
							@Override
							public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
								if (e != null) {
									e.printStackTrace();
									handleNetworkException(e);
									finishPendingTask(self);
									return;
								}
								Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
								if (AirPlayClient.this.videoStateListener != null) {
									AirPlayClient.this.videoStateListener.onVideoPlayed();
								}
								startPeriodicalPoller();
								synchronized (playVideo) {
									playVideo.notifyAll();
								}
								// Move finishPendingTask to the line after playVideo.notify. so, it won't indirectly blocked by playVideo.wait.
								finishPendingTask(self);
							}
						});
						playVideo.wait(5000);
					}
					
				} catch (URISyntaxException e) {
					e.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
					finishPendingTask(self);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}			
		};
		executeOrSchedule("playVideo", playVideo);
	}
	private Uri getServerUri(String path) throws URISyntaxException {
		return getServerUri(path, 7000, null);
	}
	private Uri getServerUri(String path, int port) throws URISyntaxException {
		return getServerUri(path, port, null);
	}
	private Uri getServerUri(String path, int port, String query) throws URISyntaxException {
		return Uri.parse(new URI("http", null, serverAddress.getHostAddress(), port, path, query, null).toASCIIString());
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
		isPlayingVideo = false;
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
	
	private synchronized void stopPeriodicalPoller() {
		if (timerThread != null) {
			timerThread.quit();
			timerThread = null;
		}
		if (null != timerHandler) {
			timerHandler = null;
		}
	}
	public void scrubVideo(float position) {
		Headers headers = new Headers();
		headers.add("User-Agent", USER_AGENT_STRING);
		headers.add("Content-Length", "0");
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
	private List<Runnable> pendingTasks = new ArrayList<Runnable>();
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
	private void finishPendingTask(Runnable pendingTask) {
		synchronized (pendingTasks) {
			pendingTasks.remove(pendingTask);
			Log.d(TAG, "finishPendingTask pendingTasks left:"+pendingTasks.size());
			if (pendingTasks.size() > 0) {
				pendingTasks.get(0).run();
			}
		}
	}
	public void stopVideo() {
		isPlayingVideo = false;
		Runnable stopVideo = new Runnable() {

			@Override
			public void run() {
				Log.d(TAG, "stopVideo");
				final Runnable self = this;
				Headers headers = new Headers();
				headers.add("User-Agent", USER_AGENT_STRING);
				headers.add("Content-Length", "0");
				final SimpleContentUriHttpFileServer detachedFileServer = simpleHttpFileServer; // since it's asynchronous, we need to detach the file server first to prevent stop wrong file server.
				simpleHttpFileServer = null;
				try {
					final AsyncHttpPost stop = new AsyncHttpPost(getServerUri("/stop"));
					synchronized (stop) {
						Future<String> stopFuture = getHttpClient().executeString(stop, new StringCallback() {
							@Override
							public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
								if (e != null) {
									e.printStackTrace();
									handleNetworkException(e);
									finishPendingTask(self);
									return;
								}
								currentSessionId = -1;
								Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
								if (detachedFileServer != null) {
									detachedFileServer.stop();			        	
								}
								if (videoStateListener != null) {
									videoStateListener.onVideoStopped(Cause.USER);
								}

								// make sure playVideo not called during stop.
								stopPeriodicalPoller();
								synchronized (stop) {
									stop.notifyAll();
								}
								// Move finishPendingTask to the line after stop.notify. so, it won't indirectly blocked by it.
								finishPendingTask(self);
							}
						});
						// make it synchronous to prevent overlapping with play command.
						stop.wait(5000);
					}
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			
		};
		executeOrSchedule("stopVideo", stopVideo);
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
		Headers headers = new Headers();
		headers.add("User-Agent", USER_AGENT_STRING);
		headers.add("Content-Length", "0");
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
	private synchronized void startPeriodicalPoller() {
		timerThread = new HandlerThread("AirPlayTimerThread");
		timerThread.start();
		timerHandler = new Handler(timerThread.getLooper());
		schedulePlaybackInfoPoller();
	}

	private synchronized void schedulePlaybackInfoPoller() {
		if (null != timerHandler) {
			timerHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					inqueryPlaybackInfo();
				}

			}, 1000);
		}
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
			Headers headers = new Headers();
			headers.add("Content-Length", "0");
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
					Log.v(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
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
						
					} catch (Throwable e1) {
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
		isPlayingVideo = false;

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
			Headers headers = new Headers();
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
		if (isPlayingVideo) {
			return;
		}
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
			Headers headers = new Headers();
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
						if (e != null) {
							e.printStackTrace();
				            handleNetworkException(e);
				            return;
						}
						Log.d(TAG, "send /photo request - cache complete\n"+source.message());
						semaphore.release();									
						Headers headers = new Headers();
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
									if (e != null) {
										Log.e(TAG, "PUT /photo, displayCached", e);
										return;
									}
									Log.d(TAG, "send /photo request - display cached complete\n"+source.message());
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
				response.getHeaders().add("Content-Type", "application/x-apple-binary-plist");
				ByteArrayOutputStream binaryPlist = new ByteArrayOutputStream();
				BinaryPropertyListWriter.write(binaryPlist, slideshowAsset);
				response.sendStream(new ByteArrayInputStream(binaryPlist.toByteArray()), binaryPlist.size());
				response.code(200);
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
				return new URL("http", Device.getHostIpAddress(context, true), m3u8ServerSocket.getLocalPort(), "").toString();
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
				return new URL("http", Device.getHostIpAddress(context, true), tsStreamer.getListeningPort(), "").toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}
