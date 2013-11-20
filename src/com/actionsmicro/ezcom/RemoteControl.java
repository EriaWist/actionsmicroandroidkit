package com.actionsmicro.ezcom;

public interface RemoteControl {
	void up();
	void down();
	void left();
	void right();
	void enter();
	void escape();
	void enterDisplayMode();
	void sendKey(int code);
}
