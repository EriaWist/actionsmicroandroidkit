package com.actionsmicro.androidkit.ezcast;

import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.actionsmicro.BuildConfig;
import com.actionsmicro.analytics.AppInfo;
import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.tracker.ActionsTracker;
import com.actionsmicro.analytics.tracker.CompoundTracker;
import com.actionsmicro.analytics.tracker.HashUtils;
import com.actionsmicro.analytics.tracker.LogTracker;
import com.actionsmicro.androidkit.ezcast.imp.airplay.AirPlayDeviceFinder;
import com.actionsmicro.androidkit.ezcast.imp.androidrx.AndroidRxFinder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.FalconDeviceFinder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.FalconDeviceFinder.ProjectorInfoFilter;
import com.actionsmicro.androidkit.ezcast.imp.googlecast.GoogleCastFinder;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.utils.Device;
import com.actionsmicro.utils.Log;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;

public class EzCastSdk {
	private static final String PREF_KEY_SUPPORT_LIST = "support_list";
	private static final String PREF_NAME_EZCAST_SDK = "ezcastsdk";
	private static final int INITIALIZATION_TIMEOUT_MS = 3000;
	private static final int LOCATION_TIMEOUT_MS = 3000;
	private static final String TAG = "EzCastSdk";
	private String appKey;
	private String appSecret;
	private boolean isInitialized;
	private Context context;
	private DeviceFinder deviceFinder;
	private static EzCastSdk sharedEzCastSdk;
	private Tracker tracker;
	private Future<JSONObject> initTask;
	private boolean initializing;
	public interface InitializationListener {

		void onInitialized(EzCastSdk ezCastSdk);

		void onInitializationFailed(Exception exception);
		
	}
	
