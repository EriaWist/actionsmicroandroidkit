package com.actionsmicro.analytics;

import java.util.Locale;
import java.util.TimeZone;

import com.actionsmicro.utils.Device;
import com.actionsmicro.utils.PackageUtils;
import com.actionsmicro.utils.Screen;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
// POJO
@SuppressWarnings("unused")
public class AppInfo extends Record {
	private String app_id;
	private String package_id;
	private String app_version;
	private String sdk_version;
	private String os_type = "android";
	private String os_version;
	private String manufacturer;
	private String model;
	private class Resolution {
		int width;
		int height;
	}
	private Resolution resolution = new Resolution();
	private class LocationData {
		double latitude;
		double longitude;
	}
	private LocationData location = new LocationData();
	private String language;
	private String country;
	private String time_zone;
	public AppInfo(Context context, String appId, String appVersion, Point screenSize, Location location, String sdkVersion) {
		super("app", "2014-10-24");
		fill(context, appId, appVersion, screenSize, location, sdkVersion);
	}
	private void fill(Context context, String appId, String appVersion,
			Point screenSize, Location location, String sdkVersion) {
		package_id = context.getPackageName();
		app_id = appId;
		app_version = appVersion;
		sdk_version = sdkVersion;
		os_version = android.os.Build.VERSION.RELEASE;
		manufacturer = android.os.Build.MANUFACTURER;
		model = android.os.Build.MODEL;
		if (screenSize != null) {
			resolution.width = screenSize.x;
			resolution.height = screenSize.y;
		}
		if (location != null) {
			this.location.latitude = location.getLatitude();
			this.location.longitude = location.getLongitude();
		}
		language = Locale.getDefault().toString();
		country = Locale.getDefault().getCountry();
		time_zone = TimeZone.getDefault().getID();
	}
	public AppInfo(Context context, Location location, String sdkVersion)  {
		super("app", "2014-10-24");
		Point screenSize = new Point();
		Screen.getResolution(context, screenSize);
		fill(context, Device.getAppUniqueId(context), PackageUtils.getAppVersion(context), screenSize, location, sdkVersion);
	}
}
