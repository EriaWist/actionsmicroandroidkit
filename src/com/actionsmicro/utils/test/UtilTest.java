package com.actionsmicro.utils.test;

import com.actionsmicro.utils.Utils;

import junit.framework.TestCase;

public class UtilTest extends TestCase {
	protected static void setUpBeforeClass() throws Exception {
	}

	protected static void tearDownAfterClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	public void testPathUtils() {
		assertEquals("bin", Utils.getLastPathComponent("/Users/jamchen/Developer/Android/actions-micro/git/EzRemoteControlAndroid/EzControl/bin/"));
		assertEquals("jarlist.cache", Utils.getLastPathComponent("/Users/jamchen/Developer/Android/actions-micro/git/EzRemoteControlAndroid/EzControl/bin/jarlist.cache"));
		assertEquals("jarlist.cache", Utils.getLastPathComponent("jarlist.cache"));
	}
}
