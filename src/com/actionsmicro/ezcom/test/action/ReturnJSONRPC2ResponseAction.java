package com.actionsmicro.ezcom.test.action;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ReturnJSONRPC2ResponseAction implements Action {
	private Object result;

	public ReturnJSONRPC2ResponseAction(Object result) {
		this.result = result;
	}
	@Override
	public void describeTo(Description description) {
		description.appendText("returns ");
        description.appendValue(result);
	}

	@Override
	public Object invoke(Invocation invocation) throws Throwable {
		JSONRPC2Request request = (JSONRPC2Request) invocation.getParameter(0);
		return new JSONRPC2Response(result, request.getID());
	}

}
