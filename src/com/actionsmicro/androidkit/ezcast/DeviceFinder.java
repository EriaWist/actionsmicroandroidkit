package com.actionsmicro.androidkit.ezcast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
/**
 * DeviceFinder provides interfaces to discovery devices.
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.1
*/
public class DeviceFinder {
	
//	private static DeviceFinder defaultFinder;
	private Context context;
	/**
	 * Create a DeviceFinder. Internal use only, please use {@link #getDefaultFinder(Context)} instead.
	 * @param context Android Context object.
	 * @since 2.1
	 */
	public DeviceFinder(Context context) {		
		this.context = context;
	}
	protected void addDeviceFinderImp(DeviceFinderBase deviceFinderImp) {
		imps.add(deviceFinderImp);
	}
//	/**
//	 * Get default device finder.
//	 * @param context Android Context object.
//	 * @return The default device finder.
//	 * @since 2.1
//	 */
//	static public DeviceFinder getDefaultFinder(Context context) {
//		if (defaultFinder == null) {
//			defaultFinder = new DeviceFinder(context);
//			defaultFinder.addDeviceFinderImp(new FalconDeviceFinder(defaultFinder));
//		}
//		return defaultFinder;
//	}
	/**
	 * Interfaces to handle events of device discovery.
	 * @author James Chen
	 *
	 * @since 2.1
	 */
	public interface Listener {
		public void onDeviceAdded(DeviceFinder deviceFinder, DeviceInfo device);
		public void onDeviceRemoved(DeviceFinder deviceFinder, DeviceInfo device);		
	}
	private ArrayList<Listener> listeners = new ArrayList<Listener>();
	/**
	 * Add listener to the device finder.
	 * @param listener {@link Listener}
	 * @since 2.1
	 */
	public void addListener(Listener listener) {
		synchronized(listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}
	/**
	 * Remove listener from the device finder.
	 * @param listener {@link Listener}
	 * @since 2.1
	 */
	public void removeListener(Listener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	Handler mainHandler = new Handler(Looper.getMainLooper());
	/**
	 * Internal use only, you shouldn't call this method directly.
	 * @param device
	 */
	public void notifyListeneroOnDeviceAdded(final DeviceInfo device) {
		mainHandler.post(new Runnable() {

			@Override
			public void run() {
				synchronized(listeners) {
					Iterator<Listener> iterator = listeners.listIterator();
					while (iterator.hasNext()) {
						Listener listener = iterator.next(); 
						listener.onDeviceAdded(DeviceFinder.this, device);						
					}
				}
			}
			
		});
		
	}
	/**
	 * Internal use only, you shouldn't call this method directly.
	 * @param device
	 */
	public void notifyListeneroOnDeviceRemoved(final DeviceInfo device) {
		mainHandler.post(new Runnable() {

			@Override
			public void run() {
				synchronized(listeners) {
					Iterator<Listener> iterator = listeners.listIterator();
					while (iterator.hasNext()) {
						Listener listener = iterator.next(); 
						listener.onDeviceRemoved(DeviceFinder.this, device);						
					}
				}
			}			
		});			
	}
	private ArrayList<DeviceFinderBase> imps = new ArrayList<DeviceFinderBase>();
	/**
	 * Get those devices which have neen discovered by the device finder.
	 * @return A list of DeviceInfo which represents devices currently found.
	 * @since 2.1
	 */
	public List<DeviceInfo> getDevices() {
		ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();
		for (DeviceFinderBase deviceFinderImp : imps) {
			devices.addAll(deviceFinderImp.getDevices());
		}
		return devices;
		
	}
	/**
	 * Stop the device finder to discover devices.
	 * @since 2.1
	 */
	public void stop() {
		for (DeviceFinderBase deviceFinderImp : imps) {
			deviceFinderImp.stop();
		}
	}
	/**
	 * Force device finder to search devices.
	 * @since 2.1
	 */
	public void search() {
		for (DeviceFinderBase deviceFinderImp : imps) {
			deviceFinderImp.search();
		}
	}
	/**
	 * Internal use only, you shouldn't call this method directly.
	 * @return Android Context object.
	 */
	public Context getContext() {
		return context;
	}
}
