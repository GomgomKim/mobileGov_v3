package com.infrawaretech.docviewer.utils;


public class Log {
	
	public static boolean ENABLE = false;

	public static void v(String tag, String msg) {
		if (ENABLE)
			android.util.Log.v(tag, msg);
	}
	
	public static void d(String tag, String msg) {
		if (ENABLE)
			android.util.Log.d(tag, msg);
	}
	
	public static void i(String tag, String msg) {
		android.util.Log.i(tag, msg);
	}
	
	public static void w(String tag, String msg) {
		android.util.Log.w(tag, msg);
	}
	
	public static void w(String tag, String msg, Throwable t) {
		android.util.Log.w(tag, msg, t);
	}
	
	public static void e(String tag, String msg) {
		android.util.Log.e(tag, msg);
	}
	
	public static void e(String tag, String msg, Throwable t) {
		android.util.Log.e(tag, msg, t);
	}
	
}
