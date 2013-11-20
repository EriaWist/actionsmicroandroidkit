package com.actionsmicro.ezcom.impl;

import java.util.HashMap;

import com.actionsmicro.ezcom.Proxy;
import com.actionsmicro.ezcom.RemoteControl;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public class RemoteControlImpl implements RemoteControl {
	private static final String METHOD_KEYBOARD = "common.keyboard";
	private static final String PARAM_KEYBOARD = "keyboard";
	private static final String METHOD_KEY = "common.key";
	private static final String PARAM_KEY = "key";
	private Proxy proxy;
	public RemoteControlImpl(Proxy proxy) {
		this.proxy = proxy;  
	}
	private void sendKey(String key) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAM_KEY, key);
		invokeMethodWithParams(METHOD_KEY, params);
	}
	private void sendKeyCode(int key) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(PARAM_KEYBOARD, Long.valueOf(key));
		invokeMethodWithParams(METHOD_KEYBOARD, params);
	}
	public void invokeMethodWithParams(String method, HashMap<String, Object> params) {
		JSONRPC2Request request = new JSONRPC2Request(method, params, proxy.generateRpcId());
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
	public void up() {
		sendKey("up");
	}
	
	@Override
	public void down() {
		sendKey("down");
	}

	@Override
	public void left() {
		sendKey("left");
	}

	@Override
	public void right() {
		sendKey("right");
	}

	@Override
	public void enter() {
		sendKey("enter");
	}

	@Override
	public void escape() {
		sendKey("esc");
	}

	@Override
	public void enterDisplayMode() {
		sendKey("display");
	}

	@Override
	public void sendKey(int code) {
		sendKeyCode(code);
	}

}
