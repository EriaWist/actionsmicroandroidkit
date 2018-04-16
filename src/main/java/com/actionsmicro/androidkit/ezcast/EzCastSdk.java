package com.actionsmicro.androidkit.ezcast;

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
import com.actionsmicro.analytics.device.EZCastFamilyDeviceTypeBuilder;
import com.actionsmicro.analytics.tracker.ActionsTracker;
import com.actionsmicro.analytics.tracker.CompoundTracker;
import com.actionsmicro.analytics.tracker.HashUtils;
import com.actionsmicro.analytics.tracker.LogTracker;
import com.actionsmicro.androidkit.ezcast.imp.airplay.AirPlayDeviceFinder;
import com.actionsmicro.androidkit.ezcast.imp.androidrx.AndroidRxFinder;
import com.actionsmicro.androidkit.ezcast.imp.dlna.DlnaDeviceFinder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.FalconDeviceFinder;
import com.actionsmicro.androidkit.ezcast.imp.googlecast.GoogleCastFinder;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.filter.FilterInterface;
import com.actionsmicro.utils.Device;
import com.actionsmicro.utils.Log;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
/**
 * EZCast SDK object 
 * 
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.1
 */
public class EzCastSdk {
	private static final String SDK_VERSION_STRING = "{SDK_VERSION_STRING_LITE}";
	private static final String PREF_KEY_SUPPORT_LIST = "support_list";
	private static final String PREF_NAME_EZCAST_SDK = "ezcastsdk";
	private static final int INITIALIZATION_TIMEOUT_MS = 10000;
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
	private String packageId;
	public interface InitializationListener {
		/**
		 * Called when initialization is successful.
		 * @param ezCastSdk The EzCastSdk instance which had been initialized.
		 */
		void onInitialized(EzCastSdk ezCastSdk);
		
