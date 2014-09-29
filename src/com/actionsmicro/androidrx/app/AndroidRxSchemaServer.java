package com.actionsmicro.androidrx.app;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.actionsmicro.androidrx.app.TimeoutableLocationListener.TimeoutLisener;
import com.actionsmicro.utils.Device;
import com.actionsmicro.utils.Utils;

public class AndroidRxSchemaServer /*extends NanoHTTPD*/ {

	public static final int PORT = 8182;
	public static final String KeyAES = "28906462822699798631919982357480";
	public static final String ANDROIDRX_SCHEMA_UUID_PREFERENCE_KEY = "com.actionsmicro.iezvu.schema.schema_uuid";
	public static final String ANDROIDRX_SCHEMA_PREFERENCE_KEY = "com.actionsmicro.iezvu.schema.schema_uuid";
	
	public enum RxFunction{
		EZCAST("com.actionsmicro.androidrx.AndroidRxSchemaServer.EZCAST", 0),
		EZAIR("com.actionsmicro.androidrx.AndroidRxSchemaServer.EZAIR", 1),
		EZAIR_MIRROR("com.actionsmicro.androidrx.AndroidRxSchemaServer.EZAIR_MIRROR", 2);
		
		private String sValue;
		private int iValue;
		private RxFunction(String sValue, int iValue) {
			this.sValue = sValue;
			this.iValue = iValue;
		}
		
		@Override
		public String toString() {
			return sValue;
		}
	}
	
	private String schemaJSONString;
	private Context context;
	
	public AndroidRxSchemaServer(Context context) {
//		super(PORT);
		this.context = context;
		createAndroidRxSchema();
	}
	
	private void createAndroidRxSchema() {
		
		DisplayMetrics displayMetrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE); // the results will be higher than using the activity context object or the getWindowManager() shortcut
		wm.getDefaultDisplay().getMetrics(displayMetrics);
		int screenWidth = displayMetrics.widthPixels;
		int screenHeight = displayMetrics.heightPixels;
		
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("schema_version", 4);
			jsonObject.put("mac_address", getAppMacAddress(context));
			jsonObject.put("uuid", getUUID(context));
			jsonObject.put("ssid_internet_ap", getCurrentSsid(context));
				JSONObject resolutionJSONObject = new JSONObject();
				resolutionJSONObject.put("width", screenWidth);
				resolutionJSONObject.put("height", screenHeight);
			jsonObject.put("resolution", resolutionJSONObject);
			jsonObject.put("os_type", "android");
			jsonObject.put("manufacturer", android.os.Build.MANUFACTURER);
			jsonObject.put("model", android.os.Build.MODEL);
			jsonObject.put("app_version", getAppVersion(context));
			jsonObject.put("country", Locale.getDefault().getCountry());
				Location location = getLocation(context);
				Double latitude;
				Double longitude;
				if (location == null) {
					latitude = 0.0;
					longitude = 0.0;
				} else {
					latitude = location.getLatitude();
					longitude = location.getLongitude();
				}
				JSONObject locationJSONObject = new JSONObject();
				locationJSONObject.put("latitude", latitude);
				locationJSONObject.put("longitude", longitude);
			jsonObject.put("location", locationJSONObject);
			jsonObject.put("language", Locale.getDefault().toString());
				JSONObject useTimeJSONObject = new JSONObject();
				useTimeJSONObject.put("ezcast", getFunctionUsedTime(RxFunction.EZCAST));
				useTimeJSONObject.put("ezair", getFunctionUsedTime(RxFunction.EZAIR));
				useTimeJSONObject.put("ezair_mirror", getFunctionUsedTime(RxFunction.EZAIR_MIRROR));
			jsonObject.put("use_time", useTimeJSONObject);
			
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String encryptString = EncryptAES(KeyAES, jsonObject);
		JSONObject encryptJSON = new JSONObject();
		try {
			encryptJSON.put("encrypted_data", encryptString);
			schemaJSONString = encryptJSON.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		sendAndroidRxInfo();
		
	}

//	@Override 
//	public Response serve(IHTTPSession session) {
//		String msg = "";
//		if (!schemaJSONString.isEmpty()) {
//			msg = schemaJSONString;
//		}
//
//		return new NanoHTTPD.Response(msg);
//	}
	
