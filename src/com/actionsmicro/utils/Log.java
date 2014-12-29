package com.actionsmicro.utils;

import com.actionsmicro.BuildConfig;

public class Log {

	public static void d(String tag, String string) {
		if (BuildConfig.DEBUG) {
			android.util.Log.d(tag, string);
		}
	}

	public static void i(String tag, String string) {
		if (BuildConfig.DEBUG) {
			android.util.Log.i(tag, string);
		}
	}

	public static void e(String tag, String string) {
		android.util.Log.e(tag, string);
	}
	public static void e(String tag, String string, Exception e) {
		android.util.Log.e(tag, string,e );
	}
	public static void v(String tag, String string) {
		if (BuildConfig.DEBUG) {
			android.util.Log.v(tag, string);
		}
	}

	public static void w(String tag, String string) {
		android.util.Log.w(tag, string);
	}

}