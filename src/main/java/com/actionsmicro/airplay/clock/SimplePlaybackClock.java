package com.actionsmicro.airplay.clock;

import com.actionsmicro.utils.Log;

public class SimplePlaybackClock implements PlaybackClock {
//	private static final long LATENCY_TOLERANCE = 200; //ms
	private static final long EARLY_TOLERANCE = 10; //ms
	private long timestampBase;
	private long systemTimeBase;
	private String debugTag;
	private long latencyTolerance;
	private static final String TAG = "SimplePlaybackClock";
	private static final boolean DEBUG_LOG = true;
	
	public SimplePlaybackClock(long presentationTime, long latencyTolerance, String debugTag) {
		this.debugTag = debugTag;
		this.latencyTolerance = latencyTolerance;
		sync(presentationTime);
	}
	public void sync(long presentationTime) {
		timestampBase = presentationTime;
		systemTimeBase = System.currentTimeMillis();
	}
	@Override
	public boolean waitUntilTime(long presentationTime) {
		presentationTime = presentationTime - timestampBase;
		if (presentationTime < now()) {
			if ((now() - presentationTime) > latencyTolerance) {
				debugLog("presentationTime:"+presentationTime+" is too late for "+(now() - presentationTime)+"ms");
				return false;
			}
		} else if ((presentationTime - now()) > EARLY_TOLERANCE) {
			while (presentationTime > now() && !Thread.currentThread().isInterrupted()) {
				try {
					long wait = presentationTime - now();
					debugLog("presentationTime:"+presentationTime+" is too early for "+wait+"ms. let's wait");
					if (wait <= 0) {
						break;
					}
					Thread.sleep(wait);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return true;
	}
	private long now() {
		return System.currentTimeMillis() - systemTimeBase;
	}
	private void debugLog(String msg) {
		if (DEBUG_LOG) {
			Log.d(TAG+"."+debugTag, msg);
		}
	}
	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}
}
