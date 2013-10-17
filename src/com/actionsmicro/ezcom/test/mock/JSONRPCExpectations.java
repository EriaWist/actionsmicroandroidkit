package com.actionsmicro.ezcom.test.mock;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.api.Action;

import com.actionsmicro.ezcom.test.action.ReturnJSONRPC2ResponseAction;
import com.actionsmicro.ezcom.test.matcher.JSONRPC2RequestWithMethod;
import com.actionsmicro.ezcom.test.matcher.JSONRPC2RequestWithNamedParams;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;

public class JSONRPCExpectations extends Expectations {
	/**
	 * Evaluates to true only if ALL of the passed in matchers evaluate to true.
	 */
	public static <T> org.hamcrest.Matcher<T> allOf(org.hamcrest.Matcher<? extends T>... matchers) {
		return Matchers.allOf(matchers);
	}

	/**
	 * Evaluates to true only if ALL of the passed in matchers evaluate to true.
	 */
	public static <T> org.hamcrest.Matcher<T> allOf(java.lang.Iterable<org.hamcrest.Matcher<? extends T>> matchers) {
		return Matchers.allOf(matchers);
	}
	
	public static <K, V> org.hamcrest.Matcher<java.util.Map<K, V>> hasEntry(K key, V value) {
		return Matchers.hasEntry(key, value);
	}
	
	public static JSONRPC2RequestWithMethod withMethod(Matcher<?> method) {
		return JSONRPC2RequestWithMethod.withMethod(method);
	}
	
	public static JSONRPC2RequestWithNamedParams withNamedParams(Matcher<?> value) {
		return JSONRPC2RequestWithNamedParams.withNamedParams(value);
	}
	
	public static Matcher<? extends JSONRPC2Request> withMethodAndNamedParams(org.hamcrest.Matcher<?> method, org.hamcrest.Matcher<?> namedParams) {
		return JSONRPCExpectations.<JSONRPC2Request>allOf(withMethod(method), withNamedParams(namedParams));
	}
	// actions
	public static Action returnJSONRPC2Response(Object result) {
        return new ReturnJSONRPC2ResponseAction(result);
    }
}
