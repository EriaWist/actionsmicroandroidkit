package com.actionsmicro.ezcast.imp.androidrx;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.actionsmicro.ezcast.Api;
import com.actionsmicro.ezcast.ApiBuilder;
import com.actionsmicro.ezcast.ConnectionManager;
import com.actionsmicro.ezcom.jsonrpc.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public class AndroidRxApi implements Api {

	private static final int HEARTBEAT_PERIOD = 1000;
	private static final String TAG = "AndroidRxApi";
	private AndroidRxInfo device;
	protected AndroidRxInfo getDevice() {
		return device;
	}

	private ConnectionManager connectionManager;
	protected ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	private Context context;
	private JSONRPC2Session jsonRpcSession;

	private JSONRPC2Session getJsonRpcSession() {
		return jsonRpcSession;
	}

	protected Context getContext() {
		return context;
	}
	
	public <T> AndroidRxApi(ApiBuilder<T> apiBuilder) {
		
		context = apiBuilder.getContext();
		connectionManager = apiBuilder.getConnectionManager();
		device = (AndroidRxInfo) apiBuilder.getDevice();
	}
	private LooperThread networkThread;
	protected Handler getNetworkHandler() {
		if (networkThread != null) {
			return networkThread.getHandler();
		}
		return null;
	}
	private class LooperThread extends Thread {
		private Looper myLooper;
		private Handler handler;
		protected Handler getHandler() {
			return handler;
		}
		@Override
		public void run() {
			Looper.prepare();
			myLooper = Looper.myLooper();
			handler = new Handler();
			getNetworkHandler().postDelayed(heartbeat, HEARTBEAT_PERIOD);
			synchronized(this) {
				this.notifyAll();
			}
			Looper.loop();
		}
		public void stopLooper() {
			if (myLooper != null) {
				myLooper.quit();
			}
		}
	}
	private Runnable heartbeat = new Runnable() {

		@Override
		public void run() {
			try {
				if (jsonRpcSession != null) {
					jsonRpcSession.send(new JSONRPC2Notification("heartbeat"));
					Log.d(TAG, "send heartbeat");
					Handler networkHandler = getNetworkHandler();
					if (networkHandler != null) {
						networkHandler.postDelayed(heartbeat, HEARTBEAT_PERIOD);
					}
				}
			} catch (JSONRPC2SessionException e) {
				e.printStackTrace();
				if (connectionManager != null) {
					connectionManager.onConnectionFailed(AndroidRxApi.this, e);
				}
			}
		}
		
	};
	@Override
	public void connect() {
		if (jsonRpcSession == null) {
			jsonRpcSession = new JSONRPC2Session(getJsonRpcUrl());
		}
		if (networkThread == null) {
			networkThread = new LooperThread();
			networkThread.start();
		}		
		try {
			synchronized(networkThread) {
				networkThread.wait();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void disconnect() {
		if (networkThread != null) {
			networkThread.stopLooper();
			try {
				networkThread.join(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			networkThread = null;
		}
		if (jsonRpcSession != null) {
			jsonRpcSession.close();
			jsonRpcSession = null;
		}
	}

	private URL getBaseUrl() {
		try {
			InetAddress host = getDevice().getIpAddress();
			URL baseUrl = new URL("http", host.getHostAddress(), getDevice().getPort(), "");
			return baseUrl;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	private URL getJsonRpcUrl() {
		try {
			return new URL(getBaseUrl(), "/jsonrpc");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	private int sRpcId = 0;
	private synchronized int generateRpcId() {
		return sRpcId++;
	}
	private void performOnNetworkThread(Runnable runnable) {
		Handler handler = getNetworkHandler();
		if (handler != null) {
			handler.post(runnable);			
		}
	}
	protected void invokeRpcMethod(final String method, final HashMap<String, Object> params) {
		invokeRpcMethod(method, params, 0);
	}
	protected void invokeRpcMethod(final String method, final HashMap<String, Object> params, long timeout) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					JSONRPC2Session jsonRpcSession = getJsonRpcSession();
					if (jsonRpcSession != null) {
						jsonRpcSession.send(new JSONRPC2Request(method, params, generateRpcId()));
					}					
				} catch (JSONRPC2SessionException e) {
					e.printStackTrace();
				} finally {
					synchronized (this) {
						this.notifyAll();
					}
				}
			}
			
		};
		performOnNetworkThread(runnable);
		if (timeout > 0) {
			synchronized (runnable) {
				try {
					runnable.wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	protected void invokeRpcMethod(final String method) {
		invokeRpcMethod(method, 0);
	}
	protected void invokeRpcMethod(final String method, long timeout) {
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				try {
					JSONRPC2Session jsonRpcSession = getJsonRpcSession();
					if (jsonRpcSession != null) {
						jsonRpcSession.send(new JSONRPC2Request(method, generateRpcId()));
					}
				} catch (JSONRPC2SessionException e) {
					e.printStackTrace();
				} finally {
					synchronized (this) {
						this.notifyAll();
					}
				}
			}
			
		};
		performOnNetworkThread(runnable);
		if (timeout > 0) {
			synchronized (runnable) {
				try {
					runnable.wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	protected void sendRpcNotification(final String notification, long timeout) {
		
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				try {
					JSONRPC2Session jsonRpcSession = getJsonRpcSession();
					if (jsonRpcSession != null) {
						jsonRpcSession.send(new JSONRPC2Notification(notification));
					}
				} catch (JSONRPC2SessionException e) {
					e.printStackTrace();
				} finally {
					synchronized (this) {
						this.notifyAll();
					}
				}
			}
			
		};
		performOnNetworkThread(runnable);
		if (timeout > 0) {
			synchronized (runnable) {
				try {
					runnable.wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
