package com.actionsmicro.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.actionsmicro.utils.Log;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Message;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import com.thetransactioncompany.jsonrpc2.server.NotificationHandler;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;


public class JsonRpcOverHttpServer extends NanoHTTPD {
	private static final String TAG = "JsonRpcOverHttpServer";
	private Context context;

	public JsonRpcOverHttpServer(Context context, int portNumber) {
		super(portNumber);
		this.context = context;
	}
	@Override 
	public Response serve(IHTTPSession session) {
		if (session.getMethod().equals(Method.OPTIONS)) {
			Response response = new Response("");
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
			response.addHeader("Access-Control-Allow-Headers", "X-Requested-With, accept, content-type");
			return response;
		}
		if (session.getMethod().equals(Method.POST)) {
			try {
				Map<String, String> files = new HashMap<String, String>();
				session.parseBody(files);
				Log.d(TAG, "files:"+files);
				String body = session.getParms().keySet().iterator().next();
				Log.d(TAG, "body:"+body);
				JSONRPC2Message message = JSONRPC2Message.parse(body);
				Log.d(TAG, "json-rpc:"+message);
				if (message instanceof JSONRPC2Request) {
					JSONRPC2Response resp = dispatcher.process((JSONRPC2Request)message, null);
					if (resp != null) {
						String jsonResponse = resp.toString();
						Response response = new Response(Status.OK, "application/json", jsonResponse);
						response.addHeader("Access-Control-Allow-Origin", "*");
						Log.d(TAG, "response:"+jsonResponse);
						return response;
					}
				} else if (message instanceof JSONRPC2Notification) {
					dispatcher.process((JSONRPC2Notification) message, null);
			        Response response = new Response("");
					response.addHeader("Access-Control-Allow-Origin", "*");
					return response;
				}		
			} catch (JSONRPC2ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ResponseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {

			}
		}
		Response response = new Response(Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid request!");
		response.addHeader("Access-Control-Allow-Origin", "*");
		return response;
	}
	private Dispatcher dispatcher = new Dispatcher();
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
}
