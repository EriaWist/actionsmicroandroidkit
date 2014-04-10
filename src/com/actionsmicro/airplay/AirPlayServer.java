package com.actionsmicro.airplay;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import com.yutel.silver.Aika;
import com.yutel.silver.exception.AirplayException;
import com.yutel.silver.vo.Device;

import vavi.apps.shairport.RTSPResponder;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AirPlayServer {
	private static final String AIRPLAY_MODEL = "AppleTV3,1";
	private static final String AIRPLAYER_VERSION_STRING = "150.33";
	public interface AirPlayServerDelegate {

		void loadVideo(String url, float rate, float position);

		void stopVideo();

		int getVideoStatus();

		void seek(int position);

		void resumeVideo();

		int getVideoPosition();

		int getVideoDuration();

		void pauseVideo();
		
	}
	private boolean stopRaopThread;
	private Thread raopThread;
	private Context context;
	private InetAddress inetAddress;
	private String name;
	private Aika airplayService;
	private AirPlayServerDelegate delegate;
	protected JmDNS jmDNS;
	public AirPlayServer(Context context, InetAddress inetAddress, String name, AirPlayServerDelegate delegate) {
		this.context = context;
		this.inetAddress = inetAddress;
		this.name = name;
		this.delegate = delegate;
	}
	public void start() {
		initRaopService();
		initAirPlayInThread();
	}
	private void initAirPlayInThread() {
		Thread init = new Thread(new Runnable() {

			@Override
			public void run() {
				initAirplayService();				
			}
			
		});
		init.start();
		try {
			init.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void stop() {
		if (raopThread != null) {
			stopRaopThread = true;
			try {
				raopThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (airplayService != null) {
			airplayService.stop();
			airplayService = null;
		}
	}
	private void initAirplayService() {
		airplayService = Aika.create(inetAddress, 0, name);
		Device dev = new Device();
		dev.setDeviceid(getMacAddress());
		dev.setFeatures("0x100029ff");
		dev.setModel(AIRPLAY_MODEL);
		dev.setProtovers("1.0");
		dev.setSrcvers(AIRPLAYER_VERSION_STRING);
		airplayService.config(dev);
		airplayService.setConnectListener(new Aika.AikaConnectListener() {

			@Override
			public void video(String url, String rate, String pos)
					throws AirplayException {
				delegate.loadVideo(url, Float.valueOf(rate), Float.valueOf(pos));
			}

			@Override
			public void photo() throws AirplayException {
				// TODO Auto-generated method stub
				
			}
			
		});
		airplayService.setControlListener(new Aika.AikaControlListener() {
			
			@Override
			public void videoStop() throws AirplayException {
				delegate.stopVideo();
			}
			
			@Override
			public int videoStatus() throws AirplayException {
				return delegate.getVideoStatus();
			}
			
			@Override
			public void videoSeek(int position) throws AirplayException {
				delegate.seek(position);
			}
			
			@Override
			public void videoResume() throws AirplayException {
				delegate.resumeVideo();
			}
			
			@Override
			public int videoPostion() throws AirplayException {
				return delegate.getVideoPosition();
			}
			
			@Override
			public void videoPause() throws AirplayException {
				delegate.pauseVideo();
			}
			
			@Override
			public int videoDuration() throws AirplayException {
				return delegate.getVideoDuration();
			}
		});
		airplayService.start();
	}
	private String getMacAddress() {
		WifiManager wim= (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		String macAddresss = wim.getConnectionInfo().getMacAddress().toUpperCase();
		return macAddresss;
	}
	private static final int RAOP_PORTNUMBER = 47000;
	
	private void initRaopService() {
		stopRaopThread = false;
		raopThread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				ServerSocket servSock = null;
				try {
					servSock = new ServerSocket(RAOP_PORTNUMBER);
					servSock.setReuseAddress(true);
					servSock.setSoTimeout(1000);
					registerRaopService();
					byte[] hwAddr = getHardwareAdress();
					while (!stopRaopThread) {
						try {
							Socket socket = servSock.accept();
							Log.d("ShairPort", "got connection from " + socket.toString());
							new RTSPResponder(hwAddr, socket).start();
						} catch(SocketTimeoutException e) {
							// ignore
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			private byte[] getHardwareAdress() {
				byte[] hwAddr = null;
				
				InetAddress local;
				try {
					local = InetAddress.getLocalHost();
					NetworkInterface ni = NetworkInterface.getByInetAddress(local);
					if (ni != null) {
						WifiManager wim= (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
						String[] as = wim.getConnectionInfo().getMacAddress().split(":");
		                hwAddr = new byte[as.length];
		                int i = 0;
		                for (String a : as) {
		                    hwAddr[i++] = Integer.valueOf(a, 16).byteValue();
		                }
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					e.printStackTrace();
				}
				return hwAddr;
			}
		});
		raopThread.start();
	}
	public void sendEvent() {
		if (airplayService != null) {
			airplayService.sendEvent();
		}
	}
	private void registerRaopService() {
		try {
			jmDNS = JmDNS.create(inetAddress);					
			String macAddressWithoutCol = getMacAddress().replace(":", "");
			HashMap<String, String> txt = new HashMap<String, String>();					
			txt.put("txtvers", "1");
			txt.put("ch", "2");
			txt.put("cn", "0,1,2,3");
			txt.put("da", "true");
			txt.put("et", "0,3,5");
			txt.put("md", "0,1,2");
			txt.put("pw", "false");
			txt.put("sv", "false");
			txt.put("sr", "44100");
			txt.put("ss", "16");
			txt.put("tp", "UDP");
			txt.put("vn", "65537");
			txt.put("vs", AIRPLAYER_VERSION_STRING);
			txt.put("rmodel", "EZAir1,1");
			txt.put("am", AIRPLAY_MODEL);
			txt.put("sf", "0x4");
			ServiceInfo serviceInfo = ServiceInfo.create("_raop._tcp.local.", macAddressWithoutCol+"@"+name, RAOP_PORTNUMBER, 0, 0, txt);
			jmDNS.registerService(serviceInfo);
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
