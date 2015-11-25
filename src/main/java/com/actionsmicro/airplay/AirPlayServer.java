package com.actionsmicro.airplay;

import android.content.Context;

import com.actionsmicro.airplay.crypto.EzAes;
import com.actionsmicro.airplay.crypto.FairPlay;
import com.actionsmicro.bonjour.BonjourServiceAdvertiser;
import com.actionsmicro.utils.Log;
import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.PropertyListFormatException;
import com.koushikdutta.async.AsyncNetworkSocket;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
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

import org.apache.commons.net.ntp.TimeStamp;

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
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;

import javax.jmdns.ServiceInfo;

import vavi.apps.shairport.RTSPResponder;

public class AirPlayServer {
	private static final int AIRPLAY_MIRROR_STREAM_PORT_NUMBER = 7100;
	public static final String AIRPLAY_MODEL = "AppleTV3,2"; // "AppleTV3,1"
	public static final String AIRPLAYER_VERSION_STRING = "220.68"; //150.33
	protected static final String TAG = "AirPlayServer";
	public static byte[] mEdPublicKey;
	public static byte[] mEdSecretKey;
	public static int eventPort;
	// TODO to check if can use state instead of flag
	public static boolean isMirroring = false;
	public static boolean isStreaming = false;

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

		void setVolume(float volume);

		void displayPhoto(byte[] jpeg, String assetKey, String transition);

		boolean displayCached(String assetKey, String transition);

		void cachePhoto(String assetKey, byte[] jpeg);
		
	}
	private boolean stopRaopThread;
	private boolean stopEventThread;
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
	// TODO create event rtp server
	private Thread mEventThread;
	protected ServerSocket eventServSock;

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
		initKey();
		initEventService();
		initRaopService();
		initAirPlayInThread();
	}

	private void initKey() {
		Log.d("ShairPort", "init key");
		int length = 32;
		SecureRandom random = new SecureRandom();
		mEdPublicKey = new byte[length];
		mEdSecretKey = new byte[length];
		byte[] seed = random.generateSeed(length);
		EzAes.sub00000000x7(mEdPublicKey, mEdSecretKey, seed);

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
		stopEventService();
		
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
			protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Log.d(TAG, "onRequest:"+request.getPath());
				return false;
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
								response.getHeaders().add("Server", "AirTunes/" +AIRPLAYER_VERSION_STRING);
								response.sendStream(new ByteArrayInputStream(responseData), responseData.length);
							} else if (body[6] == 3) {
								byte responseData[] = FairPlay.setupPhase2(body, body.length, false);
								response.setContentType("application/octet-stream");
								response.getHeaders().add("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
								response.sendStream(new ByteArrayInputStream(responseData), responseData.length);
							} else {
								response.code(404);
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
														delegate.onH264FrameAvailable(h264Frame.array(), 0, payloadSize, TimeStamp.getTime(timestamp));
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
		mirrorServer.setCustomDataCallBack(new DataCallback() {
			private DataCallback headerCallback;
			final DataEmitterReader headerReader = new DataEmitterReader();

			@Override
			public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
				Log.d(TAG, " receive data................................ ");
				AsyncSocket socket = mirrorServer.getServerSocket();
				final ByteBuffer header = ByteBuffer.allocate(128);
				final ByteBuffer payload = ByteBuffer.allocate(500 * 1024);
				final ByteBuffer h264Frame = ByteBuffer.allocate(500 * 1024);

				if (delegate != null) {
					InetAddress remoteAddress = null;
					if (socket instanceof AsyncNetworkSocket) {
						remoteAddress = ((AsyncNetworkSocket) socket).getRemoteAddress().getAddress();
					}
					delegate.onStartMirroring(remoteAddress);
					isMirroring = true;
				}
				socket.setEndCallback(new CompletedCallback() {

					@Override
					public void onCompleted(Exception ex) {
						Log.d("dddd", "mirror onCompleted:streaming");
						if (delegate != null /*&& !isMirroring*/) {
							delegate.onStopMirroring();
						}
//						isMirroring = false;
					}

				});

				mirrorServer.getServerSocket().setDataCallback(headerReader);
				headerReader.read(128, headerCallback = new DataCallback() {

					void logBytes(String prefix, byte[] buffer) {
						if (buffer.length >= 8) {
							debugLog(prefix + String.format("[%02x %02x %02x %02x  %02x %02x %02x %02x]", buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7]));
						} else if (buffer.length >= 4) {
							debugLog(prefix + String.format("[%02x %02x %02x %02x]", buffer[0], buffer[1], buffer[2], buffer[3]));
						}
					}

					@Override
					public void onDataAvailable(
							DataEmitter emitter, ByteBufferList bb) {
						if (isStreaming) {
							return;
						}
						header.position(0);
						bb.get(header.array());
						header.order(ByteOrder.LITTLE_ENDIAN);
						final int payloadSize = header.getInt();
						final int payloadType = header.getShort();
						final int header_3 = header.getShort();
						final long timestamp = header.getLong();
						debugLog("onDataAvailable:streaming:"+"payload size:"+payloadSize+", payload type:"+payloadType+", header3:"+header_3+", timestamp:"+timestamp);
						DataEmitterReader payloadReader = new DataEmitterReader();
						mirrorServer.getServerSocket().setDataCallback(payloadReader);
						payloadReader.read(payloadSize, new DataCallback() {
							private int numberOfSps;
							private byte[] sps;
							private byte numberOfPps;
							private byte[] pps;
							private byte[] nal = {0x00, 0x00, 0x00, 0x01};
							@Override
							public void onDataAvailable(
									DataEmitter emitter,
									ByteBufferList bb) {
								if (isStreaming) {
									return;
								}
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
										if (delegate != null && !isStreaming) {
											debugLog("onH264FrameAvailable ntpTime:" + TimeStamp.getTime(timestamp));
											delegate.onH264FrameAvailable(h264Frame.array(), 0, payloadSize, TimeStamp.getTime(timestamp));
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
									mirrorServer.getServerSocket().setDataCallback(headerReader);
									headerReader.read(128, headerCallback);
								} catch (Exception e) {
									mirrorServer.getServerSocket().close();
									if (delegate != null) {
										delegate.onStopMirroring();
									}
								}
							}

						});

					}

				});
			}
		});
		mirrorServer.listen(AIRPLAY_MIRROR_STREAM_PORT_NUMBER);
		Log.d(TAG, "Mirror server listening on "+AIRPLAY_MIRROR_STREAM_PORT_NUMBER);
		
		airplayService = Aika.create(inetAddress, 0, name);
        Device dev = new Device();
		dev.setDeviceid(getMacAddress());
