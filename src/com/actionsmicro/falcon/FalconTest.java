package com.actionsmicro.falcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import junit.framework.TestCase;
import android.os.Parcel;
import android.util.Log;

import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.falcon.Falcon.ProjectorInfo.MessageListener;
import com.actionsmicro.falcon.Falcon.SearchReultListener;

public class FalconTest extends TestCase {
	protected static void setUpBeforeClass() throws Exception {
		Log.d("FalconTest", "setUpBeforeClass");		
	}

	protected static void tearDownAfterClass() throws Exception {
	}
	private static DatagramSocket falconSocket;
	private static Falcon falcon;
	private static DatagramSocket ezRemoteSocket;
	private static DatagramSocket ezDisplaySocket;
	private static ServerSocket ezRemoteControlTcpSocket;
	protected void setUp() throws Exception {
		super.setUp();
		Log.d("FalconTest", "setUp");	
		if (ezRemoteSocket == null) {
			ezRemoteSocket = new DatagramSocket(null);
			ezRemoteSocket.setReuseAddress(true);
			ezRemoteSocket.bind(new InetSocketAddress("127.0.0.1", Falcon.EZ_REMOTE_CONTROL_PORT_NUMBER));
			Log.d("FalconTest", "ezRemoteSocket:" + ezRemoteSocket.getLocalAddress().getHostAddress() + ":" + ezRemoteSocket.getLocalPort());
		}
		if (ezDisplaySocket == null) {
			ezDisplaySocket = new DatagramSocket(null);
			ezDisplaySocket.setReuseAddress(true);
			ezDisplaySocket.bind(new InetSocketAddress("127.0.0.1", Falcon.EZ_WIFI_DISPLAY_PORT_NUMBER));
			Log.d("FalconTest", "ezDisplaySocket:" + ezDisplaySocket.getLocalAddress().getHostAddress() + ":" + ezDisplaySocket.getLocalPort());
		}
		if (falconSocket == null) {
			falconSocket = new DatagramSocket(null);
			falconSocket.setReuseAddress(true);
			falconSocket.bind(new InetSocketAddress("127.0.0.1", Falcon.EZ_REMOTE_CONTROL_PORT_NUMBER));
			Log.d("FalconTest", "falconSocket:" + falconSocket.getLocalAddress().getHostAddress() + ":" + falconSocket.getLocalPort());
		}
		if (falcon == null) {
			falcon = new Falcon() {
				@Override
				protected DatagramSocket createDatagramSocket() throws SocketException {
					return falconSocket;
				}
			};
		}
		if (ezRemoteControlTcpSocket == null) {
			ezRemoteControlTcpSocket = new ServerSocket();		
			ezRemoteControlTcpSocket.bind(new InetSocketAddress("127.0.0.1", Falcon.EZ_REMOTE_CONTROL_PORT_NUMBER));
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (falcon != null) {
			falcon.stop();
			falcon = null;
		}
		falconSocket = null;
		if (ezRemoteControlTcpSocket != null) {
			ezRemoteControlTcpSocket.close();
			ezRemoteControlTcpSocket = null;
		}
	}
	public void testWifiDisplayParserNormal() {
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "1:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744", ezDisplaySocket);
		assertNotNull(testContext.projectorInfo);
		assertEquals(testContext.projectorInfo.getOsVerion(), "1");
		assertEquals(testContext.projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(testContext.projectorInfo.getPasscode(), "8744");
		assertFalse(testContext.projectorInfo.hasNoPasscode());
		assertEquals(testContext.projectorInfo.getName(), "my_name");	
		assertEquals(testContext.projectorInfo.getWifiDisplayPortNumber(), ezDisplaySocket.getLocalPort());	
		assertFalse(testContext.projectorInfo.isRemoteControlEnabled());
	}
	public void testWifiDisplayParserNoName() {
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744", ezDisplaySocket);
		assertNotNull(testContext.projectorInfo);
		assertEquals(testContext.projectorInfo.getOsVerion(), "1");
		assertEquals(testContext.projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(testContext.projectorInfo.getPasscode(), "8744");
		assertFalse(testContext.projectorInfo.hasNoPasscode());
		assertTrue(testContext.projectorInfo.getName() == null);
		assertEquals(testContext.projectorInfo.getWifiDisplayPortNumber(), ezDisplaySocket.getLocalPort());	
		assertFalse(testContext.projectorInfo.isRemoteControlEnabled());
	}
	public void testWifiDisplayParserNoPasscode() {
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "1:10163:root:(none):3:root:model=BENQ_GP10:passcode=", ezDisplaySocket);
		assertNotNull(testContext.projectorInfo);
		assertEquals(testContext.projectorInfo.getOsVerion(), "1");
		assertEquals(testContext.projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(testContext.projectorInfo.getPasscode(), "");
		assertTrue(testContext.projectorInfo.hasNoPasscode());		
		assertTrue(testContext.projectorInfo.getName() == null);	
		assertEquals(testContext.projectorInfo.getWifiDisplayPortNumber(), ezDisplaySocket.getLocalPort());	
		assertFalse(testContext.projectorInfo.isRemoteControlEnabled());
	}
	public void testWifiDisplayParsereEmptySegment() {
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "1:10163:root:(none):3:root::model=BENQ_GP10:", ezDisplaySocket);
		assertNotNull(testContext.projectorInfo);
		assertEquals(testContext.projectorInfo.getOsVerion(), "1");
		assertEquals(testContext.projectorInfo.getModel(), "BENQ_GP10");
		assertTrue(testContext.projectorInfo.getPasscode() == null);
		assertTrue(testContext.projectorInfo.hasNoPasscode());		
		assertTrue(testContext.projectorInfo.getName() == null);
		assertEquals(testContext.projectorInfo.getWifiDisplayPortNumber(), ezDisplaySocket.getLocalPort());	
		assertFalse(testContext.projectorInfo.isRemoteControlEnabled());
	}
	public void testWifiDisplayParserHasNameTag() {
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "1:10163:root:(none):3:root:model=BENQ_GP10:passcode=8744:name=james", ezDisplaySocket);
		assertNotNull(testContext.projectorInfo);
		assertEquals(testContext.projectorInfo.getOsVerion(), "1");
		assertEquals(testContext.projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(testContext.projectorInfo.getPasscode(), "8744");
		assertFalse(testContext.projectorInfo.hasNoPasscode());
		assertEquals(testContext.projectorInfo.getName(), "james");	
		assertFalse(testContext.projectorInfo.supportsMediaStreaming());
		assertEquals(testContext.projectorInfo.getDiscoveryVersion(), 0);
		assertEquals(testContext.projectorInfo.getWifiDisplayPortNumber(), ezDisplaySocket.getLocalPort());	
		assertFalse(testContext.projectorInfo.isRemoteControlEnabled());
	}
	public void testWifiDisplayParserNoModel() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertFalse(Falcon.parseWifiDisplayResponseString("1:10163:root:(none):3:root::passcode=8744", projectorInfo));
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "1:10163:root:(none):3:root::passcode=8744", ezDisplaySocket);
		assertNull(testContext.projectorInfo);
	}
	public void testWifiDisplayParserFraudWithoutMd5() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertFalse(Falcon.parseWifiDisplayResponseString("2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:discovery=1", projectorInfo));
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:discovery=1", ezDisplaySocket);
		assertNull(testContext.projectorInfo);		
	}
	public void testWifiDisplayParserFraudWithWrongMd5() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		assertFalse(Falcon.parseWifiDisplayResponseString("2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=befb99b8eff320851dc5d2cd1b6853ee:discovery=1", projectorInfo));
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=befb99b8eff320851dc5d2cd1b6853ee:discovery=1", ezDisplaySocket);
		assertNull(testContext.projectorInfo);	
	}
	public void testWifiDisplayParserMd5Checksum() {
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=15269172dfe877f9e71054e36b0b6b66:discovery=1:vendor=actions", ezDisplaySocket);
		assertNotNull(testContext.projectorInfo);
		assertEquals(testContext.projectorInfo.getOsVerion(), "2");
		assertEquals(testContext.projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(testContext.projectorInfo.getPasscode(), "8744");
		assertFalse(testContext.projectorInfo.hasNoPasscode());
		assertEquals(testContext.projectorInfo.getName(), "my_name");
		assertEquals(testContext.projectorInfo.getVendor(), "actions");
		assertEquals(testContext.projectorInfo.getWifiDisplayPortNumber(), ezDisplaySocket.getLocalPort());	
		assertFalse(testContext.projectorInfo.isRemoteControlEnabled());
	}
	public void testWifiDisplayParserService() {
		final TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=9b90fb44a29d232ef62759f72b7f9f2f:discovery=1:vendor=actions:service=0A", ezDisplaySocket);
		assertNotNull(testContext.projectorInfo);
		assertEquals(testContext.projectorInfo.getDiscoveryVersion(), 1);
		assertEquals(testContext.projectorInfo.getOsVerion(), "2");
		assertEquals(testContext.projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(testContext.projectorInfo.getPasscode(), "8744");
		assertFalse(testContext.projectorInfo.hasNoPasscode());
		assertEquals(testContext.projectorInfo.getName(), "my_name");
		assertEquals(testContext.projectorInfo.getVendor(), "actions");
		assertTrue(testContext.projectorInfo.supportsMediaStreaming());
		assertFalse(testContext.projectorInfo.supportsPixViewer());
		assertTrue(testContext.projectorInfo.supportsLiveCam());
		assertFalse(testContext.projectorInfo.supportsStreamingDoc());
		assertFalse(testContext.projectorInfo.supportsSplitScreen());
		assertFalse(testContext.projectorInfo.supportsDropbox());
		assertFalse(testContext.projectorInfo.supportsWebViewer());	
		assertEquals(testContext.projectorInfo.getWifiDisplayPortNumber(), ezDisplaySocket.getLocalPort());	
		assertFalse(testContext.projectorInfo.isRemoteControlEnabled());

	}
	public void testRemoteControlParserOld() {
		final ProjectorInfo projectorInfo = new ProjectorInfo();
		Falcon.parseRemoteControlResponseString("127.0.0.1123123131", projectorInfo);
		assertTrue(projectorInfo.getVendor() == null);
		assertTrue(projectorInfo.getModel() == null);
		
		TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "127.0.0.1123123131", ezRemoteSocket);
		assertNotNull(testContext.projectorInfo);
		assertTrue(testContext.projectorInfo.getVendor() == null);
		assertTrue(testContext.projectorInfo.getModel() == null);
		assertTrue(testContext.projectorInfo.isRemoteControlEnabled());
	}
	public void testRemoteControlParserOld2() {
		TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "asdadalkj13", ezRemoteSocket);
		assertNull(testContext.projectorInfo);
	}
	public void testRemoteControlParserNew() {
		TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "EZREMOTE:1:vendor=actions:model=BENQ_GP10", ezRemoteSocket);
		assertEquals(testContext.projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(testContext.projectorInfo.getVendor(), "actions");
		assertTrue(testContext.projectorInfo.isRemoteControlEnabled());
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
	private class TestContext {
		boolean listenerGetCalled;
		ProjectorInfo projectorInfo = null;
		protected String message;			
	}
	public void testSearchResultListener() {
		TestContext testContext = new TestContext();
		testSearchResultListener(testContext, "EZREMOTE:1:vendor=actions:model=BENQ_GP10", ezRemoteSocket);
		assertNotNull(testContext.projectorInfo);
		assertEquals(testContext.projectorInfo.getVendor(), "actions");
		assertEquals(testContext.projectorInfo.getModel(), "BENQ_GP10");
		assertEquals(testContext.projectorInfo.getRemoteControlPortNumber(), ezRemoteSocket.getLocalPort());
		
		assertFalse(falcon.isSearching());
		falcon.search();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(falcon.isSearching());
		assertEquals(0, falcon.getProjectors().size());	
	}

	protected void testSearchResultListener(final TestContext testContext,
			final String commandString, DatagramSocket sender) {
		SearchReultListener searchReultListener = null;
		try {
			final String tag = "testSearchResultListener";
			searchReultListener = new SearchReultListener() {

				@Override
				public void falconSearchDidFindProjector(Falcon falcon,
						ProjectorInfo projectorInfo) {
					Log.d(tag, "falconSearchDidFindProjector");
					testContext.projectorInfo = projectorInfo;
					synchronized(this) {
						this.notify();
					}
				}
				
			};
			falcon.addSearchResultListener(searchReultListener);
			final byte[] command = commandString.getBytes();
			sender.send(new DatagramPacket(command, command.length, falconSocket.getLocalAddress(), falconSocket.getLocalPort()));
			
			synchronized(searchReultListener) {
				try {
					searchReultListener.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (searchReultListener != null) {
				falcon.removeSearchResultListener(searchReultListener);
			}			
		}
	}
	private class MockProjectorInfo extends ProjectorInfo {
		MockProjectorInfo(InetAddress address, int ezRemotePortNumber) {
			this.ipAddress = address;
			this.remoteControlPortNumber = ezRemotePortNumber;
		}
	}
	protected void testMessageListener(final TestContext testContext, String commandString, boolean privateListener) {
		MessageListener listener = null;
		ProjectorInfo projectorInfo = null;
		try {
			final String tag = "testMessageListener";			
			listener = new ProjectorInfo.MessageListener () {
				@Override
				public void onReceiveMessage(ProjectorInfo projector, String message) {
					Log.d(tag, "onReceiveMessage");
					testContext.message = message;
					testContext.listenerGetCalled = true;
					synchronized (this) {
						this.notify();	
					}
				}

				@Override
				public void onException(ProjectorInfo projector, Exception e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onDisconnect(ProjectorInfo projector) {
					// TODO Auto-generated method stub
					
				}				
			};

			projectorInfo = new MockProjectorInfo(ezRemoteSocket.getLocalAddress(), ezRemoteSocket.getLocalPort());
			Falcon.parseRemoteControlResponseString("EZREMOTE:1:vendor=actions:model=BENQ_GP10", projectorInfo);

			final byte[] messageCommand = commandString.getBytes();
			if (privateListener) {
				falcon.addPrivateMessageListener(projectorInfo, listener);
			} else {
				projectorInfo.addMessageListener(listener);
			}
			ezRemoteSocket.send(new DatagramPacket(messageCommand, messageCommand.length, falconSocket.getLocalAddress(), falconSocket.getLocalPort()));
			synchronized(listener) {
				try {
					listener.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (listener != null && projectorInfo != null) {
				if (privateListener) {
					falcon.removePrivateMessageListener(projectorInfo, listener);
				} else {
					projectorInfo.removeMessageListener(listener);
				}
			}
		}
	}
	public void testCustomerMessageListnener() {
		final String originalMessage = "hello world";			
		String commandString = "CUSTOMER:1:"+originalMessage;
		TestContext testContext = new TestContext();
		testMessageListener(testContext, commandString, false);
		assertTrue(testContext.listenerGetCalled);
		assertEquals(testContext.message, originalMessage);
		
		testContext = new TestContext();
		testMessageListener(testContext, commandString, true);
		assertFalse(testContext.listenerGetCalled);
	}
	public void testPrivateMessageListnener() {
		final String originalMessage = "hello world";			
		String commandString = "STANDARD:1:"+originalMessage;
		TestContext testContext = new TestContext();
		testMessageListener(testContext, commandString, false);
		assertFalse(testContext.listenerGetCalled);
		
		testContext = new TestContext();
		testMessageListener(testContext, commandString, true);
		assertTrue(testContext.listenerGetCalled);
		assertEquals(testContext.message, originalMessage);
	}
	public void testUnkownMessageListnener() {
		final String originalMessage = "hello world";			
		String commandString = "EZREMOTE:1:"+originalMessage;
		final TestContext testContext = new TestContext();
		testMessageListener(testContext, commandString, false);
		assertFalse(testContext.listenerGetCalled);
		testMessageListener(testContext, commandString, true);
		assertFalse(testContext.listenerGetCalled);
	}
	public void testMessageListener() {
		final String originalMessage = "hello world";			
		String commandString = "CUSTOMER:1:"+originalMessage;
		final TestContext testContext = new TestContext();
		MessageListener listener = null;
		ProjectorInfo projectorInfo = null;
		try {
			final String tag = "testMessageListener";			
			listener = new ProjectorInfo.MessageListener () {
				@Override
				public void onReceiveMessage(ProjectorInfo projector, String message) {
					Log.d(tag, "onReceiveMessage");
					testContext.message = message;
					testContext.listenerGetCalled = true;
					synchronized (this) {
						this.notify();	
					}
				}

				@Override
				public void onException(ProjectorInfo projector, Exception e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onDisconnect(ProjectorInfo projector) {
					// TODO Auto-generated method stub
					
				}				
			};
			
			projectorInfo = new MockProjectorInfo(ezRemoteSocket.getLocalAddress(), ezRemoteSocket.getLocalPort());
			Falcon.parseRemoteControlResponseString("EZREMOTE:1:vendor=actions:model=BENQ_GP10", projectorInfo);
			
			final byte[] messageCommand = commandString.getBytes();
			projectorInfo.addMessageListener(listener);
			ezRemoteSocket.send(new DatagramPacket(messageCommand, messageCommand.length, falconSocket.getLocalAddress(), falconSocket.getLocalPort()));
			synchronized(listener) {
				try {
					listener.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
			assertTrue(testContext.listenerGetCalled);
			assertEquals(testContext.message, originalMessage);
			
			testContext.listenerGetCalled = false;
			projectorInfo.removeMessageListener(listener);
			for (int i = 0; i < 10 ; i++) {
				ezRemoteSocket.send(new DatagramPacket(messageCommand, messageCommand.length, falconSocket.getLocalAddress(), falconSocket.getLocalPort()));
			}
			synchronized(listener) {
				try {
					listener.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			assertFalse(testContext.listenerGetCalled);
		} catch (SocketException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (listener != null && projectorInfo != null) {
				projectorInfo.removeMessageListener(listener);
			}
		}
		
	}
	public void testProjectorInfoParcelable() {
		ProjectorInfo projectorInfo = new ProjectorInfo();
		try {
			assertFalse(projectorInfo.createDatagramSocket() instanceof MockDatagramSocket);
		} catch (SocketException e) {
			e.printStackTrace();
			fail();
		}
		Falcon.parseWifiDisplayResponseString("2:10163:root:my_name:3:root:model=BENQ_GP10:passcode=8744:md5=9b90fb44a29d232ef62759f72b7f9f2f:discovery=1:vendor=actions:service=0A", projectorInfo);
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
		int hashCode = projectorInfo.hashCode();
		final Parcel encodeParcel = Parcel.obtain();
		encodeParcel.writeValue(projectorInfo);
		final byte[] marshallData = encodeParcel.marshall();
		final Parcel decodeParcel = Parcel.obtain();
		decodeParcel.unmarshall(marshallData, 0, marshallData.length);
		decodeParcel.setDataPosition(0);
		projectorInfo = (ProjectorInfo) decodeParcel.readValue(ProjectorInfo.class.getClassLoader());
		
		assertEquals("2", projectorInfo.getOsVerion());
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
		assertEquals(hashCode, projectorInfo.hashCode());
	}
	public void testProjectorInfoSendKey() {
		final int keyCode = 12;
		final ProjectorInfo projectorInfo = new MockProjectorInfo(ezRemoteControlTcpSocket.getInetAddress(), ezRemoteControlTcpSocket.getLocalPort()) {
			@Override
			protected DatagramSocket createDatagramSocket() throws SocketException {
				return new DatagramSocket() {
					@Override
					public void send (DatagramPacket pack) {
						pack.getData();
						final String [] receiveStrings = new String(pack.getData(), 0, pack.getLength(), Charset.forName("UTF-8")).split("\0");
						if (receiveStrings.length > 0) {
							assertTrue(receiveStrings[0].startsWith("1:"));
							assertEquals("1:"+keyCode, receiveStrings[0]);
						} else {
							fail();
						}
						synchronized(FalconTest.this) {
							FalconTest.this.notify();
						}
					}
				};
			}
		};
		projectorInfo.sendKey(keyCode);
		
		synchronized(this) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	private class MockDatagramSocket extends DatagramSocket {

		public MockDatagramSocket() throws SocketException {
			super();
		}
		
	}
	public void testProjectorInfoSendVendorKey() {
		final int keyCode = 12;
		final ProjectorInfo projectorInfo = new MockProjectorInfo(ezRemoteControlTcpSocket.getInetAddress(), ezRemoteControlTcpSocket.getLocalPort()) {
			@Override
			protected DatagramSocket createDatagramSocket() throws SocketException {
				return new MockDatagramSocket() {
					@Override
					public void send (DatagramPacket pack) {
						pack.getData();
						final String [] receiveStrings = new String(pack.getData(), 0, pack.getLength(), Charset.forName("UTF-8")).split("\0");
						if (receiveStrings.length > 0) {
							assertTrue(receiveStrings[0].startsWith("10:"));
							assertEquals("10:"+keyCode, receiveStrings[0]);
						} else {
							fail();
						}
						synchronized(FalconTest.this) {
							FalconTest.this.notify();
						}
					}
				};
			}
		};
		projectorInfo.sendVendorKey(keyCode);
		
		synchronized(this) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	public void testTcpRemoteMessage() {
		final String originalMessage = "hello world哈囉";			
		final String commandString = "CUSTOMER:1:"+originalMessage;
		final TestContext testContext = new TestContext();
		
		final MockProjectorInfo projectorInfo = new MockProjectorInfo(ezRemoteControlTcpSocket.getInetAddress(), ezRemoteControlTcpSocket.getLocalPort());
		Log.d("testTcpRemoteMessage", "projectorInfo:"+projectorInfo.getAddress());	
		final int keyCode = 12;
		final String expectedMessage = "1:"+keyCode;
		new Thread(new Runnable() {

			@Override
			public void run() {
				synchronized(FalconTest.this) {
					FalconTest.this.notify();
				}
				try {
					final Socket socket = ezRemoteControlTcpSocket.accept();
					Log.d("testTcpRemoteMessage", "ezRemoteControlTcpSocket accpet");	
					
					final InputStream inputStream = socket.getInputStream();
					final ByteBuffer header = ByteBuffer.allocate(4);
					header.order(ByteOrder.LITTLE_ENDIAN);
					inputStream.read(header.array(), 0, header.capacity());
					int payloadSize = header.getInt();
					assertTrue(payloadSize == expectedMessage.getBytes("UTF-8").length);
					final ByteBuffer payload = ByteBuffer.allocate(payloadSize);
					inputStream.read(payload.array(), 0, payload.capacity());
					assertEquals(expectedMessage, new String(payload.array()));
					Log.d("testTcpRemoteMessage", "ezRemoteControlTcpSocket read payload");	
					
					final OutputStream outputStream = socket.getOutputStream();
					final byte[] data = commandString.getBytes("UTF-8");
					final ByteBuffer packet = ByteBuffer.allocate(4+data.length);
					packet.order(ByteOrder.LITTLE_ENDIAN);
					packet.putInt(data.length);
					packet.put(data);
					outputStream.write(packet.array());
					outputStream.flush();
					Log.d("testTcpRemoteMessage", "ezRemoteControlTcpSocket send message" + commandString);	
					
				} catch (IOException e) {
					e.printStackTrace();
					fail();
				}
				synchronized(FalconTest.this) {
					FalconTest.this.notify();
				}
			}
			
		}).start();
		synchronized(this) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		final String tag = "testMessageListener";			
		MessageListener listener = new ProjectorInfo.MessageListener () {
			@Override
			public void onReceiveMessage(ProjectorInfo projector, String message) {
				Log.d(tag, "onReceiveMessage");
				testContext.message = message;
				testContext.listenerGetCalled = true;
				synchronized (this) {
					this.notify();	
				}
			}

			@Override
			public void onException(ProjectorInfo projector, Exception e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onDisconnect(ProjectorInfo projector) {
				// TODO Auto-generated method stub
				
			}				
		};
		projectorInfo.addMessageListener(listener);
		projectorInfo.sendKeyTcp(keyCode);
		
		synchronized(this) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		synchronized(listener) {
			try {
				listener.wait(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
		assertTrue(testContext.listenerGetCalled);
		assertEquals(testContext.message, originalMessage);
		projectorInfo.disconnectRemoteControl();
		
	}
}