	public EzCastSdk(Context context, String appKey, String appSecret) {
		if (context == null) {
			throw new InvalidParameterException("Context should not be null!");
		}
		if (appKey == null || appKey.isEmpty()) {
			throw new InvalidParameterException("App key should not be empty!");
		}
		if (appSecret == null || appSecret.isEmpty()) {
			throw new InvalidParameterException("App secret should not be empty!");
		}
		this.appKey = appKey;
		this.appSecret = appSecret;
		this.context = context;
		this.deviceFinder = new DeviceFinder(context);
		CompoundTracker compoundTracker = new CompoundTracker();
		if (BuildConfig.DEBUG) {
			compoundTracker.add(new LogTracker());
		}
		compoundTracker.add(new ActionsTracker(context, appKey, appSecret));
		tracker = compoundTracker;
		if (sharedEzCastSdk == null) {
			sharedEzCastSdk = this;
		}
	}
	public static EzCastSdk getSharedSdk() {
		return sharedEzCastSdk;
	}
	private String computeHash(long expire) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return HashUtils.SHA1(appSecret+"\t"+String.valueOf(expire)+"\t"+"/cloud/sdk/api/support");
	}
	private Handler mainThreadHandler = new Handler(Looper.getMainLooper());
	private void doSetupDeviceFinder(final InitializationListener listener) {
		mainThreadHandler.post(new Runnable() {

			@Override
			public void run() {
				List<String> supportList = getSupportListFromStore();
				if (supportList.contains("chromecast")) {
					deviceFinder.addDeviceFinderImp(new GoogleCastFinder(deviceFinder));
				}
				if (supportList.contains("airplay")) {
					deviceFinder.addDeviceFinderImp(new AirPlayDeviceFinder(deviceFinder));
				}
				if (supportList.contains("ezscreen")) {
					deviceFinder.addDeviceFinderImp(new AndroidRxFinder(deviceFinder));
				}
				setupFinderForEzCastAndPro(supportList);
				finishUpInitialization(listener);
			}			
		});

	}
	private void setupFinderForEzCastAndPro(List<String> supportList) {
		FalconDeviceFinder falconDeviceFinder = null;
		if (supportList.contains("ezcastpro")) {
			if (falconDeviceFinder == null) {
				falconDeviceFinder = new FalconDeviceFinder(deviceFinder);
			}
			falconDeviceFinder.addFilter(new ProjectorInfoFilter() {

				@Override
				public boolean accept(ProjectorInfo projectInfo) {
					String family = projectInfo.getParameter("family");
					if (family != null && family.equals("ezcastpro")) {
						return true;
					}
					return false;
				}
				
			});
		}
		if (supportList.contains("ezcast")) {
			if (falconDeviceFinder == null) {
				falconDeviceFinder = new FalconDeviceFinder(deviceFinder);
			}
			falconDeviceFinder.addFilter(new ProjectorInfoFilter() {

				@Override
				public boolean accept(ProjectorInfo projectInfo) {
					String family = projectInfo.getParameter("family");
					String type = projectInfo.getParameter("type");
					if (family != null && family.equals("ezcast") && (type == null || type.equals("ezcast"))) {
						return true;
					}
					return false;
				}
				
			});
		}
		if (supportList.contains("ezcastmusic")) {
			if (falconDeviceFinder == null) {
				falconDeviceFinder = new FalconDeviceFinder(deviceFinder);
			}
			falconDeviceFinder.addFilter(new ProjectorInfoFilter() {

				@Override
				public boolean accept(ProjectorInfo projectInfo) {
					String family = projectInfo.getParameter("family");
					String type = projectInfo.getParameter("type");
					if (family != null && family.equals("ezcast") && (type != null && type.equals("music"))) {
						return true;
					}
					return false;
				}
				
			});
		}
		if (supportList.contains("ezcastcar")) {
			if (falconDeviceFinder == null) {
				falconDeviceFinder = new FalconDeviceFinder(deviceFinder);
			}
			falconDeviceFinder.addFilter(new ProjectorInfoFilter() {

				@Override
				public boolean accept(ProjectorInfo projectInfo) {
					String family = projectInfo.getParameter("family");
					String type = projectInfo.getParameter("type");
					if (family != null && family.equals("ezcast") && (type != null && type.equals("car"))) {
						return true;
					}
					return false;
				}
				
			});
		}
		if (supportList.contains("ezcastlite")) {
			if (falconDeviceFinder == null) {
				falconDeviceFinder = new FalconDeviceFinder(deviceFinder);
			}
			falconDeviceFinder.addFilter(new ProjectorInfoFilter() {

				@Override
				public boolean accept(ProjectorInfo projectInfo) {
					String family = projectInfo.getParameter("family");
					String type = projectInfo.getParameter("type");
					if (family != null && family.equals("ezcast") && (type != null && type.equals("lite"))) {
						return true;
					}
					return false;
				}
				
			});
		}
		if (falconDeviceFinder != null) {
			deviceFinder.addDeviceFinderImp(falconDeviceFinder);
		}
	}
	public void init(final InitializationListener listener) {
		if (isInitialized()) {
			throw new IllegalStateException("EzCastSdk was initialized!");
		}
		if (initializing) {
			throw new IllegalStateException("EzCastSdk is initializing!");
		}
		initializing = true;
		fetchSupportListAndInit(listener);		
		fetchLocationAndLogAppInfo();
	}
	private void fetchSupportListAndInit(
			final InitializationListener listener) {
		long expire = System.currentTimeMillis() * 1000 + 60;
		try {
			AsyncHttpGet getSupportList = new AsyncHttpGet("https://cloud.iezvu.com/cloud/sdk/api/support"+"?"+"key="+appKey+"&e="+expire+"&c="+computeHash(expire));
			getSupportList.setTimeout(INITIALIZATION_TIMEOUT_MS);
			initTask = AsyncHttpClient.getDefaultInstance().executeJSONObject(getSupportList, new JSONObjectCallback() {

				@Override
				public void onCompleted(Exception e, AsyncHttpResponse source,
						JSONObject result) {
					if (e == null) {
						if (result != null) {
							try {
								if (result.getBoolean("status")) {
									saveSupportList(result.getJSONArray("support"));
									doSetupDeviceFinder(listener);
								} else {
									if (listener != null) {
										listener.onInitializationFailed(new Exception("Please check app key/secret!"));
									}
								}
							} catch (JSONException e1) {
								e1.printStackTrace();
								doSetupDeviceFinder(listener);
							}
						}
					} else { //network error or json transformation error
						e.printStackTrace();
						doSetupDeviceFinder(listener);
					}
				}
				
			});
		} catch (Throwable e) {
			doSetupDeviceFinder(listener);
		}
	}
	private void saveSupportList(JSONArray jsonArray) {
		SharedPreferences sdkSettings = context.getSharedPreferences(PREF_NAME_EZCAST_SDK, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sdkSettings.edit();
	    editor.putString(PREF_KEY_SUPPORT_LIST, jsonArray.toString());
	    editor.commit();
	}
	private List<String> getSupportListFromStore() {
		SharedPreferences sdkSettings = context.getSharedPreferences(PREF_NAME_EZCAST_SDK, Context.MODE_PRIVATE);
		String supportListString = sdkSettings.getString(PREF_KEY_SUPPORT_LIST, null);
		if (supportListString != null) {
			try {
				return convertJsonArrayToList(supportListString);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return Arrays.asList("ezcast", "ezcastscreen"); // default value
	}
	private List<String> convertJsonArrayToList(String supportListString)
			throws JSONException {
		JSONArray supportListJson = new JSONArray(supportListString);
		List<String> supportList = new ArrayList<String>();
		for (int i = 0; i < supportListJson.length(); i++) {
			supportList.add(supportListJson.getString(i));
		}
		return supportList;
	}
	private void finishUpInitialization(InitializationListener listener) {
		isInitialized = true;		
		if (listener != null) {
			listener.onInitialized(EzCastSdk.this);
		}
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
	
	protected String getAppKey() {
		return appKey;
	}

	protected String getAppSecret() {
		return appSecret;
	}
	
	protected Context getContext() {
		return context;
	}
	public boolean isInitialized() {
		return isInitialized;
	}
	
	public DeviceFinder getDeviceFinder() {
		if (!isInitialized) {
			try {
				initTask.get(); // wait until async task done
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
				Log.d(TAG, "initTask.get() failed:"+e.getCause());
			} 
		}
		if (!isInitialized) {
			throw new IllegalStateException("EzCastSdk is not successfully initalized!");
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
		DeviceInfoBuilder<?> builder = DeviceInfoBuilder.getBuilderForDevice(context, device, Device.getAppMacAddress(context));
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
