package com.actionsmicro.androidkit.ezcast.imp.dlna;

import java.util.List;

import org.fourthline.cling.model.meta.RemoteDevice;

import com.actionsmicro.androidkit.ezcast.DeviceFinder;
import com.actionsmicro.androidkit.ezcast.DeviceFinderBase;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.dlna.UpnpService.DlnaDeviceListener;

public class DlnaDeviceFinder extends DeviceFinderBase {

	public DlnaDeviceFinder(DeviceFinder deviceFinderProxy) {
		super(deviceFinderProxy);
		
	}

	@Override
	public List<? extends DeviceInfo> getDevices() {
		return UpnpService.getUpnpService().getDevices();
	}
	private DlnaDeviceListener listener = new DlnaDeviceListener() {

		@Override
		public void onDeviceAdded(RemoteDevice device) {
			getDeviceFinderProxy().notifyListeneroOnDeviceAdded(new DlnaDeviceInfo(device));
			
		}

		@Override
		public void onDeviceRemoved(RemoteDevice device) {
			getDeviceFinderProxy().notifyListeneroOnDeviceRemoved(new DlnaDeviceInfo(device));
			
		}
		
	};
	@Override
	public synchronized void stop() {
		UpnpService.getUpnpService().removeListener(listener);
	}

	@Override
	public synchronized void search() {
		UpnpService.getUpnpService().addListener(listener);
		UpnpService.getUpnpService().search();
	}

}
