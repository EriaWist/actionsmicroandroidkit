package com.actionsmicro.analytics.unittest;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.json.JSONObject;

import android.test.mock.MockContext;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.usage.WebVideoUsage;
import com.google.gson.Gson;

public class UsageArchiverTest extends TestCase {
	final Mockery context = new Mockery();
	final Gson gson = new Gson();
	final String packageName = "com.mock.app";
	final MockContext mockAndroidContext = new MockContext() {
		@Override
		public String getPackageName() {
			return packageName;
		}
	};
	public void testWebVideoUsage() {
		final String mockUrl = "http://host.com:3030/path?q=1&u=2#frag";
		final Tracker tracker = context.mock(Tracker.class);
		final WebVideoUsage usage = new WebVideoUsage(tracker, mockAndroidContext, mockUrl);
		context.checking(new Expectations() {{
			oneOf (tracker).log(with(same(usage)));
		}});
		usage.begin();
		usage.commit();
		context.assertIsSatisfied();
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(usage));
			assertEquals("web_video", jsonObject.get("type"));			
			assertEquals(mockUrl, jsonObject.get("video_url"));			
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
		
	}
}
