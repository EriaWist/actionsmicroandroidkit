package com.actionsmicro.analytics.tracker;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.actionsmicro.analytics.AppInfo;
import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;

public class CompoundTracker implements Tracker {
	private CopyOnWriteArrayList<Tracker> trackers = new CopyOnWriteArrayList<Tracker>();
	@Override
	public void log(Usage usage) {
		for (Tracker tracker : trackers) {
			tracker.log(usage);
		}
	}

	@Override
	public void log(AppInfo appInfo) {
		for (Tracker tracker : trackers) {
			tracker.log(appInfo);
		}
	}

	@Override
	public void log(Map<String, Object> map) {
		for (Tracker tracker : trackers) {
			tracker.log(map);
		}
	}
	public void add(Tracker tracker) {
		trackers.add(tracker);
	}
	public void remove(Tracker tracker) {
		trackers.remove(tracker);
	}
	public void clear() {
		trackers.clear();
	}
}
