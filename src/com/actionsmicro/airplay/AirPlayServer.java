package com.actionsmicro.airplay;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import javax.jmdns.ServiceInfo;

import org.apache.commons.net.ntp.TimeStamp;

import vavi.apps.shairport.RTSPResponder;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.actionsmicro.airplay.crypto.EzAes;
import com.actionsmicro.airplay.crypto.FairPlay;
import com.actionsmicro.bonjour.BonjourServiceAdvertiser;
import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.PropertyListFormatException;
import com.koushikdutta.async.AsyncNetworkSocket;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataEmitterReader;
import com.koushikdutta.async.FilteredDataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.http.server.UnknownRequestBody;
import com.yutel.silver.Aika;
import com.yutel.silver.exception.AirplayException;
import com.yutel.silver.vo.Device;

public class AirPlayServer {
	private static final int AIRPLAY_MIRROR_STREAM_PORT_NUMBER = 7100;
	private static final String AIRPLAY_MODEL = "AppleTV3,1";
	private static final String AIRPLAYER_VERSION_STRING = "150.33";
	protected static final String TAG = "AirPlayServer";
	
	public interface AirPlayServerDelegate {

		void loadVideo(String url, float rate, float position);

		void stopVideo();

		int getVideoStatus();

		void seek(int position);

		void resumeVideo();

		int getVideoPosition();

		int getVideoDuration();

		void pauseVideo();

		void onStartMirroring(InetAddress remoteAddress);

		void onSpsAvailable(byte[] sps);

		void onPpsAvailable(byte[] pps);

		void onH264FrameAvailable(byte[] frame, int offset, int size, long timestamp);

		void onStopMirroring();

		void onInitalizationStart();

		void onInitalizationFinished();

		void onInitalizationFailed(Exception ex);

		void onStartAirTunes(InetAddress inetAddress);

		void onStopAirTunes();

		void onReceiveAirTunesMetadata(String albumName, String artist,
				String title);

		void onReceiveAirTunesCoverArt(byte[] byteArray);

		void onAirPlayStop();

