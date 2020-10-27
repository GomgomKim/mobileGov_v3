package kr.go.mobile.support.v2;

import android.os.Bundle;

import kr.go.mobile.common.v3.CommonBasedConstants;

public class ConvertUtils {
    public static Bundle convertResult(Bundle extra) {
        extra.putString("userId", extra.getString(CommonBasedConstants.EXTRA_KEY_USER_ID));
        extra.putString("dn", extra.getString(CommonBasedConstants.EXTRA_KEY_DN));
        extra.remove(CommonBasedConstants.EXTRA_KEY_USER_ID);
        extra.remove(CommonBasedConstants.EXTRA_KEY_DN);
        return extra;
    }
}
