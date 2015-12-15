package com.actionsmicro.androidrx;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.actionsmicro.androidkit.ezcast.imp.androidrx.AndroidRxFinder;
import com.actionsmicro.bonjour.BonjourServiceAdvertiser;
import com.actionsmicro.utils.CipherUtil;
import com.actionsmicro.utils.Log;
import com.actionsmicro.web.JsonRpcOverHttpServer;
import com.koushikdutta.async.AsyncNetworkSocket;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataEmitterReader;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.NotificationHandler;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

import org.apache.commons.net.ntp.TimeStamp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.ServiceInfo;

public class EzScreenServer {
	private int mMirrorPort;
	private byte[] mAesKey;
	private Long mNtpServerPort;
	private static String mPredefinedKey = "SCREEN21SCREEN90SCREEN23SCREEN43";
	private byte[] mAesIV;
	private boolean mMirrorServiceReady = false;

	public interface EzScreenServerDelegate {

		void displayUrl(String url);

		void onConnected();

		void onDisconnected();

		void stopDisplay();

		void playVideo(String url, String callbackUrl);

		void seek(int position);

		void stopVideo();

		void decreaseVolume();

		void increaseVolume();

		void resumeVideo();

		void pauseVideo();

		void onInitializationStart();

		void onInitializationFinished();

		void onInitializationFailed(Exception e);

		void onStartMirroring(InetAddress remoteAddress, int ntpPort);

		void onStopMirroring();

		void onSpsAvailable(byte[] sps);

		void onPpsAvailable(byte[] pps);

		void onH264FrameAvailable(byte[] frame, int offset, int size, long timestamp);
		
	}
	protected static final String TAG = "EzScreenServer";
	private EzScreenServerDelegate ezScreenServerDelegate;
	private String name;
	private Context context;
	private JsonRpcOverHttpServer jsonRpcOverHttpServer;
	private InetAddress inetAddress;
	private String deviceID;
	private boolean mmr;
	private boolean wmr;

	public EzScreenServer(Context context, InetAddress inetAddress, String name, String deviceID, boolean mmr, boolean wmr, EzScreenServerDelegate delegate) {
		this.name = name;
		this.context = context;
		this.inetAddress = inetAddress;
		this.ezScreenServerDelegate = delegate;
		this.deviceID = deviceID;
		this.mmr=mmr;
		this.wmr=wmr;
	}
	public String getName() {
		return name;
	}
	private Handler mainHandler = new Handler(Looper.getMainLooper());
	private Runnable resetToStandby = new Runnable() {

		@Override
		public void run() {
			ezScreenServerDelegate.onDisconnected();
		}
		
	};
	private static final int HEARTBEAT_TIMEOUT = 13000;
	
