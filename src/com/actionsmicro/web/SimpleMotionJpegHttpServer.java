package com.actionsmicro.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;

public class SimpleMotionJpegHttpServer {
	private static final String TAG = "SimpleMotionJpegHttpServer";
	private Socket socket;
	private DataOutputStream outputStream;
	final String boundary = "ezcastmjpegstreamer";
	private boolean shouldStop;
	private Thread serverThread;
	private ServerSocket server;
    
	public SimpleMotionJpegHttpServer(final int port) {
		try {
			server = new ServerSocket(port);
			server.setReuseAddress(true);
			server.setSoTimeout(1000);
			serverThread = new Thread(new Runnable() {

				@Override
				public void run() {
					createServerAndWaitForConnection(port);
				}
				
			});
			serverThread.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	private synchronized void createServerAndWaitForConnection(int port) {
		try {
			while (!shouldStop) {
				try {
					Socket newSocket = server.accept();
					if (newSocket != null) {
						cleanUpConnection();
						socket = newSocket;
						Log.i(TAG, "New connection to :" + socket.getInetAddress());
						outputStream = new DataOutputStream(socket.getOutputStream());
						outputStream.write(("HTTP/1.0 200 OK\r\n" +
								"Server: EZCastStreamer\r\n" +
								"Connection: close\r\n" +
								"Max-Age: 0\r\n" +
								"Expires: 0\r\n" +
								"Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
								"Pragma: no-cache\r\n" + 
								"Content-Type: multipart/x-mixed-replace; " +
								"boundary=" + boundary + "\r\n" +
								"\r\n" +
								//						"--" + boundary + "\r\n" +
								"").getBytes());
					}
				} catch (SocketTimeoutException e) {

				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "", e);
			cleanup();
		}
	}
	static ByteArrayOutputStream clone = new ByteArrayOutputStream(500*1024);
	public void sendJpegStream(InputStream jpegStream, long size) {
		if (isConnected()) {
			Log.i(TAG, "sendJpegStream to :" + socket.getInetAddress());
			
			try {
				clone.reset();
				Utils.dump(jpegStream, clone);
				
				for (int i =0; i< 1; i++) {
					outputStream.write(("Content-type: image/jpeg\r\n" +
							"Content-Length: " + size + "\r\n" +
//							"X-Timestamp:" + new Date().getTime() + "\r\n" +
							"\r\n" +
							"").getBytes());
					Utils.dump(new ByteArrayInputStream(clone.toByteArray()), outputStream);
					outputStream.write(("--" + boundary + "\r\n").getBytes());
					
					outputStream.write(("Content-type: image/jpeg\r\n" +
							"Content-Length: " + 0 + "\r\n" +
							"\r\n" +
							"").getBytes());
					outputStream.write(("--" + boundary + "\r\n").getBytes());
					
//					outputStream.write(("\r\n\r\n").getBytes());
				}
				outputStream.flush();
				Log.i(TAG, "sendJpegStream to :" + socket.getInetAddress() +" done");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, e.getLocalizedMessage());				
				cleanUpConnection();
			}
		}
	}
	public boolean isConnected() {
		return outputStream != null;
	}
	public void cleanup() {
		shouldStop = true;
		try {
			serverThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cleanUpConnection();
	}
	private void cleanUpConnection() {
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			outputStream = null;
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			socket = null;
		}
	}
	public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
                        if (useIPv4) {
                            if (isIPv4) 
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
	public String getServerUrl() {
		if (server != null) {
			try {
				return new URL("http", getIPAddress(true), server.getLocalPort(), "").toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}
