package com.actionsmicro.falcon;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

import com.actionsmicro.falcon.Falcon.ProjectorInfo.MessageListener;
import com.actionsmicro.utils.CipherUtil;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.actionsmicro.falcon.Falcon.ProjectorInfo.SERVICE_APP_HTTP_STREAMING;
import static com.actionsmicro.falcon.Falcon.ProjectorInfo.SERVICE_EZENCODEPRO;
import static com.actionsmicro.falcon.Falcon.ProjectorInfo.SERVICE_MEDIA_STREAM_AUDIO;
import static com.actionsmicro.utils.CipherUtil.ALGORITHM_AES_CBC;


/**
 * This class is in charge of device discovery.
 * <p>
 * Sample steps to use this class:
 * <OL>
 * <li>Get the shared instance of Falcon by calling {@link Falcon#getInstance()}. 
 * <li>Call {@link Falcon#addSearchResultListener(SearchReultListener)} to register callbacks.
 * <li>Invoke {@link Falcon#search()}.
 * <li>Handle callbacks in {@link SearchReultListener}.
 * </OL>
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 1.0
 */
public class Falcon {

	private static final String PARAMETER_VENDOR_KEY = "vendor";
	private static final String PARAMETER_MODEL_KEY = "model";
	private static final String MD5_SECRET = ":secret=82280189";
	private static final String PARAMETER_MD5_KEY = "md5";
	private static final String PARAMETER_SERVICE_KEY = "service";
	private static final String PARAMETER_NAME_KEY = "name";
	private static final String PARAMETER_DISCOVERY_KEY = "discovery";
	private static final String TAG = "Falcon";
	private static final String REMOTE_CONTROL_MESSGAE_CHARSET = "UTF-8";
	private static final int DEVICE_ALIVE_TIMEOUT = 10000;

	/**
	 * This contains basic information about the device.
	 * @author James Chen
	 *
	 */
	public static class ProjectorInfo implements Parcelable, Comparable<ProjectorInfo> {
		public static final int SERVICE_WIFI_LAN_DISPLAY 	= 0x01 << 0;
		public static final int SERVICE_MEDIA_STREAMING 	= 0x01 << 1;
		public static final int SERVICE_APP_PHOTO_VIEWER 	= 0x01 << 2;
		public static final int SERVICE_APP_LIVE_CAM 		= 0x01 << 3;
		public static final int SERVICE_APP_STREAMIG_DOC 	= 0x01 << 4;
		public static final int SERVICE_SPLIT_SCREEN 		= 0x01 << 5;
		public static final int SERVICE_APP_DROPBOX 		= 0x01 << 6;
		public static final int SERVICE_APP_WEB_VIEWER 	= 0x01 << 7;
		public static final int SERVICE_APP_QUALITY_MODE 	= 0x01 << 8;
		public static final int SERVICE_APP_HTTP_STREAMING = 0x01 << 9;
		public static final int SERVICE_APP_REMOTE_CONTROL = 0x01 << 10;
		public static final int SERVICE_MEDIA_STREAM_AUDIO = 0x01 << 11;
		public static final int SERVICE_EZENCODEPRO = 0x01 << 11;

		// REMOTE_CONTROL_COMMAND
		private static final int COMMAND_CONNECT = 0;
		private static final int COMMAND_KEY = 1;
		private static final int COMMAND_KEYBOARD = 2;
		private static final int COMMAND_KEYBOARD_NUMPAD = 3;
		private static final int COMMAND_MOUSE = 4;
		private static final int COMMAND_STRING = 5;
		private static final int COMMAND_JSONRPC = 6;
		private static final int COMMAND_JSONRPC_ENCRYPT = 7;
		private static final int COMMAND_VENDOR = 10;
		private static final int COMMAND_VENDOR_STRING = 11;

		private String osVerion;
		private String name;
		protected InetAddress ipAddress;
		protected int wifiDisplayPortNumber;
		protected int remoteControlPortNumber;
		private String passcode;
		private String model;
		private int service = SERVICE_WIFI_LAN_DISPLAY | SERVICE_MEDIA_STREAMING | SERVICE_APP_PHOTO_VIEWER | SERVICE_APP_LIVE_CAM | SERVICE_APP_STREAMIG_DOC | SERVICE_SPLIT_SCREEN | SERVICE_APP_DROPBOX | SERVICE_APP_WEB_VIEWER;
		private String vendor;
		private boolean isFraud;
		private int discoveryVersion;
		private static String mPredefinedKey = "AM2feY5ysJAA4oAM";
		private String mRealKey = "";
		private String mCapability = "";
		private HashMap<String, String> keyValuePairs = new HashMap<String, String>();
		private AtomicInteger mRpcID = new AtomicInteger(0);
		private CapabilityListener mCapabilityListener;
		private boolean isDisconnnected;

		public void setCapabilityListener(CapabilityListener mCapabilityListener) {
			this.mCapabilityListener = mCapabilityListener;
		}

		public boolean isDisconnnected() {
			return isDisconnnected;
		}

		public void updateCapability(ProjectorInfo projectorInfo) {
			mCapability = projectorInfo.getCapability();
			mRealKey = projectorInfo.getRealKey();
		}

		public interface CapabilityListener{
			void onCapabilitySet();
		}
		/**
		 * Return the version of the device.
		 * @return The protocol version of the device.
		 */
		public final String getOsVerion() {
			return osVerion;
		}
		/**
		 * Return the name of the device.
		 * @return The name of the device.
		 */
		public final String getName() {
			return name;
		}
		/**
		 * Return the IP address of the device.
		 * @return The IP address of the device.
		 */
		public final InetAddress getAddress() {
			return ipAddress;
		}
		/**
		 * Return the port number of WiFi display service.
		 * @return The port number of WiFi display service.
		 */
		public final int getWifiDisplayPortNumber() {
			return wifiDisplayPortNumber;
		}
		/**
		 * Return the port number of WiFi remote service.
		 * @return The port number of WiFi remote service.
		 */
		public final int getRemoteControlPortNumber() {
			return remoteControlPortNumber;
		}
		/**
		 * Return whether remote control is enabled. Return true if remote control is enabled; Otherwise, return false.
		 * @return Whether remote control is enabled.
		 */
		public final boolean isRemoteControlEnabled() {
			return remoteControlPortNumber != 0;
		}
		/**
		 * Return current passcode of the device. Application is recommended to verify the passcode before connecting to the device.
		 * @return Current passcode of the device.
		 * @see ProjectorInfo#hasNoPasscode
		 */
		public final String getPasscode() {
			return passcode;
		}
		/**
		 * Return the model name of the device.
		 * @return The model name of the device.
		 */
		public final String getModel() {
			return model;
		}
		/**
		 * Return the vendor name of the device.
		 * @return The vendor name of the device.
		 */
		public String getVendor() {
			return vendor;
		}
		/**
		 * Return the version of discovery protocol
		 * @return the discoveryVersion
		 */
		public final int getDiscoveryVersion() {
			return discoveryVersion;
		}
		/**
		 * Return whether passcode verification is needed before connecting to the device.
		 * @return Whether passcode verification is needed before connecting to the device.
		 * @see ProjectorInfo#getPasscode
		 */
		public final boolean hasNoPasscode() {
			return passcode == null || passcode.length() == 0;
		}

		public String getRealKey() {
			return mRealKey;
		}

		public String getCapability() {
			return mCapability;
		}

		public AtomicInteger getRpcID(){
			return mRpcID;
		}

