package com.actionsmicro.analytics.tracker;

import java.util.Map;

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
		Log.d("LogTracker", gson.toJson(usage));
	}

	@Override
	public void log(AppInfo appInfo) {
		Log.d("LogTracker", gson.toJson(appInfo));
	}

	@Override
	public void log(Map<String, Object> map) {
		Log.d("LogTracker", gson.toJson(map));		
	}

}
