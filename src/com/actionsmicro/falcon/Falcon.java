package com.actionsmicro.falcon;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;

public class Falcon {
	
	private static final String PARAMETER_VENDOR_KEY = "vendor";
	private static final String PARAMETER_MODEL_KEY = "model";
	private static final String MD5_SECRET = ":secret=82280189";
	private static final String PARAMETER_MD5_KEY = "md5";
	private static final String PARAMETER_SERVICE_KEY = "service";
	private static final String PARAMETER_NAME_KEY = "name";
	private static final String PARAMETER_DISCOVERY_KEY = "discovery";
	static private final String TAG = "Falcon";
	public static class ProjectorInfo implements Parcelable, Comparable<ProjectorInfo>
	{
		private static final int SERVICE_WIFI_LAN_DISPLAY 	= 0x01 << 0;
		private static final int SERVICE_MEDIA_STREAMING 	= 0x01 << 1;
		private static final int SERVICE_APP_PHOTO_VIEWER 	= 0x01 << 2;
		private static final int SERVICE_APP_LIVE_CAM 		= 0x01 << 3;
		private static final int SERVICE_APP_STREAMIG_DOC 	= 0x01 << 4;
		private static final int SERVICE_SPLIT_SCREEN 		= 0x01 << 5;
		private static final int SERVICE_APP_DROPBOX 		= 0x01 << 6;
		private static final int SERVICE_APP_WEB_VIEWER 	= 0x01 << 7;

		
		public String osVerion;
		public String name;
		public InetAddress ipAddress;
		public int wifiDisplayPortNumber;
		public int remoteControlPortNumber;
		public String passcode;
		public String model;
		public int service = SERVICE_WIFI_LAN_DISPLAY | SERVICE_MEDIA_STREAMING | SERVICE_APP_PHOTO_VIEWER | SERVICE_APP_LIVE_CAM | SERVICE_APP_STREAMIG_DOC | SERVICE_SPLIT_SCREEN | SERVICE_APP_DROPBOX | SERVICE_APP_WEB_VIEWER;
		public String vendor;
		public boolean isFraud;
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
		}
		public ProjectorInfo() {
			
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
	    }
		public boolean supportsMediaStreaming() {
			return (service & SERVICE_MEDIA_STREAMING) == SERVICE_MEDIA_STREAMING;
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
			
			return result;
		}
		@Override
		public int compareTo(ProjectorInfo another) {
			return ipAddress.getHostAddress().compareTo(another.ipAddress.getHostAddress());
		}
	}
	private ArrayList<SearchReultListener> listeners = new ArrayList<SearchReultListener>();
	private ArrayList<ProjectorInfo> projectors = new ArrayList<ProjectorInfo>();
	private static Falcon singleton;
	static public Falcon getInstance() {
		if (singleton == null) {
			singleton = new Falcon();
		}
		return singleton;
	}
	private Falcon() {
		mainThreadHandler = new Handler() {
			@Override
			public void handleMessage (Message msg) {
				switch (msg.what) {
				case MSG_SearchDidStart:
					notifyListenerWillStart(); 
					break;					
				case MSG_SearchDidFind:
					notifyListenerDidFind((ProjectorInfo) msg.obj);
					break;
				case MSG_SearchDidEnd:
					notifyListenerSearchDidEnd();
					searching = false;
					break;
				}
			}
		};
		
		try {
			try {
				broadcastSocket = new DatagramSocket(EZ_WIFI_DISPLAY_PORT_NUMBER);
				broadcastSocket.setReuseAddress(true);
			} catch (SocketException e) {
				e.printStackTrace();
				broadcastSocket = new DatagramSocket();
			}
			broadcastSocket.setBroadcast(true);
			waitFeedbackInBackground();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	public ArrayList<ProjectorInfo> getProjectors() {
		return projectors;
	}
	public void addSearchResultListener(SearchReultListener listener) {
		listeners.add(listener);
	}
	public void removeSearchResultListener(SearchReultListener listener) {
		listeners.remove(listener);
	}
	static public final int EZ_REMOTE_CONTROL_PORT_NUMBER = 63630;
	static public final int EZ_WIFI_DISPLAY_PORT_NUMBER = 2425;
	static public int getBroadcastAddress(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
		return dhcpInfo.serverAddress;
	}
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
	static public String convertAddressToString(int address) {
		return String.valueOf(String.format("%d.%d.%d.%d", (address & 0xff), (address >> 8 & 0xff), (address >> 16 & 0xff), (address >> 24 | 0xff)));
	}
	static public InetAddress convertAddressInetAddress(int address) throws UnknownHostException {
		return InetAddress.getByName(convertAddressToString(address));
	}
//	static private final String lookupCommand = "1:0:am:amhost:1\0"+"\nUN:am\nHN:amhost\nNN:am\nGN:";
	
	public interface SearchReultListener {
		public void falconSearchWillStart(Falcon falcon);
		public void falconSearchDidFindProjector(Falcon falcon, ProjectorInfo projectorInfo);
		public void falconSearchDidEnd(Falcon falcon);
	}
	private Handler mainThreadHandler;
	private static final int MSG_SearchDidStart	= 0;
	private static final int MSG_SearchDidFind	= 1;
	private static final int MSG_SearchDidEnd	= 2;
	private DatagramSocket broadcastSocket;
	private boolean searching;
	private static final int INITIAL_LOOKUP_INTERVAL = 2; // in seconds
	private int lookupInterval = INITIAL_LOOKUP_INTERVAL;
	protected Runnable pendingLookup;
	public void search() {
		if (broadcastSocket != null && 
			!isSearching()) {
			projectors.clear();
			mainThreadHandler.obtainMessage(MSG_SearchDidStart).sendToTarget();
			searching = true;
			lookupInterval = INITIAL_LOOKUP_INTERVAL;
			sendLookupCommand();	
			mainThreadHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mainThreadHandler.obtainMessage(MSG_SearchDidEnd).sendToTarget();					
				}
				
			}, 3000);
		}		
	}
	public boolean isSearching() {
		return searching;
	}
	//TODO add stop method
	//TODO change to singleton
	private void waitFeedbackInBackground() {
		Thread receivingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				if (broadcastSocket != null) {
					try {
						final byte[] recvBuf = new byte[512];
						Log.d(TAG, "start receiveing");
						while (true) {
							final DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
							broadcastSocket.receive(recvPacket);
							if (recvPacket.getPort() == EZ_REMOTE_CONTROL_PORT_NUMBER) {
								Log.d(TAG, "receive EZ Remote Response");
								final ProjectorInfo projectorInfo = createProjectorWithAddressIfNeeded(recvPacket.getAddress());
								projectorInfo.remoteControlPortNumber = EZ_REMOTE_CONTROL_PORT_NUMBER;
								parseRemoteControlResponse(recvPacket, projectorInfo);
								mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
							} else if (recvPacket.getPort() == EZ_WIFI_DISPLAY_PORT_NUMBER) {
								final ProjectorInfo projectorInfo = createProjectorWithAddressIfNeeded(recvPacket.getAddress());
								if (parseWifiDisplayResponse(recvPacket, projectorInfo)) {
									Log.d(TAG, "receive EZ Wifi Response");
									projectorInfo.wifiDisplayPortNumber = EZ_WIFI_DISPLAY_PORT_NUMBER;
									mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
								} else {
									projectors.remove(projectorInfo);
								}
							} else {
								final String [] receiveStrings = new String(recvPacket.getData(), 0, recvPacket.getLength()).split("\0");
								Log.d(TAG, "datagramSocket receive:" + ((receiveStrings.length>0)?receiveStrings[0]:"null") + " from:" + recvPacket.getAddress());
							}
						}
					} catch (SocketTimeoutException e) {
						Log.d(TAG, "Search timeout");					
					} catch (SocketException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
					}
				}
			}
		});
		receivingThread.start();
	}
	private static void parseRemoteControlResponse(DatagramPacket recvPacket,
			ProjectorInfo projectorInfo) {
		final String [] receiveStrings = new String(recvPacket.getData(), 0, recvPacket.getLength(), Charset.forName("UTF-8")).split("\0");
		if (receiveStrings.length > 0) {
			final String receiveString = receiveStrings[0];
			parseRemoteControlResponseString(receiveString, projectorInfo);
		}
	}
	protected static void parseRemoteControlResponseString(final String receiveString, ProjectorInfo projectorInfo) {
		if (receiveString.startsWith("EZREMOTE")) {
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
	private static final byte[] generateNoOperationCommand(String username, String hostname) {
		return generateCommand(username, hostname, IPMSG_NOOPERATION);
	}
	private void sendLookupCommand() {
		Thread commandThread = new Thread(new Runnable() {
			@Override
			public void run() {
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
						Log.d(TAG, "Search timeout");					
					} catch (SocketException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
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
			
		});
		commandThread.start();
	}
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					broadcastSocket.close();
				}
			}
			
		});
		commandThread.start();
	}
	private void notifyListenerWillStart() {
		Iterator<SearchReultListener> iterator = listeners.listIterator();
		while (iterator.hasNext()) {
			SearchReultListener listener = iterator.next(); 
			listener.falconSearchWillStart(Falcon.this);
		}
	}
	private void notifyListenerDidFind(ProjectorInfo projector) {
		Iterator<SearchReultListener> iterator = listeners.listIterator();
		while (iterator.hasNext()) {
			SearchReultListener listener = iterator.next(); 
			listener.falconSearchDidFindProjector(Falcon.this, projector);						
		}
	}
	private void notifyListenerSearchDidEnd() {
		Iterator<SearchReultListener> iterator = listeners.listIterator();
		while (iterator.hasNext()) {
			SearchReultListener listener = iterator.next(); 
			listener.falconSearchDidEnd(Falcon.this);					
		}
	}
	private ProjectorInfo createProjectorWithAddressIfNeeded(
			final InetAddress address) {
		ProjectorInfo projectorInfo;
		projectorInfo = getProjectorInfoWithAddress(address);
		if (projectorInfo == null) {
			projectorInfo = new ProjectorInfo();
			projectorInfo.name =  new String("Projector");
			projectorInfo.ipAddress = address;
			projectors.add(projectorInfo);
		}
		return projectorInfo;
	}
	private static boolean parseWifiDisplayResponse(final DatagramPacket recvPacket, ProjectorInfo projectorInfo) {
		final String [] receiveStrings = new String(recvPacket.getData(), 0, recvPacket.getLength(), Charset.forName("UTF-8")).split("\0");
		if (receiveStrings.length > 0) {
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
		if (parameters[4].equals(String.valueOf(IPMSG_ANSENTRY))) {
			projectorInfo.osVerion = parameters[0];
			if (!parameters[3].equals("(none)")) {
				projectorInfo.name = parameters[3];
			}
			HashMap<String, String> keyValuePairs = parseKeyValuePairs(parameters);
			if (keyValuePairs.containsKey(PARAMETER_DISCOVERY_KEY)) {
				if (checkMd5(parameters)) {
					processAllValue(parameters, projectorInfo);
				} else {
					Log.i(TAG, "is fraud!!!!");
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
