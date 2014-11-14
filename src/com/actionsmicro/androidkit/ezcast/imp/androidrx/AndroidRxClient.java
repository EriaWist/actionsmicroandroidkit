package com.actionsmicro.androidkit.ezcast.imp.androidrx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.actionsmicro.androidkit.ezcast.ConnectionManager;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.TrackableApi;
import com.actionsmicro.ezcom.jsonrpc.JSONRPC2Session;
import com.actionsmicro.graphics.YuvImageToJpegHelper;
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

public class AndroidRxClient implements DisplayApi, MediaPlayerApi {
	private static final String TAG = "AndroidRxClient";
	private static final int HEARTBEAT_PERIOD = 1000;

	private JSONRPC2Session jsonRpcSession;
	private InetAddress ipAddress;
	private int port;
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
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					JSONRPC2Session jsonRpcSession = getJsonRpcSession();
					if (jsonRpcSession != null) {
						jsonRpcSession.send(new JSONRPC2Request(method, params, generateRpcId()));
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
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidStop(AndroidRxClient.this);
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
		stopHttpFileServer();		
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

}
