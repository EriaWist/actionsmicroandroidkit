package com.actionsmicro.androidkit.ezcast.imp.androidp2prx;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.actionsmicro.androidkit.ezcast.DeviceFinder;
import com.actionsmicro.androidkit.ezcast.DeviceFinderBase;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.p2p.P2PWebApi;
import com.actionsmicro.p2p.P2PDeviceListener;
public class AndroidP2PRxDeviceFinder extends DeviceFinderBase {

	public AndroidP2PRxDeviceFinder(DeviceFinder deviceFinderProxy) {
		super(deviceFinderProxy);
	}
	
	private P2PDeviceListener listener = new P2PDeviceListener() {

		@Override
		public void onDeviceAdded(String deviceuuid) {
			getDeviceFinderProxy().notifyListeneroOnDeviceAdded(new AndroidP2PRxInfo(deviceuuid));
		}

		@Override
		public void onDeviceRemoved(String deviceuuid) {
			getDeviceFinderProxy().notifyListeneroOnDeviceRemoved(new AndroidP2PRxInfo(deviceuuid));
						
		}
		
	};
	
	@Override
	public List<DeviceInfo> getDevices() {
		ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();
		Vector<String> deviceuuids=P2PWebApi.getInstance().getDeviceUUIDs();
		for (int i=0;i<deviceuuids.size(); i++)
		{
			String uuid=(String) deviceuuids.get(i);
			devices.add(new AndroidP2PRxInfo(uuid));
		}
		return devices;
	}

	@Override
	public void stop() {
		//do nothing
	}

	@Override
	public void search() {
		P2PWebApi.getInstance().addListener(listener);
		P2PWebApi.getInstance().search();
	}

}
