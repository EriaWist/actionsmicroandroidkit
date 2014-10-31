package com.actionsmicro.analytics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.airplay.AirPlayDeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.androidrx.AndroidRxInfo;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.googlecast.GoogleCastDeviceInfo;
import com.actionsmicro.utils.Device;
import com.actionsmicro.utils.Log;

@SuppressWarnings("unused")
public abstract class Usage extends Record {
	private transient Tracker tracker;
	private transient Context context;
	private String timestamp;
	private String package_id;
	private String app_id;
	private String device_type;
	private String device_id;
	private long play_time;
	private String app_os_type;
	private String firmware_version;
	private transient DeviceInfo device;
	private transient Date beginTime;
	public Usage(Tracker tracker, Context context, String appId, String packageId, DeviceInfo device, String recordType, String schemaVersion) {
		super(recordType, schemaVersion);
		fill(tracker, context, appId, packageId, device);		
	}
	private void fill(Tracker tracker, Context context, String appId,
			String packageId, DeviceInfo device) {
		this.tracker = tracker;
		this.context = context;
		this.app_id = appId;
		this.package_id = packageId;
		this.device_type = getDeviceTypeFromDevice(device);
		this.device_id = device.getParameter("deviceid"); //TODO: Consider to create a method to DeviceInfo
		this.app_os_type = "android";
		this.firmware_version = device.getParameter("srcvers"); //TODO: Consider to create a method to DeviceInfo
		this.device = device;
	}
	public Usage(Tracker tracker, Context context, DeviceInfo device, String recordType, String schemaVersion) {
		super(recordType, schemaVersion);
		fill(tracker, context, Device.getAppMacAddress(context), context.getPackageName(), device);
	}
	public static String getDeviceTypeFromDevice(DeviceInfo device) {
		if (device instanceof PigeonDeviceInfo) {
			return "ezcast";
		} else if (device instanceof AirPlayDeviceInfo) {
			return "airplay";
		} else if (device instanceof AndroidRxInfo) {
			return "ezscreen";
		} else if (device instanceof GoogleCastDeviceInfo) {
			return "chromecast";
		}
		return "unknown";
	}
	protected Tracker getTracker() {
		return tracker;
	}
	protected Context getContext() {
		return context;
	}
	public DeviceInfo getDevice() {
		return device;
	}
	
	private static final String TAG = "Usage";
	public Usage begin() {
		if (beginTime != null) {
			throw new IllegalStateException("begin is not more than once!");
		}
		beginTime = new Date();
		timestamp = ISO_8601_DATE_TIME_FORMAT.format(beginTime);
		Log.d(TAG, "Begin Usage:"+timestamp);
		return this;
	}
	
	public void commit() {
		Date now = new Date();
		if (beginTime == null) {
			throw new IllegalStateException("begin is not called!");
		}
		play_time = (now.getTime() - beginTime.getTime()) / 1000;
		Log.d(TAG, "Commit Usage from:"+beginTime+"("+beginTime.getTime()+")"+" to " + now +"("+beginTime.getTime()+")"+". play_time="+play_time);
		
		tracker.log(this);
	}
}
