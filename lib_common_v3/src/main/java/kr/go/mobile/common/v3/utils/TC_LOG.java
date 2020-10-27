package kr.go.mobile.common.v3.utils;

import android.os.RemoteException;
import android.util.Log;

public class TC_LOG {

    private final static boolean enabled = true;

    public static void d(String tc_id, String message) {
        if (TC_LOG.enabled)
            Log.d("TC-" + tc_id, message);
    }

    public static void e(String tc_id, String message) {
        if (TC_LOG.enabled)
            Log.e("TC-" + tc_id, message);
    }

    public static void i(String tc_id, String message) {
        if (TC_LOG.enabled)
            Log.i("TC-" + tc_id, message);
    }
}
