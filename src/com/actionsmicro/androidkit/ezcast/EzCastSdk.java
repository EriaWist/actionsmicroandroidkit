package com.actionsmicro.androidkit.ezcast;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import com.actionsmicro.analytics.AppInfo;
import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.tracker.LogTracker;
import com.actionsmicro.androidkit.ezcast.imp.androidrx.AndroidRxFinder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.FalconDeviceFinder;
import com.actionsmicro.utils.Log;

public class EzCastSdk {
	private static final int LOCATION_TIMEOUT_MS = 3000;
	private static final String TAG = "EzCastSdk";
	private String appId;
	private String appSecret;
	private boolean isInitialized;
	private Context context;
	private DeviceFinder deviceFinder;
	private static EzCastSdk sharedEzCastSdk;
	private Tracker tracker;
	public interface EzCastSdkInitializationListener {

		void onInitialized(EzCastSdk ezCastSdk);
		
	}
	
	public EzCastSdk(Context context, String appId, String appSecret) {
		this.appId = appId;
		this.appSecret = appSecret;
		this.context = context;
		tracker = new LogTracker();
		if (sharedEzCastSdk == null) {
			sharedEzCastSdk = this;
		}
	}
	public static EzCastSdk getSharedSdk() {
		return sharedEzCastSdk;
	}
	public void init(EzCastSdkInitializationListener listener) {
		isInitialized = true;
		
		if (listener != null) {
			listener.onInitialized(EzCastSdk.this);
		}
		fetchLocationAndLogAppInfo();
	}
	private void fetchLocationAndLogAppInfo() {
		new Thread() {
			Location fetchedlocation;
			Timer timout = new Timer();
			
			@Override
			public void run() {
				Looper.prepare();
				boolean hasPermissionToGetLocation = true;
				String networkProvider = LocationManager.NETWORK_PROVIDER;

				LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
				try {
					Log.d(TAG, "requestSingleUpdate");
					locationManager.requestSingleUpdate(networkProvider, new LocationListener() {

						@Override
						public void onLocationChanged(Location location) {
							Log.d(TAG, "onLocationChanged");
							fetchedlocation = location;
							timout.cancel();
							Looper.myLooper().quit();
						}

						@Override
						public void onProviderDisabled(String provider) {
							Log.d(TAG, "onProviderDisabled");
						}

						@Override
						public void onProviderEnabled(String provider) {
							Log.d(TAG, "onProviderEnabled");

						}

						@Override
						public void onStatusChanged(String provider, int status,
								Bundle extras) {
							Log.d(TAG, "onStatusChanged:"+status);				
						}

					}, null);
				} catch (SecurityException e) {
					Log.d(TAG, e.getLocalizedMessage());
					hasPermissionToGetLocation = false;
				}
				if (hasPermissionToGetLocation) {
					fetchedlocation = locationManager.getLastKnownLocation(networkProvider);
					timout.schedule(new TimerTask() {

						@Override
						public void run() {
							if (Looper.myLooper() != null) {
								Looper.myLooper().quit();
							}
						}

					}, LOCATION_TIMEOUT_MS);
					Looper.loop();
				}
		        tracker.log(new AppInfo(context, fetchedlocation));
			}
		}.start();
		
	}
	
	protected String getAppId() {
		return appId;
	}

	protected String getAppSecret() {
		return appSecret;
	}
	
	public Context getContext() {
		return context;
	}
	public boolean isInitialized() {
		return isInitialized;
	}
	
	public DeviceFinder getDeviceFinder() {
		if (deviceFinder == null) {
			deviceFinder = new DeviceFinder(context);
			deviceFinder.addDeviceFinderImp(new FalconDeviceFinder(deviceFinder));
			deviceFinder.addDeviceFinderImp(new AndroidRxFinder(deviceFinder));
		}
		return deviceFinder;
	}
	private DeviceInfo currentDevice;
	private int connectionCount;
	protected synchronized void connectToDevice(DeviceInfo device) {
		if (currentDevice == null) {
			connectionCount = 1;
			currentDevice = device;
			logDeviceInfo(device);
		} else if (currentDevice.compareTo(device) == 0) {
			connectionCount ++;
		} else {
			Log.e(TAG, "Connect to another device while connecting to one device already. Probably leak!");
			connectionCount = 1;
			currentDevice = device;
			logDeviceInfo(device);
		}
	}
	private void logDeviceInfo(DeviceInfo device) {
		DeviceInfoBuilder<?> builder = DeviceInfoBuilder.getBuilderForDevice(context, device, appId);
		if (tracker != null) {
			tracker.log(builder.buildDeviceInfo());
		}
	}
	protected synchronized void disconnectFromDevice(DeviceInfo device) {
		if (currentDevice == null || currentDevice.compareTo(device) != 0) {
			Log.e(TAG, "Disconnect from unknown device!");			
		} else {
			connectionCount --;
			if (connectionCount == 0) {
				currentDevice = null;
			}
		}
	}
	protected Tracker getTracker() {
		return tracker;
	}
}
