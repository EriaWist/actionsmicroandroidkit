package com.actionsmicro.falcon;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Falcon {
	static private final String TAG = "Falcon";
	public class ProjectorInfo
	{
		public String osVerion;
		public String name;
		public InetAddress ipAddress;
		public int wifiDisplayPortNumber;
		public int remoteControlPortNumber;
		public String passcode;
		public String model;
		public final boolean hasNoPasscode() {
			return passcode == null || passcode.length() == 0;
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
			broadcastSocket = new DatagramSocket();
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
	public void search() {
		if (broadcastSocket != null && 
			!isSearching()) {
			projectors.clear();
			mainThreadHandler.obtainMessage(MSG_SearchDidStart).sendToTarget();
			searching = true;
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
						byte[] recvBuf = new byte[64];
						Log.d(TAG, "start receiveing");
						while (true) {
							final DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
							broadcastSocket.receive(recvPacket);
							ProjectorInfo projectorInfo = null;
							if (recvPacket.getPort() == EZ_REMOTE_CONTROL_PORT_NUMBER) {
								Log.d(TAG, "receive EZ Remote Response");
								projectorInfo = createProjectorWithAddressIfNeeded(recvPacket.getAddress());
								projectorInfo.remoteControlPortNumber = EZ_REMOTE_CONTROL_PORT_NUMBER;
								mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
							} else if (recvPacket.getPort() == EZ_WIFI_DISPLAY_PORT_NUMBER) {
								Log.d(TAG, "receive EZ Wifi Response");
								projectorInfo = createProjectorWithAddressIfNeeded(recvPacket.getAddress());
								projectorInfo.wifiDisplayPortNumber = EZ_WIFI_DISPLAY_PORT_NUMBER;
								parseReponse(recvPacket, projectorInfo);
								mainThreadHandler.obtainMessage(MSG_SearchDidFind, projectorInfo).sendToTarget();
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
	private static final int IPMSG_VERSION = 0x001;
	
	private static final int IPMSG_NOOPERATION 	= 0x0000;
	private static final int IPMSG_BR_ENTRY		= 0x0001;
	private static final int IPMSG_BR_EXIT 		= 0x0002;
//	private static final int IPMSG_ANSENTRY 	= 0x0003;
	
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
						final InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
						byte[] command = {0,':',0};
						// send EZ Remote Lookup
						broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_REMOTE_CONTROL_PORT_NUMBER));
						
						Log.d(TAG, "send entry command");	
						// copy the logic from CSocketEx for Windows.
						command = generateNoOperationCommand("android", "android");
						broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_WIFI_DISPLAY_PORT_NUMBER));
						Thread.sleep(1000);
						command = generateEntryCommand("android", "android");
						broadcastSocket.send(new DatagramPacket(command, command.length, broadcastAddress, EZ_WIFI_DISPLAY_PORT_NUMBER));
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
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
					broadcastSocket.send(new DatagramPacket(command, command.length, InetAddress.getByName("255.255.255.255"), EZ_WIFI_DISPLAY_PORT_NUMBER));
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
	private void parseReponse(final DatagramPacket recvPacket,
			ProjectorInfo projectorInfo) {
		String [] receiveStrings = new String(recvPacket.getData(), 0, recvPacket.getLength()).split("\0");
		//07-23 13:10:54.940: D/Falcon(31650): datagramSocket receive:1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744 from:/192.168.111.1
//	    Log.d(TAG, "datagramSocket receive:" + ((receiveStrings.length>0)?receiveStrings[0]:"null") + " from:" + recvPacket.getAddress());
		if (receiveStrings.length > 0) {
			String [] parameters = receiveStrings[0].split(":");
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i].startsWith("name=")) {
					projectorInfo.name = parameters[i].split("=")[1];
				} else if (parameters[i].startsWith("passcode=")) {
					projectorInfo.passcode = parameters[i].split("=")[1];
				} else if (parameters[i].startsWith("model=")) {
					projectorInfo.model = parameters[i].split("=")[1];
				}
			}
		}
	}
}
