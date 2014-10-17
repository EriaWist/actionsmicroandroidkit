package com.actionsmicro.analytics.usage;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;

import android.content.Context;

public class WebVideoUsage extends Usage {
	public WebVideoUsage(Tracker tracker, Context context, String url) { // more parameter needed
		super(tracker, context, "web_video");
	}
}
