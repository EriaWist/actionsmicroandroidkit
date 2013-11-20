package com.actionsmicro.ezcom.impl;

import java.util.HashMap;
import java.util.Map;

import com.actionsmicro.ezcom.Moderator;
import com.actionsmicro.ezcom.Proxy;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class ModeratorImpl implements Moderator, RequestHandler {
	
	private static final String PARAM_HOSTNAME = "hostname";
	private static final String METHOD_CANCEL_REQUEST_STREAM = "common.cancel_request_stream";
	private static final String METHOD_ASK_REQUEST_STREAM = "common.ask_request_stream";
	private static final String METHOD_ANSWER_REQUEST_STREAM = "common.answer_request_stream";
	private static final String PARAM_IP_ADDRESS = "ip_address";
	private static final String PARAM_RESULT = "result";
	private Proxy proxy;
	private ModerationDelegate moderationDelegate;
	public ModeratorImpl(Proxy proxy) {
		this.proxy = proxy;
		proxy.registerRpcRequestHandler(this);
	}

	public ModerationDelegate getModerationDelegate() {
		return moderationDelegate;
	}

	public void setModerationDelegate(ModerationDelegate moderationDelegate) {
		this.moderationDelegate = moderationDelegate;
	}

	@Override
	public void replyToRequest(String userId, Reply reply) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAM_RESULT, replyToString(reply));
		params.put(PARAM_IP_ADDRESS, userId);
		JSONRPC2Request request = new JSONRPC2Request(METHOD_ANSWER_REQUEST_STREAM, params, proxy.generateRpcId());
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
	private static String replyToString(Reply reply) {
		switch (reply) {
		case ALLOW:
			return "allow";
		case DENY:
			return "deny";
		case WAIT:
			return "wait";			
		}
		return "";
	}

	@Override
	public String[] handledRequests() {
		return new String[]{METHOD_ASK_REQUEST_STREAM, METHOD_CANCEL_REQUEST_STREAM};
	}

	@Override
	public JSONRPC2Response process(JSONRPC2Request request, MessageContext context) {
		String method = request.getMethod();
		Map<String, Object> params = request.getNamedParams();
		if (method.equals(METHOD_ASK_REQUEST_STREAM)) {
			return askRequestStream(request, params);
		} else if (method.equals(METHOD_CANCEL_REQUEST_STREAM)) {
			return cancelRequestStream(request, params);
		}
		return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
	}

	private JSONRPC2Response cancelRequestStream(JSONRPC2Request request,
			Map<String, Object> params) {
		if (params.containsKey(PARAM_IP_ADDRESS)) {
			String userId = (String) params.get(PARAM_IP_ADDRESS);
			if (moderationDelegate != null) {
				moderationDelegate.userCancelPendingRequest(this, userId);
			}
			return new JSONRPC2Response(Long.valueOf(0), request.getID());
		}
		return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, request.getID());
	}

	private JSONRPC2Response askRequestStream(JSONRPC2Request request,
			Map<String, Object> params) {
		if (params.containsKey(PARAM_HOSTNAME) && params.containsKey(PARAM_IP_ADDRESS)) {
			String userName = (String) params.get(PARAM_HOSTNAME);
			String userId = (String) params.get(PARAM_IP_ADDRESS);
			Reply reply = Reply.ALLOW;
			if (moderationDelegate != null) {
				reply = moderationDelegate.userRequestToDisplay(this, userId, userName);
			}
			HashMap<String, Object> result = new HashMap<String, Object>();
			result.put(PARAM_RESULT, replyToString(reply));
			result.put(PARAM_IP_ADDRESS, userId);
			return new JSONRPC2Response(result, request.getID());
		}
		return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, request.getID());
	}
}
