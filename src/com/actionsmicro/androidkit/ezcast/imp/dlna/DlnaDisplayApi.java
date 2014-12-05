package com.actionsmicro.androidkit.ezcast.imp.dlna;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.androidkit.ezcast.DisplayApiBuilder;
import com.actionsmicro.graphics.YuvImageToJpegHelper;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.callback.WritableCallback;
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

	public DlnaDisplayApi(DisplayApiBuilder apiBuilder) {
		super(apiBuilder);
		context = apiBuilder.getContext();
		initHttpServer();
	}
	@Override
	public void connect() {
		connectToAvTransportService();
		startHttpServer();
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
		if (avtransportSubscription != null) {
			avtransportSubscription.end();
			avtransportSubscription = null;
		}
		avtransportService = null;
		stopHttpServer();
	}
	@Override
	public void startDisplaying() {
		try {
			setAVTransportURI(getHttpServerUrl()+"/"+(stupidCounter++ % 2));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void stopDisplaying() {

	}

	@Override
	public void resendLastImage() throws Exception {
		// TODO Auto-generated method stub

	}
	private AsyncHttpServer httpServer = new AsyncHttpServer() {
		@Override
		protected void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getHeaders().getHeaders().getStatusLine());
		}
	};
	private AsyncServerSocket serverSocket;
	private AsyncServer httpAsyncServer = new AsyncServer(); 
	private void startHttpServer() {
		stop = false;
		serverSocket = httpServer.listen(httpAsyncServer, 0);
		Log.d(TAG, "http server listen on port:" + serverSocket.getLocalPort());
	}
	private void stopHttpServer() {
		if (httpServer != null) {
			stop = true;
			httpServer.stop();
		}
	}
	private boolean stop = false;
	private void initHttpServer() {
		httpServer.get("/.", new HttpServerRequestCallback() {

			@Override
			public void onRequest(AsyncHttpServerRequest request,
					final AsyncHttpServerResponse response) {
				response.getHeaders().getHeaders().add("Content-Type", "video/x-motion-jpeg");
				response.responseCode(200);
				WritableCallback writer = new WritableCallback() {
					String boundaryStart = "--myboundary\r\nContent-type: image/jpg\r\nContent-Length: ";
					String boundaryEnd = "\r\n\r\n";
					
					@Override
					public void onWriteable() {
						try {
							FileOutputStream fileOut = new FileOutputStream("/sdcard/test.mjpg");
							while (!stop) {
								synchronized (jpegBuffer) {
									if (jpegBuffer.size() == 0){
										try {
											jpegBuffer.wait();
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
									if (jpegBuffer.size() > 0) {
										byte[] byteArray = jpegBuffer.toByteArray();
										byte[] boundaryBytes = (boundaryStart + byteArray.length + boundaryEnd).getBytes();
										response.sendStream(new ByteArrayInputStream(boundaryBytes), boundaryBytes.length);
										Log.d(TAG, "sendStream size:"+byteArray.length);
										response.sendStream(new ByteArrayInputStream(byteArray), byteArray.length);
										if (fileOut != null) {
											fileOut.write(boundaryBytes);
											fileOut.write(byteArray);
										}
										jpegBuffer.reset();
									}
								}
							}
							if (fileOut != null) {
								fileOut.close();
							}
						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}

					private Object FileOutputStream(String string) {
						// TODO Auto-generated method stub
						return null;
					}
				};
				response.setWriteableCallback(writer);
				writer.onWriteable();
			}			
		});
//		httpServer.addAction("HEAD", "/.", new HttpServerRequestCallback() {
//
//			@Override
//			public void onRequest(AsyncHttpServerRequest request,
//					final AsyncHttpServerResponse response) {
//				synchronized (jpegBuffer) {
//					if (jpegBuffer.size() < 0) {
//						try {
//							jpegBuffer.wait();
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					}
//					response.getHeaders().getHeaders().add("Content-Type", "image/jpeg");
//					response.getHeaders().getHeaders().add("Content-Length", String.valueOf(jpegBuffer.size()));
//					Log.d(TAG, "HEAD response Content-Length:"+jpegBuffer.size());
//				}
//				response.responseCode(200);
//				response.end();
//			}			
//		});
	}
	public int getListeningPort() {
		return serverSocket.getLocalPort();
	}
	private ByteArrayOutputStream jpegBuffer = new ByteArrayOutputStream(500*1024);
	
	private static int stupidCounter = 0;
	@Override
	public void sendJpegEncodedScreenData(InputStream input, long length)
			throws Exception {
//		if (currentTransportState == null || currentTransportState != TransportState.TRANSITIONING) {
//			if (stupidCounter % 2 == 0) {
//				setNextAvTransportUri("http://192.168.2.100/webezcast/images/163.jpg");
//			} else {
//				setNextAvTransportUri("http://192.168.2.100/webezcast/images/1youtube.jpg");			
//			}
//			stupidCounter ++;
			synchronized (jpegBuffer) {
				if (jpegBuffer.size() == 0) {
					jpegBuffer.reset();
					try {
						Utils.dump(input, jpegBuffer);
						jpegBuffer.notify();
					} catch (IOException e1) {
						e1.printStackTrace();
						jpegBuffer.reset();
					}
				}
			}
//		}
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
	//TODO refactor to DRY
	private void setAVTransportURI(String mediaUriString) throws Exception {
		Log.d(TAG+".SetAVTransportURI", "set: "+mediaUriString);
		DIDLContent didl = new DIDLContent();
		MimeType mimeType = new MimeType("video", "x-motion-jpeg");
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
	private String getHttpServerUrl() {
			try {
				return new URL("http", getIPAddress(true), getListeningPort(), "").toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return null;
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
