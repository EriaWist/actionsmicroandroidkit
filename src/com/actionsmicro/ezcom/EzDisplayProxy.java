package com.actionsmicro.ezcom;

import com.actionsmicro.ezcom.impl.RemoteControlImpl;

public class EzDisplayProxy extends Proxy implements RemoteControl {
	private RemoteControlImpl remoteControl;
	public EzDisplayProxy(String ipV4Address, int portNumber) {
		super(ipV4Address, portNumber);
		remoteControl = new RemoteControlImpl(this);
	}

	@Override
	public void up() {
		remoteControl.up();
	}

	@Override
	public void down() {
		remoteControl.down();
	}

	@Override
	public void left() {
		remoteControl.left();
	}

	@Override
	public void right() {
		remoteControl.right();
	}

	@Override
	public void enter() {
		remoteControl.enter();
	}

	@Override
	public void escape() {
		remoteControl.escape();
	}

	@Override
	public void enterDisplayMode() {
		remoteControl.enterDisplayMode();
	}

	@Override
	public void sendKey(int code) {
		remoteControl.sendKey(code);
	}

}
