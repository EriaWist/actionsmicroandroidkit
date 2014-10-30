package com.actionsmicro.androidkit.ezcast.imp.androidrx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.utils.Log;
import com.actionsmicro.web.JsonRpcOverHttpServer;
import com.actionsmicro.web.SimpleContentUriHttpFileServer;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.NotificationHandler;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class AndroidRxMediaPlayerApi extends AndroidRxApi implements
		MediaPlayerApi {

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
	protected static final String TAG = "AndroidRxMediaPlayerApi";
	private SimpleContentUriHttpFileServer simpleHttpFileServer;
	private JsonRpcOverHttpServer jsonRpcOverHttpServer;
	private MediaPlayerStateListener mediaPlayerStateListener;
	public AndroidRxMediaPlayerApi(MediaPlayerApiBuilder apiBuilder) {
		super(apiBuilder);
		mediaPlayerStateListener = apiBuilder.getMediaPlayerStateListener();
	}
	@Override
	public void connect() {
		super.connect();
		jsonRpcOverHttpServer = new JsonRpcOverHttpServer(getContext(), 0, ".*");
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
						mediaPlayerStateListener.mediaPlayerTimeDidChange(AndroidRxMediaPlayerApi.this,  Float.valueOf(namedParams.get("time").toString()).longValue());
					}
				} else if (EZCASTPLAYER_ONDURATIONCHANGE.equals(notification.getMethod())) {
					Log.d(TAG, EZCASTPLAYER_ONDURATIONCHANGE+":"+namedParams.get("duration"));
					long duration = Float.valueOf(namedParams.get("duration").toString()).longValue();
					setMediaUsageDuration((int) duration);
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDurationIsReady(AndroidRxMediaPlayerApi.this, duration);
					}
				} else if (EZCASTPLAYER_ONERROR.equals(notification.getMethod())) {
					int errorCode = convertErrorCode(Integer.valueOf(namedParams.get("error").toString()));
					setMediaUsageResultCode(namedParams.get("error").toString(), errorCode);
					commitMediaUsageTracking();
					Log.d(TAG, EZCASTPLAYER_ONERROR+":"+errorCode);
					currentState = State.STOPPED;
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidFailed(AndroidRxMediaPlayerApi.this, errorCode);
					}
				} else if (EZCASTPLAYER_ONPLAY.equals(notification.getMethod())) {
					Log.d(TAG, EZCASTPLAYER_ONPLAY+":");
					currentState = State.PLAYING;
				} else if (EZCASTPLAYER_ONLOADSTART.equals(notification.getMethod())) {
					Log.d(TAG, EZCASTPLAYER_ONLOADSTART+":");
					currentState = State.PLAYING;
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidStart(AndroidRxMediaPlayerApi.this);
					}
				} else if (EZCASTPLAYER_ONENDED.equals(notification.getMethod())) {
					Log.d(TAG, EZCASTPLAYER_ONENDED+":");
					commitMediaUsageTracking();
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidStop(AndroidRxMediaPlayerApi.this);
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
	}

	@Override
	public void disconnect() {
		stop();
		if (jsonRpcOverHttpServer != null) {
			jsonRpcOverHttpServer.stop();
			jsonRpcOverHttpServer = null;
		}
		super.disconnect();
	}
	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		// TODO Auto-generated method stub

	}
	private State currentState = State.STOPPED;
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
		invokeRpcMethod("stop");
		currentState = State.STOPPED;
		stopHttpFileServer();		
		commitMediaUsageTracking();
		return true;
	}
	private void stopHttpFileServer() {
		if (simpleHttpFileServer != null) {
			simpleHttpFileServer.stop();
			simpleHttpFileServer = null;
		}
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
			String mimeType = simpleHttpFileServer.getMimeType();
			if (mimeType != null && mimeType.startsWith("audio")) {
				beginLocalAudioUsageTracking(url, title);
			} else {
				beginLocalMediaUsageTracking(url, title);
			}
		} else {
			beginRemoteMediaUsageTracking(mediaUriString, userAgentString,
					title);
		}
		final HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("url", mediaUriString);
		params.put("callback", jsonRpcOverHttpServer.getServerUrl());
		invokeRpcMethod("play", params);		
		return true;
	}	

}