		/**
		 * Called when initialization is failed. In most case, it failed because the application key or secret is invalid.
		 * @param exception Detail about why initialization is failed.
		 */
		void onInitializationFailed(Exception exception);
		
	}
	/**
	 * Create a EzCastSdk instance. You should only create one EzCastSdk instance. 
	 * Created instance can be access via {@link #getSharedSdk()}.
	 * @param context Android context
	 * @param appKey Application key of which has authorized to use EZCast SDK.
	 * @param appSecret Application secret which is pair with the application key.
	 * @see getSharedSdk
	 */
	public EzCastSdk(Context context, String appKey, String appSecret) {
		if (sharedEzCastSdk != null) {
			throw new IllegalStateException("EzCastSdk had been created! Only allow one EzCastSdk instance.");
		}
		if (context == null) {
			throw new InvalidParameterException("Context should not be null!");
		}
		if (appKey == null || appKey.isEmpty()) {
			throw new InvalidParameterException("App key should not be empty!");
		}
		if (appSecret == null || appSecret.isEmpty()) {
			throw new InvalidParameterException("App secret should not be empty!");
		}
		this.packageId = context.getPackageName();
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
	/**
	 * Get shared instance of EzCastSdk. Return null if none exists.
	 * @return Shared EzCastSdk instance.
	 */
	public static EzCastSdk getSharedSdk() {
		return sharedEzCastSdk;
	}
	private String computeHash(long expire) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return HashUtils.EzCastHash(appSecret, expire, "/cloud/sdk/api/support", packageId);
	}
	private static Handler mainThreadHandler = new Handler(Looper.getMainLooper());
	private void doSetupDeviceFinder(final InitializationListener listener, final FilterInterface filter) {
		setupDeviceFinder(getSupportListFromStore(), deviceFinder, filter);
		finishUpInitialization(listener);
		synchronized (deviceFinder) {
			deviceFinder.notifyAll();
		}
	}
	private static void setupFinderForEzCastAndPro(final List<String> supportList, DeviceFinder deviceFinder, FilterInterface filter) {
		if (supportList.contains("ezcastpro") ||
				supportList.contains("ezcast") ||
				supportList.contains("ezcastlite") ||
				supportList.contains("ezcastmusic") ||
				supportList.contains("ezcastcar") ||
				supportList.contains("mirascreen") ||
				supportList.contains("quattro")) {
			FalconDeviceFinder falconDeviceFinder = new FalconDeviceFinder(deviceFinder);
			if (filter != null) {
				falconDeviceFinder.addFilter(filter);
			}
			falconDeviceFinder.addFilter(new FilterInterface<ProjectorInfo>() {

				@Override
				public boolean accept(ProjectorInfo projectInfo) {
					if (supportList.contains(EZCastFamilyDeviceTypeBuilder.getType(projectInfo))) {
						return true;
					}
					return false;
				}

			});
			deviceFinder.addDeviceFinderImp(falconDeviceFinder);
		}		
	}
	/**
	 * Initialize EzCastSdk instance. This method is synchronous.
	 * You can implement {@link InitializationListener} to receive the result. 
	 * Note: Initialization process is kind of lengthy, suggest to invoke this in non-UI thread.
	 * Throws IllegalStateException when EzCastSdk is initializing or has been initialized.
	 * @param listener Callback to receive initialization result.
	 */
	public void init(final InitializationListener listener, FilterInterface filter) {
		if (isInitialized()) {
			throw new IllegalStateException("EzCastSdk was initialized!");
		}
		if (initializing) {
			throw new IllegalStateException("EzCastSdk is initializing!");
		}
		initializing = true;
		fetchSupportListAndInit(listener, filter);
		fetchLocationAndLogAppInfo();
		waitUntilInitTaskDone();
	}
	private void waitUntilInitTaskDone() {
		if (initTask != null) {
			try {
				initTask.get();
				synchronized (deviceFinder) {
					deviceFinder.wait(1000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
				Log.d(TAG, "initTask.get() failed:"+e.getCause());
				synchronized (deviceFinder) {
					try {
						deviceFinder.wait(1000);
					} catch (InterruptedException e1) {
					}
				}
			}
		}
	}
	private void fetchSupportListAndInit(
			final InitializationListener listener, final FilterInterface filter) {
		long expire = System.currentTimeMillis() * 1000 + 60 + 90;
		try {
			AsyncHttpGet getSupportList = new AsyncHttpGet("https://cloud.ezcast.com/cloud/sdk/api/support"+"?"+"key="+appKey+"&e="+expire+"&c="+computeHash(expire)+"&p=1&o=android&v="+URLEncoder.encode(SDK_VERSION_STRING, "utf-8"));
			getSupportList.setTimeout(INITIALIZATION_TIMEOUT_MS);
			initTask = AsyncHttpClient.getDefaultInstance().executeJSONObject(getSupportList, new JSONObjectCallback() {

				@Override
				public void onCompleted(Exception e, AsyncHttpResponse source,
						JSONObject result) {
					Log.v(TAG, "init oncomplete called");
					if (e == null) {
						Log.v(TAG, "init oncomplete successfully");
						if (result != null) {
							try {
								if (result.getBoolean("status")) {
									saveSupportList(result.getJSONArray("support"));
									doSetupDeviceFinder(listener, filter);
								} else {
									if (listener != null) {
										listener.onInitializationFailed(new Exception("Please check app key/secret!"));
									}
								}
							} catch (JSONException e1) {
								e1.printStackTrace();
								doSetupDeviceFinder(listener, filter);
							}
						}
					} else { //network error or json transformation error
						Log.v(TAG, "init oncomplete timeout");
						e.printStackTrace();
						doSetupDeviceFinder(listener, filter);
					}
				}
				
			});
		} catch (Throwable e) {
			doSetupDeviceFinder(listener, filter);
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
		return Arrays.asList("ezcast", "ezscreen", "ezcastpro", "mirascreen"); // default value
	}
	protected static List<String> convertJsonArrayToList(String supportListString)
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
		initializing = false;
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
				try {
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
					} catch (IllegalArgumentException e) {
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
				} catch (Throwable t) {
					Log.e(TAG, t.getLocalizedMessage());					
				} finally {
					tracker.log(new AppInfo(context, fetchedlocation, SDK_VERSION_STRING));
				}
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
	/**
	 * Determine whether EzCastSdk has been initialized successfully or not. 
	 * @return Return whether EzCastSdk has been initialized successfully or not
	 */
	public boolean isInitialized() {
		return isInitialized;
	}
	/**
	 * Get the device finder to discover compatible devices.
	 * This method will be blocked until initialization has finished.
	 * Throws IllegalStateException is EzCaskSdk is not successfully initialized.
	 * @return Return the device finder.
	 * @see DeviceFinder
	 */
	public DeviceFinder getDeviceFinder() {
		if (!isInitialized && initTask != null) {
			try {
				initTask.get(); // wait until async task done
				synchronized (deviceFinder) {
					deviceFinder.wait(1000);
				}
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
				e.printStackTrace();
				Log.d(TAG, "initTask.get() failed:"+e.getCause());
				synchronized (deviceFinder) {
					try {
						deviceFinder.wait(1000);
					} catch (InterruptedException e1) {
					}
				}
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
		DeviceInfoBuilder<?> builder = DeviceInfoBuilder.getBuilderForDevice(context, device, Device.getAppUniqueId(context));
		if (tracker != null && builder != null) {
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
	protected static void setupDeviceFinder(List<String> supportList, final DeviceFinder deviceFinder, final FilterInterface filter) {
		if (supportList.contains("chromecast")) {
			// GoogleCastFinder main thread only API
			if (Looper.myLooper() == Looper.getMainLooper()) {
				deviceFinder.addDeviceFinderImp(new GoogleCastFinder(deviceFinder));				
			} else {
				mainThreadHandler.post(new Runnable() {

					@Override
					public void run() {
						deviceFinder.addDeviceFinderImp(new GoogleCastFinder(deviceFinder));
					}			
				});
			}
		}
		if (supportList.contains("airplay")) {
			deviceFinder.addDeviceFinderImp(new AirPlayDeviceFinder(deviceFinder));
		}
		if (supportList.contains("ezscreen")) {
			deviceFinder.addDeviceFinderImp(new AndroidRxFinder(deviceFinder));
		}
		if (supportList.contains("dlna")) {
			deviceFinder.addDeviceFinderImp(new DlnaDeviceFinder(deviceFinder));
		}
		setupFinderForEzCastAndPro(supportList, deviceFinder, filter);
	}
}
