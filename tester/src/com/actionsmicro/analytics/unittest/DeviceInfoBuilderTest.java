package com.actionsmicro.analytics.unittest;

import java.util.HashMap;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.jmock.Mockery;
import org.json.JSONException;
import org.json.JSONObject;

import android.test.mock.MockContext;
import android.util.Log;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.analytics.device.AirPlayDeviceInfoBuilder;
import com.actionsmicro.analytics.device.EZCastDeviceInfoBuilder;
import com.actionsmicro.analytics.device.EZCastFamilyDeviceTypeBuilder;
import com.actionsmicro.analytics.device.EZCastScreenDeviceInfoBuilder;
import com.actionsmicro.analytics.device.GoogleCastDeviceInfoBuilder;
import com.actionsmicro.analytics.unittest.mock.MockServiceInfo;
import com.actionsmicro.androidkit.ezcast.imp.airplay.AirPlayDeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.androidrx.AndroidRxInfo;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.androidkit.ezcast.imp.googlecast.GoogleCastDeviceInfo;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.google.android.gms.cast.CastDevice;
import com.google.gson.Gson;

public class DeviceInfoBuilderTest extends TestCase {
	private static final String TAG = "DeviceInfoBuilderTest";
	final Mockery context = new Mockery();
	final Gson gson = new Gson();
	final String mockPackageName = "com.mock.app";
	final MockContext mockAndroidContext = new MockContext() {
		@Override
		public String getPackageName() {
			return mockPackageName;
		}
	};
	private final String mockAppId = "12:34:56:78:9A";
	private final String mockDeviceId = "02:A4:05:04:5E:FC";
	public void testAirPlayDeviceInfo() {
		final String mockFeatures = "0x5A7FFFF7,0x1E";
		final String mockModel = "AppleTV3,2";
		final String mockSrcvers = "210.98";
		final String mockOsBuildVersion = "12A365b";
		final String mockProtovers = "1.0";
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("deviceid", mockDeviceId);
		properties.put("features", mockFeatures);
		properties.put("model", mockModel);
		properties.put("srcvers", mockSrcvers);
		properties.put("osBuildVersion", mockOsBuildVersion);
		properties.put("protovers", mockProtovers);
		
		final ServiceInfo mockServiceInfo = new MockServiceInfo(properties);
		final AirPlayDeviceInfo deviceInfo = new AirPlayDeviceInfo(mockServiceInfo) {
			
		};
		DeviceInfoBuilder<?> builder = DeviceInfoBuilder.getBuilderForDevice(mockAndroidContext, deviceInfo, mockAppId);
		assertTrue(builder instanceof AirPlayDeviceInfoBuilder);
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(builder.buildDeviceInfo()));
			Log.d(TAG, jsonObject.toString());
			assertEquals("airplay", jsonObject.get("type"));			
			assertEquals("2014-10-24", jsonObject.get("schema_version"));			
			assertEquals(mockAppId, jsonObject.get("app_id"));			
			assertEquals(mockPackageName, jsonObject.get("package_id"));			
			assertEquals("airplay", jsonObject.get("device_type"));			
			assertEquals(mockDeviceId, jsonObject.get("device_id"));			
			assertEquals(mockFeatures, jsonObject.get("features"));			
			assertEquals(mockModel, jsonObject.get("model"));			
			assertEquals(mockSrcvers, jsonObject.get("srcvers"));			
			assertEquals(mockOsBuildVersion, jsonObject.get("osBuildVersion"));			
			assertEquals(mockProtovers, jsonObject.get("protovers"));			
			assertTrue(jsonObject.has("timestamp"));
		} catch (AssertionFailedError t) {
			throw t;
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
	public void testEZCastDeviceInfoBuilderFactory() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
			@Override
			public final String getParameter(String key) {
				if ("deviceid".equals(key)) {
					return mockDeviceId;
				}
				return null;
			}			
		};
		DeviceInfoBuilder<?> builder = DeviceInfoBuilder.getBuilderForDevice(mockAndroidContext, deviceInfo, mockAppId);
		assertTrue(builder instanceof EZCastDeviceInfoBuilder);
	}
	public void testEZCastDeviceInfo() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
			@Override
			public final String getParameter(String key) {
				if ("deviceid".equals(key)) {
					return mockDeviceId;
				}
				return null;
			}			
		};
		final String mockEncryptedData = "4RJEOH4aTbG9";
		EZCastDeviceInfoBuilder builder = new EZCastDeviceInfoBuilder(mockAndroidContext, deviceInfo, mockAppId) {
			@Override
			protected JSONObject getDongleInfo() {
				try {
					return new JSONObject("{\"encrypted_data\":\""+mockEncryptedData+"\"}");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(builder.buildDeviceInfo()));
			assertEquals("device", jsonObject.get("type"));			
			assertEquals("2014-10-24", jsonObject.get("schema_version"));			
			assertEquals(mockAppId, jsonObject.get("app_id"));			
			assertEquals(mockPackageName, jsonObject.get("package_id"));			
			assertEquals("ezcast", jsonObject.get("device_type"));			
			assertEquals(mockDeviceId, jsonObject.get("device_id"));			
			assertEquals(mockEncryptedData, jsonObject.get("encrypted_data"));			
			assertTrue(jsonObject.has("timestamp"));
		} catch (AssertionFailedError t) {
			throw t;
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
	public void testEZCastScreenDeviceInfo() {
		final String mockSrcvers = "20140515";
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("deviceid", mockDeviceId);
		properties.put("srcvers", mockSrcvers);
		
		final ServiceInfo mockServiceInfo = new MockServiceInfo(properties);
		final AndroidRxInfo deviceInfo = new AndroidRxInfo(mockServiceInfo) {
			
		};
		DeviceInfoBuilder<?> builder = DeviceInfoBuilder.getBuilderForDevice(mockAndroidContext, deviceInfo, mockAppId);
		assertTrue(builder instanceof EZCastScreenDeviceInfoBuilder);
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(builder.buildDeviceInfo()));
			assertEquals("ezscreen", jsonObject.get("type"));			
			assertEquals("2014-10-24", jsonObject.get("schema_version"));			
			assertEquals(mockAppId, jsonObject.get("app_id"));			
			assertEquals(mockPackageName, jsonObject.get("package_id"));			
			assertEquals("ezscreen", jsonObject.get("device_type"));			
			assertEquals(mockDeviceId, jsonObject.get("device_id"));			
			assertEquals(mockSrcvers, jsonObject.get("srcvers"));			
			assertTrue(jsonObject.has("timestamp"));
		} catch (AssertionFailedError t) {
			throw t;
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
	public void testGoogleCastDeviceInfo() {
		final String mockSrcvers = "20140515";
		final GoogleCastDeviceInfo deviceInfo = new GoogleCastDeviceInfo((CastDevice) null) {
			@Override
			public String getParameter(String key) {
				if (key.equalsIgnoreCase("deviceid")) {
					return mockDeviceId;
				}
				if (key.equalsIgnoreCase("srcvers")) {
					return mockSrcvers;
				}
				return null;
			}
		};
		
		DeviceInfoBuilder<?> builder = DeviceInfoBuilder.getBuilderForDevice(mockAndroidContext, deviceInfo, mockAppId);
		assertTrue(builder instanceof GoogleCastDeviceInfoBuilder);
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(gson.toJson(builder.buildDeviceInfo()));
			assertEquals("chromecast", jsonObject.get("type"));			
			assertEquals("2014-10-24", jsonObject.get("schema_version"));			
			assertEquals(mockAppId, jsonObject.get("app_id"));			
			assertEquals(mockPackageName, jsonObject.get("package_id"));			
			assertEquals("chromecast", jsonObject.get("device_type"));			
			assertEquals(mockDeviceId, jsonObject.get("device_id"));			
			assertEquals(mockSrcvers, jsonObject.get("device_version"));
			assertTrue(jsonObject.has("timestamp"));
		} catch (AssertionFailedError t) {
			Log.d(TAG, jsonObject.toString());
			throw t;
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
	public void testEZCastFamilyDeviceTypeBuilderNoFamily() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
			@Override
			public final String getParameter(String key) {
				return null;
			}	
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
					
		};
		assertEquals("ezcast", EZCastFamilyDeviceTypeBuilder.getType(deviceInfo));
	}
	public void testEZCastFamilyDeviceTypeBuilderEZCastFamilyNoType() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
			@Override
			public final String getParameter(String key) {
				if ("family".equals(key)) {
					return "ezcast";
				}
				return null;
			}
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
						
		};
		assertEquals("ezcast", EZCastFamilyDeviceTypeBuilder.getType(deviceInfo));
	}
	public void testEZCastFamilyDeviceTypeBuilderEZCastFamilyEZCastType() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
			@Override
			public final String getParameter(String key) {
				if ("family".equals(key)) {
					return "ezcast";
				} else if ("type".equals(key)) {
					return "ezcast";
				}
				return null;
			}
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
						
		};
		assertEquals("ezcast", EZCastFamilyDeviceTypeBuilder.getType(deviceInfo));
	}
	public void testEZCastFamilyDeviceTypeBuilderEZCastFamilyLiteType() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
			@Override
			public final String getParameter(String key) {
				if ("family".equals(key)) {
					return "ezcast";
				} else if ("type".equals(key)) {
					return "lite";
				}
				return null;
			}
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
						
		};
		assertEquals("ezcastlite", EZCastFamilyDeviceTypeBuilder.getType(deviceInfo));
	}
	public void testEZCastFamilyDeviceTypeBuilderEZCastFamilyMusicType() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
			@Override
			public final String getParameter(String key) {
				if ("family".equals(key)) {
					return "ezcast";
				} else if ("type".equals(key)) {
					return "music";
				}
				return null;
			}
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
						
		};
		assertEquals("ezcastmusic", EZCastFamilyDeviceTypeBuilder.getType(deviceInfo));
	}
	public void testEZCastFamilyDeviceTypeBuilderEZCastFamilyCarType() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
			@Override
			public final String getParameter(String key) {
				if ("family".equals(key)) {
					return "ezcast";
				} else if ("type".equals(key)) {
					return "car";
				}
				return null;
			}
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
						
		};
		assertEquals("ezcastcar", EZCastFamilyDeviceTypeBuilder.getType(deviceInfo));
	}
	public void testEZCastFamilyDeviceTypeBuilderEZCastProFamily() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
			@Override
			public String getParameter(String key) {
				if ("family".equals(key)) {
					return "ezcastpro";
				} else if ("type".equals(key)) {
					return "box";
				}
				return null;
			}
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
						
		};
		assertEquals("ezcastpro", EZCastFamilyDeviceTypeBuilder.getType(deviceInfo));
	}
}