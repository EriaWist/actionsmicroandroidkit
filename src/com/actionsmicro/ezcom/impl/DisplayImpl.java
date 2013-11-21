package com.actionsmicro.ezcom.impl;

import java.util.HashMap;
import java.util.Map;

import com.actionsmicro.ezcom.Display;
import com.actionsmicro.ezcom.Proxy;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class DisplayImpl implements Display, RequestHandler {

	private static final String PARAM_POSITION = "position";
	private static final String PARAM_SPLIT_COUNT = "split_count";
	private static final String PARAM_ROLE = "role";
	private static final String METHOD_CHANGE_POSITION = "common.change_position";
	private static final String METHOD_ASSIGN_ROLE = "common.assign_role";
	private static final String METHOD_STOP_STREAM = "common.stop_stream";
	private static final String METHOD_REQUEST_STOP = "common.request_stop";
	private static final String METHOD_PLAY = "common.play";
	private static final String PARAM_ENCODING = "encoding";
	private static final String PARAM_VALUE_IMAGE_JPEG = "image/jpeg";
	private Proxy proxy;
	private DisplayListener listener;
	private Role currentRole = Role.GUEST;
	private int splitCount;
	private int position;
	public DisplayImpl(Proxy proxy) {
		this.proxy = proxy;
		proxy.registerRpcRequestHandler(this);
	}
	public DisplayListener getDisplayListener() {
		return listener;
	}
	public void setDisplayListener(DisplayListener listener) {
		this.listener = listener;
	}
	public Role getCurrentRole() {
		return currentRole;
	}
	public void setCurrentRole(Role currentRole) {
		this.currentRole = currentRole;
	}
	public int getSplitCount() {
		return splitCount;
	}
	public void setSplitCount(int splitCount) {
		this.splitCount = splitCount;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	@Override
	public void startDisplaying() {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAM_ENCODING, PARAM_VALUE_IMAGE_JPEG);
		JSONRPC2Request request = new JSONRPC2Request(METHOD_PLAY, params, proxy.generateRpcId());
		try {
			proxy.sendRequest(request);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stopDisplaying() {
		JSONRPC2Request request = new JSONRPC2Request(METHOD_REQUEST_STOP, proxy.generateRpcId());
		try {
			proxy.sendRequest(request);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public String[] handledRequests() {
		return new String[]{METHOD_STOP_STREAM, METHOD_ASSIGN_ROLE, METHOD_CHANGE_POSITION};
	}
	@Override
	public JSONRPC2Response process(JSONRPC2Request request, MessageContext context) {
		String method = request.getMethod();
		Map<String, Object> params = request.getNamedParams();
		if (method.equals(METHOD_STOP_STREAM)) {
			return stopStream(request);
		} else if (method.equals(METHOD_ASSIGN_ROLE)) {
			return assignRole(request, params);
		} else if (method.equals(METHOD_CHANGE_POSITION)) {
			return changePosition(request, params);
		}
		
		return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
	}
	private JSONRPC2Response changePosition(JSONRPC2Request request,
			Map<String, Object> params) {
		if (params.containsKey(PARAM_SPLIT_COUNT) && params.containsKey(PARAM_POSITION)) {
			setSplitCount(((Long)params.get(PARAM_SPLIT_COUNT)).intValue());
			setPosition(((Long)params.get(PARAM_POSITION)).intValue());
			if (listener != null) {
				listener.positionDidChange(this, splitCount, position);
			}				
			return new JSONRPC2Response(Long.valueOf(0), request.getID());
		}
		return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, request.getID());
	}
	private JSONRPC2Response assignRole(JSONRPC2Request request,
			Map<String, Object> params) {
		if (params.containsKey(PARAM_ROLE)) {
			setCurrentRole(roleCodeToRole((Long) params.get(PARAM_ROLE)));
			if (listener != null) {
				listener.roleDidChange(this, getCurrentRole());
			}				
			return new JSONRPC2Response(Long.valueOf(0), request.getID());
		}
		return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, request.getID());
	}
	private JSONRPC2Response stopStream(JSONRPC2Request request) {
		if (listener != null) {
			listener.remoteRequestToStopDisplaying(this);
		}
		return new JSONRPC2Response(Long.valueOf(0), request.getID());
	}
	private static Role roleCodeToRole(long roleCode) {
		if (roleCode == 1) {
			return Role.HOST;
		}
		return Role.UNDEFINED;
	}
}