//		dev.setFeatures("0x5A7FFFF7,0x1E");
		dev.setFeatures("0x0A7FEFF7");
		dev.setModel(AIRPLAY_MODEL);
		dev.setProtovers("2.0");
		dev.setSrcvers(AIRPLAYER_VERSION_STRING);
		String pkString = "";
		for (int i = 0; i < 32; i++) {
			pkString += String.format("%02x", mEdPublicKey[i]);
		}
		dev.setPk(pkString);
		airplayService.config(dev);
		airplayService.setConnectListener(new Aika.AikaConnectListener() {

			@Override
			public byte[] pairSetup() {
				return mEdPublicKey;
			}

			@Override
			public byte[] pairVerify(byte[] requestBody) {

				if (requestBody[0] == 1) {
					int length = 32;
					byte[] controllerPublicKey = new byte[length];
					byte[] controllerSignature = new byte[length];
					byte[] out = new byte[96];
					for (int i = 0; i < length; i++) {
						controllerPublicKey[i] = requestBody[i + 4];
						controllerSignature[i] = requestBody[i + 4 + 32];
					}

					EzAes.airplayPairVerify(mEdPublicKey, mEdSecretKey, controllerPublicKey, controllerSignature, out);
					return out;
				}
				return new byte[0];
			}

			@Override
			public void video(String url, String rate, String pos)
					throws AirplayException {
				Log.d(TAG, "playVideo:"+url);
				isStreaming = true;
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
				isStreaming = false;
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

			@Override
			public void setVolume(float volume) {
				delegate.setVolume(volume);
			}

			@Override
			public void displayPhoto(byte[] jpeg, String assetKey, String transition) {
				delegate.displayPhoto(jpeg, assetKey, transition);
			}

			@Override
			public boolean displayCached(String assetKey, String transition) {
				return delegate.displayCached(assetKey, transition);
			}

			@Override
			public void cachePhoto(String assetKey, byte[] jpeg) {
				delegate.cachePhoto(assetKey, jpeg);
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
		return com.actionsmicro.utils.Device.getAppMacAddress(context);
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
										delegate.onStopMirroring();
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
		                try {
		                	String[] as = com.actionsmicro.utils.Device.getAppMacAddress(context).split(":");
			                hwAddr = new byte[as.length];
			                int i = 0;
			                for (String a : as) {
			                    hwAddr[i++] = Integer.valueOf(a, 16).byteValue();
			                }
			            } catch (Exception e) {
			            	e.printStackTrace();
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

	private void closeEventSocket() {
		if (eventServSock != null) {
			try {
				eventServSock.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			eventServSock = null;
		}
	}

	public void sendEvent() {
		if (airplayService != null) {
			airplayService.sendEvent();
		}
	}
    private void registerRaopService() {
        try {
//  ref: https://nto.github.io/AirPlay.html#servicediscovery-airtunesservice
			/*name: 5855CA1AE288@Apple TV
			type: _raop._tcp
			port: 49152
			txt:
			txtvers=1
			ch=2
			cn=0,1,2,3
			da=true
			et=0,3,5
			md=0,1,2
			pw=false
			sv=false
			sr=44100
			ss=16
			tp=UDP
			vn=65537
			vs=130.14
			am=AppleTV2,1
			sf=0x4*/
            String macAddressWithoutCol = getMacAddress().replace(":", "");
            HashMap<String, String> txt = new HashMap<String, String>();
            txt.put("txtvers", "1");
            txt.put("ch", "2");
            txt.put("cn", "0,1,2,3");
            txt.put("da", "true");
            txt.put("et", "0,3,5");
            txt.put("md", "0,1,2");
//            txt.put("pw", "false");
            txt.put("sv", "false");
            txt.put("sr", "44100");
            txt.put("ss", "16");
            txt.put("tp", "UDP");
            txt.put("vn", "65537");
            txt.put("vs", AIRPLAYER_VERSION_STRING);
            txt.put("rmodel", "EZAir1,1");
            txt.put("am", AIRPLAY_MODEL);
            txt.put("sf", "0x4");
			txt.put("flags", "0x4");
            // TODO

			String pkString = "";
			for (int i = 0; i < 32; i++) {
				pkString += String.format("%02x", mEdPublicKey[i]);
			}
			Log.d(TAG,"pkString -------------------" + pkString);
            txt.put("pk", pkString);
			txt.put("pi", "b08f5a79-db29-4384-b456-a4784d9e6055");
			txt.put("vv", "2");
            bonjourServiceAdvertiser = new BonjourServiceAdvertiser(ServiceInfo.create("_raop._tcp.local.", macAddressWithoutCol.toUpperCase(Locale.getDefault()) + "@" + name, RAOP_PORTNUMBER, 0, 0, txt));
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

	private void initEventService() {
		stopEventThread = false;
		closeEventSocket();
		mEventThread = new Thread() {
			@Override
			public void run() {
				try {
					eventServSock = new ServerSocket(0);
					eventPort = eventServSock.getLocalPort();
					Log.d("DDDD", "eventPort = " + eventPort);
					eventServSock.setReuseAddress(true);
					eventServSock.setSoTimeout(0);
					while (!stopEventThread && !Thread.currentThread().isInterrupted()) {
						Log.d("DDDD", "waiting event");
						Socket socket = eventServSock.accept();
						Log.d("DDDD", "got event connection from " + socket.toString());
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					closeEventSocket();
				}
			}
		};
		mEventThread.start();
	}

	private void stopEventService() {
		Log.d("dddd", "STOP event service");
		if(mEventThread!=null) {
			stopEventThread = true;
			closeEventSocket();
			mEventThread.interrupt();
			try {
				mEventThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
