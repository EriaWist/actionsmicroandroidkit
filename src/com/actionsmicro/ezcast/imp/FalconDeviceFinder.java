package com.actionsmicro.ezcast.imp;

import java.util.ArrayList;
import java.util.List;

import com.actionsmicro.ezcast.DeviceFinder;
import com.actionsmicro.ezcast.DeviceFinderBase;
import com.actionsmicro.ezcast.DeviceInfo;
import com.actionsmicro.falcon.Falcon;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.falcon.Falcon.SearchReultListener;

public class FalconDeviceFinder extends DeviceFinderBase {

	public FalconDeviceFinder(DeviceFinder deviceFinderProxy) {
		super(deviceFinderProxy);
		Falcon.getInstance().addSearchResultListener(new SearchReultListener() {

			@Override
			public void falconSearchDidFindProjector(Falcon falcon,
					ProjectorInfo projectorInfo) {
				getDeviceFinderProxy().notifyListeneroOnDeviceAdded(new FalconDeviceInfo(projectorInfo));
			}
			
		});
	}

	@Override
	public List<DeviceInfo> getDevices() {
		ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();
		for (ProjectorInfo projectorInfo : Falcon.getInstance().getProjectors()) {
			devices.add(new FalconDeviceInfo(projectorInfo));
		}
		return devices;
	}

	@Override
	public void stop() {
		Falcon.getInstance().stop();
	}

	@Override
	public void search() {
		Falcon.getInstance().search();
	}

}
