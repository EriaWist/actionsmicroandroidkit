package com.actionsmicro.analytics.device;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.utils.Log;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;

public class EZCastDeviceInfoBuilder extends DeviceInfoBuilder<PigeonDeviceInfo> {

	public EZCastDeviceInfoBuilder(Context context, PigeonDeviceInfo device, String appId) {
		super(context, device, appId, "device", "2014-10-24", EZCastFamilyDeviceTypeBuilder.getType(device));
	}
	protected JSONObject getDongleInfo() {
		final String dongoleInfoUrl = "http://"+getDevice().getIpAddress().getHostAddress()+"/dongleInfo.json";
		Future<JSONObject> syncObject = AsyncHttpClient.getDefaultInstance().executeJSONObject(new AsyncHttpGet(dongoleInfoUrl), null);
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

}
