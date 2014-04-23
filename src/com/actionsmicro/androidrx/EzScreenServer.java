package com.actionsmicro.androidrx;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.actionsmicro.ezcast.imp.androidrx.AndroidRxFinder;
import com.actionsmicro.web.JsonRpcOverHttpServer;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.NotificationHandler;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class EzScreenServer {
	public interface EzScreenServerDelegate {

		void displayUrl(String url);

		void resetToStandby();

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
		
	}
	protected static final String TAG = "EzScreenServer";
	private EzScreenServerDelegate ezScreenServerDelegate;
	private String name;
	private Context context;
	private JsonRpcOverHttpServer jsonRpcOverHttpServer;
	private InetAddress inetAddress;
	
	public EzScreenServer(Context context, InetAddress inetAddress, String name, EzScreenServerDelegate delegate) {
		this.name = name;
		this.context = context;
		this.inetAddress = inetAddress;
		this.ezScreenServerDelegate = delegate;
	}
	public String getName() {
		return name;
	}
	private Handler mainHandler = new Handler(Looper.getMainLooper());
	private Runnable resetToStandby = new Runnable() {

		@Override
		public void run() {
			ezScreenServerDelegate.resetToStandby();
		}
		
	};
	private JmDNS jmDNS;
	private ServiceInfo serviceInfo;
	private static final int HEARTBEAT_TIMEOUT = 3000;
	public void start() {
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
		final JmDNS jmDNS2 = jmDNS;
		final ServiceInfo serviceInfo2 = serviceInfo;
		new Thread(new Runnable() {

			@Override
			public void run() {
				cleanUpMdns(jmDNS2, serviceInfo2);
			}
			
		}).start();
		jmDNS = null;
		mainHandler.removeCallbacks(resetToStandby);
		if (jsonRpcOverHttpServer != null) {
			jsonRpcOverHttpServer.stop();
			jsonRpcOverHttpServer = null;
		}
	}
	private void cleanUpMdns(JmDNS jmDns, ServiceInfo serviceInfo) {
		if (jmDns != null) {
			if (serviceInfo != null) {
				jmDns.unregisterService(serviceInfo);
			}
			try {
				jmDns.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private synchronized void initEzAndroidRx() {
		jsonRpcOverHttpServer = new JsonRpcOverHttpServer(context, 0) {
			@Override 
			public Response serve(IHTTPSession session) {
				
				if (session.getUri().equalsIgnoreCase("/jsonrpc")) {
					return super.serve(session);
				}
				return new Response("");
			}
		};
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
					return new String[]{"display", "stop_display", "play", "pause", "resume", "stop", "seek", "increase_volume", "decrease_volume"};
				}

				@Override
				public JSONRPC2Response process(JSONRPC2Request request,
						MessageContext arg1) {
					Map<String, Object> namedParams = request.getNamedParams();
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
					}
					return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, request.getID());
				}
				
			});
			jsonRpcOverHttpServer.start();
			
			jmDNS = JmDNS.create(inetAddress);
			HashMap<String, String> txtRecord = new HashMap<String, String>();					
			serviceInfo = ServiceInfo.create(AndroidRxFinder.SERVICE_TYPE+"local.", EzScreenServer.this.name, jsonRpcOverHttpServer.getListeningPort(), 0, 0, txtRecord);
			jmDNS.registerService(serviceInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}