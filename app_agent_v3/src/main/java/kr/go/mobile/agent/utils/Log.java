package kr.go.mobile.agent.utils;


import java.util.HashMap;
import java.util.Map;

public class Log {
	
	public static boolean DEBUGABLED = false;

	public static void v(String tag, String msg) {
		if (DEBUGABLED)
			android.util.Log.v(tag, msg);
	}

	public static void d(String tag, String msg) {
		if (DEBUGABLED)
			android.util.Log.d(tag, msg);
	}
	public static void d(String tag, String msg,Throwable t) {
		if (DEBUGABLED)
			android.util.Log.d(tag, msg, t);
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
		// TODO 개별 로깅 파일에 에러메시지는 저장하자.
		android.util.Log.e(tag, msg);
	}
	
	public static void e(String tag, String msg, Throwable t) {
		// TODO 개별 로깅 파일에 에러메시지는 저장하자.
		android.util.Log.e(tag, msg, t);
	}

	static Map<String, Long> tmp = new HashMap<>();
	public static void timeStamp(String tag) {
		if (tmp.containsKey(tag)) {
			Long now = System.currentTimeMillis();
			Long old = tmp.remove(tag);
			e("TIME-STAMP", tag + " - " + (now - old) + " ms");
		} else {
			tmp.put(tag, System.currentTimeMillis());
		}
	}

	public static void concurrency(Thread t, String message) {
		if (true) return;
		w("Concurrency", t.getName() + " - " + message);
	}

	public static void call() {
		if (false) return;
		android.util.Log.d("CALLING", "CALLER: " + new Throwable().getStackTrace()[2].toString());
		android.util.Log.d("CALLING", "CALLING: " + new Throwable().getStackTrace()[1].toString());
	}
}
