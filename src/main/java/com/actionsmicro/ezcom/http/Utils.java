package com.actionsmicro.ezcom.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import com.actionsmicro.utils.Log;

public class Utils {

	public static void logHttpResponse(String tag, HttpResponse rawResponse) {
		Log.d(tag, rawResponse.getStatusLine().toString());
		HeaderIterator iterator = rawResponse.headerIterator();
		while (iterator.hasNext()) {
			Header header = iterator.nextHeader();
			Log.d(tag, header.getName() + " : " + header.getValue());
		}
	}

	public static void sendRequestWithoutResponse(
	        final HttpRequest request,
	        final HttpClientConnection conn,
	        final HttpContext context)
	            throws IOException, HttpException {
	    if (request == null) {
	        throw new IllegalArgumentException("HTTP request may not be null");
	    }
	    if (conn == null) {
	        throw new IllegalArgumentException("HTTP connection may not be null");
	    }
	    if (context == null) {
	        throw new IllegalArgumentException("HTTP context may not be null");
	    }
	
	    context.setAttribute(ExecutionContext.HTTP_REQ_SENT, Boolean.FALSE);
	
	    conn.sendRequestHeader(request);
	    if (request instanceof HttpEntityEnclosingRequest) {
	        // Check for expect-continue handshake. We have to flush the
	        // headers and wait for an 100-continue response to handle it.
	        // If we get a different response, we must not send the entity.
	        boolean sendentity = true;
	        final ProtocolVersion ver = request.getRequestLine().getProtocolVersion();
	        if (((HttpEntityEnclosingRequest) request).expectContinue() &&
	            !ver.lessEquals(HttpVersion.HTTP_1_0)) {
	
	            conn.flush();                
	        }
	        if (sendentity) {
	            conn.sendRequestEntity((HttpEntityEnclosingRequest) request);
	        }
	    }
	    conn.flush();
	    context.setAttribute(ExecutionContext.HTTP_REQ_SENT, Boolean.TRUE);
	}

	public static void logHttpRequest(String tag, HttpRequest request) {
		Log.d(tag, request.getRequestLine().toString());
		HeaderIterator iterator = request.headerIterator();
		while (iterator.hasNext()) {
			Header header = iterator.nextHeader();
			Log.d(tag, header.getName() + " : " + header.getValue());
		}
	}

}
