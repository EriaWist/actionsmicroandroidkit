package com.actionsmicro.falcon;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

import com.actionsmicro.falcon.Falcon.ProjectorInfo.MessageListener;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;

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
	/**
	 * This contains basic information about the device.
	 * @author James Chen
	 *
	 */
	public static class ProjectorInfo implements Parcelable, Comparable<ProjectorInfo> {
		private static final int SERVICE_WIFI_LAN_DISPLAY 	= 0x01 << 0;
		private static final int SERVICE_MEDIA_STREAMING 	= 0x01 << 1;
		private static final int SERVICE_APP_PHOTO_VIEWER 	= 0x01 << 2;
		private static final int SERVICE_APP_LIVE_CAM 		= 0x01 << 3;
		private static final int SERVICE_APP_STREAMIG_DOC 	= 0x01 << 4;
		private static final int SERVICE_SPLIT_SCREEN 		= 0x01 << 5;
		private static final int SERVICE_APP_DROPBOX 		= 0x01 << 6;
		private static final int SERVICE_APP_WEB_VIEWER 	= 0x01 << 7;
		private static final int SERVICE_APP_QUALITY_MODE 	= 0x01 << 8;
		private static final int SERVICE_APP_HTTP_STREAMING = 0x01 << 9;
		private static final int SERVICE_APP_REMOTE_CONTROL = 0x01 << 10;
		private static final int SERVICE_MEDIA_STREAM_AUDIO = 0x01 << 11;	

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
		}
		protected ProjectorInfo() {
			
		}
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
			sendKey(1, keyCode);
		}
		public void sendVendorKey(final int keyCode) {
			sendKey(10, keyCode);
		}
		private void sendKey(final int commandCode, final int keyCode) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						final byte[] data = (""+commandCode+":"+keyCode).getBytes();
						final DatagramSocket udpsocket = new DatagramSocket();
						final DatagramPacket packet = new DatagramPacket(data, data.length,ipAddress, remoteControlPortNumber);
						udpsocket.send(packet);
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
			}).start();
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
	}
	private ArrayList<SearchReultListener> listeners = new ArrayList<SearchReultListener>();
	private ArrayList<ProjectorInfo> projectors = new ArrayList<ProjectorInfo>();
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
	private void removeMessageListener(ProjectorInfo projectorInfo, MessageListener listener) {
		final Set<MessageListener> listeners = messageListeners.get(projectorInfo.getAddress());
		if (listeners != null) {
			listeners.remove(listener);
		}
	}
	private void dispatchMessage(ProjectorInfo projectorInfo, String message) {
		if (message != null) {
			Log.d(TAG, "dispatchMessage:" + message);
			final Set<MessageListener> listeners = messageListeners.get(projectorInfo.getAddress());
			if (listeners != null) {
				for (final MessageListener listener : listeners) {
					Log.d(TAG, "send to " + listener);
					listener.onReceiveMessage(projectorInfo, message);
				}
			} else {
				Log.d(TAG, "no listener for " + projectorInfo.getAddress().getHostAddress());				
			}
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
		listeners.add(listener);
	}
	/**
	 * Unregister a {@link SearchReultListener} from Falcon.
	 * @param listener The {@link SearchReultListener} to be removed.
	 * @see Falcon#addSearchResultListener(SearchReultListener)
	 */
	public void removeSearchResultListener(SearchReultListener listener) {
		listeners.remove(listener);
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
	private static final int INITIAL_LOOKUP_INTERVAL = 2; // in seconds
	private int lookupInterval = INITIAL_LOOKUP_INTERVAL;
	protected Runnable pendingLookup;
	protected boolean shouldStopReceiving;
	/**
	 * Start searching devices. If it's already in searching state, it does nothing.
	 */
	public void search() {
		startListening();
		if (broadcastSocket != null) {
			Log.d(TAG, "Clear projector list");
			projectors.clear();
			searching = true;
			lookupInterval = INITIAL_LOOKUP_INTERVAL;
			sendLookupCommand();
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
		searching = false;
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
		}
	}
	private ProjectorInfo getProjectorInfoWithAddress(InetAddress address) {
		Iterator<ProjectorInfo> iterator = projectors.listIterator();
		while (iterator.hasNext()) {
			ProjectorInfo projector = iterator.next(); 
			if (projector.ipAddress.equals(address)) {
				return projector;
			}
		}
		return null;
	}
	private static final int IPMSG_VERSION = 0x002;
	
	private static final int IPMSG_NOOPERATION 	= 0x0000;
	private static final int IPMSG_BR_ENTRY		= 0x0001;
	private static final int IPMSG_BR_EXIT 		= 0x0002;
	private static final int IPMSG_ANSENTRY 	= 0x0003;
	private static final int MAX_LOOKUP_INTERVAL = 60;
	
	private static long s_commandSequenceNumber = 0;
	private static final byte[] generateCommand(String username, String hostname, int command) {
		String commandInString = IPMSG_VERSION+":"+s_commandSequenceNumber+":"+username+":"+hostname+":"+command+":"+"\0"+"\nUN:"+username+"\nHN:"+hostname+"\nNN:"+username+"\nGN:";
		s_commandSequenceNumber++;
		return commandInString.getBytes();
	}
	private static final byte[] generateEntryCommand(String username, String hostname) {
		return generateCommand(username, hostname, IPMSG_BR_ENTRY);
	}
	private static final byte[] generateExitCommand(String username, String hostname) {
		return generateCommand(username, hostname, IPMSG_BR_EXIT);
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
								byte[] command = {0,':',0};
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
		Iterator<SearchReultListener> iterator = listeners.listIterator();
		while (iterator.hasNext()) {
			SearchReultListener listener = iterator.next(); 
			listener.falconSearchDidFindProjector(Falcon.this, projector);						
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
		projectorInfo = getProjectorInfoWithAddress(address);
		if (projectorInfo == null) {
			projectorInfo = new ProjectorInfo();
			projectorInfo.ipAddress = address;
			projectors.add(projectorInfo);
		}
		return projectorInfo;
	}
	private void handleRemoteControlMessage(final DatagramPacket recvPacket) {
		final ProjectorInfo projectorInfo = createProjectorWithAddressIfNeeded(recvPacket.getAddress());
		projectorInfo.remoteControlPortNumber = EZ_REMOTE_CONTROL_PORT_NUMBER;
		final String [] receiveStrings = new String(recvPacket.getData(), 0, recvPacket.getLength(), Charset.forName("UTF-8")).split("\0");
		if (receiveStrings.length > 0) {
			final String receiveString = receiveStrings[0];
			Log.d(TAG, "receive EZ Remote message:" + receiveString + " from:" + recvPacket.getAddress().getHostAddress() + ":" + recvPacket.getPort());
			if (receiveString.startsWith(recvPacket.getAddress().getHostAddress())) { //backward compatibility
				mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
			} else if (receiveString.startsWith("EZREMOTE:")) {
				parseRemoteControlResponseString(receiveString, projectorInfo);
				mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
			} else if (receiveString.startsWith("STANDARD:")) {	//小機送給App的message (公板用)
				mainThreadHandler.post(new Runnable() {

					@Override
					public void run() {
						dispatchPrivateMessage(projectorInfo, parseMessageString(receiveString));
					}
					
				});
				
			} else if (receiveString.startsWith("CUSTOMER")) {	//小機送給App的message (客戶案用)
				mainThreadHandler.post(new Runnable() {

					@Override
					public void run() {
						dispatchMessage(projectorInfo, parseMessageString(receiveString));
					}
					
				});
			}
		}
	}
	private void handleWifiDisplayMessage(final DatagramPacket recvPacket) {
		final ProjectorInfo projectorInfo = createProjectorWithAddressIfNeeded(recvPacket.getAddress());
		if (parseWifiDisplayResponse(recvPacket, projectorInfo)) {
			Log.d(TAG, "receive EZ Wifi Response");
			projectorInfo.wifiDisplayPortNumber = EZ_WIFI_DISPLAY_PORT_NUMBER;
			mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
		} else {
			projectors.remove(projectorInfo);
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
			if (keyValuePairs.containsKey(PARAMETER_DISCOVERY_KEY)) {
				projectorInfo.discoveryVersion = Integer.valueOf(keyValuePairs.get(PARAMETER_DISCOVERY_KEY));
				if (checkMd5(parameters)) {
					processAllValue(parameters, projectorInfo);
				} else {
					Log.i(TAG, "is fraud!!!!==>" + receiveString);
					projectorInfo.isFraud = true;
				}
			} else {
				processAllValue(parameters, projectorInfo);
			}
			// Add sanity check to filter out other products which use same protocol
			if (projectorInfo.model != null && !projectorInfo.isFraud) {
				return true;
			}
		}
		return false;
	}
	private static void processAllValue(String[] parameters, ProjectorInfo projectorInfo) {
		final HashMap<String, String> keyValuePairs = parseKeyValuePairs(parameters);
		if (keyValuePairs.containsKey(PARAMETER_NAME_KEY)) {
			projectorInfo.name = keyValuePairs.get(PARAMETER_NAME_KEY);
		}
		projectorInfo.passcode = keyValuePairs.get("passcode");
		projectorInfo.model = keyValuePairs.get(PARAMETER_MODEL_KEY);
		projectorInfo.vendor = keyValuePairs.get(PARAMETER_VENDOR_KEY);
		if (keyValuePairs.containsKey(PARAMETER_DISCOVERY_KEY) && keyValuePairs.containsKey(PARAMETER_SERVICE_KEY)) {
			projectorInfo.service = Integer.parseInt(keyValuePairs.get(PARAMETER_SERVICE_KEY), 16);
		}
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
