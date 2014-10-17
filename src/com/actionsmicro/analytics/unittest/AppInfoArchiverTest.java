package com.actionsmicro.analytics.unittest;

import junit.framework.TestCase;

import org.json.JSONObject;

import android.test.mock.MockContext;

import com.actionsmicro.analytics.AppInfo;
import com.google.gson.Gson;

public class AppInfoArchiverTest extends TestCase {
	final Gson gson = new Gson();
	final String packageName = "com.mock.app";
	final MockContext mockAndroidContext = new MockContext() {
		@Override
		public String getPackageName() {
			return packageName;
		}
	};
	public void testAppInfoArchiver() {
		
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(new AppInfo(mockAndroidContext)));
			assertEquals(packageName, jsonObject.get("package_id"));			
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
}
