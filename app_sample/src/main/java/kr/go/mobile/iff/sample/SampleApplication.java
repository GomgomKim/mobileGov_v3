package kr.go.mobile.iff.sample;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.sds.BizAppLauncher.gov.aidl.MoiApplication;

public class SampleApplication extends MoiApplication {

    private String TAG = SampleApplication.class.getName();

    private String strMSMurl;
    private String strDvHost = "http://10.180.22.77:65535/MOI_API";
    private String strFlavor = "테스트베드";
    private String strVersion = "";

    public void onCreate() {
        super.onCreate();
        strMSMurl = getResources().getString(R.string.msmurl);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG,"**************** UncaughtExceptionHandler ****************");
                Log.e(TAG, Log.getStackTraceString(throwable));
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                Log.e(TAG, "**************** Process Shutdown ****************");
                Log.e(TAG, "강제 종료 발생");
            }
        });

        // application info
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            StringBuilder sb = new StringBuilder();
            sb.append(packageInfo.versionName).append("(").append(String.valueOf(packageInfo.versionCode)).append(")");
        } catch (PackageManager.NameNotFoundException e) {
            strVersion = "demo";
        }

        switch (BuildConfig.FLAVOR) {
            case "commercial":
                break;
            case "development":
                break;
            default:
                break;
        }
    }

    protected String getMSMUrl() {
        return strMSMurl;
    }

    protected String getDVHost() {
        return strDvHost;
    }

    protected String getVersion() {
        return strVersion;
    }

    protected String getFlavor() { return strFlavor; }

}
