package com.actionsmicro.analytics.unittest;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import junit.framework.TestCase;

import org.jmock.Mockery;
import org.json.JSONObject;

import android.test.mock.MockContext;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.analytics.device.AirPlayDeviceInfoBuilder;
import com.actionsmicro.analytics.device.EZCastDeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.airplay.AirPlayDeviceInfo;
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
	private final String mockAppId = "12:34:56:78:9A";
	private final String mockDeviceId = "02:A4:05:04:5E:FC";
	public void testAirPlayDeviceInfo() {
		final long mockFeatures = 130367356919L;
		final String mockModel = "AppleTV3,2";
		final String mockSrcvers = "210.98";
		final String mockOsBuildVersion = "12A365b";
		final String mockProtovers = "1.0";

		final ServiceInfo mockServiceInfo = new ServiceInfo() {


			@Override
			public boolean hasData() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public String getType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getTypeWithSubtype() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getKey() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getQualifiedName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getServer() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@Deprecated
			public String getHostAddress() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String[] getHostAddresses() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@Deprecated
			public InetAddress getAddress() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@Deprecated
			public InetAddress getInetAddress() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@Deprecated
			public Inet4Address getInet4Address() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@Deprecated
			public Inet6Address getInet6Address() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public InetAddress[] getInetAddresses() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Inet4Address[] getInet4Addresses() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Inet6Address[] getInet6Addresses() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getPort() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getPriority() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getWeight() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public byte[] getTextBytes() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@Deprecated
			public String getTextString() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@Deprecated
			public String getURL() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String[] getURLs() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@Deprecated
			public String getURL(String protocol) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String[] getURLs(String protocol) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte[] getPropertyBytes(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getPropertyString(String name) {
				if ("deviceid".equals(name)) {
					return mockDeviceId;
				} else if ("features".equals(name)) {
					return String.valueOf(mockFeatures);
				} else if ("model".equals(name)) {
					return mockModel;
				} else if ("srcvers".equals(name)) {
					return mockSrcvers;
				} else if ("osBuildVersion".equals(name)) {
					return mockOsBuildVersion;
				} else if ("protovers".equals(name)) {
					return mockProtovers;
				}
				return null;
			}

			@Override
			public Enumeration<String> getPropertyNames() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getNiceTextString() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setText(byte[] text) throws IllegalStateException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void setText(Map<String, ?> props)
					throws IllegalStateException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean isPersistent() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public String getDomain() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getProtocol() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getApplication() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getSubtype() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<Fields, String> getQualifiedNameMap() {
				// TODO Auto-generated method stub
				return null;
			}
			
		};
		final AirPlayDeviceInfo deviceInfo = new AirPlayDeviceInfo(mockServiceInfo) {
			
		};
		DeviceInfoBuilder<?> builder = DeviceInfoBuilder.getBuilderForDevice(mockAndroidContext, deviceInfo, mockAppId);
		assertTrue(builder instanceof AirPlayDeviceInfoBuilder);
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(builder.buildDeviceInfo()));
			assertEquals("airplay", jsonObject.get("type"));			
			assertEquals("2014-10-24", jsonObject.get("schema_version"));			
			assertEquals(mockAppId, jsonObject.get("app_id"));			
			assertEquals(packageName, jsonObject.get("package_id"));			
			assertEquals("airplay", jsonObject.get("device_type"));			
			assertEquals(mockDeviceId, jsonObject.get("device_id"));			
			assertEquals(mockFeatures, jsonObject.getLong("features"));			
			assertEquals(mockModel, jsonObject.get("model"));			
			assertEquals(mockSrcvers, jsonObject.get("srcvers"));			
			assertEquals(mockOsBuildVersion, jsonObject.get("osBuildVersion"));			
			assertEquals(mockProtovers, jsonObject.get("protovers"));			
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
	public void testEZCastDeviceInfo() {
		final ProjectorInfo projectorInfo = new ProjectorInfo() {
			
		};
		final PigeonDeviceInfo deviceInfo = new PigeonDeviceInfo(projectorInfo) {
			
		};
		DeviceInfoBuilder<?> builder = DeviceInfoBuilder.getBuilderForDevice(mockAndroidContext, deviceInfo, mockAppId);
		assertTrue(builder instanceof EZCastDeviceInfoBuilder);
		try {
			JSONObject jsonObject = new JSONObject(gson.toJson(builder.buildDeviceInfo()));
			assertEquals(mockAppId, jsonObject.get("app_id"));			
			assertEquals(packageName, jsonObject.get("package_id"));			
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
}
