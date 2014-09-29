package com.actionsmicro.androidkit.ezcast.imp.bonjour;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jmdns.JmmDNS;
import javax.jmdns.NetworkTopologyEvent;
import javax.jmdns.NetworkTopologyListener;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.annotation.SuppressLint;
import android.net.wifi.WifiManager.MulticastLock;

import com.actionsmicro.androidkit.ezcast.DeviceFinder;
import com.actionsmicro.androidkit.ezcast.DeviceFinderBase;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.utils.Log;

public abstract class BonjourDeviceFinder<T extends BonjourDeviceInfo> extends DeviceFinderBase {

	private static final String TAG = "BonjourDeviceFinder";
	private List<T> devices = new ArrayList<T>();
	private boolean searching;
	private static JmmDNS bonjourBrowser = JmmDNS.Factory.getInstance();
	private final NetworkTopologyListener networkTopologyListener;
	private String serviceType;
	@SuppressLint("NewApi")
	private ServiceListener serviceListener = new ServiceListener() {
	
			@Override
			public void serviceAdded(ServiceEvent event) {
				Log.d(TAG, "Service added: "  + event.getInfo() + " " + event.getName() + " " + event.getInfo().getPropertyString("passcode"));
				event.getDNS().requestServiceInfo(event.getType(), event.getName(), true, 50);
			}
	
			@Override
			public void serviceRemoved(ServiceEvent event) {
				Log.d(TAG, "Service removed: " + event.getInfo());
				T deviceFound = getDeviceFromService(event.getInfo());
	            if (deviceFound != null) {
	            	removeDevice(deviceFound);
	                getDeviceFinderProxy().notifyListeneroOnDeviceRemoved(deviceFound);
	            }
			}
	
			@Override
			public void serviceResolved(ServiceEvent event) {
				ServiceInfo newService = event.getInfo();
				Log.d(TAG, "Service resolved: " + newService);
				if (newService.getInet4Address() != null) {
					T newDevice = createFromService(newService);
					addDevice(newDevice);
					getDeviceFinderProxy().notifyListeneroOnDeviceAdded(newDevice);
				}
			}
			
		};

	@Override
	public List<? extends DeviceInfo> getDevices() {
		return new CopyOnWriteArrayList<T>(devices);
	}

	protected abstract T createFromService(ServiceInfo newService);

	@Override
	public synchronized void stop() {
			if (searching) {
				if (multicastLock != null) {
					multicastLock.release();
					multicastLock = null;
				}
	//			mNsdManager.stopServiceDiscovery(mDiscoveryListener);
				bonjourBrowser.removeNetworkTopologyListener(networkTopologyListener);
				bonjourBrowser.removeServiceListener(serviceType+"local.", serviceListener);
				searching = false;
			}
		}

	private MulticastLock multicastLock;

	public BonjourDeviceFinder(DeviceFinder deviceFinderProxy, String serviceType) {
		super(deviceFinderProxy);
//		mNsdManager = (NsdManager)deviceFinderProxy.getContext().getSystemService(Context.NSD_SERVICE);
		this.serviceType = serviceType;
		networkTopologyListener = new NetworkTopologyListener() {

			@Override
			public void inetAddressAdded(NetworkTopologyEvent event) {
				bonjourBrowser.addServiceListener(BonjourDeviceFinder.this.serviceType+"local.", serviceListener);
			}

			@Override
			public void inetAddressRemoved(
					NetworkTopologyEvent event) {
				// TODO Auto-generated method stub
				
			}
		};

	}

	@Override
	public synchronized void search() {
			if (!searching) {
				searching = true;
				android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager)getDeviceFinderProxy().getContext().getSystemService(android.content.Context.WIFI_SERVICE);
				multicastLock = wifi.createMulticastLock("BonjourDeviceFinder");
				multicastLock.setReferenceCounted(true);
				multicastLock.acquire();
				synchronized (devices) {
					devices.clear();
				}
	//			mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
				new Thread(new Runnable() {
	
					@Override
					public void run() {					
						bonjourBrowser.addNetworkTopologyListener(networkTopologyListener);
						bonjourBrowser.addServiceListener(serviceType+"local.", serviceListener);
						Log.d(TAG, "addServiceListener");
					}
					
				}).start();
			} else {
				for (BonjourDeviceInfo device : devices) {
					getDeviceFinderProxy().notifyListeneroOnDeviceAdded(device);
				}			
			}
		}

	private T getDeviceFromService(ServiceInfo info) {
		T deviceFound = null;
		for (T device : devices) {
			if (device.getName().replace("\\032", " ").equals(info.getName().replace("\\032", " "))) {
				deviceFound = device;
				break;
			}
		}
		return deviceFound;
	}
//	private AndroidRxInfo getDeviceFromService(NsdServiceInfo service) {
//	AndroidRxInfo deviceFound = null;
//	for (AndroidRxInfo device : devices) {
//		if (device.getName().replace("\\032", " ").equals(service.getServiceName().replace("\\032", " "))) {
//			deviceFound = device;
//			break;
//		}
//	}
//	return deviceFound;
// }

	private void addDevice(T newDevice) {
		synchronized (devices) {
			if (!devices.contains(newDevice)) {
				devices.add(newDevice);
			}
		}
	}

	private synchronized void removeDevice(T deviceFound) {
		synchronized (devices) {
			devices.remove(deviceFound);
		}
	}

}