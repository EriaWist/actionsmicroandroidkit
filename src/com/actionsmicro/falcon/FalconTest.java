package com.actionsmicro.falcon;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import junit.framework.Assert;
import junit.framework.TestCase;
import android.util.Log;

import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.falcon.Falcon.ProjectorInfo.MessageListener;
import com.actionsmicro.falcon.Falcon.SearchReultListener;

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
		assertEquals(projectorInfo.getOsVerion(), "1");
		assertEquals(projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(projectorInfo.getPasscode(), "8744");
		assertEquals(projectorInfo.getName(), "my_name");			
	}
	public void testWifiDisplayParserNoName() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744", projectorInfo));
		assertEquals(projectorInfo.getOsVerion(), "1");
		assertEquals(projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(projectorInfo.getPasscode(), "8744");
		assertTrue(projectorInfo.getName() == null);				
	}
	public void testWifiDisplayParserNoPasscode() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root:model=BENQ_GP10:passcode=", projectorInfo));
		assertEquals(projectorInfo.getOsVerion(), "1");
		assertEquals(projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(projectorInfo.getPasscode(), "");
		assertTrue(projectorInfo.getName() == null);				
	}
	public void testWifiDisplayParsereEmptySegment() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root::model=BENQ_GP10:", projectorInfo));
		assertEquals(projectorInfo.getOsVerion(), "1");
		assertEquals(projectorInfo.getModel(), "BENQ_GP10");
		assertTrue(projectorInfo.getPasscode() == null);
		assertTrue(projectorInfo.getName() == null);				
	}
	public void testWifiDisplayParserHasNameTag() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertTrue(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744:name=james", projectorInfo));
		assertEquals(projectorInfo.getOsVerion(), "1");
		assertEquals(projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(projectorInfo.getPasscode(), "8744");
		assertEquals(projectorInfo.getName(), "james");	
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
		assertEquals(projectorInfo.getOsVerion(), "2");
		assertEquals(projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(projectorInfo.getPasscode(), "8744");
		assertEquals(projectorInfo.getName(), "my_name");
		assertEquals(projectorInfo.getVendor(), "actions");
	}
	public void testWifiDisplayParserService() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		//2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:discovery=1:vendor=actions:service=0A:secret=82280189
		assertTrue(Falcon.parseWifiDisplayResponseString("2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=9b90fb44a29d232ef62759f72b7f9f2f:discovery=1:vendor=actions:service=0A", projectorInfo));
		assertEquals(projectorInfo.getOsVerion(), "2");
		assertEquals(projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(projectorInfo.getPasscode(), "8744");
		assertEquals(projectorInfo.getName(), "my_name");
		assertEquals(projectorInfo.getVendor(), "actions");
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
		assertTrue(projectorInfo.getVendor() == null);
		assertTrue(projectorInfo.getModel() == null);
	}
	public void testRemoteControlParserNew() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		Falcon.parseRemoteControlResponseString("EZREMOTE:1:vendor=actions:model=BENQ_GP10", projectorInfo);
		assertEquals(projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(projectorInfo.getVendor(), "actions");
	}
	public void testRemoteControlParserWithMessage() {
		assertEquals(Falcon.parseMessageString("STANDARD:1:vendor=actions:model=BENQ_GP10"), "vendor=actions:model=BENQ_GP10");
		assertEquals(Falcon.parseMessageString("CUSTOMER:1:vendor=actions:model=BENQ_GP10"), "vendor=actions:model=BENQ_GP10");
		assertNull(Falcon.parseMessageString("CUSTOMER:1:"));
		assertNull(Falcon.parseMessageString("CUSTOMER:1"));
		assertNull(Falcon.parseMessageString("CUSTOMER:"));
		assertNull(Falcon.parseMessageString("CUSTOMER"));
		assertEquals(Falcon.parseMessageString("EZREMOTE:1:vendor=actions:model=BENQ_GP10"), "vendor=actions:model=BENQ_GP10");
	}
	private boolean listenerGetCalled;
	private ProjectorInfo projectorInfo = null;
	public void testMessageListener() {
		try {
			final byte[] messageCommand = "CUSTOMER:1:hello world".getBytes();
			
			final DatagramSocket testerSocket = new DatagramSocket(null);
			testerSocket.setReuseAddress(true);
			testerSocket.bind(new InetSocketAddress((InetAddress)null, Falcon.EZ_REMOTE_CONTROL_PORT_NUMBER));
			
			final DatagramSocket falconSocket = new DatagramSocket(null);
			falconSocket.setReuseAddress(true);
			falconSocket.bind(new InetSocketAddress((InetAddress)null, Falcon.EZ_REMOTE_CONTROL_PORT_NUMBER));
					
			Log.d("testMessageListener", "testerSocket:" + testerSocket.getLocalAddress().getHostAddress() + ":" + testerSocket.getLocalPort());
			Log.d("testMessageListener", "falconSocket:" + falconSocket.getLocalAddress().getHostAddress() + ":" + falconSocket.getLocalPort());
			
			final Falcon falcon = new Falcon(falconSocket);
			final MessageListener listener = new ProjectorInfo.MessageListener () {

				
				@Override
				public void onReceiveMessage(ProjectorInfo projector, String message) {
					Log.d("testMessageListener", "onReceiveMessage");
					assertEquals(message, "hello world");
					listenerGetCalled = true;
				}
				
			};
			
			falcon.addSearchResultListener(new SearchReultListener() {

				@Override
				public void falconSearchDidFindProjector(Falcon falcon,
						ProjectorInfo projectorInfo) {
					Log.d("testMessageListener", "falconSearchDidFindProjector");
					FalconTest.this.projectorInfo = projectorInfo;
					projectorInfo.addMessageListener(listener);
					new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								testerSocket.send(new DatagramPacket(messageCommand, messageCommand.length, falconSocket.getLocalAddress(), falconSocket.getLocalPort()));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
					}).start();
				}
				
			});
			
			listenerGetCalled = false;
			projectorInfo = null;
			
			final byte[] command = "EZREMOTE:1:vendor=actions:model=BENQ_GP10".getBytes();
			testerSocket.send(new DatagramPacket(command, command.length, falconSocket.getLocalAddress(), falconSocket.getLocalPort()));
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			assertNotNull(projectorInfo);
			assertTrue(listenerGetCalled);
			
			listenerGetCalled = false;
			if (projectorInfo != null) {
				projectorInfo.removeMessageListener(listener);
				for (int i = 0; i < 10 ; i++) {
					new Thread(new Runnable() {
	
						@Override
						public void run() {
							try {
								testerSocket.send(new DatagramPacket(messageCommand, messageCommand.length, falconSocket.getLocalAddress(), falconSocket.getLocalPort()));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
					}).start();
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				assertFalse(listenerGetCalled);
			}
			
			
		} catch (SocketException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		
	}
}