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
	public void testUploadInvalidFormat() {
		final String requestBody = "hello";
		final String mockResponse = "{\"status\":false,\"remote\":\"173.255.223.33\",\"error\":\"json format\"}";
		doTestUploadRequestHandlerOnFailed(requestBody, mockResponse, "json format");
	}
	public void testUploadInvalidRecord() {
		final String requestBody = "{\"encrypted_data\":\"incorrect\"}";
		final String mockResponse = "{\"status\":false,\"remote\":\"173.255.223.33\",\"error\":\"record format\"}";
		doTestUploadRequestHandlerOnFailed(requestBody, mockResponse, "record format");
	}
	public void testUploadRequestError() {
		final String requestBody = "{\"encrypted_data\":\"incorrect\"}";
		final String mockResponse = "{\"status\":false,\"remote\":\"173.255.223.33\",\"error\":\"decrypt\"}";
		doTestUploadRequestHandlerOnFailed(requestBody, mockResponse, "decrypt");
	}
	private void doTestUploadRequestHandlerOnFailed(final String requestBody,
			final String mockResponse, final String expectedError) {
		ActionsUploader uploader = new ActionsUploader(DEV_API_KEY, DEV_SECRET_KEY);
		final RequestHandler requestHandler = context.mock(RequestHandler.class);
		context.checking(new Expectations() {{
			oneOf (requestHandler).onInvalidJson(with(equal(expectedError)), with(equal(requestBody)));
		}});
		ResultProcessor resultProcessor = uploader.new ResultProcessor();
		try {
			resultProcessor.processResult(requestBody, requestHandler, new JSONObject(mockResponse));
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
		context.assertIsSatisfied();
	}
}
