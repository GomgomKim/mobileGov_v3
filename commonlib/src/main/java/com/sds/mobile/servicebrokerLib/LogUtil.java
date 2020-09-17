package com.sds.mobile.servicebrokerLib;

import android.util.Log;

@Deprecated
class LogUtil{
	static final String TAG = "pjh";
	static boolean debugMode = true;
	
	public LogUtil(){
	}
	
	public static void errorLog(Exception e){
		if(debugMode){
			//e.printStackTrace();
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}
	
	public static void log_e(String log){
		if(debugMode){
			Log.e(TAG, log);
		}
	}
	
	public static void log_w(String log){
		if(debugMode){
			Log.w(TAG, log);
		}
	}
	
	public static void log_i(String log){
		if(debugMode){
			Log.i(TAG, log);
		}
	}
	
	public static void log_d(String log){
		if(debugMode){
			Log.d(TAG, log);
		}
	}
}