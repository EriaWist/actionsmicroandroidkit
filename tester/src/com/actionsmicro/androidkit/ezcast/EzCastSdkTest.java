package com.actionsmicro.androidkit.ezcast;

import java.util.Arrays;
import java.util.List;

import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;
import android.test.mock.MockContext;

import com.actionsmicro.androidkit.ezcast.imp.airplay.AirPlayDeviceFinder;
import com.actionsmicro.androidkit.ezcast.imp.androidrx.AndroidRxFinder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.FalconDeviceFinder;

public class EzCastSdkTest extends InstrumentationTestCase {
	public void testDeviceFinderBuilderForEzCast() {
		DeviceFinder deviceFinder = new DeviceFinder(new MockContext());
		EzCastSdk.setupDeviceFinder(Arrays.asList("ezcast"), deviceFinder);
		assertEquals(1, deviceFinder.getImps().size());
		assertTrue(deviceFinder.getImps().get(0) instanceof FalconDeviceFinder);		
	}
	@UiThreadTest
	public void testDeviceFinderBuilderForGoogleCast() {
		DeviceFinder deviceFinder = new DeviceFinder(new MockContext());
		UnsupportedOperationException ex = null;
		try {
			EzCastSdk.setupDeviceFinder(Arrays.asList("chromecast"), deviceFinder);
//			11-10 16:44:33.986 I/TestRunner(11330): 	at android.test.mock.MockContext.getApplicationContext(MockContext.java:86)
//			11-10 16:44:33.986 I/TestRunner(11330): 	at android.support.v7.media.MediaRouter.getInstance(MediaRouter.java:166)		
		} catch (UnsupportedOperationException e) {
			ex = e;
		}
		assertNotNull(ex);
	}
	public void testDeviceFinderBuilderForEzCastPro() {
		DeviceFinder deviceFinder = new DeviceFinder(new MockContext());
		EzCastSdk.setupDeviceFinder(Arrays.asList("ezcastpro"), deviceFinder);
		assertEquals(1, deviceFinder.getImps().size());
		assertTrue(deviceFinder.getImps().get(0) instanceof FalconDeviceFinder);		
	}
	public void testDeviceFinderBuilderForEzCastScreen() {
		DeviceFinder deviceFinder = new DeviceFinder(new MockContext());
		EzCastSdk.setupDeviceFinder(Arrays.asList("ezscreen"), deviceFinder);
		assertEquals(1, deviceFinder.getImps().size());
		assertTrue(deviceFinder.getImps().get(0) instanceof AndroidRxFinder);		
	}
	public void testDeviceFinderBuilderForEzCastMusic() {
		DeviceFinder deviceFinder = new DeviceFinder(new MockContext());
		EzCastSdk.setupDeviceFinder(Arrays.asList("ezcastmusic"), deviceFinder);
		assertEquals(1, deviceFinder.getImps().size());
		assertTrue(deviceFinder.getImps().get(0) instanceof FalconDeviceFinder);		
	}
	public void testDeviceFinderBuilderForEzCastCar() {
		DeviceFinder deviceFinder = new DeviceFinder(new MockContext());
		EzCastSdk.setupDeviceFinder(Arrays.asList("ezcastcar"), deviceFinder);
		assertEquals(1, deviceFinder.getImps().size());
		assertTrue(deviceFinder.getImps().get(0) instanceof FalconDeviceFinder);		
	}
	public void testDeviceFinderBuilderForEzCastLite() {
		DeviceFinder deviceFinder = new DeviceFinder(new MockContext());
		EzCastSdk.setupDeviceFinder(Arrays.asList("ezcastlite"), deviceFinder);
		assertEquals(1, deviceFinder.getImps().size());
		assertTrue(deviceFinder.getImps().get(0) instanceof FalconDeviceFinder);		
	}
	public void testDeviceFinderBuilderForAirPlay() {
		DeviceFinder deviceFinder = new DeviceFinder(new MockContext());
		EzCastSdk.setupDeviceFinder(Arrays.asList("airplay"), deviceFinder);
		assertEquals(1, deviceFinder.getImps().size());
		assertTrue(deviceFinder.getImps().get(0) instanceof AirPlayDeviceFinder);		
	}
	public void testSupportJsonArrayConverter() {
		List<String> list = null;
		try {
			list = EzCastSdk.convertJsonArrayToList("[\"ezcast\",\"chromecast\",\"ezcastpro\",\"ezscreen\",\"ezcastmusic\",\"ezcastcar\",\"ezcastlite\",\"airplay\"]");
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
		assertTrue(list.contains("ezcast"));
		assertTrue(list.contains("chromecast"));
		assertTrue(list.contains("ezcastpro"));
		assertTrue(list.contains("ezscreen"));
		assertTrue(list.contains("ezcastmusic"));
		assertTrue(list.contains("ezcastcar"));
		assertTrue(list.contains("ezcastlite"));
		assertTrue(list.contains("airplay"));
	}
}
