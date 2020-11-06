package kr.go.mobile.common.v3.broker;

class Log {
    static boolean TC = true;
    static void TC(String msg) {
        if (TC)
            android.util.Log.i("@___TEST_CASE___@", msg);
    }

    public static void e(String tag, String message, Throwable t) {
        android.util.Log.e(tag, message, t);
    }
}
