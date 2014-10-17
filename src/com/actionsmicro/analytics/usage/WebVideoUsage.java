package com.actionsmicro.analytics.usage;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;

import android.content.Context;

@SuppressWarnings("unused")
public class WebVideoUsage extends Usage {
	private String video_url;

	public WebVideoUsage(Tracker tracker, Context context, String url) { // more parameter needed
		super(tracker, context, "web_video");
		this.video_url = url;
	}
}