		public static final Parcelable.Creator<ProjectorInfo> CREATOR
		= new Parcelable.Creator<ProjectorInfo>() {
			public ProjectorInfo createFromParcel(Parcel in) {
				return new ProjectorInfo(in);
			}

			public ProjectorInfo[] newArray(int size) {
				return new ProjectorInfo[size];
			}
		};
		@Override
		public int describeContents() {
			return 0;
		}
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(osVerion);
			dest.writeString(name);
			dest.writeSerializable(ipAddress);
			dest.writeInt(wifiDisplayPortNumber);
			dest.writeInt(remoteControlPortNumber);
			dest.writeString(passcode);
			dest.writeString(model);
			dest.writeInt(service);
			dest.writeString(vendor);
			dest.writeInt(discoveryVersion);
			dest.writeSerializable(keyValuePairs);
			dest.writeString(mRealKey);
			dest.writeString(mCapability);
			dest.writeInt(mRpcID.get());
		}
		protected ProjectorInfo() {
			
		}
		@SuppressWarnings("unchecked")
		private ProjectorInfo(Parcel in) {
			osVerion = in.readString();
			name = in.readString();
			ipAddress = (InetAddress) in.readSerializable();
			wifiDisplayPortNumber = in.readInt();
			remoteControlPortNumber = in.readInt();
			passcode = in.readString();
			model = in.readString();
			service = in.readInt();
			vendor = in.readString();
			discoveryVersion = in.readInt();
			keyValuePairs = (HashMap<String, String>) in.readSerializable();
			mRealKey = in.readString();
			mCapability = in.readString();
			mRpcID = new AtomicInteger(in.readInt());
	    }
		public boolean supportsMediaStreaming() {
			return (service & SERVICE_MEDIA_STREAMING) == SERVICE_MEDIA_STREAMING && Integer.valueOf(osVerion) > 1;
		}
		public boolean supportsPixViewer() {
			return (service & SERVICE_APP_PHOTO_VIEWER) == SERVICE_APP_PHOTO_VIEWER;
		}
		public boolean supportsLiveCam() {
			return (service & SERVICE_APP_LIVE_CAM) == SERVICE_APP_LIVE_CAM;
		}
		public boolean supportsStreamingDoc() {
			return (service & SERVICE_APP_STREAMIG_DOC) == SERVICE_APP_STREAMIG_DOC;
		}
		public boolean supportsSplitScreen() {
			return (service & SERVICE_SPLIT_SCREEN) == SERVICE_SPLIT_SCREEN;
		}
		public boolean supportsDropbox() {
			return (service & SERVICE_APP_DROPBOX) == SERVICE_APP_DROPBOX;
		}
		public boolean supportsWebViewer() {
			return (service & SERVICE_APP_WEB_VIEWER) == SERVICE_APP_WEB_VIEWER;
		}
		public boolean supportsQualityMode() {
			return (service & SERVICE_APP_QUALITY_MODE) == SERVICE_APP_QUALITY_MODE;
		}
		public boolean supportsHttpStreaming() {
			return (service & SERVICE_APP_HTTP_STREAMING) == SERVICE_APP_HTTP_STREAMING;
		}
		public boolean supportsRemoteControl() {
			return (service & SERVICE_APP_REMOTE_CONTROL) == SERVICE_APP_REMOTE_CONTROL;
		}
		public boolean supportsAudioMediaStreaming() {
			return (service & SERVICE_MEDIA_STREAM_AUDIO) == SERVICE_MEDIA_STREAM_AUDIO;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof ProjectorInfo) {
				if (ipAddress.equals(((ProjectorInfo) obj).ipAddress)) {
					return true;
				}
			}
			return false;
		}
		@Override 
		public int hashCode() {
			// Start with a non-zero constant.
			int result = 213;
			// Include a hash for each field.
			result = 31 * result + (osVerion == null ? 0 : osVerion.hashCode());
			result = 31 * result + (name == null ? 0 : name.hashCode());
			result = 31 * result + (ipAddress == null ? 0 : ipAddress.hashCode());
			result = 31 * result + wifiDisplayPortNumber;
			result = 31 * result + remoteControlPortNumber;
			result = 31 * result + (passcode == null ? 0 : passcode.hashCode());
			result = 31 * result + (model == null ? 0 : model.hashCode());
			result = 31 * result + service;
			result = 31 * result + (vendor == null ? 0 : vendor.hashCode());
			result = 31 * result + discoveryVersion;
			result = 31 * result + keyValuePairs.hashCode();
			return result;
		}
		@Override
		public int compareTo(ProjectorInfo another) {
			return ipAddress.getHostAddress().compareTo(another.ipAddress.getHostAddress());
		}
		/**
		 * Send remote control key code to the device.
		 * @param keyCode The key code.
		 */
		public void sendKey(final int keyCode) {
			sendKey(COMMAND_KEY, keyCode, false);
		}
		public void sendKeyTcp(final int keyCode) {
			sendKey(COMMAND_KEY, keyCode, false, true);
		}
		public void sendKeyAndWait(final int keyCode) {
			sendKey(COMMAND_KEY, keyCode, true);
		}
		public void sendKeyTcpAndWait(final int keyCode) {
			sendKey(COMMAND_KEY, keyCode, true, true);
		}
		public void sendVendorKey(final int keyCode) {
			sendKey(COMMAND_VENDOR, keyCode, false, true);
		}
		public void sendJSONRPC(final String command){
			sendJSONRPC(COMMAND_JSONRPC, command);
		}

		private static final String RPC_SET_DEVICE_DESCRIPTION = "common.set_device_description";
		private Object setDeviceDescriptionId ;


		public void sendJSONRPC(final int commandCode , final String command){
			if(command.contains(RPC_SET_DEVICE_DESCRIPTION)){
				try {
					JSONRPC2Request request = JSONRPC2Request.parse(command);
					setDeviceDescriptionId = request.getID();
				} catch (JSONRPC2ParseException e) {
					e.printStackTrace();
				}
			}
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						final byte[] data = (""+commandCode+":"+command).getBytes(REMOTE_CONTROL_MESSGAE_CHARSET);
						Falcon.getInstance().sendTcpRemoteControlData(data, ipAddress, remoteControlPortNumber);
					} catch (IOException e) {
						Falcon.getInstance().closeSocketToRemoteControl(ipAddress, remoteControlPortNumber);
						Falcon.getInstance().dispatchExceptionOnMain(ipAddress, e);
					} 
//					finally {
//						synchronized (ProjectorInfo.this) {
//							ProjectorInfo.this.notify();	
//						}
//					}
				}
				
			}).start();
