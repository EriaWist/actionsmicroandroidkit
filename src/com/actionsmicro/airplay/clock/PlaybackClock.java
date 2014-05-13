package com.actionsmicro.airplay.clock;

public interface PlaybackClock {

	public abstract boolean waitUntilTime(long presentationTime);
	public abstract void release();
}