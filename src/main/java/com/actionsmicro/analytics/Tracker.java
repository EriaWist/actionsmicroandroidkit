package com.actionsmicro.analytics;

import java.util.Map;

public interface Tracker {

	public abstract void log(Usage usage);

	public abstract void log(AppInfo appInfo);

	public abstract void log(Map<String, Object> map);
}