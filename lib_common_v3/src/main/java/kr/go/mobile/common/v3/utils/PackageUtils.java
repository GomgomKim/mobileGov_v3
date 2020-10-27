package kr.go.mobile.common.v3.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class PackageUtils {
    public static boolean isInstalledPackage(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getApplicationInfo(package_name, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

}
