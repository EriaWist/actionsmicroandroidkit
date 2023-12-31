package com.actionsmicro.androidkit.ezcast.imp.dlna;

import android.content.Context;
import android.graphics.YuvImage;

import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.mp4.Mp4Streamer;
import com.actionsmicro.utils.Device;
import com.actionsmicro.utils.Log;
import com.koushikdutta.async.AsyncServerSocket;

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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class DlnaDisplayApi extends DlnaApi implements DisplayApi {

	private static final String TAG = "DlnaDisplayApi";
	private Service avtransportService;
	private SubscriptionCallback avtransportSubscription;
	private Context context;
	protected TransportState currentTransportState;
	private Mp4Streamer mp4Streamer;
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
					if (currentTransportState == TransportState.STOPPED) {
						if (mp4Streamer != null) {
//							mp4Streamer.release();
//							mp4Streamer = null;
						}
					}
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

	@Override
	public void sendH264EncodedScreenData(byte[] contents, int width, int height) throws Exception {

	}

	private void displayYuvImageViaTsStreamer(YuvImage yuvImage) {
		if (mp4Streamer != null) {
			mp4Streamer.displayYuvImage(yuvImage);
		}
	}
	private void requestRemoteToPlayTsStreamer() {
		if (mp4Streamer == null) {
			mp4Streamer = new Mp4Streamer();
			mp4Streamer.setDelegate(new Mp4Streamer.Delegate() {
				
				@Override
				public void onSizeChanged() {
					stopVideo();
					releaseTsStreamer();
					requestRemoteToPlayTsStreamer();
				}
			});
			mp4Streamer.start();
			Log.d(TAG, "tsStreamer running at:" + getTsServerUrl());
			
			try {
				setAVTransportURI(getTsServerUrl());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	

	private String getTsServerUrl() {
		if (mp4Streamer != null) {
			try {
				return new URL("http", Device.getHostIpAddress(context, true), mp4Streamer.getListeningPort(), "").toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	private void releaseTsStreamer() {
		if (mp4Streamer != null) {
			mp4Streamer.release();
			mp4Streamer = null;
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
}
