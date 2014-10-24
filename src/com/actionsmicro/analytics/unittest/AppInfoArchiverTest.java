package com.actionsmicro.analytics.unittest;

import junit.framework.TestCase;

import org.json.JSONObject;

import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
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
		final double mockLatitude = 25;
		final double mockLongitude = 121.5;
		
		Location mockLocation = new Location(LocationManager.NETWORK_PROVIDER) {
			@Override
			public double getLatitude () {
				return mockLatitude;
			}
			@Override
			public double getLongitude () {
				return mockLongitude;
			}
		};
		try {
			final String mockMacAddress = "01:23:45:67:89:AB";
			final String mockAppVersion = "1.1.499";			
			final int mockResWidth = 1024;
			final int mockResHeight = 768;
			JSONObject jsonObject = new JSONObject(gson.toJson(new AppInfo(mockAndroidContext, mockMacAddress, mockAppVersion, new Point(mockResWidth, mockResHeight), mockLocation)));
			assertEquals("app", jsonObject.get("type"));			
			assertEquals("2014-10-24", jsonObject.get("schema_version"));
			assertTrue(jsonObject.has("app_id"));
			assertEquals(mockMacAddress, jsonObject.get("app_id"));
			assertEquals(packageName, jsonObject.get("package_id"));			
			assertTrue(jsonObject.has("app_version"));
			assertEquals(mockAppVersion, jsonObject.get("app_version"));
			assertEquals("android", jsonObject.get("os_type"));
			assertTrue(jsonObject.has("os_version"));
			assertTrue(jsonObject.has("manufacturer"));
			assertTrue(jsonObject.has("model"));
			assertTrue(jsonObject.has("resolution"));
			assertTrue(jsonObject.getJSONObject("resolution").has("width"));
			assertEquals(mockResWidth, jsonObject.getJSONObject("resolution").get("width"));
			assertTrue(jsonObject.getJSONObject("resolution").has("height"));
			assertEquals(mockResHeight, jsonObject.getJSONObject("resolution").get("height"));
			assertTrue(jsonObject.has("location"));
			assertEquals(mockLatitude, jsonObject.getJSONObject("location").getDouble("latitude"));
			assertEquals(mockLongitude, jsonObject.getJSONObject("location").getDouble("longitude"));
			assertTrue(jsonObject.has("language"));
			assertTrue(jsonObject.has("country"));
			assertTrue(jsonObject.has("time_zone"));
			
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
}
