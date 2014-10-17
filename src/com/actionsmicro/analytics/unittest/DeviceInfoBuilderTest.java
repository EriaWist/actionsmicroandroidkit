package com.actionsmicro.analytics.unittest;

import junit.framework.TestCase;

import org.jmock.Mockery;
import org.json.JSONObject;

import android.test.mock.MockContext;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.analytics.device.EZCastDeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.google.gson.Gson;

public class DeviceInfoBuilderTest extends TestCase {
	final Mockery context = new Mockery();
	final Gson gson = new Gson();
	final String packageName = "com.mock.app";
	final MockContext mockAndroidContext = new MockContext() {
		@Override
		public String getPackageName() {
			return packageName;
		}
	};
	final ProjectorInfo projectorInfo = new ProjectorInfo() {
		
	};
	final PigeonDeviceInfo pigeonDeviceInfo = new PigeonDeviceInfo(projectorInfo) {
		
	};
	public void testDeviceInfoBuilderFactory() {
		DeviceInfoBuilder builder = DeviceInfoBuilder.getBuilderForDevice(mockAndroidContext, pigeonDeviceInfo);
		assertTrue(builder instanceof EZCastDeviceInfoBuilder);
	}
	public void testEZCastDeviceInfo() {
		EZCastDeviceInfoBuilder deviceInfoBuilder= new EZCastDeviceInfoBuilder(mockAndroidContext, pigeonDeviceInfo);
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(deviceInfoBuilder.buildDeviceInfo()));
			assertEquals(packageName, jsonObject.get("package_id"));			
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
}
