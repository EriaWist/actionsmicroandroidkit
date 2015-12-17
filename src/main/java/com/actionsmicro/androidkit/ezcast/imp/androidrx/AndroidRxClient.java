package com.actionsmicro.androidkit.ezcast.imp.androidrx;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.actionsmicro.androidkit.ezcast.ConnectionManager;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.TrackableApi;
import com.actionsmicro.ezcom.jsonrpc.JSONRPC2Session;
import com.actionsmicro.graphics.YuvImageToJpegHelper;
import com.actionsmicro.utils.CipherUtil;
import com.actionsmicro.utils.Log;
import com.actionsmicro.web.JsonRpcOverHttpServer;
import com.actionsmicro.web.SimpleContentUriHttpFileServer;
import com.actionsmicro.web.SimpleMotionJpegHttpServer;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.NotificationHandler;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

import org.apache.commons.net.ntp.TimeStamp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import vavi.apps.shairport.UDPDelegate;
import vavi.apps.shairport.UDPListener;

public class AndroidRxClient implements DisplayApi, MediaPlayerApi {
	private static final short PACKET_TYPE_VIDEO_BITSTREAM = 0;
	private static final short PACKET_TYPE_CODEC = 1;
	private static final short PACKET_TYPE_HEARTBEAT = 2;
	private static final short PACKET_TYPE_MSG = 3;
	private static String mPredefinedKey = "SCREEN21SCREEN90SCREEN23SCREEN43";
	private static final String TAG = "AndroidRxClient";
	private static final int HEARTBEAT_PERIOD = 1000;
	private static final Date DATE_BASE_TIME = new Date(0);

	private JSONRPC2Session jsonRpcSession;
	private InetAddress ipAddress;
	private int port;
	private boolean mIsHandShaking = false;
	private Socket mMirrorClientSocket = null;
	private byte[] mAesKey = null;
	private byte[] mAesIV = null;
	private boolean stopNtpServerThread;
	private Thread mNtpServerThread;
	private int mNtpPort;
	private DatagramSocket mNtpServSock;
	private UDPListener udpListener;

	public interface JSonResponseDelegate {
		void onComplete(JSONRPC2Response response);
	}

