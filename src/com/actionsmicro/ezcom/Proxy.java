package com.actionsmicro.ezcom;

import java.io.InputStream;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public interface Proxy {

	public abstract int generateRpcId();

	public abstract JSONRPC2Response sendRequest(JSONRPC2Request request)
			throws JSONRPC2SessionException, IllegalStateException;

	public abstract void sendNotification(JSONRPC2Notification notification)
			throws JSONRPC2SessionException, IllegalStateException;

	public abstract void registerRpcRequestHandler(RequestHandler requestHandler)
			throws IllegalStateException;

	public abstract void sendJpegEncodedScreenData(InputStream input,
			long length);

}