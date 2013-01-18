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
	public void testWifiDisplayParserNormal() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseWifiDisplayResponseString("1:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "8744");
		assertEquals(projectorInfo.name, "my_name");			
	}
	public void testWifiDisplayParserNoName() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "8744");
		assertTrue(projectorInfo.name == null);				
	}
	public void testWifiDisplayParserNoPasscode() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root:model=BENQ_GP10:passcode=", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "");
		assertTrue(projectorInfo.name == null);				
	}
	public void testWifiDisplayParsereEmptySegment() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root::model=BENQ_GP10:", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertTrue(projectorInfo.passcode == null);
		assertTrue(projectorInfo.name == null);				
	}
	public void testWifiDisplayParserHasNameTag() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744:name=james", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "8744");
		assertEquals(projectorInfo.name, "james");	
		assertFalse(projectorInfo.supportsMediaStreaming());		
	}
	public void testWifiDisplayParserNoModel() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertFalse(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root::passcode=8744", projectorInfo));
	}
	public void testWifiDisplayParserFraudWithoutMd5() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertFalse(Falcon.parseWifiDisplayResponseString("2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:discovery=1", projectorInfo));
	}
	public void testWifiDisplayParserFraudWithWrongMd5() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertFalse(Falcon.parseWifiDisplayResponseString("2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=befb99b8eff320851dc5d2cd1b6853ee:discovery=1", projectorInfo));
	}
	public void testWifiDisplayParserMd5Checksum() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		//2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:discovery=1:vendor=actions:secret=82280189
		assertTrue(Falcon.parseWifiDisplayResponseString("2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=15269172dfe877f9e71054e36b0b6b66:discovery=1:vendor=actions", projectorInfo));
		assertEquals(projectorInfo.osVerion, "2");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "8744");
		assertEquals(projectorInfo.name, "my_name");
		assertEquals(projectorInfo.vendor, "actions");
	}
	public void testWifiDisplayParserService() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		//2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:discovery=1:vendor=actions:service=0A:secret=82280189
		assertTrue(Falcon.parseWifiDisplayResponseString("2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=9b90fb44a29d232ef62759f72b7f9f2f:discovery=1:vendor=actions:service=0A", projectorInfo));
		assertEquals(projectorInfo.osVerion, "2");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "8744");
		assertEquals(projectorInfo.name, "my_name");
		assertEquals(projectorInfo.vendor, "actions");
		assertTrue(projectorInfo.supportsMediaStreaming());
		assertFalse(projectorInfo.supportsPixViewer());
		assertTrue(projectorInfo.supportsLiveCam());
		assertFalse(projectorInfo.supportsStreamingDoc());
		assertFalse(projectorInfo.supportsSplitScreen());
		assertFalse(projectorInfo.supportsDropbox());
		assertFalse(projectorInfo.supportsWebViewer());	
	}
	public void testRemoteControlParserOld() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		Falcon.parseRemoteControlResponseString("123.12321.31232", projectorInfo);
		assertTrue(projectorInfo.vendor == null);
		assertTrue(projectorInfo.model == null);
	}
	public void testRemoteControlParserNew() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		Falcon.parseRemoteControlResponseString("EZREMOTE:1:vendor=actions:model=BENQ_GP10", projectorInfo);
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.vendor, "actions");
	}
}