//			synchronized(this) {
//				try {
//					this.wait();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
		}
		
		private void sendKey(final int commandCode, final int keyCode, final boolean wait) {
			sendKey(commandCode, keyCode, wait, false);
		}
		private void sendKey(final int commandCode, final int keyCode, final boolean wait, final boolean useTcp) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Log.d(TAG, "sendKey:(commandCode="+commandCode+", keyCode=" + keyCode+")");
						final byte[] data = (""+commandCode+":"+keyCode).getBytes(REMOTE_CONTROL_MESSGAE_CHARSET);
						if (useTcp) {
							Falcon.getInstance().sendTcpRemoteControlData(data, ipAddress, remoteControlPortNumber);
						} else {
							final DatagramSocket udpsocket = createDatagramSocket();
							final DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, remoteControlPortNumber);
							udpsocket.send(packet);
						}
					} catch (IOException e) {
						Falcon.getInstance().closeSocketToRemoteControl(ipAddress, remoteControlPortNumber);
						Falcon.getInstance().dispatchExceptionOnMain(ipAddress, e);
					} finally {
						if (wait) {
							synchronized (ProjectorInfo.this) {
								ProjectorInfo.this.notify();	
							}
						}
					}
				}
				
			}).start();
			if (wait) {
				synchronized(this) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		public void processJSONMsg(String receiveString) {

			JSONRPC2Response response = null;
			try {
				response = JSONRPC2Response.parse(parseMessageString(receiveString));
				if (setDeviceDescriptionId != null && setDeviceDescriptionId.equals(response.getID())) {
					JSONObject jsonObject = new JSONObject(response.getResult().toString());
					String key = jsonObject.optString("key");
					if (!key.isEmpty()) {
						mRealKey = CipherUtil.DecryptAES(key, mPredefinedKey, ALGORITHM_AES_CBC);
					}
					mCapability = jsonObject.optString("capability", "");

					Falcon.getInstance().updateProjector(this);
					if (!mCapability.isEmpty() && mCapabilityListener != null) {
						mCapabilityListener.onCapabilitySet();
					}
				}
			} catch (JSONRPC2ParseException e) {
				Log.d(TAG, e.getMessage());
			} catch (JSONException e) {
				Log.d(TAG, e.getMessage());
			}

		}

		/**
		 * Implement this interface if you want to receive message from the device.
		 * 
		 * @see ProjectorInfo#addMessageListener(MessageListener)
		 * @see ProjectorInfo#removeMessageListener(MessageListener)
		 * 
		 * @author jamchen
		 *
		 */
		public interface MessageListener {
			public void onReceiveMessage(ProjectorInfo projector, String message);
			public void onException(ProjectorInfo projector, Exception e);
			public void onDisconnect(ProjectorInfo projector);			
		}
		/**
		 * Add message listener to receive message from the device.
		 * @param listener
		 * @see MessageListener
		 */
		public void addMessageListener(MessageListener listener) {
			Falcon.getInstance().addMessageListener(this, listener);
		}
		/**
		 * Remove listener from the device if application doesn't want to receive message from the device. 
		 * @param listener 
		 * @see MessageListener
		 */
		public void removeMessageListener(MessageListener listener) {
			Falcon.getInstance().removeMessageListener(this, listener);
		}
		protected DatagramSocket createDatagramSocket() throws SocketException {
			return new DatagramSocket();
		}		
		public void disconnectRemoteControl() {
			mRealKey = "";
			mCapability = "";
			mRpcID.set(0);
			isDisconnnected = true;
			Falcon.getInstance().closeSocketToRemoteControl(ipAddress, remoteControlPortNumber);
		}
		/**
		 * Get parameter value from given key.
		 * @param key
		 * @return parameter value from given key
		 */
		public String getParameter(String key) {
			return keyValuePairs.get(key);
		}
		public static final int SERVICE_CLIENT_MODE = 0x200000;
		private int getServiceFlags() {
			String serviceStringParam;
			String serviceString;
			serviceStringParam = "ezcast.service.android." + Locale.getDefault().getCountry();
			serviceString = getParameter(serviceStringParam);

			if (serviceString == null) {
				serviceStringParam = "ezcast.service.android";
				serviceString = getParameter(serviceStringParam);
			}
			
			if (serviceString == null) {
				android.util.Log.e("ServiceBitmask","Can not parse service!");
				return -1;
			}
			
			return ((int) Long.parseLong(serviceString, 16));
		}
		public boolean supportClientMode() {
			return true;
			//20190423 Henry: deprecate this service bit
			//return (getServiceFlags() & SERVICE_CLIENT_MODE) != 0;
		}
	}
	private ArrayList<SearchReultListener> listeners = new ArrayList<SearchReultListener>();
	private ArrayList<ProjectorInfo> projectors = new ArrayList<ProjectorInfo>();
	private ArrayList<ProjectorInfo> tempProjectors = new ArrayList<ProjectorInfo>(); // for WiFi display response only
	private HashMap<String,Runnable> mProjectorTimeoutMap =  new HashMap<String, Runnable>();
	private static Falcon singleton;
	/**
	 * Return the shared instance of Falcon.
	 * @return The shared instance of Falcon.
	 */
	static public Falcon getInstance() {
		if (singleton == null) {
			singleton = new Falcon();
		}
		return singleton;
	}
	synchronized protected void sendTcpRemoteControlData(byte[] data, InetAddress ipAddress, int remoteControlPortNumber) throws IOException {
		final Socket socketToRemoteControl = createSocketToRemoteControlIfNeeded(3000, ipAddress, remoteControlPortNumber);
		final OutputStream socketOutputStream = socketToRemoteControl.getOutputStream();
		final ByteBuffer packet = ByteBuffer.allocate(4+data.length);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		packet.putInt(data.length);
		packet.put(data);
		socketOutputStream.write(packet.array());
		socketOutputStream.flush();
	}
	private class RemoteControlReceiverThread extends Thread {
		private boolean isReady;
		public synchronized boolean isReady() {
			return isReady;
		}
		public synchronized void setReady(boolean isReady) {
			this.isReady = isReady;
		}
		public RemoteControlReceiverThread(Runnable runnable) {
			super(runnable);
		}
		public void run() {
			synchronized(this) {
				setReady(true);
				this.notify();
			}
			super.run();
		}
		public void waitUntilThreadRun() {
			synchronized(this) {
				try {
					while (!this.isReady()) {
						this.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private Socket createSocketToRemoteControlIfNeeded(int timeout, final InetAddress ipAddress, final int portNumber) throws IOException {
		synchronized (socketsToRemoteControls) {
			Log.d(TAG, "Try to find socket for address:" + ipAddress.toString());
			Socket socketToRemoteControl = socketsToRemoteControls.get(ipAddress);
			if (socketToRemoteControl == null) {
				Log.d(TAG, "Cannot find socket for address:" + ipAddress.toString());
				socketToRemoteControl = new Socket();
				socketToRemoteControl.connect(new InetSocketAddress(ipAddress, portNumber), timeout);
				socketsToRemoteControls.put(ipAddress, socketToRemoteControl);
				final InputStream inputStream = socketToRemoteControl.getInputStream();
				RemoteControlReceiverThread remoteControlReceiver = new RemoteControlReceiverThread(new Runnable() {
					private ProjectorInfo projectorInfo;
					@Override
					public void run() {
						try {
							projectorInfo = createProjectorWithAddressIfNeeded(ipAddress);
							projectorInfo.remoteControlPortNumber = EZ_REMOTE_CONTROL_PORT_NUMBER;
							while (true) {
								final ByteBuffer header = ByteBuffer.allocate(4);
								header.order(ByteOrder.LITTLE_ENDIAN);
								final int headerSize = inputStream.read(header.array(), 0, header.capacity());
								if (headerSize == -1) {
									break;
								} else if (headerSize == 4) {
									int payloadSize = header.getInt();
									final ByteBuffer payload = ByteBuffer.allocate(payloadSize);
									payload.order(ByteOrder.LITTLE_ENDIAN);

									if (payloadSize > 0) {
										int readBytes = 0;
										int len = payload.array().length;
										while(readBytes < len) {
											int read = inputStream.read(payload.array(), readBytes, len - readBytes);
											if (read == -1) {
												break;
											}
											readBytes += read;
										}
									}

									Log.d(TAG, "Receive TCP packet. Payload size:" + payloadSize);
									final String receiveString = new String(payload.array(), REMOTE_CONTROL_MESSGAE_CHARSET);
									processRemoteControlMessage(projectorInfo, receiveString);
								}
							}
						} catch (Exception e) {
							if(!projectorInfo.isDisconnnected()){
								dispatchExceptionOnMain(ipAddress, e);
							}
						} finally {
							closeSocketToRemoteControl(ipAddress, portNumber);
							if(!projectorInfo.isDisconnnected()){
								dispatchOnDisconnect(ipAddress);
							}
						}
					}

				});
				remoteControlReceiver.start();
				// waiting for thread to be executed
				remoteControlReceiver.waitUntilThreadRun();
			} else {
				Log.d(TAG, "Find socket for address:" + ipAddress.toString());				
			}
			return socketToRemoteControl;
		}

	}
	private void dispatchOnDisconnect(InetAddress ipAddress) {
		Log.d(TAG, "dispatchOnDisconnect");
		final ProjectorInfo projector = createProjectorWithAddressIfNeeded(ipAddress);
		mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				dispatchDisconnection(projector);
			}				
		});
	}
	private void closeSocketToRemoteControl(InetAddress ipAddress, int portNumber) {
		synchronized (socketsToRemoteControls) {
			final Socket socketToRemoteControl = socketsToRemoteControls.get(ipAddress);
			if (socketToRemoteControl != null) {
				try {
					socketToRemoteControl.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					socketsToRemoteControls.remove(ipAddress);
				}
			}
		}
	}
	private final HashMap<InetAddress, Socket> socketsToRemoteControls = new HashMap<InetAddress, Socket>();
	private final HashMap<InetAddress, Set<MessageListener>> privareMessageListeners = new HashMap<InetAddress, Set<MessageListener>>();
	
	protected void addPrivateMessageListener(ProjectorInfo projectorInfo, MessageListener listener) {
		Set<MessageListener> listeners = privareMessageListeners.get(projectorInfo.getAddress());
		if (listeners == null) {
			listeners = new HashSet<MessageListener>();
			privareMessageListeners.put(projectorInfo.getAddress(), listeners);
		}
		if (listeners != null) {
			listeners.add(listener);
		}
	}
	protected void removePrivateMessageListener(ProjectorInfo projectorInfo, MessageListener listener) {
		final Set<MessageListener> listeners = privareMessageListeners.get(projectorInfo.getAddress());
		if (listeners != null) {
			listeners.remove(listener);
		}
	}
	private void dispatchPrivateMessage(ProjectorInfo projectorInfo, String message) {
		if (message != null) {
			final Set<MessageListener> listeners = privareMessageListeners.get(projectorInfo.getAddress());
			if (listeners != null) {
				for (final MessageListener listener : listeners) {
					listener.onReceiveMessage(projectorInfo, message);
				}
			}
		}
	}
	private final HashMap<InetAddress, Set<MessageListener>> messageListeners = new HashMap<InetAddress, Set<MessageListener>>();
	
	private void addMessageListener(ProjectorInfo projectorInfo, MessageListener listener) {
		synchronized (messageListeners) {
			Set<MessageListener> listeners = messageListeners.get(projectorInfo.getAddress());
			if (listeners == null) {
				Log.d(TAG, "Create MessageListener container for " + projectorInfo.getAddress().getHostAddress());
				listeners = new HashSet<MessageListener>();
				messageListeners.put(projectorInfo.getAddress(), listeners);
			}
			if (listeners != null) {
				Log.d(TAG, "Add MessageListener to container");
				listeners.add(listener);
			}
		}
	}
	private void removeMessageListener(ProjectorInfo projectorInfo, MessageListener listener) {
		synchronized (messageListeners) {
			final Set<MessageListener> listeners = messageListeners.get(projectorInfo.getAddress());
			if (listeners != null) {
				listeners.remove(listener);
			}
		}
	}

	private void dispatchMessage(ProjectorInfo projectorInfo, String message) {
		if (message != null) {
			Log.d(TAG, "dispatchMessage:" + message);
			Set<MessageListener> listenersCopy = null;
			synchronized (messageListeners) {
				Set<MessageListener> listeners = messageListeners.get(projectorInfo.getAddress());
				if (listeners != null) {
					listenersCopy = new HashSet<MessageListener>(listeners);
				}
			}
			if (listenersCopy != null) {
				for (final MessageListener listener : listenersCopy) {
					Log.d(TAG, "send to " + listener);
					listener.onReceiveMessage(projectorInfo, message);
				}
			} else {
				Log.d(TAG, "no listener for " + projectorInfo.getAddress().getHostAddress());
			}
		}
	}
	private void dispatchExceptionOnMain(InetAddress address, final Exception e) {
		if (e != null) {
			Log.d(TAG, "dispatchException:" + e);
			e.printStackTrace();
			final ProjectorInfo projector = createProjectorWithAddressIfNeeded(address);
			mainThreadHandler.post(new Runnable() {

				@Override
				public void run() {
					dispatchException(projector, e);
				}				
			});

		}
	}
	private static class MainThreadHandler extends Handler {
		private Falcon falcon;

		public MainThreadHandler(Looper mainLooper, Falcon falcon) {
			super(mainLooper);
			this.falcon = falcon;
		}

		@Override
		public void handleMessage (Message msg) {
			switch (msg.what) {
			case MSG_SearchDidStart:
//				falcon.notifyListenerWillStart(); 
				break;					
			case MSG_SearchDidFind:
				falcon.notifyListenerDidFind((ProjectorInfo) msg.obj);
				break;
			case MSG_SearchDidEnd:
//				falcon.notifyListenerSearchDidEnd();
				break;
			}
		}
	}
	protected Falcon() {		
		singleton = this;
		startListening();
	}
	protected DatagramSocket createDatagramSocket() throws SocketException {
		DatagramSocket broadcastSocket = null;
		try {
			broadcastSocket = new DatagramSocket(null);
			broadcastSocket.setReuseAddress(true);
			broadcastSocket.bind(new InetSocketAddress((InetAddress)null, EZ_REMOTE_CONTROL_PORT_NUMBER));
		} catch (SocketException e) {
			e.printStackTrace();
			broadcastSocket = new DatagramSocket();
		}
		return broadcastSocket;
	}
	private void startListening() {
		if (broadcastSocket == null && receivingThread == null) {
			try {
				broadcastSocket = createDatagramSocket();
				broadcastSocket.setBroadcast(true);
				waitFeedbackInBackground();
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
	}	
	/**
	 * Get a list of found devices.
	 * @return A list of found devices.
	 */
	public ArrayList<ProjectorInfo> getProjectors() {
		return projectors;
	}
	/**
	 * Register a {@link SearchReultListener} to Falcon. 
	 * @param listener A {@link SearchReultListener} which receives notifications of Falcon.
	 * @see Falcon#removeSearchResultListener(SearchReultListener)
	 */
	public void addSearchResultListener(SearchReultListener listener) {
		synchronized(listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}
	/**
	 * Unregister a {@link SearchReultListener} from Falcon.
	 * @param listener The {@link SearchReultListener} to be removed.
	 * @see Falcon#addSearchResultListener(SearchReultListener)
	 */
	public void removeSearchResultListener(SearchReultListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	static public final int EZ_REMOTE_CONTROL_PORT_NUMBER = 63630;
	static public final int EZ_WIFI_DISPLAY_PORT_NUMBER = 2425;
	private static ArrayList<InetAddress> getBroadcastAddresses() {
        ArrayList<InetAddress> listOfBroadcasts = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> list;
        try {
        	list = NetworkInterface.getNetworkInterfaces();

        	while(list.hasMoreElements()) {
        		NetworkInterface iface = (NetworkInterface) list.nextElement();

        		if(iface == null) continue;

        		if(!iface.isLoopback() && iface.isUp()) {
        			Log.i(TAG, "Found non-loopback, up interface:" + iface);

        			Iterator<InterfaceAddress> it = iface.getInterfaceAddresses().iterator();
        			while (it.hasNext()) {
        				InterfaceAddress address = it.next();

        				Log.i(TAG, "Found address: " + address);

        				if(address == null) continue;
        				InetAddress broadcast = address.getBroadcast();
        				if(broadcast != null) listOfBroadcasts.add(broadcast);
        			}
        		}
        	}
        } catch (SocketException ex) {
        	return new ArrayList<InetAddress>();
        } catch (NullPointerException e) {

        }

        return listOfBroadcasts;
	}	
//	static private final String lookupCommand = "1:0:am:amhost:1\0"+"\nUN:am\nHN:amhost\nNN:am\nGN:";
	
	/**
	 * Interface definition for a callback to be invoked when Falcon is looking for devices.
	 * @author James Chen
	 *
	 */
	public interface SearchReultListener {
//		/**
//		 * Called when the Falcon starts to search.
//		 * @param falcon The falcon which starts to search.
//		 */
//		public void falconSearchWillStart(Falcon falcon);
		/**
		 * Called when the Falcon found a device.
		 * @param falcon The falcon which is searching for devices.
		 * @param projectorInfo The device which was just found.
		 */
		public void falconSearchDidFindProjector(Falcon falcon, ProjectorInfo projectorInfo);

		public void falconSearchDidRemoveProjector(Falcon falcon, ProjectorInfo projectorInfo);
//		/**
//		 * Called when the Falcon stops searching.
//		 * @param falcon The falcon which is about to stop searching.
//		 */
//		public void falconSearchDidEnd(Falcon falcon);
	}
	private final Handler mainThreadHandler = new MainThreadHandler(Looper.getMainLooper(), this);
	private static final int MSG_SearchDidStart	= 0;
	private static final int MSG_SearchDidFind	= 1;
	private static final int MSG_SearchDidEnd	= 2;
	private DatagramSocket broadcastSocket;
	private boolean searching;
	private static final int INITIAL_LOOKUP_INTERVAL = 1;
	private int lookupInterval = INITIAL_LOOKUP_INTERVAL;
	protected Runnable pendingLookup;
	protected boolean shouldStopReceiving;
	/**
	 * Start searching devices. If it's already in searching state, it does nothing.
	 */
	synchronized public void search() {
		startListening();
		if (broadcastSocket != null) {
			Log.d(TAG, "Clear projector list");
			synchronized(projectors) {
				projectors.clear();
			}
			searching = true;
			lookupInterval = INITIAL_LOOKUP_INTERVAL;
			sendLookupCommand();
		}		
	}

	synchronized public void search(String targetHost) {
		startListening();
		if (broadcastSocket != null) {
			Log.d(TAG, "Clear projector list");
			synchronized(projectors) {
				projectors.clear();
			}
			searching = true;
			lookupInterval = INITIAL_LOOKUP_INTERVAL;
			sendLookupCommand(targetHost);
		}
	}
	/**
	 * Indicate whether it's searching.
	 * @return Whether it's searching.
	 */
	public boolean isSearching() {
		return searching;
	}
	/**
	 * Stop searching. 
	 */
	public void stop() {
		if (receivingThread != null) {
			shouldStopReceiving = true;
			try {
				receivingThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			receivingThread = null;
		}
		synchronized(this) {
			if (broadcastSocket != null) {
				broadcastSocket.close();
				broadcastSocket = null;
			}
		}
		if (pendingLookup != null) {
			mainThreadHandler.removeCallbacks(pendingLookup);
			pendingLookup = null;
		}

		removeTimeOutRunnable();

		searching = false;
	}

	private void removeTimeOutRunnable() {
		if (null != mProjectorTimeoutMap) {
			Iterator it = mProjectorTimeoutMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Runnable> pair = (Map.Entry) it.next();
				mainThreadHandler.removeCallbacks(pair.getValue());
				it.remove(); // avoids a ConcurrentModificationException
			}
		}
	}

	private Thread receivingThread;
	private void waitFeedbackInBackground() {
		shouldStopReceiving = false;
		receivingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized(Thread.currentThread()) {
					Thread.currentThread().notify();
				}
				if (broadcastSocket != null) {
					try {
						final byte[] recvBuf = new byte[512*1024];
						Log.d(TAG, "start receiveing");
						broadcastSocket.setSoTimeout(1000);
						while (!shouldStopReceiving) {
							final DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
							try {
								broadcastSocket.receive(recvPacket);
								if (recvPacket.getPort() == EZ_REMOTE_CONTROL_PORT_NUMBER) {
									handleRemoteControlMessage(recvPacket);
								} else if (recvPacket.getPort() == EZ_WIFI_DISPLAY_PORT_NUMBER) {
									handleWifiDisplayMessage(recvPacket);
								} else {
									final String [] receiveStrings = new String(recvPacket.getData(), 0, recvPacket.getLength()).split("\0");
									Log.d(TAG, "datagramSocket receive:" + ((receiveStrings.length>0)?receiveStrings[0]:"null") + " from:" + recvPacket.getAddress());
								}
							} catch (SocketTimeoutException e) {
								Log.d(TAG, "Search timeout");					
							}
						}
					} catch (SocketTimeoutException e) {
						Log.d(TAG, "Search timeout");					
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
					}
				}
			}
		});
		receivingThread.start();
		synchronized(receivingThread) {
			try {
				receivingThread.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	protected static String parseMessageString(final String receiveString) {
		final String [] parameters = receiveString.split(":");
		if (parameters.length >= 3) {
			@SuppressWarnings("unused")
			final String version = parameters[1];
			return Utils.concatStringsWithSeparator(Arrays.asList(Arrays.copyOfRange(parameters, 2, parameters.length)), ":");

		}
		return null;
	}
	protected static void parseRemoteControlResponseString(final String receiveString, ProjectorInfo projectorInfo) {
		if (receiveString.startsWith("EZREMOTE:")) {
			final String [] parameters = receiveString.split(":");
			final HashMap<String, String> keyValuePairs = parseKeyValuePairs(parameters);
			if (keyValuePairs.containsKey(PARAMETER_MODEL_KEY)) {
				projectorInfo.model = keyValuePairs.get(PARAMETER_MODEL_KEY);
			}
			if (keyValuePairs.containsKey(PARAMETER_VENDOR_KEY)) {
				projectorInfo.vendor = keyValuePairs.get(PARAMETER_VENDOR_KEY);
			}
			if (keyValuePairs.containsKey("hostname")) { // Add since EZCast
				projectorInfo.name = keyValuePairs.get("hostname");
			}
			projectorInfo.keyValuePairs.putAll(keyValuePairs);
		}
	}
	private ProjectorInfo getProjectorInfoWithAddressInList(InetAddress address, List<ProjectorInfo> list) {
		synchronized(list) {
			Iterator<ProjectorInfo> iterator = list.listIterator();
			while (iterator.hasNext()) {
				ProjectorInfo projector = iterator.next(); 
				if (projector.ipAddress.equals(address)) {
					return projector;
				}
			}
			return null;
		}
	}
	private ProjectorInfo getProjectorInfoWithAddress(InetAddress address) {
		ProjectorInfo projector = getProjectorInfoWithAddressInList(address, projectors);
		if (projector != null) {
			return projector;
		}
		projector = getProjectorInfoWithAddressInList(address, tempProjectors);
		if (projector != null) {
			return projector;
		}			
		return null;
	}
	private static final int IPMSG_VERSION = 0x002;
	
	private static final int IPMSG_NOOPERATION 	= 0x0000;
	private static final int IPMSG_BR_ENTRY		= 0x0001;
	private static final int IPMSG_BR_EXIT 		= 0x0002;
	private static final int IPMSG_ANSENTRY 	= 0x0003;
    private static final int IPMSG_SENDDATA 	= 0x0022;
    private static final int IMAGE_PACKET_HEADER_SIZE = 16;
	private static final int MAX_LOOKUP_INTERVAL = 2;
    private static final int EZ_DISPLAY_QUERY_HEADER_SIZE = 24;
    private static final int PICO_QUERY_CMD = 1;
	
	private static long s_commandSequenceNumber = 0;
	private static final byte[] generateCommand(String username, String hostname, int command) {
		String commandInString = IPMSG_VERSION+":"+s_commandSequenceNumber+":"+username+":"+hostname+":"+command+":"+"\0"+"\nUN:"+username+"\nHN:"+hostname+"\nNN:"+username+"\nGN:";
		s_commandSequenceNumber++;
		return commandInString.getBytes();
	}
    private static final byte[] generateCleanCommand(String username, String hostname, int command) {
        String commandInString = IPMSG_VERSION+":"+s_commandSequenceNumber+":"+username+":"+hostname+":"+command+":"+"\0";
        s_commandSequenceNumber++;
        return commandInString.getBytes();
    }
	private static final byte[] generateEntryCommand(String username, String hostname) {
		return generateCommand(username, hostname, IPMSG_BR_ENTRY);
	}
	private static final byte[] generateExitCommand(String username, String hostname) {
		return generateCommand(username, hostname, IPMSG_BR_EXIT);
	}
    private static final byte[] generateQueryCommand(String username, String hostname) {
        return generateCleanCommand(username, hostname, IPMSG_SENDDATA);
    }
	@SuppressWarnings("unused")
	private static final byte[] generateNoOperationCommand(String username, String hostname) {
		return generateCommand(username, hostname, IPMSG_NOOPERATION);
	}
	private void sendLookupCommand() {
		Log.d(TAG, "sendLookupCommand");	
		
		Thread commandThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "sendLookupCommand thread");
				synchronized(Falcon.this) {
					Log.d(TAG, "sendLookupCommand thread:" + broadcastSocket);
					if (broadcastSocket != null) {
						try {
							for (final InetAddress broadcastAddress : Falcon.getBroadcastAddresses()) {
								byte[] command = {'0',':','3'};
								// send EZ Remote Lookup
								broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_REMOTE_CONTROL_PORT_NUMBER));

								Log.d(TAG, "send entry command");	
								// copy the logic from CSocketEx for Windows.
								//							command = generateNoOperationCommand("android", "android");
								//							broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_WIFI_DISPLAY_PORT_NUMBER));
								//							Thread.sleep(1000);
								command = generateEntryCommand("android", "android");
								broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_WIFI_DISPLAY_PORT_NUMBER));
							}
						} catch (SocketTimeoutException e) {
							e.printStackTrace();					
						} catch (SocketException e) {
							e.printStackTrace();
						} catch (UnknownHostException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							synchronized(Falcon.this) {
								if (pendingLookup != null) {
									mainThreadHandler.removeCallbacks(pendingLookup);
									pendingLookup = null;
								}

								pendingLookup = new Runnable() {
									@Override
									public void run() {
										synchronized(Falcon.this) {
											pendingLookup = null;
										}
										sendLookupCommand();
									}
								};
							}
							mainThreadHandler.postDelayed(pendingLookup, lookupInterval * 1000);
							lookupInterval = lookupInterval + 1;
							if (lookupInterval > MAX_LOOKUP_INTERVAL) {
								lookupInterval = MAX_LOOKUP_INTERVAL;
							}
						}
					}
				}
			}
		});
		commandThread.start();
	}

	private void sendLookupCommand(final String targetHost) {
		Log.d(TAG, "sendLookupCommand");

		Thread commandThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "sendLookupCommand thread");
				synchronized(Falcon.this) {
					Log.d(TAG, "sendLookupCommand thread:" + broadcastSocket);
					if (broadcastSocket != null) {
						try {
							InetAddress broadcastAddress = InetAddress.getByName(targetHost);
								byte[] command = {'0',':','3'};
								// send EZ Remote Lookup
							broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_REMOTE_CONTROL_PORT_NUMBER));

							Log.d(TAG, "send entry command");
							// copy the logic from CSocketEx for Windows.
							//							command = generateNoOperationCommand("android", "android");
							//							broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_WIFI_DISPLAY_PORT_NUMBER));
							//							Thread.sleep(1000);
							command = generateEntryCommand("android", "android");
							broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_WIFI_DISPLAY_PORT_NUMBER));
						} catch (SocketTimeoutException e) {
							e.printStackTrace();
						} catch (SocketException e) {
							e.printStackTrace();
						} catch (UnknownHostException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							synchronized(Falcon.this) {
								if (pendingLookup != null) {
									mainThreadHandler.removeCallbacks(pendingLookup);
									pendingLookup = null;
								}

								pendingLookup = new Runnable() {
									@Override
									public void run() {
										synchronized(Falcon.this) {
											pendingLookup = null;
										}
										sendLookupCommand();
									}
								};
							}
							mainThreadHandler.postDelayed(pendingLookup, lookupInterval * 1000);
							lookupInterval = lookupInterval + 1;
							if (lookupInterval > MAX_LOOKUP_INTERVAL) {
								lookupInterval = MAX_LOOKUP_INTERVAL;
							}
						}
					}
				}
			}
		});
		commandThread.start();
	}
	/**
	 *
	 * @deprecated 
	 */
	public static void sendExitCommand() {
		Thread commandThread = new Thread(new Runnable() {
			@Override
			public void run() {
				DatagramSocket broadcastSocket = null;
				try {
					Log.d(TAG, "send exit command");	
					broadcastSocket = new DatagramSocket();
					broadcastSocket.setBroadcast(true);
					byte command[] = generateExitCommand("android", "android");
					for (final InetAddress broadcastAddress : Falcon.getBroadcastAddresses()) {
						broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_WIFI_DISPLAY_PORT_NUMBER));
					}
				} catch (SocketTimeoutException e) {
					Log.d(TAG, "Exit timeout");					
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					broadcastSocket.close();
				}
			}
			
		});
		commandThread.start();
	}
