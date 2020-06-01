package com.actionsmicro.analytics.device;

import android.content.Context;
import android.net.Uri;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.AMCertificate;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.helper.SSLHelper;
import com.actionsmicro.utils.Log;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EZCastDeviceInfoBuilder extends DeviceInfoBuilder<PigeonDeviceInfo> {

	public EZCastDeviceInfoBuilder(Context context, PigeonDeviceInfo device, String appId) {
		super(context, device, appId, "device", "2014-10-24", EZCastFamilyDeviceTypeBuilder.getType(device));
	}
	protected JSONObject getDongleInfo() {
		final String dongoleInfoUrl = getWebRoot(getDevice()) + "dongleInfo.json";
		AsyncHttpClient asyncHttpClient = AsyncHttpClient.getDefaultInstance();
		try {
			URL url = new URL(dongoleInfoUrl);
			if (url.getProtocol().toUpperCase().equals("HTTPS")) {
				SSLHelper.trustSSL(asyncHttpClient);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		Future<JSONObject> syncObject = asyncHttpClient.executeJSONObject(new AsyncHttpGet(dongoleInfoUrl), null);
		try {
			return syncObject.get(3, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}

	@Override
	public Map<String, Object> buildDeviceInfo() {
		Map<String, Object> deviceInfo = super.buildDeviceInfo();
		deviceInfo.put("device_id", getDevice().getParameter("deviceid"));
		JSONObject result = getDongleInfo();
		if (result != null && result.has("encrypted_data")) {
			try {
				deviceInfo.put("encrypted_data", result.get("encrypted_data"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Log.e("EZCastDeviceInfoBuilder", "no encrypted_data found in dongleInfo.json");
		}

		return deviceInfo;
	}


	// TODO DRY
	private String getWebRoot(DeviceInfo deviceInfo) {
		if (deviceInfo == null || deviceInfo.getIpAddress() == null || deviceInfo.getIpAddress().getHostAddress() == null) {
			return "";
		}
		String webroot="";
		if(deviceInfo instanceof AMCertificate){
			webroot = ((AMCertificate)deviceInfo).getCertificateWebRoot();
		}
		if(!webroot.isEmpty()){
			return Uri.parse(webroot).toString()+"/";
		}

		webroot = deviceInfo.getParameter("webroot");
		if (webroot != null && !webroot.isEmpty()) {
			return decodeHtmlUrl(webroot);
		}
		return "http://" + deviceInfo.getIpAddress().getHostAddress() + "/";
	}

	private String decodeHtmlUrl(String url) {
		String decodedUrl = "";
		try {
			decodedUrl = URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return decodedUrl;
	}
}
