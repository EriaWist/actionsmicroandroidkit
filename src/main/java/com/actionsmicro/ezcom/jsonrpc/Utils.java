package com.actionsmicro.ezcom.jsonrpc;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Message;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public class Utils {

	private static final String MIME_TYPE_APPLICATION_JSON = "application/json"/* + HTTP.CHARSET_PARAM + HTTP.UTF_8*/;
	public static HttpPost createRpcPostRequestAndPreprocess(JSONRPC2Request request, URI uri) {
		try {
			HttpPost postRequest = createRpcPostRequest(request, uri);
			HttpEntity entity = postRequest.getEntity();
			postRequest.setHeader(HTTP.CONTENT_LEN, Long.valueOf(entity.getContentLength()).toString());
			postRequest.setHeader(entity.getContentType());
			postRequest.setHeader(entity.getContentEncoding());
			postRequest.setHeader(HTTP.TARGET_HOST, uri.getHost()+":"+uri.getPort());
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
		entity.setContentType(MIME_TYPE_APPLICATION_JSON);
		entity.setContentEncoding(HTTP.UTF_8);
		postRequest.setEntity(entity);
		return postRequest;
	}

	public static void buildHttpResponse(HttpResponse httpResponse, JSONRPC2Response resp) {
		try {
			StringEntity rpcentity = new StringEntity(resp.toString(), HTTP.UTF_8);
			rpcentity.setContentType(MIME_TYPE_APPLICATION_JSON);
			rpcentity.setContentEncoding(HTTP.UTF_8);
			httpResponse.setEntity(rpcentity);
			httpResponse.setHeader(HTTP.CONTENT_LEN, Long.valueOf(rpcentity.getContentLength()).toString());
			httpResponse.setHeader(rpcentity.getContentType());
			httpResponse.setHeader(rpcentity.getContentEncoding());
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
}
