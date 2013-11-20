package com.actionsmicro.ezcom.test.matcher;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;

public class JSONRPC2RequestWithMethod extends TypeSafeMatcher<JSONRPC2Request> {
	private final Matcher<?> value;

    public JSONRPC2RequestWithMethod(Matcher<?> value) {
        this.value = value;
    }
    
	@Override
	public void describeTo(Description description) {
		description.appendText("withMethod(").appendDescriptionOf(value).appendText(")");
		
	}

	@Override
	public boolean matchesSafely(JSONRPC2Request item) {
		return value.matches(item.getMethod());
	}
	
	@Factory
    public static JSONRPC2RequestWithMethod requestWithMethod(Matcher<?> value) {
        return new JSONRPC2RequestWithMethod(value);
    }
}
