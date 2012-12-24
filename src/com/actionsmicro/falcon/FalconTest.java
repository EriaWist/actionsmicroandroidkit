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
	public void testParserFraudWithoutMd5() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertFalse(Falcon.parseResponseString("1:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:discovery=1", projectorInfo));
	}
	public void testParserFraudWithWrongMd5() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertFalse(Falcon.parseResponseString("1:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=befb99b8eff320851dc5d2cd1b6853ee:discovery=1", projectorInfo));
	}
	public void testParserMd5Checksum() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		//1:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:discovery=1:vendor=actions:secret=82280189
		assertTrue(Falcon.parseResponseString("1:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=f32bb4bf95cad9ac7a50cca2391fdb39:discovery=1:vendor=actions", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
		assertEquals(projectorInfo.model, "BENQ_GP10");
		assertEquals(projectorInfo.passcode, "8744");
		assertEquals(projectorInfo.name, "my_name");
		assertEquals(projectorInfo.vendor, "actions");
	}
	public void testParserService() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		//1:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:discovery=1:vendor=actions:service=10:secret=82280189
		assertTrue(Falcon.parseResponseString("1:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=67fc211a6033d1e84f61f8d5421b751a:discovery=1:vendor=actions:service=10", projectorInfo));
		assertEquals(projectorInfo.osVerion, "1");
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
}