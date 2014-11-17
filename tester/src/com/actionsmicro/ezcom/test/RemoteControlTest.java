package com.actionsmicro.ezcom.test;

import java.util.Map;

import junit.framework.TestCase;

import com.actionsmicro.ezcom.BasicProxy;
import com.actionsmicro.ezcom.impl.RemoteControlImpl;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public class RemoteControlTest extends TestCase {
	RemoteControlImpl remoteControl;
	JSONRPC2Request catchedRequest;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		remoteControl = new RemoteControlImpl(new BasicProxy("", 0) {
			@Override
			public JSONRPC2Response sendRequest(JSONRPC2Request request) throws JSONRPC2SessionException, IllegalStateException {
				catchedRequest = request;
				return new JSONRPC2Response(Integer.valueOf(0));
			}
		});
	}
	private void verifyRequest(String method, String paramKey, Object paramValue) {
		assertNotNull(catchedRequest);
		if (catchedRequest != null) {
			assertEquals(method, catchedRequest.getMethod());
			assertNotNull(catchedRequest.getNamedParams());
			Map<String, Object> namedParams = catchedRequest.getNamedParams();
			if (namedParams != null) {
				assertTrue(namedParams.containsKey(paramKey));
				assertEquals(paramValue, namedParams.get(paramKey));
			}
		}
	}
	public void testUp() {
		remoteControl.up();
		verifyRequest("common.key", "key", "up");
	}
	public void testDown() {
		remoteControl.down();
		verifyRequest("common.key", "key", "down");
	}
	public void testLeft() {
		remoteControl.left();
		verifyRequest("common.key", "key", "left");
	}
	public void testRight() {
		remoteControl.right();
		verifyRequest("common.key", "key", "right");
	}
	public void testEnter() {
		remoteControl.enter();
		verifyRequest("common.key", "key", "enter");
	}
	public void testEscape() {
		remoteControl.escape();
		verifyRequest("common.key", "key", "esc");
	}
	public void testEnterDisplaymode() {
		remoteControl.enterDisplayMode();
		verifyRequest("common.key", "key", "display");
	}
	public void testSendKey() {
		remoteControl.sendKey(-1);
		verifyRequest("common.keyboard", "keyboard", Long.valueOf(-1));
	}
}