		void onAirPlayStart();
		
	}
	private boolean stopRaopThread;
	private Thread raopThread;
	private Context context;
	private InetAddress inetAddress;
	private String name;
	private Aika airplayService;
	private AirPlayServerDelegate delegate;
	private AsyncHttpServer mirrorServer;
	protected ServerSocket servSock;	
	private BonjourServiceAdvertiser bonjourServiceAdvertiser;
	private boolean raopServiceReady;
	private boolean airplayServiceReady;
	protected RTSPResponder rtspResponder;
	private static boolean DEBUG_LOG = false;
	private static void debugLog(String msg) {
		if (DEBUG_LOG) {
			Log.d(TAG, msg);
		}
	}
	public AirPlayServer(Context context, InetAddress inetAddress, String name, AirPlayServerDelegate delegate) {
		this.context = context;
		this.inetAddress = inetAddress;
		this.name = name;
		this.delegate = delegate;
	}
	public void start() {
		if (delegate != null) {
			delegate.onInitalizationStart();
		}
		initRaopService();
		initAirPlayInThread();
	}
	private void initAirPlayInThread() {
		Thread init = new Thread(new Runnable() {

			@Override
			public void run() {
				initAirplayService();				
			}
			
		});
		init.start();
		try {
			init.join();
		} catch (InterruptedException e) {
		}
	}
	public void stop() {
		if (mirrorServer != null) {
			mirrorServer.stop();
			mirrorServer = null;
		}
		if (raopThread != null) {
			stopRaopThread = true;
			closeServerSocket();
			raopThread.interrupt();
			try {
				raopThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		stopRtspResponder();
		
		if (airplayService != null) {
			airplayService.stop();
			airplayService = null;
		}
		onAirPlayServiceDown();
	}
	private void onAirPlayServiceDown() {
		airplayServiceReady = false;
	}
	private void initAirplayService() {
		mirrorServer = new AsyncHttpServer() {
			@Override
			protected void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Log.d(TAG, "onRequest:"+request.getHeaders().getHeaders().getStatusLine());
		    }
		};
		mirrorServer.get("/stream.xml", new HttpServerRequestCallback() {

			@Override
			public void onRequest(AsyncHttpServerRequest request,
					AsyncHttpServerResponse response) {
				NSDictionary dict = new NSDictionary();
				dict.put("height", NSNumber.wrap(720));
				dict.put("width", NSNumber.wrap(1280));
				dict.put("version", "150.33");
				response.send("text/x-apple-plist+xml", dict.toXMLPropertyList());
			}
			
		});
		mirrorServer.post("/fp-setup", new HttpServerRequestCallback() {

			@Override
			public void onRequest(final AsyncHttpServerRequest request,
					final AsyncHttpServerResponse response) {
				final int contentLength = request.getHeaders().getContentLength();
				final byte body[] = new byte[contentLength];
				final AsyncHttpRequestBody requestBody = request.getBody();
				if (requestBody instanceof UnknownRequestBody) {
					((UnknownRequestBody)requestBody).setCallbacks(new DataCallback() {

						@Override
						public void onDataAvailable(DataEmitter emitter,
								ByteBufferList bb) {
							Log.d(TAG, "onDataAvailable:"+bb.remaining());						
							bb.get(body);
						}
						
					}, new CompletedCallback() {
						
						@Override
						public void onCompleted(Exception ex) {
							debugLog("onCompleted:"+body.length);						
							EndEmitter ender = EndEmitter.create(request.getSocket().getServer(), null);
							ender.setDataEmitter(request);
							ender.setEndCallback((CompletedCallback)request);
							if (body[6] == 1) {
								FairPlay.init();
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								byte responseData[] = FairPlay.setupPhase1(body, body.length, false);
								response.setContentType("application/octet-stream");
								response.getHeaders().getHeaders().add("Server", "AirTunes/150.33");
								response.sendStream(new ByteArrayInputStream(responseData), responseData.length);
							} else if (body[6] == 3) {
								byte responseData[] = FairPlay.setupPhase2(body, body.length, false);
								response.setContentType("application/octet-stream");
								response.getHeaders().getHeaders().add("Server", "AirTunes/150.33");
								response.sendStream(new ByteArrayInputStream(responseData), responseData.length);
							} else {
								response.responseCode(404);
								response.end();
							}
							response.setClosedCallback(new CompletedCallback() {

								@Override
								public void onCompleted(Exception ex) {
									Log.d(TAG, "respones closed");
								}
								
							});
						}
						
					});
				}
			}
			
		});
		mirrorServer.post("/stream", new HttpServerRequestCallback() {
			private long totalRead = 0;
			@Override
			public void onRequest(final AsyncHttpServerRequest request,
					final AsyncHttpServerResponse response) {
				totalRead = 0;
				final int contentLength = request.getHeaders().getContentLength();
				final byte body[] = new byte[contentLength];
				final AsyncHttpRequestBody requestBody = request.getBody();				
				if (requestBody instanceof UnknownRequestBody) {
					((UnknownRequestBody)requestBody).setCallbacks(new DataCallback() {


						@Override
						public void onDataAvailable(DataEmitter emitter,
								ByteBufferList bb) {
							debugLog("onDataAvailable:"+bb.remaining());						
							totalRead += bb.remaining();
							bb.get(body);
							try {
								NSDictionary streamInfo = (NSDictionary)BinaryPropertyListParser.parse(body);
								Log.d(TAG, streamInfo.toXMLPropertyList());
								byte[] aesKey = ((NSData)streamInfo.get("param1")).bytes();
								aesKey = FairPlay.decrypt(aesKey, aesKey.length);
								byte[] iv = ((NSData)streamInfo.get("param2")).bytes();
								EzAes.init(aesKey, iv);								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (PropertyListFormatException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} 
						}
						
					}, new CompletedCallback() {
						
						private DataCallback headerCallback;
						private final boolean DUMP_H264 = false;
						
						@Override
						public void onCompleted(Exception ex) {
							final DataEmitterReader headerReader = new DataEmitterReader();
							request.getSocket().setDataCallback(headerReader);
							if (delegate != null) {
								InetAddress remoteAddress = null;
								if (request.getSocket() instanceof AsyncNetworkSocket) {
									remoteAddress = ((AsyncNetworkSocket) request.getSocket()).getRemoteAddress().getAddress();
								}
								delegate.onStartMirroring(remoteAddress);
							}							
							
							headerReader.read(128, headerCallback = new DataCallback() {
								private ByteBuffer header = ByteBuffer.allocate(128);
								void logBytes(String prefix, byte[] buffer) {
									if (buffer.length >= 8) {
										debugLog(prefix+String.format("[%02x %02x %02x %02x  %02x %02x %02x %02x]", buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7]));
									} else if (buffer.length >= 4) {
										debugLog(prefix+String.format("[%02x %02x %02x %02x]", buffer[0], buffer[1], buffer[2], buffer[3]));										
									}
								}
								@Override
								public void onDataAvailable(
										DataEmitter emitter, ByteBufferList bb) {
									header.position(0);
									bb.get(header.array());
									header.order(ByteOrder.LITTLE_ENDIAN);
									final int payloadSize = header.getInt();
									final int payloadType = header.getShort();
									final int header_3 = header.getShort();
									final long timestamp = header.getLong();
									debugLog("onDataAvailable:streaming:"+"payload size:"+payloadSize+", payload type:"+payloadType+", header3:"+header_3+", timestamp:"+timestamp);
									DataEmitterReader payloadReader = new DataEmitterReader();
									request.getSocket().setDataCallback(payloadReader);
									payloadReader.read(payloadSize, new DataCallback() {
										private ByteBuffer payload = ByteBuffer.allocate(500*1024);
										private ByteBuffer h264Frame = ByteBuffer.allocate(500*1024);
										private int numberOfSps;
										private byte[] sps;
										private byte numberOfPps;
										private byte[] pps;
										private byte[] nal = {0x00, 0x00, 0x00, 0x01};
										@Override
										public void onDataAvailable(
												DataEmitter emitter,
												ByteBufferList bb) {
											try {
												payload.position(0);
												bb.get(payload.array(), 0, payloadSize);
												debugLog("onDataAvailable:streaming:"+payloadSize+" bytes of payload read");
												if (payloadType == 0) { // video bitstream
													logBytes("onDataAvailable:streaming:payload:", payload.array());
													h264Frame.position(0);			
													EzAes.decrypt(payload.array(), payloadSize, h264Frame.array());
													logBytes("onDataAvailable:streaming:h.264:", h264Frame.array());
													h264Frame.order(ByteOrder.BIG_ENDIAN);
													h264Frame.position(0);
													int length = 0;
													int offset = 0;
													while (offset < payloadSize-4) {
														length = h264Frame.getInt();
														debugLog(String.format("onDataAvailable:streaming:h.264 frame offset: %d, length:%d ", offset, length));
														if (length == 0) {
															break;
														}
														offset += 4+length;
														h264Frame.position(h264Frame.position()-4);
														h264Frame.put(nal);
														h264Frame.position(h264Frame.position()+length);
													}
													if (delegate != null) {
														debugLog("onH264FrameAvailable ntpTime:"+TimeStamp.getTime(timestamp));
														delegate.onH264FrameAvailable(h264Frame.array(), 0, payloadSize, timestamp);
													}


												} else if (payloadType == 1) { //codec data

													payload.order(ByteOrder.BIG_ENDIAN);
													payload.position(5);
													numberOfSps = payload.get()&0x1f;
													short sizeOfSps = payload.getShort();
													sps = new byte[sizeOfSps];
													payload.get(sps);
													if (delegate != null) {
														delegate.onSpsAvailable(sps);
													}
													debugLog("onDataAvailable:streaming:sps: number:"+numberOfSps+" size:"+sizeOfSps);
													logBytes("onDataAvailable:streaming:sps:", sps);

													numberOfPps = payload.get();
													short sizeOfPps = payload.getShort();
													pps = new byte[sizeOfPps];
													payload.get(pps);
													if (delegate != null) {
														delegate.onPpsAvailable(pps);
													}
													debugLog("onDataAvailable:streaming:pps: number:"+numberOfPps+" size:"+sizeOfPps);
													logBytes("onDataAvailable:streaming:pps:", pps);

												} else if (payloadType == 2) { // heartbeat

												}
												request.getSocket().setDataCallback(headerReader);
												headerReader.read(128, headerCallback);
											} catch (Exception e) {
												request.getSocket().close();
												if (delegate != null) {
													delegate.onStopMirroring();
												}
											}
										}
										
									});
									
								}
								
							});
							request.getSocket().setEndCallback(new CompletedCallback() {

								@Override
								public void onCompleted(Exception ex) {
									Log.d(TAG, "onCompleted:streaming:"+totalRead);
									if (delegate != null) {
										delegate.onStopMirroring();
									}									
								}
								
							});
						}
						
					});
				}
			}
			
		});
		mirrorServer.setErrorCallback(new CompletedCallback() {

			@Override
			public void onCompleted(Exception ex) {
				if (!airplayServiceReady) {
					informDelegateInitializationFailed(ex);
				}
			}
			
		});
		mirrorServer.listen(AIRPLAY_MIRROR_STREAM_PORT_NUMBER);
		Log.d(TAG, "Mirror server listening on "+AIRPLAY_MIRROR_STREAM_PORT_NUMBER);
		
		airplayService = Aika.create(inetAddress, 0, name);
		Device dev = new Device();
		dev.setDeviceid(getMacAddress());
		dev.setFeatures("0x100029ff");
		dev.setModel(AIRPLAY_MODEL);
		dev.setProtovers("1.0");
		dev.setSrcvers(AIRPLAYER_VERSION_STRING);
		airplayService.config(dev);
		airplayService.setConnectListener(new Aika.AikaConnectListener() {

			@Override
			public void video(String url, String rate, String pos)
					throws AirplayException {
				Log.d(TAG, "playVideo:"+url);
				delegate.loadVideo(url, Float.valueOf(rate), Float.valueOf(pos));
			}

			@Override
			public void photo() throws AirplayException {
				// TODO Auto-generated method stub
				
			}
			
		});
		airplayService.setControlListener(new Aika.AikaControlListener() {
			
			@Override
			public void videoStop() throws AirplayException {
				delegate.stopVideo();
			}
			
			@Override
			public int videoStatus() throws AirplayException {
				return delegate.getVideoStatus();
			}
			
			@Override
			public void videoSeek(int position) throws AirplayException {
				delegate.seek(position);
			}
			
			@Override
			public void videoResume() throws AirplayException {
				delegate.resumeVideo();
			}
			
			@Override
			public int videoPostion() throws AirplayException {
				return delegate.getVideoPosition();
			}
			
			@Override
			public void videoPause() throws AirplayException {
				delegate.pauseVideo();
			}
			
			@Override
			public int videoDuration() throws AirplayException {
				return delegate.getVideoDuration();
			}

			@Override
			public void onAirPlayStop() {
				delegate.onAirPlayStop();
				
			}

			@Override
			public void onAirPlayStart() {
				delegate.onAirPlayStart();
			}
		});
		if (airplayService.start()) {
			onAirplayServiceReady();
		} else {
			// TODO add more precise error message
			informDelegateInitializationFailed(null);
		}
	}
	private void informDelegateInitializationFailed(Exception e) {
		if (delegate != null) {
			delegate.onInitalizationFailed(e);
		}
	}
	private void onAirplayServiceReady() {
		airplayServiceReady = true;
		checkInitializationState();
	}
	private String getMacAddress() {
		WifiManager wim= (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		String macAddresss = wim.getConnectionInfo().getMacAddress().toUpperCase();
		return macAddresss;
	}
	private static final int RAOP_PORTNUMBER = 47000;
	
	private void initRaopService() {
		closeServerSocket();
		stopRaopThread = false;
		raopThread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				try {
					servSock = new ServerSocket();
					servSock.setReuseAddress(true);
					servSock.setSoTimeout(0);
					servSock.bind(new InetSocketAddress(RAOP_PORTNUMBER));
					registerRaopService();
					onRaopServiceReady();
					byte[] hwAddr = getHardwareAdress();
					while (!stopRaopThread && !Thread.currentThread().isInterrupted()) {
						try {
							Socket socket = servSock.accept();
							Log.d("ShairPort", "got connection from " + socket.toString());
							stopRtspResponder();
							if (delegate != null) {
								delegate.onStartAirTunes(socket.getInetAddress());
							}
							rtspResponder = new RTSPResponder(hwAddr, socket);
							rtspResponder.setAirTunesListener(new RTSPResponder.AirTunesListener() {

								@Override
								public void onDisconnected() {
									if (delegate != null) {
										delegate.onStopAirTunes();
									}
								}

								@Override
								public void onReceiveMeta(String albumName,
										String artist, String title) {
									if (delegate != null) {
										delegate.onReceiveAirTunesMetadata(albumName, artist, title);
									}
								}

								@Override
								public void onReceiveCoverArt(byte[] byteArray) {
									if (delegate != null) {
										delegate.onReceiveAirTunesCoverArt(byteArray);
									}
								}
								
							});
							rtspResponder.start();
						} catch(SocketTimeoutException e) {
							// ignore
						}
					}
				} catch (IOException e) {
					if (!raopServiceReady) {
						informDelegateInitializationFailed(e);
					}
				} finally {
					cleanUpMdns();
					closeServerSocket();
					onRaopServiceDown();
				}
			}
			private byte[] getHardwareAdress() {
				byte[] hwAddr = null;
				
				InetAddress local;
				try {
					local = InetAddress.getLocalHost();
					NetworkInterface ni = NetworkInterface.getByInetAddress(local);
					if (ni != null) {
						WifiManager wim= (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
						String[] as = wim.getConnectionInfo().getMacAddress().split(":");
		                hwAddr = new byte[as.length];
		                int i = 0;
		                for (String a : as) {
		                    hwAddr[i++] = Integer.valueOf(a, 16).byteValue();
		                }
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					e.printStackTrace();
				}
				return hwAddr;
			}
		});
		raopThread.start();
	}
	protected void onRaopServiceDown() {
		raopServiceReady = false;
	}
	protected void onRaopServiceReady() {
		raopServiceReady = true;
		checkInitializationState();
	}
	private void checkInitializationState() {
		if (raopServiceReady && airplayServiceReady) {
			if (delegate != null) {
				delegate.onInitalizationFinished();
			}
		}
	}
	private void closeServerSocket() {
		if (servSock != null) {
			try {
				servSock.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			servSock = null;
		}
	}
	public void sendEvent() {
		if (airplayService != null) {
			airplayService.sendEvent();
		}
	}
	private void registerRaopService() {
		try {
			String macAddressWithoutCol = getMacAddress().replace(":", "");
			HashMap<String, String> txt = new HashMap<String, String>();					
			txt.put("txtvers", "1");
			txt.put("ch", "2");
			txt.put("cn", "0,1,2,3");
			txt.put("da", "true");
			txt.put("et", "0,3,5");
			txt.put("md", "0,1,2");
			txt.put("pw", "false");
			txt.put("sv", "false");
			txt.put("sr", "44100");
			txt.put("ss", "16");
			txt.put("tp", "UDP");
			txt.put("vn", "65537");
			txt.put("vs", AIRPLAYER_VERSION_STRING);
			txt.put("rmodel", "EZAir1,1");
			txt.put("am", AIRPLAY_MODEL);
			txt.put("sf", "0x4");
			bonjourServiceAdvertiser = new BonjourServiceAdvertiser(ServiceInfo.create("_raop._tcp.local.", macAddressWithoutCol+"@"+name, RAOP_PORTNUMBER, 0, 0, txt));
			bonjourServiceAdvertiser.register();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	private void cleanUpMdns() {
		if (bonjourServiceAdvertiser != null) {
			final BonjourServiceAdvertiser bonjour = this.bonjourServiceAdvertiser;
			new Thread(new Runnable() {

				@Override
				public void run() {
					bonjour.unregister();
					bonjour.close();
				}

			}).start();
			bonjourServiceAdvertiser = null;
		}		
	}
	private void stopRtspResponder() {
		if (rtspResponder != null) {
			rtspResponder.stopThread();
			rtspResponder = null;
		}
	}
	static class EndEmitter extends FilteredDataEmitter {
        private EndEmitter() {
        }
        
        public static EndEmitter create(AsyncServer server, final Exception e) {
            final EndEmitter ret = new EndEmitter();
            // don't need to worry about any race conditions with post and this return value
            // since we are in the server thread.
            server.post(new Runnable() {
                @Override
                public void run() {
                    ret.report(e);
                }
            });
            return ret;
        }
    }
	public void closeAirTunesConnection() {
		stopRtspResponder();
	}
	public void closeAirPlayConnection() {
		if (airplayService != null) {
			airplayService.closeCurrentConnection();
		}
		if (mirrorServer != null) {
			mirrorServer.stop();
			mirrorServer.listen(AIRPLAY_MIRROR_STREAM_PORT_NUMBER);
		}
	}
}
