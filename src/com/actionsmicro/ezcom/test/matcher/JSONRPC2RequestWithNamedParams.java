package com.actionsmicro.ezcom.test.matcher;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import android.util.Log;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;

public class JSONRPC2RequestWithNamedParams extends TypeSafeMatcher<JSONRPC2Request> {
	private final Matcher<?> value;

    public JSONRPC2RequestWithNamedParams(Matcher<?> value) {
        this.value = value;
    }
    
	@Override
	public void describeTo(Description description) {
		description.appendText("withNamedParams(").appendDescriptionOf(value).appendText(")");
		
	}

	@Override
	public boolean matchesSafely(JSONRPC2Request item) {
		Log.d("UnitTest", "getNamedParams:" + item.getNamedParams());
		return value.matches(item.getNamedParams());
	}
	
	@Factory
    public static JSONRPC2RequestWithNamedParams withNamedParams(Matcher<?> value) {
        return new JSONRPC2RequestWithNamedParams(value);
    }
}
