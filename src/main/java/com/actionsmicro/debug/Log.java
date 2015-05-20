package com.actionsmicro.debug;

public class Log {
	void logBytes(String tag, String prefix, byte[] buffer) {
		if (buffer.length >= 8) {
			com.actionsmicro.utils.Log.d(tag, prefix+String.format("[%02x %02x %02x %02x  %02x %02x %02x %02x]", buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7]));
		} else if (buffer.length >= 4) {
			com.actionsmicro.utils.Log.d(tag, prefix+String.format("[%02x %02x %02x %02x]", buffer[0], buffer[1], buffer[2], buffer[3]));										
		}
	}
}
