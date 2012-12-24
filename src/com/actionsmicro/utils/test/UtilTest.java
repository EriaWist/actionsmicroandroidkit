package com.actionsmicro.utils.test;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.Arrays;

import junit.framework.TestCase;

import com.actionsmicro.utils.Utils;

@SuppressWarnings("deprecation")
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
	public void testGetLastPathComponent() {
		assertEquals("bin", Utils.getLastPathComponent("/Users/jamchen/Developer/Android/actions-micro/git/EzRemoteControlAndroid/EzControl/bin/"));
		assertEquals("jarlist.cache", Utils.getLastPathComponent("/Users/jamchen/Developer/Android/actions-micro/git/EzRemoteControlAndroid/EzControl/bin/jarlist.cache"));
		assertEquals("jarlist.cache", Utils.getLastPathComponent("jarlist.cache"));
	}
	public void testConvertInputStreamToString() {
		final String inputString = "d;alkjd;lkjaD;LKJ JJJDAS;LJD;J;ldjsaldkj";
		try {
			assertEquals(inputString, Utils.convertInputStreamToString(new StringBufferInputStream(inputString)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void testMakeTimeString() {
		final String longFormatString = "%1$d:%3$02d:%5$02d";
		final String shortFormatString = "%2$d:%5$02d";
		assertEquals(Utils.makeTimeString(longFormatString, 1), "0:00:01");
		assertEquals(Utils.makeTimeString(longFormatString, -1), "-0:00:01");
		assertEquals(Utils.makeTimeString(shortFormatString, 1), "0:01");
		assertEquals(Utils.makeTimeString(shortFormatString, -1), "-0:01");
		assertEquals(Utils.makeTimeString(longFormatString, 59), "0:00:59");
		assertEquals(Utils.makeTimeString(longFormatString, -59), "-0:00:59");
		assertEquals(Utils.makeTimeString(shortFormatString, 59), "0:59");
		assertEquals(Utils.makeTimeString(shortFormatString, -59), "-0:59");
		assertEquals(Utils.makeTimeString(longFormatString, 60), "0:01:00");
		assertEquals(Utils.makeTimeString(longFormatString, -60), "-0:01:00");
		assertEquals(Utils.makeTimeString(shortFormatString, 60), "1:00");
		assertEquals(Utils.makeTimeString(shortFormatString, -60), "-1:00");
		assertEquals(Utils.makeTimeString(longFormatString, 3600), "1:00:00");
		assertEquals(Utils.makeTimeString(longFormatString, -3600), "-1:00:00");
		assertEquals(Utils.makeTimeString(shortFormatString, 3600), "60:00");
		assertEquals(Utils.makeTimeString(shortFormatString, -3600), "-60:00");
	}
	public void testConcatStringsWithSeparator() {
		final String strings[] = {"brian", "Jesse", "Sanders", "James"};
		assertEquals(Utils.concatStringsWithSeparator(Arrays.asList(strings), ":"), "brian:Jesse:Sanders:James");
	}
	public void testMd5() {
		assertEquals(Utils.md5("1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744"), "befb99b8eff320851dc5d2cd1b6853ee");
	}
}
