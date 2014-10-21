package com.actionsmicro.analytics.unittest;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.json.JSONObject;

import com.actionsmicro.analytics.tracker.ActionsUploader;
import com.actionsmicro.analytics.tracker.ActionsUploader.RequestHandler;
import com.actionsmicro.analytics.tracker.ActionsUploader.ResultProcessor;

public class UploaderTest extends TestCase {
	private static final String DEV_SECRET_KEY = "dev_secret_key";
	private static final String DEV_API_KEY = "dev_api_key";
	final Mockery context = new Mockery();
	public void testUploadInvalidFormat() {
		ActionsUploader uploader = new ActionsUploader(DEV_API_KEY, DEV_SECRET_KEY);
		final String requestBody = "hello";
		final RequestHandler requestHandler = context.mock(RequestHandler.class);
		context.checking(new Expectations() {{
			oneOf (requestHandler).onInvalidJson(with(equal("json format")), with(equal(requestBody)));
		}});
		ResultProcessor resultProcessor = uploader.new ResultProcessor();
		try {
			resultProcessor.processResult(requestBody, requestHandler, new JSONObject("{\"status\":false,\"remote\":\"173.255.223.33\",\"error\":\"json format\"}"));
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
		context.assertIsSatisfied();
	}
	public void testUploadSuccess() {
		ActionsUploader uploader = new ActionsUploader(DEV_API_KEY, DEV_SECRET_KEY);
		final String requestBody = "{\"type\":\"app\"}";
		final RequestHandler requestHandler = context.mock(RequestHandler.class);
		context.checking(new Expectations() {{
			oneOf (requestHandler).onSuccuess(with(equal(requestBody)));
		}});
		ResultProcessor resultProcessor = uploader.new ResultProcessor();
		try {
			resultProcessor.processResult(requestBody, requestHandler, new JSONObject("{\"status\":true,\"remote\":\"173.255.223.33\"}"));
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
		context.assertIsSatisfied();
	}
}
