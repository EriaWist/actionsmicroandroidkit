package com.actionsmicro.analytics.unittest;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.actionsmicro.analytics.tracker.HashUtils;

public class AuthenticationHashTest extends TestCase {
	public void testHash() {
    	try {
			assertEquals("38e5efa1992888c6de32111f425772bf4896364b", HashUtils.SHA1("secret_key\t1413789111\t/cloud/sdk/api"));
		} catch (AssertionFailedError t) {
			throw t;
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
    }
}