	private String getCurrentSsid(Context context) {
		String ssid = null;
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo.isConnected()) {
			final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null && connectionInfo.getSSID().length() != 0) {
				ssid = connectionInfo.getSSID();
			}
		}
		return ssid;
	}
	private Location getLocation(Context context) {
		LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		Location location = null;
		try {
			location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		} catch (IllegalArgumentException illegalArgumentException) {
			illegalArgumentException.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (location != null) {
			updateWithNewLocation(location, lm);
			return location;
		} else {
			return null;
		}
	}
	private void updateWithNewLocation(Location location, LocationManager lm) {
		TimeoutableLocationListener timeoutableLocationListener = new TimeoutableLocationListener (lm, 60000, new TimeoutLisener() {
			@Override
			public void onTimeouted(LocationListener sender) {
			}
		});
		try {
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, timeoutableLocationListener);
		} catch (IllegalArgumentException illegalArgumentException) {
			illegalArgumentException.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public static String getUUID(Context context) {
        return Device.getUUID(context, ANDROIDRX_SCHEMA_UUID_PREFERENCE_KEY);
	}
	
	private String EncryptAES(String key, JSONObject jsonObject) {
		byte[] textByte;
		try {
			textByte = EncryptAES(key.getBytes("UTF-8"), jsonObject.toString().getBytes("UTF-8"));
			return Base64.encodeToString(textByte, Base64.DEFAULT);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	private byte[] EncryptAES(byte[] key,byte[] text) {
		try
		{
		    SecretKeySpec mSecretKeySpec = new SecretKeySpec(key, "AES");
		    Cipher mCipher = null;
		    mCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		    mCipher.init(Cipher.ENCRYPT_MODE,mSecretKeySpec);
		    return mCipher.doFinal(text);
		}
		catch(Exception ex)
		{
			return null;
		}
	}
	public static String getAppMacAddress(Context context) {
		return Device.getAppMacAddress(context);
	}
	public static String getAppVersion(Context context) {
		String appVersion = "";
		try {
			appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return appVersion;
	}
	private RxFunction currentFunc = null;
	private Long startAt;
	public void startFunction(RxFunction func) {
		sendAndroidRxInfo();
		currentFunc = func;
		startAt = TimeUnit.MILLISECONDS.toSeconds(SystemClock.elapsedRealtime());
	}
	public void stopFunction() {
		saveUsedTime();
	}
	private void saveUsedTime() {
		if (currentFunc == null)
			return;
		SharedPreferences prefs;
		prefs = context.getSharedPreferences(ANDROIDRX_SCHEMA_PREFERENCE_KEY, 0);
		SharedPreferences.Editor editor = prefs.edit();
		String functionKey = currentFunc.toString();
		if (startAt > 0L) {
			Long usedTime;
			Long lastUsedTime;
			Long totalUseTime;
			usedTime = TimeUnit.MILLISECONDS.toSeconds(SystemClock.elapsedRealtime()) - startAt;
			lastUsedTime = prefs.getLong(functionKey, 0);
			totalUseTime = usedTime + lastUsedTime;
			editor.putLong(functionKey, totalUseTime);
			editor.commit();
			startAt = 0L;
		}

	}
	private Long getFunctionUsedTime(RxFunction func) {
		SharedPreferences prefs;
		prefs = context.getSharedPreferences(ANDROIDRX_SCHEMA_PREFERENCE_KEY, 0);
		return prefs.getLong(func.toString(), 0);
	}
	private void sendAndroidRxInfo() {
		String url = "http://cloud.iezvu.com/dongleinfo_rx.php?" + "&type=android";
		Utils.sendDataToServer(url, schemaJSONString);
	}

}