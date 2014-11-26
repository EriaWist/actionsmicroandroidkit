package com.actionsmicro.androidkit.ezcast.imp.dlna;

import java.util.ArrayList;
import java.util.List;

import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.message.header.UDADeviceTypeHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.utils.Log;

class UpnpService {
	private static final String DEVICE_TYPE_MEDIA_RENDERER = "MediaRenderer";
	private static final String TAG = "UpnpService";
	
	public Device getDeviceById(String uid) {
		return upnpService.getControlPoint().getRegistry().getDevice(new UDN(uid), false); 
	}
	private List<DlnaDeviceListener> listeners = new ArrayList<DlnaDeviceListener>();
	
	private UpnpService() {
		upnpService = new UpnpServiceImpl(new AndroidUpnpServiceConfiguration(), new RegistryListener() {

			@Override
			public void afterShutdown() {
				Log.d(TAG, "afterShutdown");
			}

			@Override
			public void beforeShutdown(Registry arg0) {
				Log.d(TAG, "beforeShutdown");
				
			}

			@Override
			public void localDeviceAdded(Registry arg0, LocalDevice arg1) {
				Log.d(TAG, "localDeviceAdded:"+arg1);
				
			}

			@Override
			public void localDeviceRemoved(Registry arg0, LocalDevice arg1) {
				Log.d(TAG, "localDeviceRemoved:"+arg1);
				
			}

			@Override
			public void remoteDeviceAdded(Registry arg0, RemoteDevice device) {
				String type = device.getType().getType();
				Log.d(TAG, "remoteDeviceAdded: type:"+type+" :"+device);
				if (type.equals(DEVICE_TYPE_MEDIA_RENDERER)) {
					synchronized (listeners) {
						for (DlnaDeviceListener listener : listeners) {
							listener.onDeviceAdded(device);
						}
					}
				}
				
			}

			@Override
			public void remoteDeviceDiscoveryFailed(Registry arg0,
					RemoteDevice arg1, Exception arg2) {
				Log.d(TAG, "remoteDeviceDiscoveryFailed:"+arg1+", exp:"+arg2.getLocalizedMessage());
				
			}

			@Override
			public void remoteDeviceDiscoveryStarted(Registry arg0,
					RemoteDevice arg1) {
				Log.d(TAG, "remoteDeviceDiscoveryStarted:"+arg1);
				
			}

			@Override
			public void remoteDeviceRemoved(Registry arg0, RemoteDevice device) {
				String type = device.getType().getType();
				Log.d(TAG, "remoteDeviceRemoved: type:"+type+" :"+device);
				if (type.equals(DEVICE_TYPE_MEDIA_RENDERER)) {
					synchronized (listeners) {
						for (DlnaDeviceListener listener : listeners) {
							listener.onDeviceRemoved(device);
						}
					}
				}
			}

			@Override
			public void remoteDeviceUpdated(Registry arg0, RemoteDevice arg1) {
				Log.d(TAG, "remoteDeviceUpdated:"+arg1);

			}
			
		});
	}
	private UpnpServiceImpl upnpService;
	public void removeListener(DlnaDeviceListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}

	}
	public void addListener(DlnaDeviceListener listener) {
		synchronized (listeners) {

			if (!listeners.contains(listener)) {
				for (RemoteDevice device: upnpService.getControlPoint().getRegistry().getRemoteDevices()) {
					if (device.getType().getType().equals(DEVICE_TYPE_MEDIA_RENDERER)) {
						listener.onDeviceAdded(device);
					}
				}
				listeners.add(listener);
			}
		}
	}
	public interface DlnaDeviceListener {

		void onDeviceAdded(RemoteDevice device);

		void onDeviceRemoved(RemoteDevice device);
		
	}

	public void search() {
        upnpService.getControlPoint().search(new UDADeviceTypeHeader(new UDADeviceType(DEVICE_TYPE_MEDIA_RENDERER)));
	}
	private static UpnpService sharedUpnpService = new UpnpService();
	public static UpnpService getUpnpService() {
		return sharedUpnpService;
	}
	public List<? extends DeviceInfo> getDevices() {
		ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();
		for (RemoteDevice device: upnpService.getControlPoint().getRegistry().getRemoteDevices()) {
			devices.add(new DlnaDeviceInfo(device));
        }
        return devices;
	}
	public void execute(ActionCallback action) {
		upnpService.getControlPoint().execute(action);
	}
}
