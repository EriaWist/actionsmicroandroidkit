package com.actionsmicro.ezcom.jsonrpc;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;

import com.actionsmicro.utils.Log;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Message;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public class Utils {

	public static HttpPost createRpcPostRequestAndPreprocess(JSONRPC2Request request, URI uri) {
		try {
			HttpPost postRequest = createRpcPostRequest(request, uri);
			HttpEntity entity = postRequest.getEntity();
			postRequest.setHeader(HTTP.CONTENT_LEN, Long.valueOf(entity.getContentLength()).toString());
			postRequest.setHeader(HTTP.CONTENT_TYPE, entity.getContentType().getValue());
			postRequest.setHeader(HTTP.CONTENT_ENCODING, entity.getContentEncoding().getValue());
			
			return postRequest;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally {
	
		}
		return null;
	}
	public static HttpPost createRpcPostRequest(final JSONRPC2Message request, URI uri)
			throws UnsupportedEncodingException {
		HttpPost postRequest = new HttpPost(uri);
		StringEntity entity = new StringEntity(request.toString(), HTTP.UTF_8);
		entity.setContentType("application/json" + HTTP.CHARSET_PARAM + HTTP.UTF_8);
		entity.setContentEncoding(HTTP.UTF_8);
		postRequest.setEntity(entity);
		return postRequest;
	}

	public static void buildHttpResponse(HttpResponse httpResponse, JSONRPC2Response resp) {
		try {
			StringEntity rpcentity = new StringEntity(resp.toString(), HTTP.UTF_8);
			rpcentity.setContentType("application/json" + HTTP.CHARSET_PARAM + HTTP.UTF_8);
			rpcentity.setContentEncoding(HTTP.UTF_8);
			httpResponse.setEntity(rpcentity);
			httpResponse.setHeader(HTTP.CONTENT_LEN, Long.valueOf(rpcentity.getContentLength()).toString());
			httpResponse.setHeader(HTTP.CONTENT_TYPE, rpcentity.getContentType().getValue());
			httpResponse.setHeader(HTTP.CONTENT_ENCODING, rpcentity.getContentEncoding().getValue());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally {
	
		}
	}

	public static boolean matchRequestResponseId(final JSONRPC2Request request, JSONRPC2Response response) {
		Object reqID = request.getID();
		Object resID = response.getID();

		if (reqID != null && resID != null && reqID.toString().equals(resID.toString()) ) {
			// ok
		} else if (reqID == null && resID == null) {
			// ok
		} else if (! response.indicatesSuccess() && ( response.getError().getCode() == -32700 ||
				response.getError().getCode() == -32600 ||
				response.getError().getCode() == -32603    )) {
			// ok
		} else {
			return false;
		}
		return true;
	}
	public static boolean matchRequestResponseIdAndThrow(final JSONRPC2Request request, JSONRPC2Response response) throws JSONRPC2SessionException {
		if (!matchRequestResponseId(request, response)) {
			throw new JSONRPC2SessionException(
					"Invalid JSON-RPC 2.0 response: ID mismatch: Returned " + 
							response.getID() + ", expected " + request.getID(),
							JSONRPC2SessionException.BAD_RESPONSE);
		}
		return true;
	}
	public static void logHttpRequest(String tag, HttpRequest request) {
		Log.d(tag, request.getRequestLine().toString());
		HeaderIterator iterator = request.headerIterator();
		while (iterator.hasNext()) {
			Header header = iterator.nextHeader();
			Log.d(tag, header.getName() + " : " + header.getValue());
		}
	}
	public static void logHttpResponse(String tag, HttpResponse rawResponse) {
		Log.d(tag, rawResponse.getStatusLine().toString());
		HeaderIterator iterator = rawResponse.headerIterator();
		while (iterator.hasNext()) {
			Header header = iterator.nextHeader();
			Log.d(tag, header.getName() + " : " + header.getValue());
		}
	}
}
