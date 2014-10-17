package com.actionsmicro.analytics.unittest;

import junit.framework.TestCase;

import org.jmock.Mockery;
import org.json.JSONObject;

import android.test.mock.MockContext;

import com.actionsmicro.analytics.device.EZCastDeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.google.gson.Gson;

public class DeviceInfoBuilderTest extends TestCase {
	Mockery context = new Mockery();
	Gson gson = new Gson();

	public void testEZCastDeviceInfo() {
		
		final String packageName = "com.mock.app";
		MockContext mockContext = new MockContext() {
			@Override
			public String getPackageName() {
				return packageName;
			}
		};
		ProjectorInfo projectorInfo = new ProjectorInfo() {
			
		};
		
		EZCastDeviceInfoBuilder deviceInfoBuilder= new EZCastDeviceInfoBuilder(mockContext, new PigeonDeviceInfo(projectorInfo));
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(deviceInfoBuilder.buildDeviceInfo()));
			assertEquals(packageName, jsonObject.get("package_id"));			
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
}