//	private void notifyListenerWillStart() {
//		Iterator<SearchReultListener> iterator = listeners.listIterator();
//		while (iterator.hasNext()) {
//			SearchReultListener listener = iterator.next(); 
//			listener.falconSearchWillStart(Falcon.this);
//		}
//	}
	private void notifyListenerDidFind(ProjectorInfo projector) {
		synchronized(listeners) {
			Iterator<SearchReultListener> iterator = listeners.listIterator();
			while (iterator.hasNext()) {
				SearchReultListener listener = iterator.next(); 
				listener.falconSearchDidFindProjector(Falcon.this, projector);						
			}
		}
	}

	private void notifyListenerDidRemove(ProjectorInfo projector) {
		synchronized(listeners) {
			Iterator<SearchReultListener> iterator = listeners.listIterator();
			while (iterator.hasNext()) {
				SearchReultListener listener = iterator.next();
				listener.falconSearchDidRemoveProjector(Falcon.this, projector);
			}
		}
	}
//	private void notifyListenerSearchDidEnd() {
//		Iterator<SearchReultListener> iterator = listeners.listIterator();
//		while (iterator.hasNext()) {
//			SearchReultListener listener = iterator.next(); 
//			listener.falconSearchDidEnd(Falcon.this);					
//		}
//	}
	private ProjectorInfo createProjectorWithAddressIfNeeded(
			final InetAddress address) {
		ProjectorInfo projectorInfo;
		synchronized(tempProjectors) {
			projectorInfo = getProjectorInfoWithAddress(address);
			if (projectorInfo == null) {
				projectorInfo = new ProjectorInfo();
				projectorInfo.ipAddress = address;
				tempProjectors.add(projectorInfo);
			}
		}
		return projectorInfo;
	}
	private void handleRemoteControlMessage(final DatagramPacket recvPacket) {
		final ProjectorInfo projectorInfo = createProjectorWithAddressIfNeeded(recvPacket.getAddress());
		projectorInfo.remoteControlPortNumber = EZ_REMOTE_CONTROL_PORT_NUMBER;
		final String receiveString = getRemoteControlMessageFromDatagramPacket(recvPacket);
		if (receiveString != null) {
			processRemoteControlMessage(projectorInfo, receiveString);
		}
	}
	private String getRemoteControlMessageFromDatagramPacket(
			final DatagramPacket recvPacket) {
		final String [] receiveStrings = new String(recvPacket.getData(), 0, recvPacket.getLength(), Charset.forName(REMOTE_CONTROL_MESSGAE_CHARSET)).split("\0");
		if (receiveStrings.length > 0) {
			return receiveStrings[0];
		}
		return null;
	}
	private void processRemoteControlMessage(final ProjectorInfo projectorInfo, final String receiveString) {
		Log.d(TAG, "receive EZ Remote message:" + receiveString + " from:" + projectorInfo.getAddress().getHostAddress());
		if (receiveString.startsWith(projectorInfo.getAddress().getHostAddress())) { //backward compatibility
			addProjector(projectorInfo);
			mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
		} else if (receiveString.startsWith("EZREMOTE:")) {
			parseRemoteControlResponseString(receiveString, projectorInfo);
			if (projectorInfo.supportClientMode() || isDirectConnenctedIpAddress(projectorInfo.getAddress())) {
				if(null == projectorInfo.getOsVerion()) {
					projectorInfo.osVerion = "2";
					projectorInfo.service |= SERVICE_APP_HTTP_STREAMING | SERVICE_MEDIA_STREAM_AUDIO | SERVICE_EZENCODEPRO;
				}
				addProjector(projectorInfo);
				mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
			} else {
				synchronized (tempProjectors) {
					tempProjectors.remove(projectorInfo);
				}
			}
		} else if (receiveString.startsWith("STANDARD:")) {	//小機發給App的message (公版）
			mainThreadHandler.post(new Runnable() {

				@Override
				public void run() {
					dispatchPrivateMessage(projectorInfo, parseMessageString(receiveString));
				}
				
			});
			
		} else if (receiveString.startsWith("CUSTOMER")) {	//小機發給App的message (客戶案用)
			mainThreadHandler.post(new Runnable() {

				@Override
				public void run() {
					dispatchMessage(projectorInfo, parseMessageString(receiveString));
				}
				
			});
		} else if(receiveString.startsWith("JSONRPC")){//dispatch JSON message
			projectorInfo.processJSONMsg(receiveString);
			// avoid post msg in main handler for JRPC to avoid deadlock issue
			dispatchMessage(projectorInfo, receiveString);
		}
	}

	private boolean isDirectConnenctedIpAddress(InetAddress address) {
		if (address != null) {
			if (address.getHostAddress().equals("192.168.111.1") ||
					address.getHostAddress().equals("192.168.203.1") ||
					address.getHostAddress().equals("192.168.168.1")) {
				return true;
			}
		}
		return false;
	}
	private void addProjector(final ProjectorInfo projectorInfo) {
		synchronized (projectors) {
			if (!projectors.contains(projectorInfo)) {
				projectors.add(projectorInfo);
				Runnable timeoutRunnable = new Runnable() {
					@Override
					public void run() {
						Log.d(TAG, "notifyListenerDidRemove:" + projectorInfo.getName());
						notifyListenerDidRemove(projectorInfo);
					}
				};
				mProjectorTimeoutMap.put(projectorInfo.getName(), timeoutRunnable);
				mainThreadHandler.postDelayed(timeoutRunnable, DEVICE_ALIVE_TIMEOUT);
			} else{
				Runnable timeoutRunnable = mProjectorTimeoutMap.get(projectorInfo.getName());
				if (null != timeoutRunnable) {
					mainThreadHandler.removeCallbacks(timeoutRunnable);
					mainThreadHandler.postDelayed(timeoutRunnable, DEVICE_ALIVE_TIMEOUT);
				}
			}
		}
		synchronized (tempProjectors) {
			tempProjectors.remove(projectorInfo);
		}
	}

	public void updateProjector(ProjectorInfo projectorInfo){
		for (int i = 0; i < projectors.size(); i++) {
			ProjectorInfo projector = projectors.get(i);
			if(projectorInfo.equals(projector)){
				projector.updateCapability(projectorInfo);
			}
		}
	}
	private void handleWifiDisplayMessage(final DatagramPacket recvPacket) {
		final ProjectorInfo projectorInfo = createProjectorWithAddressIfNeeded(recvPacket.getAddress());
		if (parseWifiDisplayResponse(recvPacket, projectorInfo)) {
			Log.d(TAG, "receive EZ Wifi Response");
			projectorInfo.wifiDisplayPortNumber = EZ_WIFI_DISPLAY_PORT_NUMBER;
			// we don't report wifi display discovery anymore.
//			mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
		} else {
			// it's fraud, let's remove it from tempProjectors if it's still in tempProjectors.
			synchronized(tempProjectors) {
				tempProjectors.remove(projectorInfo);
			}
		}
	}
	private void dispatchException(final ProjectorInfo projector,
			final Exception e) {
		synchronized (messageListeners) {
			Set<MessageListener> listeners = messageListeners.get(projector.getAddress());
			if (listeners != null) {
				listeners = new HashSet<MessageListener>(listeners);
				for (final MessageListener listener : listeners) {
					Log.d(TAG, "send to " + listener);
					listener.onException(projector, e);
				}
			} else {
				Log.d(TAG, "no listener for " + projector.getAddress().getHostAddress());
			}
		}
	}
	private void dispatchDisconnection(final ProjectorInfo projector) {
		synchronized (messageListeners) {
			Set<MessageListener> listeners = messageListeners.get(projector.getAddress());
			if (listeners != null) {
				listeners = new HashSet<MessageListener>(listeners);
				for (final MessageListener listener : listeners) {
					Log.d(TAG, "send to " + listener);
					listener.onDisconnect(projector);
				}
			} else {
				Log.d(TAG, "no listener for " + projector.getAddress().getHostAddress());
			}
		}
	}
	private static boolean parseWifiDisplayResponse(final DatagramPacket recvPacket, ProjectorInfo projectorInfo) {
		final String [] receiveStrings = new String(recvPacket.getData(), 0, recvPacket.getLength(), Charset.forName("UTF-8")).split("\0");
		if (receiveStrings.length > 0) {
			Log.d(TAG, "parseWifiDisplayResponse:" + receiveStrings[0]);
			return parseWifiDisplayResponseString(receiveStrings[0], projectorInfo);
		}
		return false;
	}
	private static HashMap<String, String> parseKeyValuePairs(String [] parameters) {
		final HashMap<String, String> keyValuePairs = new HashMap<String, String>();
		for (final String parameter : parameters) {
			if (parameter.contains("=")) {
				final String[] keyAndValue = parameter.split("=");
				if (keyAndValue.length > 1) {
					keyValuePairs.put(keyAndValue[0], keyAndValue[1]);
				} else {
					keyValuePairs.put(keyAndValue[0], "");					
				}
			}
		}
		return keyValuePairs;
	}
	protected static boolean parseWifiDisplayResponseString(String receiveString, ProjectorInfo projectorInfo) {
		//07-23 13:10:54.940: D/Falcon(31650): datagramSocket receive:1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744 from:/192.168.111.1
//	    Log.d(TAG, "datagramSocket receive:" + ((receiveStrings.length>0)?receiveStrings[0]:"null") + " from:" + recvPacket.getAddress());
		final String [] parameters = receiveString.split(":");
		if (parameters.length >= 5 && parameters[4].equals(String.valueOf(IPMSG_ANSENTRY))) {
			projectorInfo.osVerion = parameters[0];
			if (!parameters[3].equals("(none)")) {
				projectorInfo.name = parameters[3];
			}
			HashMap<String, String> keyValuePairs = parseKeyValuePairs(parameters);

            int[] res = getMaxResoulution(projectorInfo);
			keyValuePairs.put("max_w", String.valueOf(res[0]));
			keyValuePairs.put("max_h", String.valueOf(res[1]));
            if (keyValuePairs.containsKey(PARAMETER_DISCOVERY_KEY)) {
				projectorInfo.discoveryVersion = Integer.valueOf(keyValuePairs.get(PARAMETER_DISCOVERY_KEY));
				if (checkMd5(parameters)) {
					processAllValue(keyValuePairs, projectorInfo);
				} else {
					Log.i(TAG, "is fraud!!!!==>" + receiveString);
					projectorInfo.isFraud = true;
				}
			} else {
				processAllValue(keyValuePairs, projectorInfo);
			}
			// Add sanity check to filter out other products which use same protocol
			if (projectorInfo.model != null && !projectorInfo.isFraud) {
				return true;
			}
		}
		return false;
	}

    private synchronized static int[] getMaxResoulution(ProjectorInfo projector) {
        int[] res = new int[]{1920, 1080};
        try {
            InetAddress targetAddress = projector.getAddress();
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(1000);
            byte[] command = generateQueryCommand("android", "android");
            ByteBuffer packetBuffer = ByteBuffer.allocate(command.length + IMAGE_PACKET_HEADER_SIZE + EZ_DISPLAY_QUERY_HEADER_SIZE);
            ByteBuffer imagepacketHeader = createPacketHeaderForImagePackeyHeader();
            ByteBuffer queryHeader = createPacketHeaderForQueryStatus();
            packetBuffer.order(ByteOrder.LITTLE_ENDIAN);
            // command
            packetBuffer.put(command);
            packetBuffer.put(imagepacketHeader.array());
            packetBuffer.put(queryHeader.array());

            byte[] packetBytes = packetBuffer.array();
            clientSocket.send(new DatagramPacket(packetBytes, packetBytes.length, targetAddress, EZ_WIFI_DISPLAY_PORT_NUMBER));
            final byte[] recvBuf = new byte[1024];
            final DatagramPacket recvPacket2 = new DatagramPacket(recvBuf, recvBuf.length);
            clientSocket.receive(recvPacket2);

            final String[] receiveStrings = new String(recvPacket2.getData(), 0, recvPacket2.getLength(), Charset.forName("UTF-8")).split("\0");
            byte[] status_packet = new byte[24];
            System.arraycopy(recvBuf, receiveStrings[0].length() + 1, status_packet, 0, 24);
            ByteBuffer picoStatusBuffer = ByteBuffer.wrap(status_packet);
            picoStatusBuffer.order(ByteOrder.LITTLE_ENDIAN);
            picoStatusBuffer.position(8);
            res[0] = picoStatusBuffer.getInt();
            res[1] = picoStatusBuffer.getInt();
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    private static ByteBuffer createPacketHeaderForImagePackeyHeader() {
        ByteBuffer header = ByteBuffer.allocate(IMAGE_PACKET_HEADER_SIZE);
        header.order(ByteOrder.LITTLE_ENDIAN);
        int packetLength = 24;
        header.putShort((short) 1); // totalPacket
        header.putShort((short) 0); // curPacketIdx
        header.putInt(0);  // offset
        header.putInt(packetLength);
        header.putInt(0);

        return header;
    }

    private static ByteBuffer createPacketHeaderForQueryStatus() {
        ByteBuffer header = ByteBuffer.allocate(EZ_DISPLAY_QUERY_HEADER_SIZE);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(PICO_QUERY_CMD); //tag == 1;
        header.put((byte) 0); // flag = 0
        header.put((byte) 0);
        header.put((byte) 0); // reserve0
        header.put((byte) 0); // reserve1
        header.putInt(0);
        header.putInt(0);
        header.putInt(0);
        header.putInt(0);
        return header;
    }

	private static void processAllValue(HashMap<String, String> keyValuePairs, ProjectorInfo projectorInfo) {
		if (keyValuePairs.containsKey(PARAMETER_NAME_KEY)) {
			projectorInfo.name = keyValuePairs.get(PARAMETER_NAME_KEY);
		}
		projectorInfo.passcode = keyValuePairs.get("passcode");
		if (projectorInfo.model == null) { // Use EZRemote's parameter as first priority
			projectorInfo.model = keyValuePairs.get(PARAMETER_MODEL_KEY);
		}
		if (projectorInfo.vendor == null) { // Use EZRemote's parameter as first priority
			projectorInfo.vendor = keyValuePairs.get(PARAMETER_VENDOR_KEY);
		}
		if (keyValuePairs.containsKey(PARAMETER_DISCOVERY_KEY) && keyValuePairs.containsKey(PARAMETER_SERVICE_KEY)) {
			projectorInfo.service = Integer.parseInt(keyValuePairs.get(PARAMETER_SERVICE_KEY), 16);
		}
		projectorInfo.keyValuePairs.putAll(keyValuePairs);
	}
	private static boolean checkMd5(String[] parameters) {
		final HashMap<String, String> keyValuePairs = parseKeyValuePairs(parameters);
		if (keyValuePairs.containsKey(PARAMETER_MD5_KEY)) {
			final String responseStringWithoutMD5Pair = responseStringWithoutMD5Pair(parameters);
			Log.d(TAG, "responseStringWithoutMD5Pair:" + responseStringWithoutMD5Pair);
			Log.d(TAG, "md5:" + keyValuePairs.get(PARAMETER_MD5_KEY));
			Log.d(TAG, "md5:" + Utils.md5(responseStringWithoutMD5Pair+MD5_SECRET));
			if (Utils.md5(responseStringWithoutMD5Pair+MD5_SECRET).equalsIgnoreCase(keyValuePairs.get(PARAMETER_MD5_KEY))) {
				return true;
			}
		}
		return false;
	}
	private static String responseStringWithoutMD5Pair(String[] parameters) {
		final ArrayList<String> parametersWithoutMd5Pair = new ArrayList<String>();
		for (final String parameter : parameters) {
			if (!parameter.startsWith(PARAMETER_MD5_KEY+"=")) {
				parametersWithoutMd5Pair.add(parameter);
			}
		}
		
		return Utils.concatStringsWithSeparator(parametersWithoutMd5Pair, ":");
	}
	
}
