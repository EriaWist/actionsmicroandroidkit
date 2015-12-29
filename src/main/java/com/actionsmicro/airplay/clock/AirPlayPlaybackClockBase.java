package com.actionsmicro.airplay.clock;

import com.actionsmicro.utils.Log;

import org.apache.commons.net.ntp.TimeStamp;

public abstract class AirPlayPlaybackClockBase implements PlaybackClock {

	private static final long EARLY_TOLERANCE = 200;
	private static final String TAG = "AirPlayPlaybackClockBase";
	private long clockOffset;
	private long latencyTolerance;
	private boolean debugLog;
	private String debugPrefix;
	private int exceptionCount;
	private int exceptionLimit;

	public AirPlayPlaybackClockBase(long latencyTolerance, int exceptionLimit, boolean debugLog, String debugPrefix) {
		super();
		this.debugPrefix = debugPrefix;
		this.debugLog = debugLog;
		this.latencyTolerance = latencyTolerance;
		this.exceptionLimit = exceptionLimit;
	}

	@Override
	public boolean waitUntilTime(long presentationTime) {
		presentationTime = presentationTime - clockOffset;
		if (presentationTime < now() && clockOffset != 0) {
			if ((now() - presentationTime) > latencyTolerance) {
				debugLogW("presentationTime:"+presentationTime+" is too late for "+(now() - presentationTime)+"ms"+", exceptionCount:"+exceptionCount);
				exceptionCount++;
				if (exceptionCount > exceptionLimit) {
					debugLogW("drop frame");
					return false;
				} else {
					return true;
				}
			}
		} else if ((presentationTime - now()) > EARLY_TOLERANCE) {
			while (presentationTime > now() && !Thread.currentThread().isInterrupted()) {
				try {
					long wait = presentationTime - now();
					if (wait <= 0) {
						break;
					} else if (wait > 1000) {//exception
						if (clockOffset == 0) { // clock is not synced yet
							debugLogW("presentationTime:"+presentationTime+" is way too early for "+wait+"ms. but clock is not synced yet, let it pass, now is "+now());
							break;
						}
						Log.e(TAG+"."+debugPrefix, "presentationTime:"+presentationTime+" is way too early for "+wait+"ms. let's skip, now is "+now());
						return true;
					}
					debugLogW("presentationTime:"+presentationTime+" is too early for "+wait+"ms. let's wait");
					Thread.sleep(wait);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		debugLog("presentationTime:"+presentationTime+" is good to go, now is "+now());
		exceptionCount = 0;
		return true;
	}
	protected void updateSyncInfo(long roundTripDelay, long offset) {
		if (roundTripDelay < 20) {
			this.clockOffset = offset;
			debugLog("roundTripDelay:"+roundTripDelay+", offset:"+clockOffset);
		} else {
			debugLogW("roundTripDelay is too much, ignore this sync."+", offset:"+clockOffset);
		}
	}
	protected long now() {
		return TimeStamp.getCurrentTime().getTime();
	}

	protected void debugLog(String msg) {
		if (debugLog) {
			Log.d(TAG+"."+debugPrefix, msg);
		}
	}

	protected void debugLogW(String msg) {
		if (debugLog) {
			Log.w(TAG+"."+debugPrefix, msg);
		}
	}

}