package com.actionsmicro.ezcom.impl;

import java.util.HashMap;
import java.util.Map;

import com.actionsmicro.ezcom.Authorizer;
import com.actionsmicro.ezcom.Authorizer.AuthorizationListener.DeniedReason;
import com.actionsmicro.ezcom.Proxy;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class AuthorizerImpl implements Authorizer, RequestHandler {
	private static final boolean FROM_RESPONSE = true;
	private static final String METHOD_ANSWER_REQUEST_STREAM = "common.answer_request_stream";
	private static final String METHOD_CANCEL_REQUEST_STREAM = "common.cancel_request_stream";
	private static final String PARAM_REASON = "reason";
	private static final String PARAM_VALUE_DENY = "deny";
	private static final String PARAM_VALUE_ALLOW = "allow";
	private static final String PARAM_RESULT = "result";
	private static final String PARAM_POSITION = "position";
	private static final String PARAM_SPLIT_COUNT = "split_count";
	private static final String METHOD_REQUEST_STREAM = "common.request_stream";
	private Proxy proxy;
	private AuthorizationListener listener;
	
	public AuthorizerImpl(Proxy proxy) {
		this.proxy = proxy;
		this.proxy.registerRpcRequestHandler(this);
	}

	@Override
	public void requestToDisplay(int splitCount, int position) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAM_SPLIT_COUNT, Long.valueOf(splitCount));
		params.put(PARAM_POSITION, Long.valueOf(position));
		JSONRPC2Request request = new JSONRPC2Request(METHOD_REQUEST_STREAM, params, proxy.generateRpcId());
		try {
			JSONRPC2Response response = proxy.sendRequest(request);
			if (response.indicatesSuccess()) {
				handleRequestResult((Map<String, Object>) response.getResult(), FROM_RESPONSE);
			} else {
				if (listener != null) {
					listener.authorizationIsDenied(this, DeniedReason.UNDEFINED);
				}
			}
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONRPC2SessionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean handleRequestResult(Map<String, Object> resultData, boolean fromResponse) {
		if (resultData.containsKey(PARAM_RESULT)) {
			String result = (String) resultData.get(PARAM_RESULT);
			if (result.equals(PARAM_VALUE_ALLOW) && 
					resultData.containsKey(PARAM_SPLIT_COUNT) && 
					resultData.containsKey(PARAM_POSITION)) {
				long splitCount = (Long)resultData.get(PARAM_SPLIT_COUNT);
				long position = (Long)resultData.get(PARAM_POSITION);
				if (listener != null) {
					listener.authorizationIsGranted(this, (int)splitCount, (int)position);
				}
				return true;
			} else if (result.equals(PARAM_VALUE_DENY) && resultData.containsKey(PARAM_REASON)){
				if (listener != null) {
					listener.authorizationIsDenied(this, reasonCodeToDeniedReason((Long) resultData.get(PARAM_REASON), fromResponse));
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void cancelPendingRequest() {
		JSONRPC2Request request = new JSONRPC2Request(METHOD_CANCEL_REQUEST_STREAM, proxy.generateRpcId());
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
	public AuthorizationListener getAuthorizationListener() {
		return listener;
	}

	public void setAuthorizationListener(AuthorizationListener listener) {
		this.listener = listener;
	}

	@Override
	public String[] handledRequests() {
		return new String[] {METHOD_ANSWER_REQUEST_STREAM};
	}

	@Override
	public JSONRPC2Response process(JSONRPC2Request request, MessageContext context) {
		if (request.getMethod().equals(METHOD_ANSWER_REQUEST_STREAM)) {
			return answerRequestStream(request);
		}
		return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
	}

	private JSONRPC2Response answerRequestStream(JSONRPC2Request request) {
		Map<String, Object> params = request.getNamedParams();
		if (handleRequestResult(params, !FROM_RESPONSE)) {
			return new JSONRPC2Response(Long.valueOf(0), request.getID()); 
		}
		return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, request.getID());
	}
	private static DeniedReason reasonCodeToDeniedReason(long reason, boolean fromResponse) {
		if (fromResponse) {
			if (reason == 1) {
				return DeniedReason.FULLY_OCCUPIED;
			}
		} else {
			if (reason == 1) {
				return DeniedReason.DENIED_BY_HOST_AUTOMATICALLY;
			} else if (reason == 2) {
				return DeniedReason.DENIED_BY_HOST_MANUALLY;
			}
		}
		return DeniedReason.UNDEFINED;
	}
}
