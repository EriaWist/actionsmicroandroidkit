package com.actionsmicro.analytics.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.json.JSONObject;

import android.test.mock.MockContext;

import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;
import com.actionsmicro.analytics.unittest.mock.MockDeviceInfo;
import com.actionsmicro.analytics.usage.LocalAudioUsage;
import com.actionsmicro.analytics.usage.LocalVideoUsage;
import com.actionsmicro.analytics.usage.MediaUsage;
import com.actionsmicro.analytics.usage.RemoteMediaUsage;
import com.actionsmicro.analytics.usage.WebAudioUsage;
import com.actionsmicro.analytics.usage.WebVideoUsage;
import com.actionsmicro.analytics.usage.WifiDisplayUsage;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.google.gson.Gson;

public class UsageArchiverTest extends TestCase {
	private final Mockery context = new Mockery();
	private final Gson gson = new Gson();
	private final String mockPackageName = "com.mock.app";
	private final String mockAppId = "01:12:23:34:45";
	private final String mockDeviceId = "32:12:12:32:55";
	private final String mockResult = "Good Result";
	private final int mockNormalizedResult = 3;
	private final String mockFirmwareVersion = "1.23.2";
	private final long mockDuration = 1224;
	private final String mockTitle = "Untitled";
	private final String mockUserAgent = "Linux; Android 4.4.4; Nexus 7 Build/KTU84P) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/33.0.0.0 Safari/537.36/ezcast";
	private final String mockUrl = "http://host.com:3030/path?q=1&u=2#frag";

	final MockContext mockAndroidContext = new MockContext() {
		@Override
		public String getPackageName() {
			return mockPackageName;
		}
	};
	public void testWebVideoUsage() {
		final Tracker tracker = context.mock(Tracker.class);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("deviceid", mockDeviceId);
		properties.put("srcvers", mockFirmwareVersion);
		final DeviceInfo device = new MockDeviceInfo(properties) ;
		final WebVideoUsage usage = new WebVideoUsage(tracker, mockAndroidContext, mockAppId, mockPackageName, device, mockUrl);
		context.checking(new Expectations() {{
			oneOf (tracker).log(with(same(usage)));
		}});
		usage.setDuration(mockDuration);
		usage.setResult(mockResult, mockNormalizedResult);
		usage.setTitle(mockTitle);
		usage.setUserAgent(mockUserAgent);
		usage.begin();
		usage.commit();
		context.assertIsSatisfied();
		verifyRemoteMediaUsgaeJson(mockUrl, usage, "web_video");
		
	}
	public void testWebAudioUsage() {
		final Tracker tracker = context.mock(Tracker.class);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("deviceid", mockDeviceId);
		properties.put("srcvers", mockFirmwareVersion);
		final DeviceInfo device = new MockDeviceInfo(properties) ;
		final WebAudioUsage usage = new WebAudioUsage(tracker, mockAndroidContext, mockAppId, mockPackageName, device, mockUrl);
		context.checking(new Expectations() {{
			oneOf (tracker).log(with(same(usage)));
		}});
		usage.setDuration(mockDuration);
		usage.setResult(mockResult, mockNormalizedResult);
		usage.setTitle(mockTitle);
		usage.setUserAgent(mockUserAgent);
		usage.begin();
		usage.commit();
		context.assertIsSatisfied();
		verifyRemoteMediaUsgaeJson(mockUrl, usage, "web_audio");		
	}
	private void verifyRemoteMediaUsgaeJson(final String mockUrl,
			final RemoteMediaUsage usage, String recordType) {
		verifyMediaUsgaeJson(usage, recordType);
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(usage));
			assertEquals(mockUrl, jsonObject.get("url"));			
			assertEquals(mockUserAgent , jsonObject.get("user_agent"));
		} catch (AssertionFailedError t) {
			throw t;
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
	public void testLocalAudioUsage() {
		final Tracker tracker = context.mock(Tracker.class);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("deviceid", mockDeviceId);
		properties.put("srcvers", mockFirmwareVersion);
		final DeviceInfo device = new MockDeviceInfo(properties) ;
		final LocalAudioUsage usage = new LocalAudioUsage(tracker, mockAndroidContext, mockAppId, mockPackageName, device);
		context.checking(new Expectations() {{
			oneOf (tracker).log(with(same(usage)));
		}});
		usage.setDuration(mockDuration);
		usage.setResult(mockResult, mockNormalizedResult);
		usage.setTitle(mockTitle);
		usage.begin();
		usage.commit();
		context.assertIsSatisfied();
		verifyMediaUsgaeJson(usage, "local_audio");		
	}
	public void testLocalVideoUsage() {
		final Tracker tracker = context.mock(Tracker.class);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("deviceid", mockDeviceId);
		properties.put("srcvers", mockFirmwareVersion);
		final DeviceInfo device = new MockDeviceInfo(properties) ;
		final LocalVideoUsage usage = new LocalVideoUsage(tracker, mockAndroidContext, mockAppId, mockPackageName, device);
		context.checking(new Expectations() {{
			oneOf (tracker).log(with(same(usage)));
		}});
		usage.setDuration(mockDuration);
		usage.setResult(mockResult, mockNormalizedResult);
		usage.setTitle(mockTitle);
		usage.begin();
		usage.commit();
		context.assertIsSatisfied();
		verifyMediaUsgaeJson(usage, "local_video");		
	}
	private void verifyMediaUsgaeJson(final MediaUsage usage, String recordType) {
		verifyUsgaeJson(usage, recordType);
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(usage));
			assertEquals(mockResult, jsonObject.get("result"));
			assertEquals(mockNormalizedResult, jsonObject.getInt("normalized_result"));
			assertEquals(mockDuration , jsonObject.getLong("duration"));
			assertEquals(mockTitle , jsonObject.get("title"));
		} catch (AssertionFailedError t) {
			throw t;
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
	public void testWifiDisplayUsage() {
		final Tracker tracker = context.mock(Tracker.class);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("deviceid", mockDeviceId);
		properties.put("srcvers", mockFirmwareVersion);
		final DeviceInfo device = new MockDeviceInfo(properties) ;
		final WifiDisplayUsage usage = new WifiDisplayUsage(tracker, mockAndroidContext, mockAppId, mockPackageName, device);
		context.checking(new Expectations() {{
			oneOf (tracker).log(with(same(usage)));
		}});
		usage.begin();
		usage.commit();
		context.assertIsSatisfied();
		verifyUsgaeJson(usage, "wifi_display");		
	}
	private void verifyUsgaeJson(final Usage usage, String recordType) {
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(usage));
			assertEquals(recordType, jsonObject.get("type"));			
			assertEquals("2014-10-24", jsonObject.get("schema_version"));			
			assertEquals(mockAppId, jsonObject.get("app_id"));			
			assertEquals(mockPackageName, jsonObject.get("package_id"));			
			assertEquals("unknown", jsonObject.get("device_type"));			
			assertEquals(mockDeviceId, jsonObject.get("device_id"));			
			assertTrue(jsonObject.has("timestamp"));
			assertTrue(jsonObject.has("play_time"));
			assertEquals("android", jsonObject.get("app_os_type"));
			assertEquals(mockFirmwareVersion, jsonObject.get("firmware_version"));
		} catch (AssertionFailedError t) {
			throw t;
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
}
