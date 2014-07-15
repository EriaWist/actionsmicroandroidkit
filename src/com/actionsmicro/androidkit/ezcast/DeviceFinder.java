package com.actionsmicro.androidkit.ezcast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class DeviceFinder {
	
	private static DeviceFinder singleton;
	private Context context;
	public DeviceFinder(Context context) {		
		singleton = this;
		this.context = context;
	}
	public void addDeviceFinderImp(DeviceFinderBase deviceFinderImp) {
		imps.add(deviceFinderImp);
	}
	static public DeviceFinder getInstance(Context context) {
		if (singleton == null) {
			singleton = new DeviceFinder(context);
		}
		return singleton;
	}
	
	public interface Listener {
		public void onDeviceAdded(DeviceFinder deviceFinder, DeviceInfo device);
		public void onDeviceRemoved(DeviceFinder deviceFinder, DeviceInfo device);		
	}
	private ArrayList<Listener> listeners = new ArrayList<Listener>();
	public void addListener(Listener listener) {
		synchronized(listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}
	public void removeListener(Listener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	Handler mainHandler = new Handler(Looper.getMainLooper());
	public void notifyListeneroOnDeviceAdded(final DeviceInfo projector) {
		mainHandler.post(new Runnable() {

			@Override
			public void run() {
				synchronized(listeners) {
					Iterator<Listener> iterator = listeners.listIterator();
					while (iterator.hasNext()) {
						Listener listener = iterator.next(); 
						listener.onDeviceAdded(DeviceFinder.this, projector);						
					}
				}
			}
			
		});
		
	}
	public void notifyListeneroOnDeviceRemoved(final DeviceInfo projector) {
		mainHandler.post(new Runnable() {

			@Override
			public void run() {
				synchronized(listeners) {
					Iterator<Listener> iterator = listeners.listIterator();
					while (iterator.hasNext()) {
						Listener listener = iterator.next(); 
						listener.onDeviceRemoved(DeviceFinder.this, projector);						
					}
				}
			}			
		});			
	}
	private ArrayList<DeviceFinderBase> imps = new ArrayList<DeviceFinderBase>();
	
	public List<DeviceInfo> getDevices() {
		ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();
		for (DeviceFinderBase deviceFinderImp : imps) {
			devices.addAll(deviceFinderImp.getDevices());
		}
		return devices;
		
	}
	public void stop() {
		for (DeviceFinderBase deviceFinderImp : imps) {
			deviceFinderImp.stop();
		}
	}
	public void search() {
		for (DeviceFinderBase deviceFinderImp : imps) {
			deviceFinderImp.search();
		}
	}
	public Context getContext() {
		return context;
	}
}
