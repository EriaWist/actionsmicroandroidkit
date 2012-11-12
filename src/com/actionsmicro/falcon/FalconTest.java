package com.actionsmicro.falcon;

import junit.framework.TestCase;

import com.actionsmicro.falcon.Falcon.ProjectorInfo;

public class FalconTest extends TestCase {
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
	public void testParserNormal() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseResponseString("1:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "8744");
		assertEquals(projectorInfo.name, "my_name");			
	}
	public void testParserNoName() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseResponseString("1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "8744");
		assertTrue(projectorInfo.name == null);				
	}
	public void testParserHasNameTag() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseResponseString("1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744:name=james", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "8744");
		assertEquals(projectorInfo.name, "james");			
	}
	public void testParserNoModel() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertFalse(Falcon.parseResponseString("1:10163:root:(none):3:root::passcode=8744", projectorInfo));
	}
}