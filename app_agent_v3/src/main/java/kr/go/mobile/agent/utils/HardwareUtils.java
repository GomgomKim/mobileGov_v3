package kr.go.mobile.agent.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class HardwareUtils {

    /**
     * 안드로이드 보안 정책으로 인하여 AOS 10 부터 사용할 수 없음.
     * @param ctx
     * @return
     */
    @Deprecated
    public static String getDeviceId(@NotNull Context ctx) {
        String deviceId = null;
        TelephonyManager manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            deviceId = manager.getMeid();
            if (deviceId == null)
                deviceId = manager.getImei();
        } else {
            deviceId = manager.getDeviceId();
        }
        return deviceId;
    }


    public static String getLine1Number(@NotNull Context ctx) throws Exception {
        String phoneNum = null;
        TelephonyManager manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            throw new Exception("필수 권한이 허용되어 있지 않습니다. (Permission : READ_PHONE_STATE)");
        }
        phoneNum = manager.getLine1Number();

        if(Objects.equals(ctx.getPackageName(), "kr.go.mobile.testbed.iff")) {
            if (phoneNum == null) {
                phoneNum = "dummy-01012345698";
            }
        }

        if (phoneNum.startsWith("+82"))
            phoneNum = phoneNum.replace("+82", "0");

        return phoneNum;
    }

    public static String getAndroidID(@NotNull Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
