package com.actionsmicro.analytics.tracker;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.actionsmicro.analytics.AppInfo;
import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;
import com.actionsmicro.utils.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LogTracker implements Tracker {
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	@Override
	public void log(Usage usage) {
		logJson(gson.toJson(usage));
	}

	private void logJson(String json) {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(json);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (jsonObject != null && jsonObject.has("type")) {
			try {
				Log.d("LogTracker", "Record type:"+jsonObject.getString("type") + "\n" + json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.d("LogTracker", json);
		}
	}

	@Override
	public void log(AppInfo appInfo) {
		logJson(gson.toJson(appInfo));
	}

	@Override
	public void log(Map<String, Object> map) {
		logJson(gson.toJson(map));		
	}

}
