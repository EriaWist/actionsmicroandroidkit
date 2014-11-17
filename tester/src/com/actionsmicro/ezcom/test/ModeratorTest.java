package com.actionsmicro.ezcom.test;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.actionsmicro.ezcom.Moderator;
import com.actionsmicro.ezcom.Moderator.ModerationDelegate;
import com.actionsmicro.ezcom.Moderator.Reply;
import com.actionsmicro.ezcom.Proxy;
import com.actionsmicro.ezcom.impl.ModeratorImpl;
import com.actionsmicro.ezcom.test.mock.JSONRPCExpectations;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public class ModeratorTest extends TestCase{
	Mockery context = new Mockery();
	private Proxy proxy;
	private ModerationDelegate delegate;
	private ModeratorImpl moderator;
	protected void setUp() throws Exception {
		super.setUp();
		delegate = context.mock(Moderator.ModerationDelegate.class);
		proxy = context.mock(Proxy.class);
		context.checking(new Expectations() {{
			oneOf (proxy).registerRpcRequestHandler(with(aNonNull(ModeratorImpl.class)));			
		}});
		moderator = new ModeratorImpl(proxy);
		moderator.setModerationDelegate(delegate);
	}
	public void testReplyToRequestWithAllow() {
		final String userId = "192.168.32.10";
		try {
			context.checking(new JSONRPCExpectations() {{
				allowing (proxy).generateRpcId();
				oneOf (proxy).sendRequest(with(requestWithMethodAndNamedParams(equal("common.answer_request_stream"), 
						allOf(hasEntry("ip_address", userId), 
								hasEntry("result", "allow")))));
			}			
			});
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moderator.replyToRequest(userId, Reply.ALLOW);
		context.assertIsSatisfied();
	}
	public void testReplyToRequestWithDeny() {
		final String userId = "192.168.32.10";
		try {
			context.checking(new JSONRPCExpectations() {{
				allowing (proxy).generateRpcId();
				oneOf (proxy).sendRequest(with(requestWithMethodAndNamedParams(equal("common.answer_request_stream"), 
						allOf(hasEntry("ip_address", userId), 
								hasEntry("result", "deny")))));
			}			
			});
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moderator.replyToRequest(userId, Reply.DENY);
		context.assertIsSatisfied();
	}
	public void testModerationDelegateReplyAllow() {
		final String userId = "192.168.32.10";
		final String userName = "abcdefg";
		context.checking(new Expectations() {{
			allowing (proxy).generateRpcId();
			oneOf (delegate).userRequestToDisplay(moderator, userId, userName); will(returnValue(Reply.ALLOW));

		}});
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("ip_address", userId);
		params.put("hostname", userName);
		JSONRPC2Response response = moderator.process(new JSONRPC2Request("common.ask_request_stream", params, proxy.generateRpcId()), null);
		Map<String, Object> result = (Map<String, Object>) response.getResult();
		assertEquals(userId, result.get("ip_address"));
		assertEquals("allow", result.get("result"));
		
		context.assertIsSatisfied();

	}
	public void testModerationDelegateReplyDeny() {
		final String userId = "192.168.32.10";
		final String userName = "abcdefg";
		context.checking(new Expectations() {{
			allowing (proxy).generateRpcId();
			oneOf (delegate).userRequestToDisplay(moderator, userId, userName); will(returnValue(Reply.DENY));

		}});
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("ip_address", userId);
		params.put("hostname", userName);
		JSONRPC2Response response = moderator.process(new JSONRPC2Request("common.ask_request_stream", params, proxy.generateRpcId()), null);
		Map<String, Object> result = (Map<String, Object>) response.getResult();
		assertEquals(userId, result.get("ip_address"));
		assertEquals("deny", result.get("result"));
		
		context.assertIsSatisfied();

	}
	public void testModerationDelegateReplyWait() {
		final String userId = "192.168.32.10";
		final String userName = "abcdefg";
		context.checking(new Expectations() {{
			allowing (proxy).generateRpcId();
			oneOf (delegate).userRequestToDisplay(moderator, userId, userName); will(returnValue(Reply.WAIT));

		}});
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("ip_address", userId);
		params.put("hostname", userName);
		JSONRPC2Response response = moderator.process(new JSONRPC2Request("common.ask_request_stream", params, proxy.generateRpcId()), null);
		Map<String, Object> result = (Map<String, Object>) response.getResult();
		assertEquals(userId, result.get("ip_address"));
		assertEquals("wait", result.get("result"));
		
		context.assertIsSatisfied();

	}
	public void testModerationDelegateReceivesUserCancelRequest() {
		final String userId = "192.168.32.10";
		context.checking(new Expectations() {{
			allowing (proxy).generateRpcId();
			oneOf (delegate).userCancelPendingRequest(moderator, userId);

		}});
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("ip_address", userId);
		JSONRPC2Response response = moderator.process(new JSONRPC2Request("common.cancel_request_stream", params, proxy.generateRpcId()), null);
		Long result = (Long)response.getResult();
		assertEquals(0, result.intValue());
		context.assertIsSatisfied();		
	}
}
