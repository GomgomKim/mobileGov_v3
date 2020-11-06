package kr.go.mobile.common.v3;

import android.content.Context;

import kr.go.mobile.common.BuildConfig;

class Utils {
    static boolean TC = true;
    static void TC(String msg) {
        if (TC)
            android.util.Log.i("@___TEST_CASE___@", msg);
    }

    static boolean DEV_MODE(Context context) {
        String pkgBuildConfigClassName = context.getPackageName() + ".BuildConfig";
        try {
            Class<?> clsBuildConfig = context.getClassLoader().loadClass(pkgBuildConfigClassName);
            return clsBuildConfig.getField("devMode").getBoolean(clsBuildConfig);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    static String getMsmURL(Context context) {
        if (DEV_MODE(context)) {
            return BuildConfig.devMSMUrl;
        } else {
            return BuildConfig.msmUrl;
        }
    }

    static String getLauncherName(Context context) {
        if (DEV_MODE(context)) {
            return BuildConfig.devLauncherPkg;
        } else {
            return BuildConfig.launcherPkg;
        }
    }
}
