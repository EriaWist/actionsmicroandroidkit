package com.actionsmicro.utils;

public class TimeProber {
	private long avgInterval; 
	private long startTime; 
	
	public void start() {
		startTime = System.currentTimeMillis();
	}
	public void stop() {
		if (avgInterval == 0) {
			avgInterval = System.currentTimeMillis() - startTime;
		} else {
			avgInterval = (avgInterval * 7 + (System.currentTimeMillis() - startTime) * 3)/10;
		}
	}
	public long getAverageInterval() {
		return avgInterval;
	}
}
