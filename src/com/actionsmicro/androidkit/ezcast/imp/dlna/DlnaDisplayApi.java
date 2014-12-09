package com.actionsmicro.androidkit.ezcast.imp.dlna;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable.CurrentMediaDuration;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.model.item.VideoItem;
import org.seamless.util.MimeType;

import android.content.Context;
import android.graphics.YuvImage;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.actionsmicro.airplay.mirror.TsStreamer;
import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.utils.Log;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

public class DlnaDisplayApi extends DlnaApi implements DisplayApi {

	private static final String TAG = "DlnaDisplayApi";
	private Service avtransportService;
	private SubscriptionCallback avtransportSubscription;
	private Context context;
	protected TransportState currentTransportState;
	private TsStreamer tsStreamer;
	private AsyncServerSocket m3u8ServerSocket;

	public DlnaDisplayApi(DisplayApiBuilder apiBuilder) {
		super(apiBuilder);
		context = apiBuilder.getContext();
	}
	@Override
	public void connect() {
		connectToAvTransportService();
		super.connect();
	}
	private void connectToAvTransportService() {
		if (avtransportService != null) {
			return;
		}
		DlnaDeviceInfo dlnaDevice = (DlnaDeviceInfo)getDevice();
		avtransportService = dlnaDevice.getDevice().findService(new UDAServiceId("AVTransport"));
		avtransportSubscription = new SubscriptionCallback(avtransportService) {

			@Override
			protected void ended(GENASubscription arg0, CancelReason arg1,
					UpnpResponse arg2) {
				Log.d(TAG+".SubscriptionCallback", "ended");
				if (avtransportSubscription != null) {
					avtransportSubscription.end();
					avtransportSubscription = null;
				}
			}

			@Override
			protected void established(GENASubscription arg0) {
				Log.v(TAG+".SubscriptionCallback", "established:"+arg0.toString());
				
			}

			@Override
			protected void eventReceived(GENASubscription sub) {
				Map<String, StateVariableValue<?>> values = sub.getCurrentValues();
				Log.v(TAG+".SubscriptionCallback", "eventReceived:"+sub.toString()+"\n values:"+values);
				if (values.containsKey("LastChange")) {
					try {
						LastChange lastChange = new LastChange(new AVTransportLastChangeParser(), values.get("LastChange").toString());
						updateStateIfNeeded(lastChange);
						updateDurationIfNeeded(lastChange);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			private void updateDurationIfNeeded(LastChange lastChange) {
				CurrentMediaDuration eventedValue = lastChange.getEventedValue(0, AVTransportVariable.CurrentMediaDuration.class);
				if (eventedValue == null) return;
				String durationStr = eventedValue.getValue();
				if (durationStr != null) {
					int duration = parseFormattedTimeString(durationStr);
					Log.d(TAG, "CurrentMediaDuration:"+durationStr+"("+duration+")");
				}
			}


			private void updateStateIfNeeded(LastChange lastChange) {
				org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable.TransportState eventedValue = lastChange.getEventedValue(0, AVTransportVariable.TransportState.class);
				if (eventedValue == null) return;
				TransportState transportState = eventedValue.getValue();
				if (transportState != null) {
					Log.d(TAG+".SubscriptionCallback", "transportState:"+transportState);
					currentTransportState = transportState;
				}
			}

			@Override
			protected void eventsMissed(GENASubscription arg0, int arg1) {
				Log.d(TAG+".SubscriptionCallback", "eventsMissed:"+arg0.toString());
				
			}

			@Override
			protected void failed(GENASubscription arg0, UpnpResponse arg1,
					Exception arg2, String arg3) {
				Log.d(TAG+".SubscriptionCallback", "failed:"+arg3);
				
			}
			
		};
		UpnpService.getUpnpService().execute(avtransportSubscription);
	}
	@Override
	public void disconnect() {
		super.disconnect();
		stopVideo();
		if (avtransportSubscription != null) {
			avtransportSubscription.end();
			avtransportSubscription = null;
		}
		avtransportService = null;
		releaseTsStreamer();
		stopM3u8Server();
		m3u8Server = null;
	}
	@Override
	public void startDisplaying() {
	}
	@Override
	public void stopDisplaying() {

	}

	@Override
	public void resendLastImage() throws Exception {
		// TODO Auto-generated method stub

	}
	
	private static int stupidCounter = 0;
	@Override
	public void sendJpegEncodedScreenData(InputStream input, long length)
			throws Exception {

	}

	@Override
	public void sendYuvScreenData(YuvImage yuvImage, int quailty)
			throws Exception {
		requestRemoteToPlayTsStreamer();
		displayYuvImageViaTsStreamer(yuvImage);
	}
	private void displayYuvImageViaTsStreamer(YuvImage yuvImage) {
		if (tsStreamer != null) {
			tsStreamer.displayYuvImage(yuvImage);
		}
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
			
			try {
				setAVTransportURI(getTsServerUrl());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private void stopM3u8Server() {
		if (m3u8Server != null) {
			m3u8Server.stop();
		}
	}
	private String getM3u8ServerUrl() {
		if (m3u8ServerSocket != null) {
			try {
				return new URL("http", getIPAddress(true), m3u8ServerSocket.getLocalPort(), "").toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	private AsyncHttpServer m3u8Server = new AsyncHttpServer() {
		@Override
		protected void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getHeaders().getHeaders().getStatusLine());
		}
	};
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
	private String getTsServerUrl() {
		if (tsStreamer != null) {
			try {
				return new URL("http", getIPAddress(true), tsStreamer.getListeningPort(), "").toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	private void releaseTsStreamer() {
		if (tsStreamer != null) {
			tsStreamer.release();
			tsStreamer = null;
		}
	}
	protected void stopVideo() {
		if (avtransportService == null) return;
		UpnpService.getUpnpService().execute(new Stop(avtransportService) {
        	@Override
			public void success(ActionInvocation invocation) {
			}
            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
            	Log.e(TAG, defaultMsg);
            }
        });
	}
	//TODO refactor to DRY
	private void setAVTransportURI(String mediaUriString) throws Exception {
		Log.d(TAG+".SetAVTransportURI", "set: "+mediaUriString);
		DIDLContent didl = new DIDLContent();
		MimeType mimeType = new MimeType("video", "mp2t");
        didl.addItem(new VideoItem("1", "0", null, null, new Res(mimeType, 0l, null, null, mediaUriString)));
		
		String metadata = new DIDLParser().generate(didl);
		UpnpService.getUpnpService().execute(new SetAVTransportURI(avtransportService, mediaUriString, metadata) {
			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse response, String defaultMsg) {
				Log.e(TAG+".SetAVTransportURI", defaultMsg);
			}
			@Override
			public void success(ActionInvocation invocation) {
				if (avtransportService == null) return;
				playImp();
			}
		});
	}
	//TODO refactor to DRY
	private void playImp() {
		if (avtransportService == null) return;
		Log.d(TAG+".Play", "playImp");		
		UpnpService.getUpnpService().execute(new Play(avtransportService) {
			@Override
			public void success(ActionInvocation invocation) {
			}
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
				// Something was wrong
				Log.e(TAG, defaultMsg);
			}
		});
	}
	private String getIPAddress(boolean useIPv4) { //TODO  DRY
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();

		String ipString = String.format(
				"%d.%d.%d.%d",
				(ip & 0xff),
				(ip >> 8 & 0xff),
				(ip >> 16 & 0xff),
				(ip >> 24 & 0xff));

		return ipString;
    }
}
