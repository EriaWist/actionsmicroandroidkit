package com.actionsmicro.analytics.tracker;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;

import android.content.Context;

public class ActionsTracker implements Tracker {
//	private final static Tracker sharedTracker; 
//	public static Tracker getSharedTracker(Context context) {
//		if (sharedTracker == null) {
//			sharedTracker = new Tracker(context);
//		}
//		return sharedTracker;
//	}
	public ActionsTracker(Context context, String appKey, String appSecret) {
		
	}

	/* (non-Javadoc)
	 * @see com.actionsmicro.analytics.Tracker#log(com.actionsmicro.analytics.Usage)
	 */
	@Override
	public void log(Usage usage) {
		// TODO Auto-generated method stub
		
	}
}