	public AndroidRxClient(Context context, InetAddress ipAddress, int port) {
		this.context = context;
		this.ipAddress = ipAddress;
		this.port = port;
	}
	private JSONRPC2Session getJsonRpcSession() {
		return jsonRpcSession;
	}
	private Runnable heartbeat = new Runnable() {

		@Override
		public void run() {
			try {
				if (jsonRpcSession != null) {
					jsonRpcSession.send(new JSONRPC2Notification("heartbeat"));
					Log.d(TAG, "send heartbeat");
					Handler networkHandler = getNetworkHandler();
					if (networkHandler != null) {
						networkHandler.postDelayed(heartbeat, HEARTBEAT_PERIOD);
					}
				}
			} catch (JSONRPC2SessionException e) {
				e.printStackTrace();
				notifyConnectionManagerDidFailed(e);
			}
		}
		
	};
	private List<ConnectionManager> connectionManagers = new ArrayList<ConnectionManager>();
	public void addConnectionManager(ConnectionManager listener) {
		synchronized(connectionManagers) {
			if (!connectionManagers.contains(listener)) {
				connectionManagers.add(listener);
			}
		}
	}
	public void removeConnectionManager(ConnectionManager manager) {
		synchronized(connectionManagers) {
			connectionManagers.remove(manager);
		}
	}
	private void notifyConnectionManagerDidFailed(Exception e) {
		synchronized(connectionManagers) {
			Iterator<ConnectionManager> iterator = new CopyOnWriteArrayList<ConnectionManager>(connectionManagers).listIterator();
			while (iterator.hasNext()) {
				ConnectionManager manager = iterator.next(); 
				if (manager != null) {
					manager.onConnectionFailed(this, e);
				}						
			}
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
			getNetworkHandler().postDelayed(heartbeat, HEARTBEAT_PERIOD);
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
	private URL getJsonRpcUrl() {
		try {
			return new URL(getBaseUrl(), "/jsonrpc");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	private int sRpcId = 0;
	private Context context;
	private synchronized int generateRpcId() {
		return sRpcId++;
	}
	private void performOnNetworkThread(Runnable runnable) {
		Handler handler = getNetworkHandler();
		if (handler != null) {
			handler.post(runnable);			
		}
	}
	protected void invokeRpcMethod(final String method, final HashMap<String, Object> params) {
		invokeRpcMethod(method, params, 0);
	}
	protected void invokeRpcMethod(final String method, final HashMap<String, Object> params, long timeout) {
		invokeRpcMethod(method, params, 0, null);
	}

	protected void invokeRpcMethod(final String method, final HashMap<String, Object> params, long timeout, final JSonResponseDelegate delegate) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					JSONRPC2Session jsonRpcSession = getJsonRpcSession();
					if (jsonRpcSession != null) {
						JSONRPC2Response response = jsonRpcSession.send(new JSONRPC2Request(method, params, generateRpcId()));
						if (null != delegate) {
							delegate.onComplete(response);
						}
					}
				} catch (JSONRPC2SessionException e) {
					e.printStackTrace();
				} finally {
					synchronized (this) {
						this.notifyAll();
					}
				}
			}

		};
		performOnNetworkThread(runnable);
		if (timeout > 0) {
			synchronized (runnable) {
				try {
					runnable.wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	protected void invokeRpcMethod(final String method) {
		invokeRpcMethod(method, 0);
	}
	protected void invokeRpcMethod(final String method, long timeout) {
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				try {
					JSONRPC2Session jsonRpcSession = getJsonRpcSession();
					if (jsonRpcSession != null) {
						jsonRpcSession.send(new JSONRPC2Request(method, generateRpcId()));
					}
				} catch (JSONRPC2SessionException e) {
					e.printStackTrace();
				} finally {
					synchronized (this) {
						this.notifyAll();
					}
				}
			}
			
		};
		performOnNetworkThread(runnable);
		if (timeout > 0) {
			synchronized (runnable) {
				try {
					runnable.wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	protected void sendRpcNotification(final String notification, long timeout) {
		
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				try {
					JSONRPC2Session jsonRpcSession = getJsonRpcSession();
					if (jsonRpcSession != null) {
						jsonRpcSession.send(new JSONRPC2Notification(notification));
					}
				} catch (JSONRPC2SessionException e) {
					e.printStackTrace();
				} finally {
					synchronized (this) {
						this.notifyAll();
					}
				}
			}
			
		};
		performOnNetworkThread(runnable);
		if (timeout > 0) {
			synchronized (runnable) {
				try {
					runnable.wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private URL getBaseUrl() {
		try {
			URL baseUrl = new URL("http", ipAddress.getHostAddress(), port, "");
			return baseUrl;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	private SimpleMotionJpegHttpServer simpleMotionJpegHttpServer;

	private SimpleMotionJpegHttpServer getMjpegServer() {
		synchronized (this) {
			if (simpleMotionJpegHttpServer == null) {
				simpleMotionJpegHttpServer = new SimpleMotionJpegHttpServer(context, 0, new SimpleMotionJpegHttpServer.OnConnectionListener() {
					@Override
					public void onClientConnected(SimpleMotionJpegHttpServer simpleMotionJpegHttpServer) {
					}
				});
			}
			return simpleMotionJpegHttpServer;
		}
	}
	private static final String EZCASTPLAYER_ONENDED = "ezcastplayer.onended";
	private static final String EZCASTPLAYER_ONLOADSTART = "ezcastplayer.onloadstart";
	private static final String EZCASTPLAYER_ONERROR = "ezcastplayer.onerror";
	private static final String EZCASTPLAYER_ONPLAY = "ezcastplayer.onplay";
	private static final String EZCASTPLAYER_ONTIMEUPDATE = "ezcastplayer.ontimeupdate";
	private static final String EZCASTPLAYER_ONDURATIONCHANGE = "ezcastplayer.ondurationchange";
	private static final String[] SUPPORTED_REQUESTS = new String[] {};
	private static final String[] SUPPORTED_NOTIFICATIONS = new String[] {
		EZCASTPLAYER_ONDURATIONCHANGE, 
		EZCASTPLAYER_ONTIMEUPDATE, 
		EZCASTPLAYER_ONPLAY, 
		EZCASTPLAYER_ONERROR, 
		EZCASTPLAYER_ONLOADSTART,
		EZCASTPLAYER_ONENDED};
	private JsonRpcOverHttpServer jsonRpcOverHttpServer;
	private MediaPlayerStateListener mediaPlayerStateListener;

	public MediaPlayerStateListener getMediaPlayerStateListener() {
		return mediaPlayerStateListener;
	}
	public void setMediaPlayerStateListener(
			MediaPlayerStateListener mediaPlayerStateListener) {
		this.mediaPlayerStateListener = mediaPlayerStateListener;
	}
	@Override
	public void connect() {
		jsonRpcOverHttpServer = new JsonRpcOverHttpServer(context, 0, ".*");
		jsonRpcOverHttpServer.registerRpcNotificationHandler(new NotificationHandler() {
			private static final int MEDIA_ERR_ABORTED = 1;
			private static final int MEDIA_ERR_NETWORK = 2;
			private static final int MEDIA_ERR_DECODE = 3;
			private static final int MEDIA_ERR_SRC_NOT_SUPPORTED = 4;
			@Override
			public String[] handledNotifications() {
				return SUPPORTED_NOTIFICATIONS;
			}

			@Override
			public void process(JSONRPC2Notification notification,
					MessageContext arg1) {
				Map<String, Object> namedParams = notification.getNamedParams();
				if (EZCASTPLAYER_ONTIMEUPDATE.equals(notification.getMethod())) {
					Log.d(TAG, EZCASTPLAYER_ONTIMEUPDATE+":"+namedParams.get("time"));
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerTimeDidChange(AndroidRxClient.this,  Float.valueOf(namedParams.get("time").toString()).longValue());
					}
				} else if (EZCASTPLAYER_ONDURATIONCHANGE.equals(notification.getMethod())) {
					Log.d(TAG, EZCASTPLAYER_ONDURATIONCHANGE+":"+namedParams.get("duration"));
					long duration = Float.valueOf(namedParams.get("duration").toString()).longValue();
					setMediaUsageDuration((int) duration);
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDurationIsReady(AndroidRxClient.this, duration);
					}
				} else if (EZCASTPLAYER_ONERROR.equals(notification.getMethod())) {
					int errorCode = convertErrorCode(Integer.valueOf(namedParams.get("error").toString()));
					setMediaUsageResultCode(namedParams.get("error").toString(), errorCode);
					commitMediaUsageTracking();
					Log.d(TAG, EZCASTPLAYER_ONERROR+":"+errorCode);
					currentState = State.STOPPED;
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidFailed(AndroidRxClient.this, errorCode);
					}
				} else if (EZCASTPLAYER_ONPLAY.equals(notification.getMethod())) {
					Log.d(TAG, EZCASTPLAYER_ONPLAY+":");
					currentState = State.PLAYING;
				} else if (EZCASTPLAYER_ONLOADSTART.equals(notification.getMethod())) {
					Log.d(TAG, EZCASTPLAYER_ONLOADSTART+":");
					currentState = State.PLAYING;
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidStart(AndroidRxClient.this);
					}
				} else if (EZCASTPLAYER_ONENDED.equals(notification.getMethod())) {
					Log.d(TAG, EZCASTPLAYER_ONENDED+":");
					commitMediaUsageTracking();
					if (currentState != State.STOPPED) {
						if (mediaPlayerStateListener != null) {
							mediaPlayerStateListener.mediaPlayerDidStop(AndroidRxClient.this, Cause.REMOTE);
						}
					}
					currentState = State.STOPPED;
				}
			}

			private int convertErrorCode(int error) {
				int errorCode = AV_RESULT_ERROR_GENERIC;
				switch(error) {
				case MEDIA_ERR_ABORTED:
					errorCode = AV_RESULT_ERROR_STOP_ABORTED;
					break;
				case MEDIA_ERR_NETWORK:
					errorCode = AV_RESULT_ERROR_URL_DIVERT_LINK_ERROR;
					break;
				case MEDIA_ERR_DECODE:
				case MEDIA_ERR_SRC_NOT_SUPPORTED:
					errorCode = AV_RESULT_ERROR_STOP_FILE_FORMAT_UNSOPPORTED;
					break;
				}
				return errorCode;
			}
			
		});
		jsonRpcOverHttpServer.registerRpcRequestHandler(new RequestHandler() {

			@Override
			public String[] handledRequests() {
				return SUPPORTED_REQUESTS;
			}

			@Override
			public JSONRPC2Response process(JSONRPC2Request request,
					MessageContext arg1) {
				return null;
			}
			
		});
		jsonRpcOverHttpServer.start();
		if (jsonRpcSession == null) {
			jsonRpcSession = new JSONRPC2Session(getJsonRpcUrl());
		}
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
		sendRpcNotification("connect", 0);
	}

	protected void setMediaUsageResultCode(String string, int errorCode) {
		if (tracker != null) {
			tracker.setMediaUsageResultCode(string, errorCode);
		}
	}
	protected void setMediaUsageDuration(int duration) {
		if (tracker != null) {
			tracker.setMediaUsageDuration(duration);
		}
	}
	@Override
	public void disconnect() {
		this.sendRpcNotification("disconnect", 3000);
		stopMjpegServerIfNeeded();
		closeMirrorServer();
		stop();
		if (jsonRpcOverHttpServer != null) {
			jsonRpcOverHttpServer.stop();
			jsonRpcOverHttpServer = null;
		}
		
		if (networkThread != null) {
			networkThread.stopLooper();
			try {
				networkThread.join(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			networkThread = null;
		}
		if (jsonRpcSession != null) {
			jsonRpcSession.close();
			jsonRpcSession = null;
		}
	}

	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public State getState() {
		return currentState;
	}

	@Override
	public boolean pause() {
		invokeRpcMethod("pause");
		currentState = State.PAUSED;
		return true;
	}

	@Override
	public boolean resume() {
		invokeRpcMethod("resume");
		currentState = State.PLAYING;
		return true;
	}

	@Override
	public boolean increaseVolume() {
		invokeRpcMethod("increase_volume");
		return true;
	}

	@Override
	public boolean decreaseVolume() {
		invokeRpcMethod("decrease_volume");
		return true;
	}

	@Override
	public boolean seek(int position) {
		final HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("time", position);
		invokeRpcMethod("seek", params);
		return true;
	}

	@Override
	public boolean stop() {
		if (currentState == State.UNKNOWN || currentState == State.STOPPED) {
			return true;
		}
		invokeRpcMethod("stop");
		currentState = State.STOPPED;
		if (mediaPlayerStateListener != null) {
			mediaPlayerStateListener.mediaPlayerDidStop(AndroidRxClient.this, Cause.USER);
		}
		stopHttpFileServer();
		closeMirrorServer();
		commitMediaUsageTracking();
		return true;
	}

	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		commitMediaUsageTracking();
		stopHttpFileServer();
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
		beginMediaUsageTracking(context, url, userAgentString, title);
		final HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("url", mediaUriString);
		params.put("callback", jsonRpcOverHttpServer.getServerUrl());
		invokeRpcMethod("play", params);		
		return true;
	}

	private void commitMediaUsageTracking() {
		if (tracker != null) {
			tracker.commitMediaUsageTracking();
		}
	}
	private void beginMediaUsageTracking(Context context2, String url,
			String userAgentString, String title) {
		if (tracker != null) {
			tracker.beginMediaUsageTracking(context2, url, userAgentString, title);
		}
	}
	@Override
	public void startDisplaying() {
		final HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("url", getMjpegServer().getServerUrl());
		invokeRpcMethod("display", params);
	}

	@Override
	public void stopDisplaying() {
		invokeRpcMethod("stop_display", 3000);
		synchronized (this) {
			stopMjpegServerIfNeeded();
			closeMirrorServer();
		}
	}
	private void stopMjpegServerIfNeeded() {
		if (simpleMotionJpegHttpServer != null) {
			simpleMotionJpegHttpServer.cleanup();
			simpleMotionJpegHttpServer = null;
		}
	}

	@Override
	public void resendLastImage() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendJpegEncodedScreenData(InputStream input, long length)
			throws Exception {
		SimpleMotionJpegHttpServer mjpegServer = getMjpegServer();
		if (mjpegServer != null) {
			mjpegServer.sendJpegStream(input, length);
		}
	}

	@Override
	public void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception {
		YuvImageToJpegHelper helper = YuvImageToJpegHelper.getDefaultHelper();
		synchronized (helper) {
			InputStream inputStream = helper.compressYuvImageToJpegStream(yuvImage, quailty);
			sendJpegEncodedScreenData(inputStream, inputStream.available());
		}
	}

	public static int unsignedToBytes(byte b) {
		return b & 0xFF;
	}
	private final byte[] nal = {0x00, 0x00, 0x00, 0x01};

	// ref: http://stackoverflow.com/questions/1507780/searching-for-a-sequence-of-bytes-in-a-binary-file-with-java
	/**
	 * Finds the first occurrence of the pattern in the text.
	 */
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
	public void sendH264EncodedScreenData(final byte[] contents, int width, int height) throws Exception {
		if (!mIsHandShaking && null == mMirrorClientSocket) {
			h264Queue = new ArrayList<byte[]>();
			mIsHandShaking = true;
			Log.d(TAG, "Mirror Service is not Ready yet");
			final HashMap<String, Object> params = new HashMap<String, Object>();
			SecureRandom random = new SecureRandom();
			mAesIV = genAlphaNumber(random,16).getBytes("UTF-8");
			String encryptIV = Base64.encodeToString(mAesIV, Base64.NO_WRAP);
			mAesKey = genAlphaNumber(random,32).getBytes("UTF-8");
			String encryptKey = Base64.encodeToString(CipherUtil.EncryptAESCBC(mPredefinedKey.getBytes("UTF-8"), mAesKey, mAesIV), Base64.NO_WRAP);
			params.put("param1", encryptKey);
			params.put("param2", encryptIV);
			initNtpServerService();
			synchronized (mNtpServerThread) {
				mNtpServerThread.wait();
			}
			Log.d(TAG,"ntp server ready, mNtpPort = " + mNtpPort);
			params.put("ntp-server-port", mNtpPort);

			invokeRpcMethod("stream", params, 0, new JSonResponseDelegate() {
				@Override
				public void onComplete(JSONRPC2Response response) {
					HashMap<String, Object> resMap = (HashMap<String, Object>) response.getResult();
					String connectType = (String) resMap.get("connection-type");
					int mirrorPort = ((Long) resMap.get("tcp-port")).intValue();
					String version = (String) resMap.get("version");
					Log.d(TAG, "connectType = " + connectType + " tcpPort = " + mirrorPort + " version = " + version);

					try {
						mMirrorClientSocket = new Socket();
						mMirrorClientSocket.connect(new InetSocketAddress(ipAddress, mirrorPort), 0);
						String msg = "Luke, I am your Father!";
						byte[] body = msg.getBytes("UTF-8");
						final byte[] encryptBody = CipherUtil.EncryptAESCBC(mAesKey, body, mAesIV);
						sendMirrorData(PACKET_TYPE_MSG,encryptBody);
						final InputStream in = mMirrorClientSocket.getInputStream();
						new Thread(new Runnable() {
							@Override
							public void run() {
								int headerSize = 32;

								try {
									byte[] payloadHead = new byte[headerSize];
									int ret = in.read(payloadHead);
									if (ret != -1) {
										ByteBuffer payloadBuf = ByteBuffer.wrap(payloadHead);
										payloadBuf.order(ByteOrder.LITTLE_ENDIAN);
										int payloadSize = payloadBuf.getInt();
										final short payloadType = payloadBuf.getShort();
										final long timestamp = payloadBuf.getLong();
										StringBuilder headerBuilder = new StringBuilder();
										for (int i = 0; i < payloadHead.length; i++) {
											headerBuilder.append((unsignedToBytes(payloadHead[i]) + " "));
										}
										Log.d(TAG, " read body ........ payloadsize = " + payloadSize + " payloadType = " + payloadType);

										byte[] payloadBody = new byte[payloadSize];
										ret = in.read(payloadBody);
										if (ret != -1) {
											byte[] decrypByte = CipherUtil.DecryptAESCBC(mAesKey, payloadBody, mAesIV, false);
											String decryptString = new String(decrypByte);
											Log.d(TAG, "decryptString = " + decryptString);

											if (decryptString.equals("Hello!")) {
												Log.d(TAG, "right msg");
												dequeueH264Data();

												// TODO to check if sendhearbeat is nesscessary
//												Handler networkHandler = getNetworkHandler();
//												if (networkHandler != null) {
//													networkHandler.postDelayed(mirrorHeartBeat, HEARTBEAT_PERIOD);
//												}
											} else {
												Log.d(TAG, "wrong body msg");
												closeMirrorServer();
											}

										} else {
											Log.d(TAG, "wrong body length = -1");
											closeMirrorServer();
										}
									} else {
										Log.d(TAG, "wrong header length = -1");
										closeMirrorServer();
									}
								} catch (IOException e) {
									e.printStackTrace();
									closeMirrorServer();
								}
								Log.d(TAG, "handshake complete");

							}
						}).start();
					} catch (IOException e) {
						e.printStackTrace();
						closeMirrorServer();
					}

				}
			});
			enqueueH264Data(contents);
		} else if(null == mMirrorClientSocket){
			enqueueH264Data(contents);
		} else {
			sendMirrorData(PACKET_TYPE_VIDEO_BITSTREAM, contents);
		}
	}

	private String genAlphaNumber(SecureRandom random, int len) {
		return new BigInteger(len * 5, random).toString(32);
	}

	private void sendMirrorData(short type, byte[] contents) {

		ByteBuffer msgHeadBuf = ByteBuffer.allocate(32);
		msgHeadBuf.order(ByteOrder.LITTLE_ENDIAN);
		TimeStamp ntpStamp = TimeStamp.getCurrentTime();
		long ntpTime = ntpStamp.ntpValue();
		byte[] msgBody = new byte[0];
		switch (type) {
			case PACKET_TYPE_VIDEO_BITSTREAM:
				msgBody = CipherUtil.EncryptAESCBC(mAesKey, contents, mAesIV);

				msgHeadBuf.putInt(msgBody.length);
				msgHeadBuf.putShort(type);
				msgHeadBuf.putLong(ntpTime);
				msgHeadBuf.position(0);

				break;
			case PACKET_TYPE_CODEC:
				int nalSpsPos = indexOf(contents, nal, 0);
				int nalPpsPos = indexOf(contents, nal, nalSpsPos + 4);

				final int spsLen = nalPpsPos - 4;
				final int ppsLen = contents.length - nalPpsPos - 4;

				final byte[] spsData = new byte[spsLen];
				final byte[] ppsData = new byte[ppsLen];
				System.arraycopy(contents, nalSpsPos + 4, spsData, 0, spsLen);
				System.arraycopy(contents, nalPpsPos + 4, ppsData, 0, ppsLen);

				ByteBuffer codecBody = ByteBuffer.allocate(11 + spsLen + ppsLen);
				// version
				codecBody.put((byte) 1);
				// profile(high)
				codecBody.put((byte) 100);
				// compatibility
				codecBody.put((byte) 0xc0);
				// level(4.0)
				codecBody.put((byte) 40);
				// 6bits:0x3f reserved. 2 bits:nal unuts length-1
				codecBody.put((byte) 0xff);
				// 3bits:0x7 reserved, 5 bits:number of SPS
				codecBody.put((byte) 0xe1);
				// SPS len
				codecBody.putShort((short) spsLen);
				// SPS data
				codecBody.put(spsData);
				// number of PPS
				codecBody.put((byte) 1);
				// PPS len
				codecBody.putShort((short) ppsLen);
				// SPS data
				codecBody.put(ppsData);

				codecBody.position(0);

				msgBody = CipherUtil.EncryptAESCBC(mAesKey, codecBody.array(), mAesIV);
				// send codec
				msgHeadBuf.putInt(msgBody.length);
				msgHeadBuf.putShort(type);
				msgHeadBuf.putLong(ntpTime);
				msgHeadBuf.position(0);
				break;
			case PACKET_TYPE_HEARTBEAT:
				msgHeadBuf.putInt(0);
				msgHeadBuf.putShort(type);
				msgHeadBuf.putLong(ntpTime);
				msgHeadBuf.position(0);
				break;
			case PACKET_TYPE_MSG:
				msgBody = contents;
				msgHeadBuf.putInt(msgBody.length);
				msgHeadBuf.putShort(type);
				msgHeadBuf.putLong(ntpTime);
				msgHeadBuf.position(0);
				break;
		}

		sendDataToMirrorServer(msgHeadBuf.array());
		sendDataToMirrorServer(msgBody);
	}


	private void sendDataToMirrorServer(byte[] data) {
		if (mMirrorClientSocket != null) {
			synchronized (mMirrorClientSocket) {
				try {
					mMirrorClientSocket.getOutputStream().write(data);
					mMirrorClientSocket.getOutputStream().flush();
				} catch (IOException e) {
					closeMirrorServer();
					e.printStackTrace();
				}
			}
		}
	}

	private SimpleContentUriHttpFileServer simpleHttpFileServer;
	private State currentState = State.STOPPED;
	private TrackableApi tracker;
	private void stopHttpFileServer() {
		if (simpleHttpFileServer != null) {
			simpleHttpFileServer.stop();
			simpleHttpFileServer = null;
		}
	}
	public void setTracker(TrackableApi trackableApi) {
		this.tracker = trackableApi;
	}

	private void closeMirrorServer() {
		if (mMirrorClientSocket != null) {
			synchronized (mMirrorClientSocket) {
				closeNtpServer();

				if (mMirrorClientSocket != null) {
					try {
						mMirrorClientSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mMirrorClientSocket = null;

					mIsHandShaking = false;
					h264Queue.clear();
					h264Queue = null;
				}
			}
		}
	}

	private ArrayList<byte[]> h264Queue;

	private void enqueueH264Data(byte[] contents) {
		h264Queue.add(contents);
	}

	private void dequeueH264Data() {
		while (h264Queue.size() > 0) {
			byte[] contents = h264Queue.remove(0);
			byte nalByte = contents[4];
			int nalType = nalByte & 0x1f;
			Log.d(TAG,"dequeueH264Data nalType = " + nalType);
			if(nalType == 7) {
				sendMirrorData(PACKET_TYPE_CODEC, contents);
			} else{
				sendMirrorData(PACKET_TYPE_VIDEO_BITSTREAM, contents);
			}
		}
	}

	private Runnable mirrorHeartBeat = new Runnable() {
		@Override
		public void run() {
			if (mMirrorClientSocket != null) {
                Handler networkHandler = getNetworkHandler();
                if (networkHandler != null) {
                    networkHandler.postDelayed(mirrorHeartBeat, HEARTBEAT_PERIOD);
                }
                sendMirrorData(PACKET_TYPE_HEARTBEAT,null);
                Log.d(TAG, "send mirror heartbeat");

            }
		}

	};

	private void initNtpServerService() {
		stopNtpServerThread = false;
		closeNtpServer();
		mNtpServerThread = new Thread() {
			@Override
			public void run() {
				try {
					mNtpServSock = new DatagramSocket();
					mNtpPort = mNtpServSock.getLocalPort();
					synchronized(this) {
						this.notifyAll();
					}
					udpListener = new UDPListener(mNtpServSock, new UDPDelegate() {

						@Override
						public void packetReceived(DatagramSocket socket, DatagramPacket packet) {
							Log.d(TAG, "receive ntp data");

							/*0000   24 01 02 e8 00 00 00 00 00 00 00 00 41 49 52 50
							0010   00 00 00 00 00 00 00 00 00 00 01 c4 c8 ac 5d b5
							0020   00 00 01 c4 c9 6a 0b a1 00 00 01 c4 c9 78 73 d2
							Network Time Protocol
							Flags: 0x24
							00.. .... = Leap Indicator: no warning (0)
									..10 0... = Version number: NTP Version 4 (4)
							.... .100 = Mode: server (4)
							Peer Clock Stratum: primary reference (1)
							Peer Polling Interval: invalid (2)
							Peer Clock Precision: 0.000000 sec
							Root Delay: 0.0000 sec
							Root Dispersion: 0.0000 sec
							Reference ID: Unidentified reference source 'AIRP'
							Reference Timestamp: Jan 1, 1970 00:00:00.000000000 UTC
							Origin Timestamp: Jan 1, 1900 00:07:32.783880000 UTC
							Receive Timestamp: Jan 1, 1900 00:07:32.786774000 UTC
							Transmit Timestamp: Jan 1, 1900 00:07:32.786994000 UTC*/

							long receiveTime = System.currentTimeMillis();
							TimeStamp serverReceiveTime = new TimeStamp(new Date(receiveTime));

							ByteBuffer packetBuffer = ByteBuffer.wrap(packet.getData());
							packetBuffer.order(ByteOrder.BIG_ENDIAN);
							packetBuffer.position(40);

							long clientSentTimeTime = packetBuffer.getLong();
							ByteBuffer ntpPacket = ByteBuffer.allocate(48);
							ntpPacket.order(ByteOrder.BIG_ENDIAN);
							ntpPacket.put((byte)0x24);
							ntpPacket.put((byte)0x01);
							ntpPacket.put((byte) 0x02);
							ntpPacket.put((byte) 0x00);

							// delay
							ntpPacket.putInt(0);
							// Dispersion
							ntpPacket.putInt(0);
							// Reference ID
							String refId = "AIRP";
							try {
								ntpPacket.put(refId.getBytes("UTF-8"));
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							TimeStamp ntpBaseTime = new TimeStamp(DATE_BASE_TIME);
							Log.d(TAG,"ntp sever port: ntpBaseTime = " + ntpBaseTime.getDate());
							ntpPacket.putLong(ntpBaseTime.ntpValue());
							TimeStamp clientTimeStamp = new TimeStamp(clientSentTimeTime);
							Log.d(TAG,"clientTimeStamep = " + clientTimeStamp.getDate());
							ntpPacket.putLong(clientSentTimeTime);
							ntpPacket.putLong(serverReceiveTime.ntpValue());
							Log.d(TAG, "serverReceiveTime  = " + serverReceiveTime.getDate() );
							TimeStamp ntpServerSendTime = TimeStamp.getCurrentTime();
							ntpPacket.putLong(ntpServerSendTime.ntpValue());
							Log.d(TAG,"ntp sever port: currentNtpTime = " + ntpServerSendTime.getDate());

							try {
								socket.send(new DatagramPacket(ntpPacket.array(), ntpPacket.capacity(), packet.getAddress(), packet.getPort()));
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}
		};
		mNtpServerThread.start();
	}

	private void closeNtpServer() {
		if (mNtpServSock != null) {
			if(null != udpListener) {
				udpListener.stopThread();
			}
			mNtpServSock.close();
			mNtpServSock = null;
		}
	}
}
