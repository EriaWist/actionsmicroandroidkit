package com.actionsmicro.airplay;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.net.ntp.TimeStamp;
import org.xml.sax.SAXException;

import vavi.apps.shairport.UDPDelegate;
import vavi.apps.shairport.UDPListener;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import com.actionsmicro.airplay.airtunes.AirTunesClock;
import com.actionsmicro.airplay.http.PlistBody;
import com.actionsmicro.airplay.mirror.AvcEncoder;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;
import com.actionsmicro.web.SimpleContentUriHttpFileServer;
import com.dd.plist.BinaryPropertyListWriter;
import com.dd.plist.NSArray;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.XMLPropertyListParser;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.DownloadCallback;
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
	private HandlerThread timerThread;
	private Handler timerHandler;
	private VideoStateListener videoStateListener;
	private String sessionId = UUID.randomUUID().toString();
	private AsyncHttpClient asyncHttpClient;
	
	private SimpleContentUriHttpFileServer simpleHttpFileServer;
	private Context context;
	private List<ConnectionManager> managers = new ArrayList<ConnectionManager>();
	private AvcEncoder avcEncoder;
	protected FileOutputStream avcOut;
	private DatagramSocket tsock;
	private UDPListener timingPortListener;
	private Cipher cipher;
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
		initializeAvcEncoder();
		
		try {
			tsock = new DatagramSocket(7010);
			timingPortListener = new UDPListener(tsock, new UDPDelegate() {

				@Override
				public void packetReceived(DatagramSocket socket,
						DatagramPacket packet) {
					
					
				}
				
			});
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			cipher = Cipher.getInstance("AES/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		fp_setup();
//		postStream();
	}
	private static final byte[] nalHeader = {0x00,0x00,0x00,0x01};
	private void encrypt(byte[] input, int offset, byte[] output) {
		int i = offset;
 		for (; i+16 <= input.length; i += 16) {
 			try {
 				cipher.update(input, i, 16, output, i - offset);
 				
 			} catch (ShortBufferException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 		}
 	// The rest of the packet is unencrypted
		for (int k = 0; k<((input.length-offset) % 16); k++){
			output[i-offset+k] = input[i+k];
		}
		
	}
	private byte[] encryptBuffer = new byte[1024*1024];
	
	private void initializeAvcEncoder() {
		try {
			avcEncoder = new AvcEncoder();
			avcEncoder.setParameterSetsListener(new AvcEncoder.ParameterSetsListener() {
				@Override
				public void avcParametersSetsEstablished(byte[] sps, byte[] pps) {
					try {
						
						
						
						ByteBuffer payload = ByteBuffer.allocate(11+sps.length+pps.length);
						payload.order(ByteOrder.BIG_ENDIAN);
						payload.put((byte)1);
						payload.put((byte)100);
						payload.put((byte)0xc0);
						payload.put((byte)40);
						payload.put((byte)0xff);
						payload.putShort((short) sps.length);
						encrypt(sps, 0, encryptBuffer);
						payload.put(encryptBuffer, 0, sps.length);
						payload.put((byte)1);
						payload.putShort((short) pps.length);
						encrypt(pps, 0, encryptBuffer);
						payload.put(encryptBuffer, 0, pps.length);
						
						header.clear();
						header.order(ByteOrder.LITTLE_ENDIAN);
						header.putInt(payload.position());
						header.putShort((short) 1);
						header.putShort((short) 6);
						header.putLong(TimeStamp.getCurrentTime().ntpValue());
						pipedOutputStream.write(header.array());
						pipedOutputStream.write(payload.array(), 0, payload.position());
						
//						avcOut = new FileOutputStream("/sdcard/testavcencoder.h264");
						if (avcOut != null) {
							avcOut.write(nalHeader);
							avcOut.write(sps);
							avcOut.write(nalHeader);
							avcOut.write(pps);
						}
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			avcEncoder.setFrameListener(new AvcEncoder.EncodedFrameListener() {
				@Override
				public void frameReceived(byte[] outData, int index, int length) {
					try {
						//						
						header.clear();
						header.order(ByteOrder.LITTLE_ENDIAN);
						header.putInt(length - 4);
						header.putShort((short) 0);
						header.putShort((short) 6);
						header.putLong(TimeStamp.getCurrentTime().ntpValue());
						pipedOutputStream.write(header.array());
						encrypt(outData, 4, encryptBuffer);
						pipedOutputStream.write(encryptBuffer, 0, length - 4);

						if (avcOut != null) {
							avcOut.write(nalHeader);
							avcOut.write(outData, index, length);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			});
		} catch (Throwable t) {
			
		}
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
							notifyConnectionManagerDidFailed(e);
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
							notifyConnectionManagerDidFailed(e);
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
		if (avcEncoder != null) {
			try {
				avcEncoder.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (avcOut != null) {
			try {
				avcOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (pipedOutputStream != null) {
			try {
				pipedOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pipedOutputStream = null;
		}
		if (timingPortListener != null) {
			timingPortListener.stopThread();
			timingPortListener = null;
		}
		if (tsock != null) {
			tsock.close();
			tsock = null;
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
		notifyConnectionManagerDidFailed(e);
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
//		
//		if (semaphore.tryAcquire()) {
//			Log.d(TAG, "clone jpeg buffer");
//			synchronized (jpegBuffer) {
//				jpegBuffer.reset();
//				try {
//					Utils.dump(jpegStream, jpegBuffer);
//					jpegBuffer.notify();
//				} catch (IOException e1) {
//					e1.printStackTrace();
//					jpegBuffer.reset();
//				}
//			}
//			Log.d(TAG, "send /photo request");
//			RawHeaders headers = new RawHeaders();
//			headers.add("User-Agent", USER_AGENT_STRING);
//			headers.add("X-Apple-AssetAction", "cacheOnly");
//			headers.add("X-Apple-Session-ID", getSessionId());
//			final String photoId = UUID.randomUUID().toString();
//			headers.add("X-Apple-AssetKey", photoId);
//			headers.add("Content-Length", String.valueOf(jpegBuffer.size()));
//			try {
//				AsyncHttpRequest putPhoto = new AsyncHttpRequest(getServerUri("/photo"), "PUT", headers);
//				StreamBody jpegDataBody = new StreamBody(new ByteArrayInputStream(jpegBuffer.toByteArray()), jpegBuffer.size());
//				putPhoto.setBody(jpegDataBody);
//				getHttpClient().executeString(putPhoto, new StringCallback() {
//
//					@Override
//					public void onCompleted(Exception e, AsyncHttpResponse source,
//							String result) {
//						Log.d(TAG, "send /photo request - cache complete\n"+source.getHeaders().getHeaders().getResponseMessage());
//						semaphore.release();									
//						
//						RawHeaders headers = new RawHeaders();
//						headers.add("User-Agent", USER_AGENT_STRING);
//						headers.add("X-Apple-AssetAction", "displayCached");
//						headers.add("X-Apple-Session-ID", getSessionId());
//						headers.add("X-Apple-AssetKey", photoId);
//						headers.add("Content-Length", "0");
//						headers.add("X-Apple-Transition", "None");
//						
//						try {
//							AsyncHttpRequest putPhoto = new AsyncHttpRequest(getServerUri("/photo"), "PUT", headers);
//							getHttpClient().executeString(putPhoto, new StringCallback() {
//
//								@Override
//								public void onCompleted(Exception e,
//										AsyncHttpResponse source, String result) {
//									Log.d(TAG, "send /photo request - display cached complete\n"+source.getHeaders().getHeaders().getResponseMessage());
//								}
//								
//							});
//						
//						} catch (URISyntaxException e1) {
//							// TODO Auto-generated catch block
//							e1.printStackTrace();
//						}
//						
//					}
//
//				});
//			} catch (URISyntaxException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} else {
//			Log.d(TAG, "send /photo busy");
//		}
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
	public void displayYuvImage(byte[] yuvData) {
		if (pipedOutputStream != null) {
			if (avcEncoder != null) {
				avcEncoder.offerEncoder(yuvData);
			}
		}
	}
	private byte[] fp_setup_1 = {0x46, 0x50, 0x4C, 0x59, 0x03, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x04, 0x02, 0x00, 0x01, (byte) 0xBB}; 
	private byte[] fp_setup_2 = {0x46, 0x50, 0x4C, 0x59, 0x03, 0x01, 0x03, 0x00, 0x00, 0x00, 0x00, (byte) 0x98, 0x01, (byte) 0x8F, 0x1A, (byte) 0x9C, 
								 0x0D, 0x6D, 0x04, (byte) 0x85, (byte) 0xD0, (byte) 0xFA, (byte) 0xF2, (byte) 0x94, (byte) 0xE7, 0x6C, (byte) 0xC4, (byte) 0x84, 0x58, 0x40, 0x22, (byte) 0xDD,
								 (byte) 0xE8, (byte) 0xC2, 0x07, 0x0B, (byte) 0x80, 0x0D, 0x46, (byte) 0xED, (byte) 0x94, (byte) 0xDB, 0x27, (byte) 0xA1, (byte) 0xF0, 0x03, (byte) 0xF9, (byte) 0xCD,
								 (byte) 0xE2, (byte) 0xF9, (byte) 0xF3, 0x3A, (byte) 0x94, (byte) 0xB3, 0x5B, 0x46, (byte) 0xAE, 0x68, 0x37, (byte) 0x91, (byte) 0xC5, 0x2A, (byte) 0xF0, 0x14,
								 0x46, (byte)0x88, (byte)0xEE, (byte)0xEB, (byte)0xE0, (byte)0xDF, (byte)0xF4, 0x74, (byte)0xF4, 0x77, (byte)0xB5, (byte)0xC8, 0x6D, 0x30, 0x28, (byte)0xF7,
								 0x50, (byte)0xE1, 0x1A, 0x48, 0x23, 0x06, (byte)0x9C, (byte)0xE6, 0x17, (byte)0xB2, 0x61, (byte)0xF9, 0x4B, 0x2D, 0x29, (byte)0xB3,
								 0x3D, 0x13, (byte)0xF7, 0x15, 0x5E, (byte)0xA4, 0x14, (byte)0x92, 0x6D, 0x2C, 0x29, 0x2D, 0x00, 0x51, (byte)0xD5, (byte)0x9B,
								 (byte)0xA1, 0x49, 0x3D, (byte)0xA3, (byte)0xD8, (byte)0xC6, 0x1C, (byte)0xC0, 0x0B, (byte)0xAE, 0x23, 0x57, 0x2D, 0x5D, (byte)0xB7, 0x11,
								 (byte)0xBD, (byte)0xC5, (byte)0x91, (byte)0x86, 0x5E, 0x29, (byte)0xC0, 0x69, (byte)0xB1, 0x20, (byte)0xA3, 0x38, (byte)0xDE, (byte)0xDA, 0x1F, 0x6F,
								 0x4E, (byte)0x89, (byte)0xDC, (byte)0xB6, (byte)0x8F, (byte)0x8E, (byte)0x9E, (byte)0xF5, (byte)0x94, 0x5E, (byte)0x89, 0x4B, 0x1D, 0x58, (byte)0xDF, (byte)0x8A,
								 (byte)0xEF, 0x2A, (byte)0xF9, (byte)0xDC};
	private boolean fp_setup_ready;
	private byte[] encryptedAesKey = {(byte)0x46, (byte)0x50, (byte)0x4C, (byte)0x59, (byte)0x01, (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x3C, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
								(byte)0xBD, (byte)0xC3, (byte)0x3A, (byte)0xF6, (byte)0x98, (byte)0x01, (byte)0xD7, (byte)0xE4, (byte)0xE2, (byte)0xA4, (byte)0x49, (byte)0x40, (byte)0x11, (byte)0x12, (byte)0xD4, (byte)0x96,
								(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0xE7, (byte)0x6E, (byte)0x91, (byte)0xC1, (byte)0xDE, (byte)0x46, (byte)0xCD, (byte)0x55, (byte)0x90, (byte)0x90, (byte)0x2A, (byte)0x70,
								(byte)0x51, (byte)0x08, (byte)0x7E, (byte)0x7F, (byte)0x3B, (byte)0xED, (byte)0xD1, (byte)0xDA, (byte)0xC7, (byte)0x17, (byte)0xDB, (byte)0xAD, (byte)0x76, (byte)0x43, (byte)0x71, (byte)0x85,
								(byte)0x90, (byte)0x97, (byte)0xF2, (byte)0xFD, (byte)0x71, (byte)0xBC, (byte)0x71, (byte)0x94};
	private byte[] aesKey = {(byte)0xBE, (byte)0x54, (byte)0xF7, (byte)0x2B, (byte)0x26, (byte)0xCF, (byte)0x92, (byte)0x79, (byte)0xC0, (byte)0xBD, (byte)0x86, (byte)0x90, (byte)0x53, (byte)0xDC, (byte)0x75, (byte)0x29, (byte)0xBD, 
							 (byte)0xC3, (byte)0x3A, (byte)0xF6, (byte)0x98, (byte)0x01, (byte)0xD7, (byte)0xE4, (byte)0xE2, (byte)0xA4, (byte)0x49, (byte)0x40, (byte)0x11, (byte)0x12, (byte)0xD4, (byte)0x96, (byte)0x00, (byte)0x00,
							 (byte)0x00, (byte)0x10, (byte)0xE7, (byte)0x6E, (byte)0x91, (byte)0xC1, (byte)0xDE, (byte)0x46, (byte)0xCD, (byte)0x55, (byte)0x90, (byte)0x90, (byte)0x2A, (byte)0x70, (byte)0x51, (byte)0x08, (byte)0x7E,
							 (byte)0x7F, (byte)0x3B, (byte)0xED, (byte)0xD1, (byte)0xDA, (byte)0xC7, (byte)0x17, (byte)0xDB, (byte)0xAD, (byte)0x76, (byte)0x43, (byte)0x71, (byte)0x85, (byte)0x90, (byte)0x97, (byte)0xF2, (byte)0xFD,
							 (byte)0x71, (byte)0xBC, (byte)0x71, (byte)0x94};
	private byte[] iv = {(byte)0xAF, (byte)0x14, (byte)0x0F, (byte)0x72, (byte)0xB3, (byte)0x18, (byte)0xC5, (byte)0x4B, (byte)0xC6, (byte)0x04, (byte)0xD0, (byte)0x87, (byte)0x66, (byte)0x85, (byte)0x89, (byte)0xA6};
	private PipedOutputStream pipedOutputStream;
	private void fp_setup() {
		fp_setup(fp_setup_1, new DownloadCallback() {

			@Override
			public void onCompleted(Exception e, AsyncHttpResponse source,
					ByteBufferList result) {
				byte[] test = new byte[result.remaining()];
				result.get(test);
				fp_setup(fp_setup_2, new DownloadCallback() {


					@Override
					public void onCompleted(Exception e, AsyncHttpResponse source,
							ByteBufferList result) {
						byte[] test2 = new byte[result.remaining()];
						result.get(test2);
						fp_setup_ready = true;
						postStream();
					}

				});
			}

		});
	}
	private void fp_setup(byte[] fp_setup_input, DownloadCallback callback) {
		try {
			RawHeaders headers = new RawHeaders();
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("Content-Type", "application/octet-stream");
			AsyncHttpRequest fp_setup = new AsyncHttpRequest(getServerUri("/fp-setup"), "POST", headers);
			StreamBody streamBody = new StreamBody(new ByteArrayInputStream(fp_setup_input), fp_setup_input.length);
			fp_setup.setBody(streamBody);
			getHttpClient().executeByteBufferList(fp_setup, callback);
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			
		}
	}
	private static NSDictionary disctionaryWithKeyAndValue(String key, String value) {
		NSDictionary dict = new NSDictionary();
		dict.put(key, value);
		return dict;
	}
	private ByteBuffer header = ByteBuffer.allocate(128);
	private void postStream() {
		try {
			
			NSDictionary streamHeader = generateStreamXmlHeader();
			ByteArrayOutputStream binaryPlist = new ByteArrayOutputStream();
			BinaryPropertyListWriter.write(binaryPlist, streamHeader);
			
			RawHeaders headers = new RawHeaders();
			headers.add("User-Agent", USER_AGENT_STRING);
			headers.add("X-Apple-Device-ID", "0x872DB87");
			headers.add("Content-Length", String.valueOf(binaryPlist.size()));
			AsyncHttpRequest postStream = new AsyncHttpRequest(getServerUri("/stream", 7100), "POST", headers);
			PipedOutputStream pipedOutputStream = new PipedOutputStream();
			postStream.setBody(new StreamBody(new PipedInputStream(pipedOutputStream, 1024*1024), -1));
			Utils.dump(new ByteArrayInputStream(binaryPlist.toByteArray(), 0, binaryPlist.size()), pipedOutputStream);
			this.pipedOutputStream = pipedOutputStream;
			getHttpClient().executeString(postStream, new StringCallback() {

				@Override
				public void onCompleted(Exception e, AsyncHttpResponse source,
						String result) {
					// TODO Auto-generated method stub

				}

			});
			
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			
		}
	}
	private NSDictionary generateStreamXmlHeader() {
		NSDictionary streamHeader = new NSDictionary();
		streamHeader.put("sessionID", 2094878580);
		
		NSArray fpsInfo = new NSArray(
				disctionaryWithKeyAndValue("name", "SubS"),
				disctionaryWithKeyAndValue("name", "B4En"),
				disctionaryWithKeyAndValue("name", "EnDp"),
				disctionaryWithKeyAndValue("name", "IdEn"),
				disctionaryWithKeyAndValue("name", "IdDp"),
				disctionaryWithKeyAndValue("name", "EQDp"),
				disctionaryWithKeyAndValue("name", "QueF"),
				disctionaryWithKeyAndValue("name", "Sent"));
		
		streamHeader.put("fpsInfo", fpsInfo);
		streamHeader.put("macAddress", "80:EA:96:3B:C6:1C");
		streamHeader.put("version", "200.54");
		streamHeader.put("channel", 1);
		streamHeader.put("connectTime", 0.007103979587554932);
		streamHeader.put("querySRVTime", 0.001972019672393799);
		streamHeader.put("prepareTime", 0.005765020847320557);
		streamHeader.put("querySRVTime", 0.001972019672393799);
		streamHeader.put("param2", new NSData(iv));
		streamHeader.put("deviceInfoTime", -4.31323781260842E8);
		streamHeader.put("authTime", 4.31323781485719E8);
		NSArray timestampInfo = new NSArray(
				disctionaryWithKeyAndValue("name", "SubSu"),
				disctionaryWithKeyAndValue("name", "BePxT"),
				disctionaryWithKeyAndValue("name", "AfPxT"),
				disctionaryWithKeyAndValue("name", "BefEn"),
				disctionaryWithKeyAndValue("name", "EmEnc"),
				disctionaryWithKeyAndValue("name", "QueFr"),
				disctionaryWithKeyAndValue("name", "SndFr")
				);
		streamHeader.put("timestampInfo", timestampInfo);
		streamHeader.put("param1", new NSData(encryptedAesKey));
		streamHeader.put("deviceID", 141745031);
		streamHeader.put("resolveDNSTime", 0.001932978630065918);
		streamHeader.put("configTime", 0.003574967384338379);
		streamHeader.put("latencyMs", 90);
		return streamHeader;
	}

}
