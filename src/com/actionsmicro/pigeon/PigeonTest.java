package com.actionsmicro.pigeon;

import junit.framework.TestCase;

public class PigeonTest extends TestCase {

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
	public void testPigeonLifeCycle() {
		final String version = "1";
		final String serverAddress = "192.168.32.10";
		final int portNumber = 1234;
		final Client clientA = Pigeon.createPigeonClient(version, serverAddress, portNumber);
		final Client clientB = Pigeon.createPigeonClient(version, serverAddress, portNumber);
		assertTrue(clientA == clientB);
		Pigeon.releasePigeonClient(clientA);
		Pigeon.releasePigeonClient(clientB);
		final Client clientC = Pigeon.createPigeonClient(version, serverAddress, portNumber);
		assertFalse(clientC == clientA);
		Pigeon.releasePigeonClient(clientC);		
	}
	public void testClient() {
		final String version = "1";
		final String serverAddress = "192.168.32.10";
		final int portNumber = 1234;
		final Client client = Pigeon.createPigeonClient(version, serverAddress, portNumber);
		assertFalse(client instanceof MediaStreaming);
		assertFalse(client instanceof MultiRegionsDisplay);
		assertFalse(client instanceof ClientV2);
		assertEquals(version, client.getVersion());
		assertEquals(serverAddress, client.getServerAddress());
		assertEquals(portNumber, client.getPortNumber());
		Pigeon.releasePigeonClient(client);
	}
	public void testClientV2() {
		final String version = "2";
		final String serverAddress = "192.168.32.10";
		final int portNumber = 1234;
		final Client client = Pigeon.createPigeonClient(version, serverAddress, portNumber);
		assertTrue(client instanceof MediaStreaming);
		assertTrue(client instanceof MultiRegionsDisplay);
		assertTrue(client instanceof ClientV2);
		assertEquals(version, client.getVersion());
		assertEquals(serverAddress, client.getServerAddress());
		assertEquals(portNumber, client.getPortNumber());
		Pigeon.releasePigeonClient(client);
	}
}
