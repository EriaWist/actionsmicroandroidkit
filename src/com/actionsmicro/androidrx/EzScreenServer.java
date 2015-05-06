package com.actionsmicro.androidrx;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.actionsmicro.androidkit.ezcast.EzCastSdk;
import com.actionsmicro.androidkit.ezcast.imp.androidrx.AndroidRxFinder;
import com.actionsmicro.bonjour.BonjourServiceAdvertiser;
import com.actionsmicro.utils.Log;
import com.actionsmicro.web.JsonRpcOverHttpServer;
import com.actionsmicro.p2p.P2PHelper;
import com.actionsmicro.p2p.P2PWebApi;
import com.actionsmicro.p2p.HostDevice;
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
	
	private P2PHelper p2phelper= null;
	private P2PWebApi p2pwebapi=null;
	
	private String hostuuidDisplay;
	private int hostportDisplay;
	
	private String hostuuidcb;
	private int hostportcb;
	
	private String hostuuidMedia;
	private int hostportMedia;
	
	private static final int DISPLAY_API_LISTENED_PORT=10007;
	private static final int MEDIAPLAYER_API_LISTRENED_PORT=10005;
	private static final int JSONRPC_CALLBACK_LISTENED_PORT=10006;		
	
	public EzScreenServer(Context context, InetAddress inetAddress, String name, String deviceID, boolean mmr, boolean wmr, EzScreenServerDelegate delegate) {
		this.name = name;
		this.context = context;
		this.inetAddress = inetAddress;
		this.ezScreenServerDelegate = delegate;
		this.deviceID = deviceID;
		this.mmr=mmr;
		this.wmr=wmr;
		p2phelper=new P2PHelper(context);
		p2pwebapi=new P2PWebApi();
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
			
			//TODO: modified by eric
			String strHostUUID=P2PWebApi.getEzScreenHostuuidFromSharePreferences(context);//EzScreenServer.this.name;
			String account=EzCastSdk.getp2pwebapi().getEzcastAccountFromSharePreferences();
			EzCastSdk.getp2pwebapi().DeleteAccountDevice(account, strHostUUID);
			//EzCastSdk.getp2phelper().stopConntHost(strHostUUID);
			//end
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
					return new String[]{"display", "stop_display", "play", "pause", "resume", "stop", "seek", "increase_volume", "decrease_volume"};
				}

				@Override
				public JSONRPC2Response process(JSONRPC2Request request,
						MessageContext arg1) {
					Map<String, Object> namedParams = request.getNamedParams();
					if ("display".equals(request.getMethod())) {
						String url = (String) namedParams.get("url");
						if (url != null) {
							
							//TODO:modified by eric
							if (url.contains("?hostuuid="))
							{
								//start conn client
								//parse url(hostuuid:hostport)
								com.actionsmicro.p2p.HostDevice hostdevice=EzCastSdk.getp2pwebapi().parseP2PURI(url);
								hostuuidDisplay= hostdevice.hostuuid;
								hostportDisplay =  hostdevice.hostport;
								int nTcpListenedPortDisplay = DISPLAY_API_LISTENED_PORT;
								int nTimeoutMS=10000;
								nTcpListenedPortDisplay=EzCastSdk.getp2phelper().startConnClient(nTcpListenedPortDisplay,hostuuidDisplay,hostportDisplay,
										P2PWebApi.mCONN_SERVICE_DOMAIN, P2PWebApi.mSERVICE_PORT, nTimeoutMS);
								//redirect to local url 
								url="http://127.0.0.1:"+String.valueOf(nTcpListenedPortDisplay)+"/";
																
							}
							ezScreenServerDelegate.displayUrl(url);
							return new JSONRPC2Response(Long.valueOf(0), request.getID());
						}
						return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, request.getID());
					} else if ("stop_display".equals(request.getMethod())) {
						ezScreenServerDelegate.stopDisplay();
						EzCastSdk.getp2phelper().stopConnClient(DISPLAY_API_LISTENED_PORT,hostuuidDisplay,hostportDisplay);
						return new JSONRPC2Response(Long.valueOf(0), request.getID());
					} else if ("play".equals(request.getMethod())) {
						if (namedParams.containsKey("url")) {
							//ezScreenServerDelegate.playVideo(
							String url=(String)namedParams.get("url");
							String callback=(String)namedParams.get("callback");
							
							//TODO:modified by eric
							if (url.contains("?hostuuid="))
							{
								//start conn client
								//parse url(hostuuid:hostport)
								com.actionsmicro.p2p.HostDevice hostdevice1=P2PWebApi.parseP2PURI(url);
								int nTcpListenedPortMedia = MEDIAPLAYER_API_LISTRENED_PORT;
								hostuuidMedia= hostdevice1.hostuuid;
								hostportMedia =  hostdevice1.hostport;
								int nTimeoutMS=10000;
								nTcpListenedPortMedia=EzCastSdk.getp2phelper().startConnClient(nTcpListenedPortMedia, hostuuidMedia, hostportMedia,
										P2PWebApi.mCONN_SERVICE_DOMAIN, P2PWebApi.mSERVICE_PORT, nTimeoutMS);
								//redirect to local url 
								url="http://127.0.0.1:"+String.valueOf(nTcpListenedPortMedia);
							}
							if (callback.contains("?hostuuid="))
							{
								//start conn client
								//parse url(hostuuid:hostport)
								HostDevice hostdevicecb=P2PWebApi.parseP2PURI(callback);
								int nTcpListenedPortcb = JSONRPC_CALLBACK_LISTENED_PORT;
								hostuuidcb= hostdevicecb.hostuuid;
								hostportcb =  hostdevicecb.hostport;
								int nTimeoutMS=10000;
								nTcpListenedPortcb=EzCastSdk.getp2phelper().startConnClient(nTcpListenedPortcb, hostuuidcb, hostportcb,
										P2PWebApi.mCONN_SERVICE_DOMAIN, P2PWebApi.mSERVICE_PORT, nTimeoutMS);
								//redirect to local callback 
								callback="http://127.0.0.1:"+String.valueOf(nTcpListenedPortcb);
								
							}
							//end
							ezScreenServerDelegate.playVideo(url, callback);
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
						EzCastSdk.getp2phelper().stopConnClient(JSONRPC_CALLBACK_LISTENED_PORT, hostuuidcb, hostportcb);
						EzCastSdk.getp2phelper().stopConnClient(MEDIAPLAYER_API_LISTRENED_PORT, hostuuidMedia, hostportMedia);
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
			HashMap<String, String> txtRecord = new HashMap<String, String>();
			txtRecord.put("txtvers", "20140515");
			txtRecord.put("srcvers", "20140515");
			txtRecord.put("deviceid", deviceID);
			//2015-03-13 erichwang for mac-mirror and windows-mirror
			txtRecord.put("mmr", String.valueOf(mmr));
			txtRecord.put("wmr", String.valueOf(wmr));
			
			//TODO: modified by eric
			String strHostUUID=P2PWebApi.getEzScreenHostuuidFromSharePreferences(context);//EzScreenServer.this.name;
			int nTimeoutMS=5000;
			EzCastSdk.getp2phelper().starConntHost(strHostUUID, P2PWebApi.mCONN_SERVICE_DOMAIN, P2PWebApi.mSERVICE_PORT, nTimeoutMS);
			int port = jsonRpcOverHttpServer.getListeningPort();
			String type = "jsonrpc";
			EzCastSdk.getp2pwebapi().UpdateHostUUIDPort(strHostUUID,String.valueOf(port), type);
			String account=EzCastSdk.getp2pwebapi().getEzcastAccountFromSharePreferences();
			Log.d(TAG, "InsertAccountDeviceURL account="+account+" deviceuuid="+strHostUUID);
			EzCastSdk.getp2pwebapi().InsertAccountDeviceURL(account,strHostUUID);
			//end
			
			bonjourServiceAdvertiser = new BonjourServiceAdvertiser(ServiceInfo.create(AndroidRxFinder.SERVICE_TYPE+"local.", EzScreenServer.this.name, jsonRpcOverHttpServer.getListeningPort(), 0, 0, txtRecord));
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
	}
}
