package com.actionsmicro.ezcom.test;

import java.util.HashMap;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.actionsmicro.ezcast.DisplayApi;
import com.actionsmicro.ezcast.DisplayApi.DisplayListener;
import com.actionsmicro.ezcast.DisplayApi.Role;
import com.actionsmicro.ezcom.Proxy;
import com.actionsmicro.ezcom.impl.DisplayImpl;
import com.actionsmicro.ezcom.test.mock.JSONRPCExpectations;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public class DisplayTest extends TestCase {
	Mockery context = new Mockery();
	private DisplayListener listener;
	private Proxy proxy;
	private DisplayImpl display;
	
	protected void setUp() throws Exception {
		super.setUp();
		listener = context.mock(DisplayApi.DisplayListener.class);
		proxy = context.mock(Proxy.class);
		context.checking(new Expectations() {{
			oneOf (proxy).registerRpcRequestHandler(with(aNonNull(DisplayImpl.class)));			
		}});
		display = new DisplayImpl(proxy);
		display.setDisplayListener(listener);
	}
	public void testStartDisplaying() {
		try {
			context.checking(new JSONRPCExpectations() {{
				allowing (proxy).generateRpcId();
				
				oneOf (proxy).sendRequest(with(requestWithMethodAndNamedParams(equal("common.play"), hasEntry("encoding", "image/jpeg"))));
			}			
			});
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		display.startDisplaying();
		
		context.assertIsSatisfied();
	}
	public void testStopDisplaying() {
		try {
			context.checking(new JSONRPCExpectations() {{
				allowing (proxy).generateRpcId();
				
				oneOf (proxy).sendRequest(with(requestWithMethod(equal("common.request_stop"))));
			}			
			});
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		display.stopDisplaying();
		
		context.assertIsSatisfied();
	}
	public void testDisplayListenerReceviesStopDisplay() {
		context.checking(new Expectations() {{
			allowing (proxy).generateRpcId();
			oneOf (listener).remoteRequestToStopDisplaying(display);
		}			
		});
		display.process(new JSONRPC2Request("common.stop_stream", proxy.generateRpcId()), null);
		context.assertIsSatisfied();
	}
	public void testDisplayListenerReceviesChangePosition() {
		final int splitCount = 4;
		final int position = 2;
		context.checking(new Expectations() {{
			allowing (proxy).generateRpcId();
			oneOf (listener).positionDidChange(display, splitCount, position);
		}			
		});
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("split_count", Long.valueOf(splitCount));
		params.put("position", Long.valueOf(position));		
		display.process(new JSONRPC2Request("common.change_position", params, proxy.generateRpcId()), null);
		context.assertIsSatisfied();
	}
	public void testDisplayListenerReceviesChangeRole() {
		context.checking(new Expectations() {{
			allowing (proxy).generateRpcId();
			oneOf (listener).roleDidChange(display, Role.HOST);
		}			
		});
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("role", Long.valueOf(1));
		display.process(new JSONRPC2Request("common.assign_role", params, proxy.generateRpcId()), null);
		context.assertIsSatisfied();
	}
}
