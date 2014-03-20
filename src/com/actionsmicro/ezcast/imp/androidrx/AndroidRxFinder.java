package com.actionsmicro.ezcast.imp.androidrx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;

import com.actionsmicro.ezcast.DeviceFinder;
import com.actionsmicro.ezcast.DeviceFinderBase;
import com.actionsmicro.ezcast.DeviceInfo;
import com.actionsmicro.utils.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AndroidRxFinder extends DeviceFinderBase {

	private static final String TAG = "AndroidRxFinder";
	public static final String SERVICE_TYPE = "_androidrx-actions-micro._tcp.";
	private NsdManager mNsdManager;
	private List<AndroidRxInfo> devices = new ArrayList<AndroidRxInfo>();
	private ResolveListener mResolveListener = new NsdManager.ResolveListener() {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails.  Use the error code to debug.
            Log.e(TAG, "Resolve failed:" + errorCode+"; "+serviceInfo);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Resolve Succeeded. " + serviceInfo);
            AndroidRxInfo newDevice = new AndroidRxInfo(serviceInfo);
            addDevice(newDevice);
			getDeviceFinderProxy().notifyListeneroOnDeviceAdded(newDevice);
        }
    };
	private DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        //  Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Service discovery started");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found!  Do something with it.
            Log.d(TAG, "Service discovery success:" + service);
            if (!service.getServiceType().equals(SERVICE_TYPE)) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
            } else {
            	mNsdManager.resolveService(service, mResolveListener );
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.d(TAG, "service lost:" + service);
            AndroidRxInfo deviceFound = getDeviceFromService(service);
            if (deviceFound != null) {
            	removeDevice(deviceFound);
                getDeviceFinderProxy().notifyListeneroOnDeviceRemoved(deviceFound);
            }
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.d(TAG, "Discovery stopped: " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "onStartDiscoveryFailed failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "onStopDiscoveryFailed failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }
    };
	private boolean searching;;

	public AndroidRxFinder(DeviceFinder deviceFinderProxy) {
		super(deviceFinderProxy);
		mNsdManager = (NsdManager)deviceFinderProxy.getContext().getSystemService(Context.NSD_SERVICE);

	}

	@Override
	public List<? extends DeviceInfo> getDevices() {
		return new CopyOnWriteArrayList<AndroidRxInfo>(devices);
	}

	@Override
	public synchronized void stop() {
		if (searching) {
			mNsdManager.stopServiceDiscovery(mDiscoveryListener);
			searching = false;
		}
	}

	@Override
	public synchronized void search() {
		if (!searching) {
			searching = true;
			synchronized (devices) {
				devices.clear();
			}
			mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
		} else {
			for (AndroidRxInfo device : devices) {
				getDeviceFinderProxy().notifyListeneroOnDeviceAdded(device);
			}			
		}
	}

	private void addDevice(AndroidRxInfo newDevice) {
		synchronized (devices) {
			if (!devices.contains(newDevice)) {
				devices.add(newDevice);
			}
		}
	}

	private synchronized void removeDevice(AndroidRxInfo deviceFound) {
		synchronized (devices) {
			devices.remove(deviceFound);
		}
	}

	private AndroidRxInfo getDeviceFromService(NsdServiceInfo service) {
		AndroidRxInfo deviceFound = null;
		for (AndroidRxInfo device : devices) {
			if (device.getServiceInfo().getServiceName().replace("\\032", " ").equals(service.getServiceName().replace("\\032", " "))) {
				deviceFound = device;
				break;
			}
		}
		return deviceFound;
	}

}
