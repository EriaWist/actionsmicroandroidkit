package com.actionsmicro.airplay;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.actionsmicro.airplay.http.PlistBody;
import com.actionsmicro.utils.Log;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.XMLPropertyListParser;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.libcore.RawHeaders;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

public class AirPlayClient {
	private static final String TAG = "AirPlayClient";
	private InetAddress serverAddress;
	private AsyncServer reverseConnection = new AsyncServer();
	private AsyncHttpServer eventServer;
	private HandlerThread timerThread;
	private Handler timerHandler;
	public AirPlayClient(Context context, InetAddress inetAddress) {
		this.serverAddress = inetAddress;
		reverseConnection.run(true, true);
		eventServer = new AsyncHttpServer() {
			@Override
			protected void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Log.d(TAG, "onRequest:"+request.getHeaders().getHeaders().getStatusLine());
		    }
			@Override
			protected AsyncHttpRequestBody<String> onUnknownBody(RawHeaders headers) {
		        return new PlistBody();
		    }
		};
		eventServer.post("/event", new HttpServerRequestCallback() {
			@Override
			public void onRequest(final AsyncHttpServerRequest request,
					final AsyncHttpServerResponse response) {
				try {
					NSDictionary event = (NSDictionary)XMLPropertyListParser.parse(request.getBody().get().toString().getBytes());
					Log.d(TAG, event.toXMLPropertyList());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (PropertyListFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				response.responseCode(200);
				response.end();
			}			
		});

		establishReverseHttpConnection();
		
		timerThread = new HandlerThread("AirPlayTimerThread");
		timerThread.start();
		timerHandler = new Handler(timerThread.getLooper());
	}
	private void establishReverseHttpConnection() {
		try {			
			RawHeaders headers = new RawHeaders();
			headers.add("Upgrade", "PTTH/1.0");
			headers.add("Connection", "Upgrade");
			headers.add("X-Apple-Purpose", "event");
			headers.add("Content-Lengthe", "0");
			headers.add("User-Agent", "MediaControl/1.0");
			headers.add("X-Apple-Session-ID", "368e90a4-5de6-4196-9e58-9917bdd4ffd7");
			AsyncHttpPost reverse = new AsyncHttpPost(getServerUri("/reverse"), headers);
			reverse.setTimeout(0);
			AsyncHttpClient httpClient = new AsyncHttpClient(reverseConnection);
			httpClient.executeString(reverse, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
			            return;
			        }
			        AsyncSocket socket = source.detachSocket();
			        Log.d(TAG, "Server says: " + source.getHeaders().getHeaders().getStatusLine());
			        eventServer.establishConnection(socket);
			    }
			});
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	public void playVideo(String url) {
		try {
			AsyncHttpPost playVideo = new AsyncHttpPost(getServerUri("/play"));
			playVideo.addHeader("User-Agent", "MediaControl/1.0");
			playVideo.addHeader("X-Apple-Session-ID", "368e90a4-5de6-4196-9e58-9917bdd4ffd7");
			NSDictionary playbackInfo = new NSDictionary();
			playbackInfo.put("Content-Location", url);
			playbackInfo.put("Start-Position", 0.0);
			
			StringBody body = new PlistBody(playbackInfo.toXMLPropertyList());
			playVideo.setBody(body);
			AsyncHttpClient.getDefaultInstance().executeString(playVideo, new StringCallback() {
			    @Override
			    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
			        if (e != null) {
			            e.printStackTrace();
			            return;
			        }
			        Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
			        timerHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							try {
								AsyncHttpClient.getDefaultInstance().getString(getServerUri("/playback-info").toASCIIString(), new StringCallback() {

									@Override
									public void onCompleted(Exception e,
											AsyncHttpResponse source, String result) {
										if (e != null) {
								            e.printStackTrace();
								            return;
								        }
										Log.d(TAG, source.getRequest().getRequestLine() + " Server says: " + result);
									}
									
								});
							} catch (URISyntaxException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							timerHandler.postDelayed(this, 1000);
						}
			        	
			        }, 1000);
			    }
			});
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	private URI getServerUri(String path) throws URISyntaxException {
		return new URI("http", null, serverAddress.getHostAddress(), 7000, path, null, null);
	}
	public void close() {
		if (reverseConnection != null) {
			reverseConnection.stop();
		}
		if (timerThread != null) {			
			timerThread.quit();
			timerThread = null;
		}
	}
}
