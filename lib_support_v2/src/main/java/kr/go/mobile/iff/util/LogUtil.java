package kr.go.mobile.iff.util;

import android.util.Log;

public class LogUtil {
    // log level INFO - begin
    public static void i(Class<?> clazz, String message) {
        LogUtil.i(clazz.getSimpleName(), message, true);
    }

    public static void i(Class<?> clazz, String message, final boolean enable) {
        LogUtil.i(clazz.getSimpleName(), message, enable);
    }

    public static void i(String tag, String message) {
        LogUtil.i(tag, message, true);
    }

    public static void i(String tag, String message, final boolean enable) {
        if (enable) {
            Log.i(tag, message);
        }
    }
    // log level INFO - end

    // log level DEBUG - begin
    public static void d(Class<?> clazz, String message) {
        LogUtil.d(clazz.getSimpleName(), message, true);
    }

    public static void d(Class<?> clazz, String message, final boolean enable) {
        LogUtil.d(clazz.getSimpleName(), message, enable);
    }

    public static void d(String tag, String message) {
        LogUtil.d(tag, message, true);
    }

    public static void d(String tag, String message, boolean enable) {
        if (enable) {
            Log.d(tag, message);
        }
    }
    // log level DEBUG - end

    // log level WARN - begin
    public static void w(Class<?> clazz, String message) {
        LogUtil.w(clazz.getSimpleName(), message, null, true);
    }

    public static void w(Class<?> clazz, String message, Throwable t) {
        LogUtil.w(clazz.getSimpleName(), message, t, true);
    }

    public static void w(Class<?> clazz, String message, final boolean enable) {
        LogUtil.w(clazz.getSimpleName(), message, null, enable);
    }

    public static void w(String tag, String message) {
        LogUtil.w(tag, message, null, true);
    }

    public static void w(String tag, String message, Throwable t) {
        LogUtil.w(tag, message, t, true);
    }

    public static void w(String tag, String message, Throwable t, boolean enable) {
        if (enable) {
            Log.w(tag, message, t);
        }
    }
    // log level WARN - end

    // log level ERROR - begin
    public static void e(Class<?> clazz, String message) {
        LogUtil.e(clazz.getSimpleName(), message, null);
    }

    public static void e(Class<?> clazz, String message, Throwable t) {
        LogUtil.e(clazz.getSimpleName(), message, t);
    }

    public static void e(String tag, String message) {
        LogUtil.e(tag, message, null);
    }

    public static void e(String tag, String message, Throwable t) {
        Log.e(tag, message, t);
    }
    // log level ERROR - end
}
