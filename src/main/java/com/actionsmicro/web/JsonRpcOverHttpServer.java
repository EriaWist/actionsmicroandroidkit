package com.actionsmicro.web;

import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.actionsmicro.utils.Log;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Message;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import com.thetransactioncompany.jsonrpc2.server.NotificationHandler;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;


public class JsonRpcOverHttpServer {
	private static final String TAG = "JsonRpcOverHttpServer";
	private Context context;
	private int portNumber;
	private AsyncHttpServer httpServer;

	public JsonRpcOverHttpServer(Context context, int portNumber, String path) {		
		this.context = context;
		this.portNumber = portNumber;
		httpServer = new AsyncHttpServer() {
			@Override
			protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Log.d(TAG, "onRequest:"+request.getMethod()+" "+request.getPath());
				return false;
		    }
		};
		httpServer.post(path, new HttpServerRequestCallback() {

			@Override
			public void onRequest(AsyncHttpServerRequest request,
					AsyncHttpServerResponse response) {
				try {
				JSONRPC2Message message = JSONRPC2Message.parse(request.getBody().get().toString());
				Log.d(TAG, "json-rpc:"+message);
				if (message instanceof JSONRPC2Request) {
					JSONRPC2Response resp = dispatcher.process((JSONRPC2Request)message, null);
					if (resp != null) {
						String jsonResponse = resp.toString();
						response.setContentType("application/json");
						response.getHeaders().add("Access-Control-Allow-Origin", "*");
						response.send("application/json", jsonResponse);
						Log.d(TAG, "response:"+jsonResponse);
					}
				} else if (message instanceof JSONRPC2Notification) {
					dispatcher.process((JSONRPC2Notification) message, null);
					response.code(200);
					response.getHeaders().add("Access-Control-Allow-Origin", "*");
					response.end();
				}		
				} catch (JSONRPC2ParseException e) {
					response.code(400);
					response.getHeaders().add("Access-Control-Allow-Origin", "*");
					response.end();
				} finally {
					
				}
			}
			
		});
		httpServer.setErrorCallback(new CompletedCallback() {

			@Override
			public void onCompleted(Exception ex) {
				Log.e(TAG, "httpServer error:", ex);
			}
			
		});
		httpServer.addAction("OPTIONS", path, new HttpServerRequestCallback() {

			@Override
			public void onRequest(AsyncHttpServerRequest request,
					AsyncHttpServerResponse response) {
				Headers headers = response.getHeaders();
				headers.add("Access-Control-Allow-Origin", "*");
				headers.add("Access-Control-Allow-Methods", "POST, OPTIONS");
				headers.add("Access-Control-Allow-Headers", "X-Requested-With, accept, content-type");
				response.end();
			}
			
		});
	}
	private Dispatcher dispatcher = new Dispatcher();
	private AsyncServerSocket serverSocket;
	public String getServerUrl() {
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
	public void registerRpcRequestHandler(RequestHandler requestHandler) throws IllegalStateException {
		if (dispatcher != null) {
			dispatcher.register(requestHandler);
		} else {
			throw new IllegalStateException("dispatcher is null");
		}
	}
	public void registerRpcNotificationHandler(NotificationHandler notificationHandler) throws IllegalStateException {
		if (dispatcher != null) {
			dispatcher.register(notificationHandler);
		} else {
			throw new IllegalStateException("dispatcher is null");
		}
	}
	public void start() {
		serverSocket = httpServer.listen(portNumber);
	}
	public void stop() {
		AsyncServer.getDefault().stop();
		httpServer.stop();
	}
	public int getListeningPort() {
		return serverSocket.getLocalPort();
	}
}
