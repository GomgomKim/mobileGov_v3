package kr.go.mobile.support.v2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import kr.go.mobile.common.v3.CommonBasedConstants;

public class Utils {
    public static Bundle insertV2Type(Bundle extra) {
        extra.putString("userId", extra.getString(CommonBasedConstants.EXTRA_KEY_USER_ID));
        extra.putString("dn", extra.getString(CommonBasedConstants.EXTRA_KEY_DN));
        return extra;
    }

    public static void sendFinishApp(Context context) {
        Intent intent = new Intent("kr.go.mobile.ACTION_CONTROL");
        intent.putExtra("extra_type", 100 /*KILL*/);
        context.sendBroadcast(intent);
    }
}