	private BonjourServiceAdvertiser bonjourServiceAdvertiser;
	public void start() {
		if (ezScreenServerDelegate != null) {
			ezScreenServerDelegate.onInitializationStart();
		}
		Thread initThread = new Thread(new Runnable() {

			@Override
			public void run() {
				Thread thisThread = Thread.currentThread();
				initEzAndroidRx();	
				synchronized (thisThread) {
					thisThread.notifyAll();
				}
			}
			
		});
		initThread.start();
	}
	public synchronized void stop() {
		if (bonjourServiceAdvertiser != null) {
			final BonjourServiceAdvertiser bonjour = bonjourServiceAdvertiser;
			new Thread(new Runnable() {

				@Override
				public void run() {
					bonjour.unregister();
					bonjour.close();
				}

			}).start();
			bonjourServiceAdvertiser = null;
		}
		mainHandler.removeCallbacks(resetToStandby);
		if (jsonRpcOverHttpServer != null) {
			jsonRpcOverHttpServer.stop();
			jsonRpcOverHttpServer = null;
		}
	}	
	private synchronized void initEzAndroidRx() {
		jsonRpcOverHttpServer = new JsonRpcOverHttpServer(context, 0, "/jsonrpc");
		try {
			jsonRpcOverHttpServer.registerRpcNotificationHandler(new NotificationHandler() {

				@Override
				public String[] handledNotifications() {
					return new String[]{"heartbeat", "connect", "disconnect"};
				}

				@Override
				public void process(JSONRPC2Notification notification,
						MessageContext context) {
					if ("heartbeat".equals(notification.getMethod())) {
						mainHandler.removeCallbacks(resetToStandby);
						mainHandler.postDelayed(resetToStandby, HEARTBEAT_TIMEOUT);						
					} else if ("connect".equals(notification.getMethod())) {
						ezScreenServerDelegate.onConnected();
						mainHandler.postDelayed(resetToStandby, HEARTBEAT_TIMEOUT);						
					} else if ("disconnect".equals(notification.getMethod())) {
						ezScreenServerDelegate.onDisconnected();
						mainHandler.removeCallbacks(resetToStandby);
						
					}
				}
				
			});
			jsonRpcOverHttpServer.registerRpcRequestHandler(new RequestHandler() {

				@Override
				public String[] handledRequests() {
					return new String[]{"display", "stop_display", "play", "pause", "resume", "stop", "seek", "increase_volume", "decrease_volume", "stream", "getStreamInfo"};
				}

				@Override
				public JSONRPC2Response process(JSONRPC2Request request,
						MessageContext arg1) {
					Map<String, Object> namedParams = request.getNamedParams();
					Log.d(TAG,"method = " + request.getMethod());
					if ("display".equals(request.getMethod())) {
						String url = (String) namedParams.get("url");
						if (url != null) {
							ezScreenServerDelegate.displayUrl(url);
							return new JSONRPC2Response(Long.valueOf(0), request.getID());
						}
						return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, request.getID());
					} else if ("stop_display".equals(request.getMethod())) {
						ezScreenServerDelegate.stopDisplay();
						return new JSONRPC2Response(Long.valueOf(0), request.getID());
					} else if ("play".equals(request.getMethod())) {
						if (namedParams.containsKey("url")) {
							ezScreenServerDelegate.playVideo((String)namedParams.get("url"), (String)namedParams.get("callback"));
							return new JSONRPC2Response(Long.valueOf(0), request.getID());					
						}
						return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, request.getID());
					} else if ("pause".equals(request.getMethod())) {
						ezScreenServerDelegate.pauseVideo();
						return new JSONRPC2Response(Long.valueOf(0), request.getID());
					} else if ("resume".equals(request.getMethod())) {
						ezScreenServerDelegate.resumeVideo();
						return new JSONRPC2Response(Long.valueOf(0), request.getID());
					} else if ("increase_volume".equals(request.getMethod())) {
						ezScreenServerDelegate.increaseVolume();
						return new JSONRPC2Response(Long.valueOf(0), request.getID());
					} else if ("decrease_volume".equals(request.getMethod())) {
						ezScreenServerDelegate.decreaseVolume();
						return new JSONRPC2Response(Long.valueOf(0), request.getID());
					} else if ("stop".equals(request.getMethod())) {
						ezScreenServerDelegate.stopVideo();
						return new JSONRPC2Response(Long.valueOf(0), request.getID());
					} else if ("seek".equals(request.getMethod())) {
						if (namedParams.containsKey("time")) {
							ezScreenServerDelegate.seek(Integer.valueOf(namedParams.get("time").toString()));
							return new JSONRPC2Response(Long.valueOf(0), request.getID());
						}
						return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, request.getID());
					} else if ("stream".equals(request.getMethod())) {
						String encryptedKey = (String) namedParams.get("param1");

						String aesIV = (String) namedParams.get("param2");
						mNtpServerPort = (Long) namedParams.get("ntp-server-port");
						mAesIV = Base64.decode(aesIV.getBytes(), Base64.NO_WRAP);
						try {
							mAesKey = CipherUtil.DecryptAESCBC(mPredefinedKey.getBytes("UTF-8"),encryptedKey.getBytes("UTF-8"), mAesIV,true);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}

						HashMap<String,Object> resMap = new HashMap();
						resMap.put("connection-type","tcp");
						resMap.put("tcp-port", mMirrorPort);
						resMap.put("version","screen-stream-1.0");
						return new JSONRPC2Response(resMap, request.getID());

					} else if ("getStreamInfo".equals(request.getMethod())) {

						String supportH264Decode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ? "true" : "false";
						HashMap<String,Object> resMap = new HashMap();
						resMap.put("h264stream",supportH264Decode);
						if ("true".equals(supportH264Decode)) {
							resMap.put("height", "1280");
							resMap.put("width", "720");
						}
						return new JSONRPC2Response(resMap, request.getID());

					}
					return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, request.getID());
				}
				
			});
			jsonRpcOverHttpServer.start();
			HashMap<String, String> txtRecord = new HashMap<String, String>();
			txtRecord.put("txtvers", "20151019");
			txtRecord.put("srcvers", "20151019");
			txtRecord.put("deviceid", deviceID);
			//2015-03-13 erichwang for mac-mirror and windows-mirror
			txtRecord.put("mmr", String.valueOf(mmr));
			txtRecord.put("wmr", String.valueOf(wmr));
			String supportH264Decode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ? "enable" : "disable";
			txtRecord.put("h264stream", supportH264Decode);
			ServiceInfo screenServiceInfo = ServiceInfo.create(AndroidRxFinder.SERVICE_TYPE + "local.", EzScreenServer.this.name, jsonRpcOverHttpServer.getListeningPort(), 0, 0, txtRecord);
			bonjourServiceAdvertiser = new BonjourServiceAdvertiser(screenServiceInfo);
			bonjourServiceAdvertiser.register();
			if (ezScreenServerDelegate != null) {
				ezScreenServerDelegate.onInitializationFinished();
			}
			
		} catch (IOException e) {
			Log.e(TAG, "initialize android rx failed", e);
			if (ezScreenServerDelegate != null) {
				ezScreenServerDelegate.onInitializationFailed(e);
			}
		}

		initMirrorServer();
	}

	private AsyncHttpServer mirrorServer;
	private static boolean DEBUG_LOG = true;
	private static void debugLog(String msg) {
		if (DEBUG_LOG) {
			Log.d(TAG, msg);
		}
	}
	private void initMirrorServer() {
		mirrorServer = new AsyncHttpServer() {
			@Override
			protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Log.d(TAG, "onRequest:"+request.getPath());
				return false;
			}
		};
		mirrorServer.setErrorCallback(new CompletedCallback() {

			@Override
			public void onCompleted(Exception ex) {
				if (!mMirrorServiceReady) {
//					informDelegateInitializationFailed(ex);
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
				final ByteBuffer header = ByteBuffer.allocate(32);
				final ByteBuffer payload = ByteBuffer.allocate(500 * 1024);
				final ByteBuffer decryptPayload = ByteBuffer.allocate(500 * 1024);
				mMirrorServiceReady = false;

				if (ezScreenServerDelegate != null) {
					InetAddress remoteAddress = null;
					if (socket instanceof AsyncNetworkSocket) {
						remoteAddress = ((AsyncNetworkSocket) socket).getRemoteAddress().getAddress();
					}
					ezScreenServerDelegate.onStartMirroring(remoteAddress, mNtpServerPort.intValue());
				}
				socket.setEndCallback(new CompletedCallback() {

					@Override
					public void onCompleted(Exception ex) {
						Log.d(TAG, "mirror onCompleted:streaming");
						if (ezScreenServerDelegate != null /*&& airplayState != AIRPLAY_VIDEO_ON_MIRROR*/) {
							ezScreenServerDelegate.onStopMirroring();
						}
					}

				});

				mirrorServer.getServerSocket().setDataCallback(headerReader);
				headerReader.read(32, headerCallback = new DataCallback() {

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
						Log.d(TAG, " header read complete  ");

						header.clear();
						bb.get(header.array());
						header.order(ByteOrder.LITTLE_ENDIAN);

						final int payloadSize = header.getInt();
						final short payloadType = header.getShort();
						final long timestamp = header.getLong();
						debugLog("onDataAvailable:streaming:"+"payload size:"+payloadSize+", payload type:"+payloadType+", timestamp:"+timestamp);
						DataEmitterReader payloadReader = new DataEmitterReader();
						mirrorServer.getServerSocket().setDataCallback(payloadReader);
						payloadReader.read(payloadSize, new DataCallback() {
							private int numberOfSps;
							private byte[] sps;
							private byte numberOfPps;
							private byte[] pps;
							private byte[] nal = {0x00, 0x00, 0x00, 0x01};

							public int indexOf(byte[] data, byte[] pattern,int fromIndex) {
								int[] failure = computeFailure(pattern);

								int j = 0;
								if (data.length == 0) return -1;

								for (int i = fromIndex; i < data.length; i++) {
									while (j > 0 && pattern[j] != data[i]) {
										j = failure[j - 1];
									}
									if (pattern[j] == data[i]) { j++; }
									if (j == pattern.length) {
										return i - pattern.length + 1;
									}
								}
								return -1;
							}

							/**
							 * Computes the failure function using a boot-strapping process,
							 * where the pattern is matched against itself.
							 */
							private int[] computeFailure(byte[] pattern) {
								int[] failure = new int[pattern.length];

								int j = 0;
								for (int i = 1; i < pattern.length; i++) {
									while (j > 0 && pattern[j] != pattern[i]) {
										j = failure[j - 1];
									}
									if (pattern[j] == pattern[i]) {
										j++;
									}
									failure[i] = j;
								}

								return failure;
							}

							@Override
							public void onDataAvailable(
									DataEmitter emitter,
									ByteBufferList bb) {
								try {
									payload.clear();
									decryptPayload.clear();
									bb.get(payload.array(), 0, payloadSize);
									debugLog("onDataAvailable:streaming:"+payloadSize+" bytes of payload read");
									if (payloadType == 0) { // video bitstream
										logBytes("onDataAvailable:streaming:payload:", payload.array());
										byte[] content = new byte[payloadSize];
										System.arraycopy(payload.array(), 0, content, 0, payloadSize);
										byte[] decrypByte = CipherUtil.DecryptAESCBC(mAesKey, content, mAesIV, false);

										int nalPos = 0;
										while(nalPos >= 0)
										{
											nalPos = indexOf(decrypByte, nal, nalPos);
											Log.d("dddd","nalPos = " + nalPos);

											if(nalPos >= 0)
											{
												nalPos +=4;
											}
										}


										decryptPayload.order(ByteOrder.BIG_ENDIAN);
										decryptPayload.put(decrypByte);
										decryptPayload.position(0);

										if (ezScreenServerDelegate != null) {
											debugLog("onH264FrameAvailable ntpTime:" + TimeStamp.getTime(timestamp));
											ezScreenServerDelegate.onH264FrameAvailable(decryptPayload.array(), 0, decrypByte.length, TimeStamp.getTime(timestamp));
										}


									} else if (payloadType == 1) { //codec data
										byte[] content = new byte[payloadSize];
										System.arraycopy(payload.array(), 0, content, 0, payloadSize);
										byte[] decrypByte = CipherUtil.DecryptAESCBC(mAesKey, content, mAesIV, false);
										decryptPayload.put(decrypByte);
										decryptPayload.position(0);

										decryptPayload.order(ByteOrder.BIG_ENDIAN);
										decryptPayload.position(5);
										numberOfSps = decryptPayload.get()&0x1f;
										short sizeOfSps = decryptPayload.getShort();
										sps = new byte[sizeOfSps];
										decryptPayload.get(sps);
										if (ezScreenServerDelegate != null) {
											ezScreenServerDelegate.onSpsAvailable(sps);
										}
										debugLog("onDataAvailable:streaming:sps: number:"+numberOfSps+" size:"+sizeOfSps);
										logBytes("onDataAvailable:streaming:sps:", sps);

										numberOfPps = decryptPayload.get();
										short sizeOfPps = decryptPayload.getShort();
										pps = new byte[sizeOfPps];
										decryptPayload.get(pps);
										if (ezScreenServerDelegate != null) {
											ezScreenServerDelegate.onPpsAvailable(pps);
										}
										debugLog("onDataAvailable:streaming:pps: number:"+numberOfPps+" size:"+sizeOfPps);
										logBytes("onDataAvailable:streaming:pps:", pps);
									} else if (payloadType == 2) { // heartbeat
										debugLog("onDataAvailable:streaming: receive heartbeat");
									} else if (payloadType == 3) { // msg
										byte[] content = new byte[payloadSize];
										System.arraycopy(payload.array(), 0, content, 0, payloadSize);
										byte[] decrypByte = CipherUtil.DecryptAESCBC(mAesKey, content, mAesIV, false);
										String decryptString = new String(decrypByte);
										Log.d(TAG, "decryptString = " + decryptString);
										String msg;
										if (decryptString.equals("Luke, I am your Father!")) {
											msg = "Hello!";
											mMirrorServiceReady = true;
										} else {
											msg = "Who's your father?";
										}
										byte[] body = msg.getBytes("UTF-8");
										byte[] encryptBody = CipherUtil.EncryptAESCBC(mAesKey,body,mAesIV);
										ByteBuffer msgHeadBuf = ByteBuffer.allocate(32);
										msgHeadBuf.order(ByteOrder.LITTLE_ENDIAN);
										msgHeadBuf.putInt(encryptBody.length);
										msgHeadBuf.putShort(payloadType);
										long currentTime = System.currentTimeMillis();
										msgHeadBuf.putLong(currentTime);
										msgHeadBuf.position(0);

										ByteBuffer msgBodyBuf = ByteBuffer.allocate(encryptBody.length);
										msgBodyBuf.put(encryptBody);
										msgBodyBuf.position(0);
										ByteBufferList msgBufList = new ByteBufferList(msgHeadBuf,msgBodyBuf);
										mirrorServer.getServerSocket().write(msgBufList);

									}

									if(mMirrorServiceReady) {
										mirrorServer.getServerSocket().setDataCallback(headerReader);
										headerReader.read(32, headerCallback);
									}
								} catch (Exception e) {
									mirrorServer.getServerSocket().close();
									if (ezScreenServerDelegate != null) {
										ezScreenServerDelegate.onStopMirroring();
									}
								}
							}

						});

					}

				});
			}
		});
		AsyncServerSocket mirrorServerSock = mirrorServer.listen(0);
		mMirrorPort = mirrorServerSock.getLocalPort();

		Log.d(TAG, "Mirror server listening on "+mMirrorPort);
	}

	public void closeAirPlayConnection() {
		if (mirrorServer != null) {
			mirrorServer.stop();
			AsyncServerSocket mirrorServerSock = mirrorServer.listen(0);
			mMirrorPort = mirrorServerSock.getLocalPort();
		}
	}


}
