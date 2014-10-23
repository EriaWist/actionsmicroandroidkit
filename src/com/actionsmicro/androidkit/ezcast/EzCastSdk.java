package com.actionsmicro.androidkit.ezcast;

import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.FalconDeviceFinder;

import android.content.Context;

public class EzCastSdk {
	private String appId;
	private String appSecret;
	private boolean isInitialized;
	private Context context;
	private DeviceFinder deviceFinder;
	private static EzCastSdk sharedEzCastSdk;
	public interface EzCastSdkInitializationListener {

		void onInitialized(EzCastSdk ezCastSdk);
		
	}
	
	public EzCastSdk(Context context, String appId, String appSecret) {
		this.appId = appId;
		this.appSecret = appSecret;
		this.context = context;
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
		}
		return deviceFinder;
	}
}
