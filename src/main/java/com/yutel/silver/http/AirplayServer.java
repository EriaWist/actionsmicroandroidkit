package com.yutel.silver.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yutel.silver.AikaProxy;
import com.yutel.silver.vo.Device;

public class AirplayServer extends Thread {
	public interface ConnectionListener {

		void onAirPlayStop();

		void onAirPlayStart();
		
	}
	private static Logger logger = Logger.getLogger(AirplayServer.class
			.getName());
	public int mDuration;
	public int mPosition;
	public int mState;

	private Device mDevice;
	private AikaProxy mProxy;
	private ServerSocket mServerSocket;
	private Map<String, Socket> reverseResponse;
	private int mPort;
	private List<HttpClient> currentConnections = new ArrayList<HttpClient>();
	private ConnectionListener connectionListener;
	
	public static void main(String[] args) {
		try {
			new AirplayServer(8888).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public AirplayServer(int port) throws IOException {
		mPort = port;
		mServerSocket = new ServerSocket(mPort);
	}

	public Device getDevice() {
		return mDevice;
	}

	public void setDevice(Device device) {
		mDevice = device;
	}

	public AikaProxy getProxy() {
		return mProxy;
	}

	public void setProxy(AikaProxy mProxy) {
		this.mProxy = mProxy;
	}

	public void addReverseResponse(String sessionid, Socket socket) {
		reverseResponse.put(sessionid, socket);
	}

	public void sendReverseResponse(String sessionid, String body) {
		try {
			Socket s = reverseResponse.get(sessionid);
			if (s != null && s.isConnected()) {
				System.out.println("EVENT:" + body);
				OutputStream os = s.getOutputStream();
				os.flush();
				
				StringBuffer sb = new StringBuffer();
				sb.append("POST /event HTTP/1.1").append(HttpAnaly.CR).append(HttpAnaly.LF);
				sb.append("Content-Type: application/x-apple-plist").append(HttpAnaly.CR).append(HttpAnaly.LF);
				sb.append("Content-Length: ").append(body.getBytes().length).append(HttpAnaly.CR).append(HttpAnaly.LF);
				sb.append("X-Apple-Session-ID: ").append(sessionid).append(HttpAnaly.CR).append(HttpAnaly.LF);
				sb.append(HttpAnaly.CR).append(HttpAnaly.LF);
				sb.append(body);
				os.write(sb.toString().getBytes());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void sendReverseResponse(String body) {
		for (String session : reverseResponse.keySet()) {
			sendReverseResponse(session, body);
		}
	}
	public void forceStop() {
		if (mServerSocket != null) {
			try {
				mServerSocket.close();
				logger.log(Level.INFO, "Http Server close :" + mPort);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		closeCurrentConnections();
	}

	@Override
	public void run() {
		try {
			reverseResponse = new HashMap<String, Socket>();
			logger.log(Level.INFO, "Http Server started listening:" + mPort);
			while (true) {
				Socket socket = mServerSocket.accept();
				String ip = socket.getInetAddress().getHostAddress();
				logger.log(Level.INFO, "client:" + ip + "/" + socket.getPort());
				synchronized (currentConnections) {
					HttpClient httpClientConnection = new HttpClient(socket, this);
					httpClientConnection.setName(httpClientConnection.toString());
					httpClientConnection.start();
					if (currentConnections.size() == 0) {
						if (connectionListener != null) {
							connectionListener.onAirPlayStart();
						}
					}
					currentConnections.add(httpClientConnection);
				}
			}
		} catch (Exception e) {
			logger.log(Level.INFO, e.getMessage());
		}
		logger.log(Level.INFO, "Http Server stopped");
	}

	public synchronized void closeCurrentConnections() {
		List<HttpClient> connections = null;
		synchronized (currentConnections) {
			connections  = currentConnections;
			currentConnections.clear();
		}
		for (HttpClient connection : connections) {
			connection.close();
		}
	}

	@Override
	public void finalize() {
		try {
			if (mServerSocket != null) {
				mServerSocket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		mServerSocket = null;
	}

	public void removeReverseResponse(String session) {
		reverseResponse.remove(session);
	}

	public void onConnectionClosed(HttpClient httpClient) {
		Socket socket = httpClient.getSocket();
		logger.log(Level.INFO, "client closed:" + socket.getInetAddress().getHostAddress() + "/" + socket.getPort());
		synchronized (currentConnections) {
			if (currentConnections.contains(httpClient)) {
				currentConnections.remove(httpClient);
				if (currentConnections.size() == 0 && connectionListener != null) {
					connectionListener.onAirPlayStop();
				}
			}
		}
	}

	public ConnectionListener getConnectionListener() {
		return connectionListener;
	}

	public void setConnectionListener(ConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
	}
}
