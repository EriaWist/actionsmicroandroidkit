package com.actionsmicro.ezcom.test;

import java.util.HashMap;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.actionsmicro.androidkit.ezcast.AuthorizationApi;
import com.actionsmicro.androidkit.ezcast.AuthorizationApi.AuthorizationListener;
import com.actionsmicro.androidkit.ezcast.AuthorizationApi.AuthorizationListener.DeniedReason;
import com.actionsmicro.ezcom.Proxy;
import com.actionsmicro.ezcom.impl.AuthorizerImpl;
import com.actionsmicro.ezcom.test.mock.JSONRPCExpectations;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public class AuthorizerTest extends TestCase {
	Mockery context = new Mockery();
	private AuthorizationListener listener;
	private Proxy proxy;
	private AuthorizerImpl authorizer;
	protected void setUp() throws Exception {
		super.setUp();
		listener = context.mock(AuthorizationListener.class);
		proxy = context.mock(Proxy.class);
		context.checking(new Expectations() {{
			oneOf (proxy).registerRpcRequestHandler(with(aNonNull(AuthorizerImpl.class)));			
		}});
		authorizer = new AuthorizerImpl(proxy);
		authorizer.setAuthorizationListener(listener);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	public void testAuthorizationListenerReceivesMessages() {
		
		final int splitCount = 4;
		final int position = 3;
		context.checking(new Expectations() {{
			allowing (proxy).generateRpcId();
			oneOf (listener).authorizationIsGranted(authorizer, splitCount, position);

		}});
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("result", "allow");
		params.put("split_count", Long.valueOf(splitCount));
		params.put("position", Long.valueOf(position));
		authorizer.process(new JSONRPC2Request("common.answer_request_stream", params, proxy.generateRpcId()), null);

		context.checking(new Expectations() {{
			oneOf (listener).authorizationIsDenied(authorizer, DeniedReason.DENIED_BY_HOST_AUTOMATICALLY);
		}});
		params = new HashMap<String, Object>();
		params.put("result", "deny");
		params.put("reason", Long.valueOf(1));
		authorizer.process(new JSONRPC2Request("common.answer_request_stream", params, proxy.generateRpcId()), null);

		context.checking(new Expectations() {{
			oneOf (listener).authorizationIsDenied(authorizer, DeniedReason.DENIED_BY_HOST_MANUALLY);
		}});
		params = new HashMap<String, Object>();
		params.put("result", "deny");
		params.put("reason", Long.valueOf(2));
		authorizer.process(new JSONRPC2Request("common.answer_request_stream", params, proxy.generateRpcId()), null);

		context.assertIsSatisfied();
	}
	public void testAuthorizerRequestToDisplayAllowed() {
		final int splitCount = 4;
		final int position = 3;
		try {
			context.checking(new JSONRPCExpectations() {{
				allowing (proxy).generateRpcId();

				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("split_count", Long.valueOf(splitCount));
				params.put("position", Long.valueOf(position));
				HashMap<String, Object> result = new HashMap<String, Object>();
				result.put("result", "allow");
				result.put("split_count", Long.valueOf(splitCount));
				result.put("position", Long.valueOf(position));

				oneOf (proxy).sendRequest(with(requestWithMethodAndNamedParams(equal("common.request_stream"), 
						allOf(hasEntry("split_count", (Object)Long.valueOf(splitCount)), 
								hasEntry("position", (Object)Long.valueOf(position))))));
				will(returnJSONRPC2Response(result));

				oneOf (listener).authorizationIsGranted(authorizer, splitCount, position);
			}});
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		authorizer.requestToDisplay(splitCount, position);

		context.assertIsSatisfied();
	}
	public void testAuthorizerRequestToDisplayDenied() {
		final int splitCount = 4;
		final int position = 3;
		try {
			context.checking(new JSONRPCExpectations() {{
				allowing (proxy).generateRpcId();

				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("split_count", Long.valueOf(splitCount));
				params.put("position", Long.valueOf(position));
				HashMap<String, Object> result = new HashMap<String, Object>();
				result.put("result", "deny");
				result.put("reason", Long.valueOf(1));

				oneOf (proxy).sendRequest(with(requestWithMethodAndNamedParams(equal("common.request_stream"), 
						allOf(hasEntry("split_count", (Object)Long.valueOf(splitCount)), 
								hasEntry("position", (Object)Long.valueOf(position))))));
				will(returnJSONRPC2Response(result));

				oneOf (listener).authorizationIsDenied(authorizer, DeniedReason.FULLY_OCCUPIED);
			}});
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		authorizer.requestToDisplay(splitCount, position);
		context.assertIsSatisfied();

	}
	public void testAuthorizerRequestToDisplayWait() {
		final int splitCount = 4;
		final int position = 3;
		try {
			context.checking(new JSONRPCExpectations() {{
				allowing (proxy).generateRpcId();

				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("split_count", Long.valueOf(splitCount));
				params.put("position", Long.valueOf(position));
				HashMap<String, Object> result = new HashMap<String, Object>();
				result.put("result", "wait");

				oneOf (proxy).sendRequest(with(requestWithMethodAndNamedParams(equal("common.request_stream"), 
						allOf(hasEntry("split_count", (Object)Long.valueOf(splitCount)), 
								hasEntry("position", (Object)Long.valueOf(position))))));
				will(returnJSONRPC2Response(result));

				never (listener).authorizationIsDenied(with(any(AuthorizationApi.class)), with(any(DeniedReason.class)));
				never (listener).authorizationIsGranted(authorizer, splitCount, position);

			}});
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		authorizer.requestToDisplay(splitCount, position);
		context.assertIsSatisfied();
	}
	public void testAuthorizerCancelPendingRequest() {
		try {
			context.checking(new JSONRPCExpectations() {{
				allowing (proxy).generateRpcId();

				HashMap<String, Object> result = new HashMap<String, Object>();
				result.put("result", Long.valueOf(0));

				oneOf (proxy).sendRequest(with(requestWithMethod(equal("common.cancel_request_stream")))); will(returnJSONRPC2Response(result));

			}});
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		authorizer.cancelPendingRequest();
		context.assertIsSatisfied();
		
	}
}
