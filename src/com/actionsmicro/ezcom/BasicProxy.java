package com.actionsmicro.ezcom;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerResolver;
import org.apache.http.protocol.HttpService;
import org.apache.http.util.EntityUtils;

import android.os.Handler;
import android.os.Looper;

import com.actionsmicro.ezcom.jsonrpc.JSONRPC2Session;
import com.actionsmicro.ezcom.jsonrpc.Utils;
import com.actionsmicro.utils.Log;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.client.RawResponse;
import com.thetransactioncompany.jsonrpc2.client.RawResponseInspector;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class BasicProxy implements Proxy {
	private static final String METHOD_SET_HOSTNAME = "common.set_hostname";
	private static final String PATH_JSONRPC = "/jsonrpc";
	private static final String PATH_SCREEN = "/screen";
	private static final String TAG = "Proxy";
	private static final int SOCKET_OPERATION_TIMEOUT = 10*1000;
	private int portNumber;
	private String address;
	private JSONRPC2Session controlSession;
	private ConnectionManager connectionManager;
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	public void setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	private Handler uiThreadHandler = new Handler(Looper.getMainLooper());
	private Socket reverseConnection;
	protected boolean shouldStop = false;
	private Thread controlSessionWorker;
	private boolean isConnecting;
	public BasicProxy(String ipV4Address, int portNumber) {
		this.address = ipV4Address;
		this.portNumber = portNumber;
	}
	public void open() {
		Thread initializer = new Thread(new Runnable(){
			@Override
			public void run() {
				if (!isConnecting()) { 
					beginConnecting();
					connect();
					endConnecting();
				}
			}			
		});
		initializer.setName("EZCom - Initializer");
		initializer.start();
	}
	public void reconnect() {
		Thread initializer = new Thread(new Runnable(){
			@Override
			public void run() {
				if (!isConnecting()) { 
					beginConnecting();
					uiThreadHandler.post(new Runnable() {
						@Override
						public void run() {
							if (connectionManager != null) {
								connectionManager.tryToReconnect(BasicProxy.this);
							}
						}

					});
					close();
					connect();
					endConnecting();
				}
			}
			
		});
		initializer.setName("EZCom - Initializer");
		initializer.start();
	}
	protected synchronized void endConnecting() {
		isConnecting = false;
	}
	protected synchronized void beginConnecting() {
		isConnecting = true;
	}
	protected synchronized boolean isConnecting() {
		return isConnecting;
	}
	public void close() {
		if (!isClosing()) {
			beginClosing();
			closeControlSession();
			closeReverseSession();
			closeScreenConnection();
			endClosing();
		}
	}
	public synchronized boolean isConnected() {
		return reverseConnection != null && reverseConnection != null;
	}
	private void closeReverseSession() {
		if (reverseConnection != null) {
			try {
				reverseConnection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (reverseSessionRequestHandler != null) {
				if (!reverseSessionRequestHandler.equals(Thread.currentThread())) {
					try {
						reverseSessionRequestHandler.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				reverseSessionRequestHandler = null;
			}
			reverseConnection = null;
		}		
	}
	private void closeControlSession() {
		stopAndWaitControlSessionWorker();
		if (controlSession != null) {
			controlSession.close();
			controlSession = null;
		}
	}
	private void stopAndWaitControlSessionWorker() {
		shouldStop = true;
		if (controlSessionWorker != null) {
			if (!controlSessionWorker.equals(Thread.currentThread())) {
				try {
					controlSessionWorker.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			controlSessionWorker = null;
		}
	}
	public String echo(String msg) throws JSONRPC2SessionException {
		List<Object> echoParam = new LinkedList<Object>();
		echoParam.add(msg);
		JSONRPC2Request request = new JSONRPC2Request("echo", echoParam, generateRpcId());
		JSONRPC2Response response = sendRequest(request);
		if (response.indicatesSuccess()) {
			return (String) response.getResult();
		} else {
			return null;
		}
	}
	private int sRpcId = 0;
	/* (non-Javadoc)
	 * @see com.actionsmicro.ezcom.Proxy#generateRpcId()
	 */
	@Override
	public synchronized int generateRpcId() {
		return sRpcId++;
	}
	public long add(long a, long b) throws JSONRPC2SessionException {
		JSONRPC2Request request = new JSONRPC2Request("add", Arrays.asList((Object)Long.valueOf(a), Long.valueOf(b)), generateRpcId());
		JSONRPC2Response response = sendRequest(request);
		if (response.indicatesSuccess()) {
			return (Long)response.getResult();
		} else {
			return 0;
		}
	}
	public void setHostname(String hostname) throws JSONRPC2SessionException {
		JSONRPC2Request request = new JSONRPC2Request(METHOD_SET_HOSTNAME, Arrays.asList((Object)hostname), generateRpcId());
		try {
			JSONRPC2Response response = sendRequest(request);
			if (!response.indicatesSuccess()) {
				Log.e(TAG, "METHOD_SET_HOSTNAME failed:"+response.getError().getMessage());
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}
	private void connect() {
		shouldStop = false;
		try {
			createControlSession();
			try {
				createReverseSession();
				spamControlSessionWorker();
				uiThreadHandler.post(new Runnable() {
					@Override
					public void run() {
						if (connectionManager != null) {
							connectionManager.connectionsEstablishedSuccessfully(BasicProxy.this);
						}
					}
					
				});
			} catch (Exception e) {
				e.printStackTrace();
				handleReverseSessionInitException();
			}
		} catch (Exception e) {
			e.printStackTrace();
			handleControlSessionInitException();
		}		
	}
	private void handleControlSessionInitException() {
		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				if (connectionManager != null) {
					connectionManager.controlConnectionDidFail(BasicProxy.this);
				}
			}
			
		});
	}
	private void handleReverseSessionInitException() {
		closeControlSession();
		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				if (connectionManager != null) {
					connectionManager.reverseConnectionDidFail(BasicProxy.this);
				}
			}
			
		});
	}
	private ArrayBlockingQueue<JSONRPC2Request> pendingRequests = new ArrayBlockingQueue<JSONRPC2Request>(10);
	private boolean isClosingConnection;
	private Thread reverseSessionRequestHandler;
	
	private void spamControlSessionWorker() {
		controlSessionWorker = new Thread(new Runnable() {

			@Override
			public void run() {
				while (!shouldStop ) {
					try {
						JSONRPC2Request request = pendingRequests.poll(1, TimeUnit.SECONDS);
						if (request != null) {
							
						} else {
							sendHeartBeat();
						}
					} catch (InterruptedException e1) {
						e1.printStackTrace();
						shouldStop = true;
					} catch (JSONRPC2SessionException e) {
						if (e.getCauseType() == JSONRPC2SessionException.NETWORK_EXCEPTION) {
							handleControlSessionNetworkException(e);
							shouldStop = true;
						} else {
							e.printStackTrace();
						}
					}
				}
			}			
		});
		controlSessionWorker.setName("EZCom - Control Session Worker");
		controlSessionWorker.start();
	}
	private void createControlSession() throws IOException {
		if (controlSession == null) {
			controlSession = new JSONRPC2Session(new URL("http", address, portNumber, PATH_JSONRPC));
			controlSession.setRawResponseInspector(new RawResponseInspector() {

				@Override
				public void inspect(RawResponse rawResp) {
					Log.d(TAG, rawResp.toString());
				}
				
			});
		}
	}
	/* (non-Javadoc)
	 * @see com.actionsmicro.ezcom.Proxy#sendRequest(com.thetransactioncompany.jsonrpc2.JSONRPC2Request)
	 */
	@Override
	public JSONRPC2Response sendRequest(JSONRPC2Request request) throws JSONRPC2SessionException, IllegalStateException {
		JSONRPC2Response response = null;
		if (controlSession != null) {
			Log.d(TAG, "send json-rpc request:"+request);
			response = controlSession.send(request);
			return response;
		}
		throw new IllegalStateException("controlSession is not ready");
	}
	/* (non-Javadoc)
	 * @see com.actionsmicro.ezcom.Proxy#sendNotification(com.thetransactioncompany.jsonrpc2.JSONRPC2Notification)
	 */
	@Override
	public void sendNotification(JSONRPC2Notification notification) throws JSONRPC2SessionException, IllegalStateException {
		if (controlSession != null) {
			controlSession.send(notification);
		}
		throw new IllegalStateException("controlSession is not ready");
	}
	private void sendHeartBeat() throws JSONRPC2SessionException {
		JSONRPC2Notification notification = new JSONRPC2Notification("heartbeat");
		controlSession.send(notification);
	}
	
	private void createReverseSession() throws ReverseConnectionException {
		
		try {
			reverseConnection = new Socket(address, portNumber);
			// to close this socket
			invokeReverseRpc(reverseConnection);
			spamReverseRequestHandler();
			
		} catch (Exception e) {
			if (e instanceof ReverseConnectionException) {
				throw (ReverseConnectionException)e;
			} else {
				throw new ReverseConnectionException(e);
			}
		} finally {
			
		}
	}
	private void spamReverseRequestHandler() {
		reverseSessionRequestHandler = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					runRpcServer(reverseConnection);
				} catch (final IOException e) {
					if (!isClosing()) {
						handleReverseSessionNetworkException(e);
					}
				}
			}

		});
		reverseSessionRequestHandler.setName("EZCom - Reverse Session Handler");
		reverseSessionRequestHandler.start();
	}
	protected synchronized boolean isClosing() {
		return isClosingConnection;
	}
	private synchronized void endClosing() {
		isClosingConnection = false;
	}
	private synchronized void beginClosing() {
		isClosingConnection = true;		
	}
	private Dispatcher dispatcher = new Dispatcher();
	/* (non-Javadoc)
	 * @see com.actionsmicro.ezcom.Proxy#registerRpcRequestHandler(com.thetransactioncompany.jsonrpc2.server.RequestHandler)
	 */
	@Override
	public void registerRpcRequestHandler(RequestHandler requestHandler) throws IllegalStateException {
		if (dispatcher != null) {
			dispatcher.register(requestHandler);
		} else {
			throw new IllegalStateException("dispatcher is null");
		}
	}
	private void runRpcServer(Socket socket) throws IOException {
		DefaultHttpServerConnection serverConnection = createServerConnection(socket);
		HttpService httpService = new HttpService(new BasicHttpProcessor(), new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
		httpService.setHandlerResolver(new HttpRequestHandlerResolver() {

			@Override
			public HttpRequestHandler lookup(String requestURI) {
				return new HttpRequestHandler() {

					@Override
					public void handle(HttpRequest receivedRequest,
							HttpResponse httpResponse, HttpContext context)
							throws HttpException, IOException {
						Log.d(TAG, "Handle request:" + receivedRequest.getRequestLine());
						com.actionsmicro.ezcom.http.Utils.logHttpRequest(TAG, receivedRequest);
						try {
							if (receivedRequest instanceof HttpEntityEnclosingRequest) {
								HttpEntity entity = ((HttpEntityEnclosingRequest) receivedRequest).getEntity();
								String jsonString = EntityUtils.toString(entity);
								entity.consumeContent();
								JSONRPC2Request req = JSONRPC2Request.parse(jsonString);
								Log.d(TAG, "json-rpc:"+req);
								JSONRPC2Response resp = dispatcher.process(req, null);
								Utils.buildHttpResponse(httpResponse, resp);
								Log.d(TAG, "httpResponse to " + receivedRequest.getRequestLine() + ":" + httpResponse.getStatusLine());
							}
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (JSONRPC2ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				};
			}
			
		});
		BasicHttpContext serverContext = new BasicHttpContext();
		while (true) {
			try {
				httpService.handleRequest(serverConnection, serverContext);
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private DefaultHttpServerConnection createServerConnection(Socket socket)
			throws IOException {
		DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();
		BasicHttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        serverConnection.bind(socket, params);
		return serverConnection;
	}
	private class ReverseConnectionException extends Exception {
		private static final long serialVersionUID = 1L;
		public ReverseConnectionException (String detailMessage) {
			super(detailMessage);
		}
		public ReverseConnectionException(Throwable throwable) {
			super(throwable);
		}
	}
	private void invokeReverseRpc(Socket socket) throws ReverseConnectionException {
		try {
			DefaultHttpClientConnection clientConnection = createClientConnection(socket);
			JSONRPC2Request request = new JSONRPC2Request("reverse", generateRpcId());
			HttpPost postRequest = Utils.createRpcPostRequestAndPreprocess(request, new URL("http", address, portNumber, PATH_JSONRPC).toURI());
			HttpRequestExecutor executor = new HttpRequestExecutor();
			HttpContext context = new BasicHttpContext();
			com.actionsmicro.ezcom.http.Utils.logHttpRequest(TAG, postRequest);
			HttpResponse rawResponse = executor.execute(postRequest, clientConnection, context);
			Log.d(TAG, "reverse http raw response:");
			String tag = TAG;
			com.actionsmicro.ezcom.http.Utils.logHttpResponse(tag, rawResponse);
			HttpEntity entity = rawResponse.getEntity();
			String jsonString = EntityUtils.toString(entity);
			entity.consumeContent();
			JSONRPC2Response response = JSONRPC2Response.parse(jsonString, 
					false, 
					false,
					false);
			Log.d(tag, "reverse method json response:"+response.toString());
			Utils.matchRequestResponseIdAndThrow(request, response);
			if (!response.indicatesSuccess()) {
				throw new ReverseConnectionException(response.getError());
			}
		} catch (Exception e) {
			if (e instanceof ReverseConnectionException) {
				throw (ReverseConnectionException)e;
			} else {
				throw new ReverseConnectionException(e);
			}
		} finally {

		}
	}
	private DefaultHttpClientConnection createClientConnection(Socket socket)
			throws IOException {
		DefaultHttpClientConnection clientConnection = new DefaultHttpClientConnection();
		BasicHttpParams params = new BasicHttpParams();
		params.setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8192);
		HttpConnectionParams.setConnectionTimeout(params, SOCKET_OPERATION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_OPERATION_TIMEOUT);
		clientConnection.bind(socket, params);
		return clientConnection;
	}
	private void handleControlSessionNetworkException(
			final JSONRPC2SessionException e) {
		close();
		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				if (connectionManager != null) {
					connectionManager.controlConnectionDidDisconnected(BasicProxy.this, e);
				}
			}
			
		});
	}
	private void handleReverseSessionNetworkException(final IOException e) {
		close();
		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				if (connectionManager != null) {
					connectionManager.reverseConnectionDidDisconnected(BasicProxy.this, e);
				}
			}
			
		});
	}
	private static void uploadDataToServer(InputStream input, String address, int portNumber, String path) throws IOException {
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL("http://"+address+":"+portNumber+path);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setDoOutput(true);
			urlConnection.setRequestMethod("PUT");
			Log.d(TAG, "connecting to "+ url.toString());
			urlConnection.connect();
			Log.d(TAG, "connected to "+ url.toString());
			OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
			Log.d(TAG, "writing to "+ url.toString());
			com.actionsmicro.utils.Utils.dump(input, out);
			Log.d(TAG, "reading "+ url.toString());
			urlConnection.getResponseCode();
			Log.d(TAG, "reading.opening input stream "+ url.toString());
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			Log.d(TAG, "reading.input stream opened"+ url.toString());
			byte buffer[] = new byte[4096];
			int rc = in.read(buffer, 0, buffer.length);
			int totalRead = 0;
			while (rc > 0) {
				totalRead += rc;
				rc = in.read(buffer, 0, buffer.length);
			}
			Log.d(TAG, "reading done. totalRead:" + totalRead);
		} catch (MalformedURLException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
				urlConnection = null;
			}
		}
	}
	public static void uploadTestDataToServer(InputStream input, String address, int portNumber) throws IOException {
		uploadDataToServer(input, address, portNumber, "/object/echo");
	}
	private HttpClientConnection screenConnection = null;
	private synchronized HttpClientConnection getScreenSession() {
		if (screenConnection == null) {
			try {
				Socket screen = new Socket(address, portNumber);
				screenConnection = createClientConnection(screen);
				Log.d(TAG, "create screen connection:"+screenConnection);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return screenConnection;
	}
	/* (non-Javadoc)
	 * @see com.actionsmicro.ezcom.Proxy#sendJpegEncodedScreenData(java.io.InputStream, long)
	 */
	@Override
	public synchronized void sendJpegEncodedScreenData(InputStream input, long length) {
		if (isConnected() && !isClosing()) {
			try {
				HttpClientConnection screenSession = getScreenSession();
				if (screenSession != null) {
					URI targetUri = new URL("http", address, portNumber, PATH_SCREEN).toURI();
					HttpPut putRequest = new HttpPut(targetUri);
					InputStreamEntity entity = new InputStreamEntity(input, length);
					entity.setContentType("image/jpeg");
					putRequest.setEntity(entity);
					putRequest.setHeader(HTTP.CONTENT_LEN, Long.valueOf(entity.getContentLength()).toString());
					putRequest.setHeader(entity.getContentType());
					putRequest.setHeader(HTTP.TARGET_HOST, targetUri.getHost()+":"+targetUri.getPort());
					HttpContext context = new BasicHttpContext();
					com.actionsmicro.ezcom.http.Utils.logHttpRequest(TAG, putRequest);
					com.actionsmicro.ezcom.http.Utils.sendRequestWithoutResponse(putRequest, screenSession, context);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				closeScreenConnection();
			} catch (HttpException e) {
				e.printStackTrace();
			}
		}
	}
	private void closeScreenConnection() {
		if (screenConnection != null) {
			Log.d(TAG, "close screen connection:"+screenConnection);
			try {
				screenConnection.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				screenConnection = null;
			}
		}
	}
}
