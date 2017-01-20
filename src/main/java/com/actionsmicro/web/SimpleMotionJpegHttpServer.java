package com.actionsmicro.web;

import android.content.Context;

import com.actionsmicro.utils.Device;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;

public class SimpleMotionJpegHttpServer {
	private static final String TAG = "SimpleMotionJpegHttpServer";
	private Socket socket;
	private DataOutputStream outputStream;
	final String boundary = "ezcastmjpegstreamer";
	private boolean shouldStop;
	private Thread serverThread;
	private ServerSocket server;
	private Context context;
    
	public SimpleMotionJpegHttpServer(Context context, final int port, OnConnectionListener listener) {
		this.context = context;
		this.onConnectionListener = listener;
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
	public interface OnConnectionListener {

		void onClientConnected(
				SimpleMotionJpegHttpServer simpleMotionJpegHttpServer);
		
	}
	private OnConnectionListener onConnectionListener;
	private void createServerAndWaitForConnection(int port) {
		try {
			while (!shouldStop) {
				try {
					Socket newSocket = server.accept();
					if (newSocket != null) {
						synchronized (SimpleMotionJpegHttpServer.this) {
							cleanUpConnection();
							socket = newSocket;
							Log.d(TAG, "New connection to :" + socket.getInetAddress());
							outputStream = new DataOutputStream(socket.getOutputStream());
							outputStream.write(("HTTP/1.0 200 OK\r\n" +
//									"Server: EZCastStreamer\r\n" +
//									"Connection: close\r\n" +
//									"Max-Age: 0\r\n" +
//									"Expires: 0\r\n" +
//									"Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
//									"Pragma: no-cache\r\n" + 
									"Content-Type: multipart/x-mixed-replace; " +
									"boundary=--" + boundary + "\r\n" +
									"\r\n" +
									//						"--" + boundary + "\r\n" +
									"").getBytes());
							if (onConnectionListener != null) {
								onConnectionListener.onClientConnected(SimpleMotionJpegHttpServer.this);
							}
							sendJepgStreamImp();
						}
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
	public synchronized void sendJpegStream(InputStream jpegStream, long size) {
		Log.d(TAG, "clone start");			
		clone.reset();
		try {
			Utils.dump(jpegStream, clone);
		} catch (IOException e1) {
			e1.printStackTrace();
			clone.reset();
		}
		Log.d(TAG, "clone end");			
		sendJepgStreamImp();
	}
	private synchronized void sendJepgStreamImp() {
		if (isConnected() && clone.size() > 0) {
			Log.d(TAG, "sendJpegStream to :" + socket.getInetAddress());			
			try {
				for (int i =0; i< 2; i++) {
					outputStream.write(("--"+boundary+"\r\n"+"Content-type: image/jpeg\r\n" +
							"Content-Length: " + clone.size() + "\r\n" +
//							"X-Timestamp:" + new Date().getTime() + "\r\n" +
							"\r\n" +
							"").getBytes());
					Utils.dump(new ByteArrayInputStream(clone.toByteArray()), outputStream);
					outputStream.write(("\r\n").getBytes());
					
//					outputStream.write(("Content-type: image/jpeg\r\n" +
//							"Content-Length: " + 0 + "\r\n" +
//							"\r\n" +
//							"").getBytes());
//					outputStream.write(("--" + boundary + "\r\n").getBytes());
					
//					outputStream.write(("\r\n\r\n").getBytes());
				}
				outputStream.flush();
				Log.d(TAG, "sendJpegStream to :" + socket.getInetAddress() +" done");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, e.getLocalizedMessage());				
				cleanUpConnection();
			}
		} else {
			Log.d(TAG, "No connection or clone is empty. clone size:"+clone.size());
		}
	}
	public synchronized boolean isConnected() {
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
	public String getServerUrl() {
		if (server != null) {
			try {
				return new URL("http", Device.getHostIpAddress(context, true), server.getLocalPort(), "").toString();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	public OnConnectionListener getOnConnectionListener() {
		return onConnectionListener;
	}
	public void setOnConnectionListener(OnConnectionListener onConnectionListener) {
		this.onConnectionListener = onConnectionListener;
	}
}
