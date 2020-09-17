package com.infrawaretech.docloader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.sds.BizAppLauncher.gov.util.CertiticationUtil;
import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

class DocUtils {

    private static final String TAG = "DocUtils";

    private final String osType = "A";
    private final String osVer = android.os.Build.VERSION.RELEASE;
    private final String modelName = android.os.Build.DEVICE;;
    private String resourceVer = "0";
    private int disWidth = 0;
    private int disHeight = 0;
    private String dpi;
    private String td = "N";
    private String uuid;
    private String userId = "";
    private String officeCode = "";
    private String officeName = "";


    private static DocUtils mInstance;

    public static DocUtils getInstance() {
        if (mInstance == null)
            mInstance = new DocUtils();

        return mInstance;
    }

    private DocUtils() { /* private */ }

    public void init(final Context ctx) {
        ServiceBrokerLib serviceBroker = new ServiceBrokerLib(ctx,
                new ResponseListener() {

                    @Override
                    public void receive(ResponseEvent paramResponseEvent) {
                        // 함수가 호출된다면 에러
                        int errorCode = paramResponseEvent.getResultCode();
                        String errorMsg = paramResponseEvent.getResultData();
                        Log.e(TAG, "errorMsg :: " + errorMsg + ", errorCode :: " + errorCode);
                    }
                },
                new ServiceBrokerLib.ServiceBrokerCB() {

                    @SuppressWarnings("deprecation")
                    @Override
                    public void onServiceBrokerResponse(String retMsg) {
                        try {
                            CertiticationUtil certUtil = CertiticationUtil. parse (retMsg);
                            userId = certUtil.getInfo(CertiticationUtil.KEY_CN);
                            officeCode = certUtil.getInfo(CertiticationUtil.KEY_OU_CODE);
                            officeName = certUtil.getInfo(CertiticationUtil.KEY_OU);
                        } catch (JSONException e) {
                            Log.e(TAG, "사용자 정보 획득 중 에러가 발생하였습니다. ", e);
                        }

                        Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                        disWidth = display.getWidth();
                        disHeight = display.getHeight();
                        dpi = getDPI(ctx);
                        uuid = Settings.Secure.getString(ctx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID); //디바이스 uuid 체크
                    }

                    private String getDPI(Context ctx) {
                        String dpi = "";
                        DisplayMetrics metrics = new DisplayMetrics();
                        Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                        display.getMetrics(metrics);
                        int densityDpi = metrics.densityDpi; // 단말의 density. 120 dpi 해상도의 경우 120 값

                        if (densityDpi >= 480) {
                            dpi = "XXHDPI";
                        } else if (densityDpi == 320) {
                            dpi = "XHDPI";
                        } else if (densityDpi == 240) {
                            dpi = "HDPI";
                        } else if (densityDpi == 160) {
                            dpi = "MDPI";
                        } else if (densityDpi == 120) {
                            dpi = "LDPI";
                        } else {
                            Log.d("pjh", "getDPI() densityDpi : " + densityDpi);
                        }

                        return dpi;
                    }
                });
        CertiticationUtil cert = new CertiticationUtil();
        cert.setRequestData(CertiticationUtil.KEY_CN);
        cert.setRequestData(CertiticationUtil.KEY_OU);
        cert.setRequestData(CertiticationUtil.KEY_OU_CODE);

        // 파라미터 설정
        Intent intent = new Intent();
        // 인증 정보 조회 sCode
        intent.putExtra(ServiceBrokerLib.KEY_SERVICE_ID , ServiceBrokerLib.SERVICE_GET_INFO);
        intent.putExtra(ServiceBrokerLib. KEY_PARAMETER , cert.toRequestData());
        serviceBroker.request(intent);
    }

    public String getServiceHeader(Context context) throws PackageManager.NameNotFoundException {
        StringBuilder header = new StringBuilder();
        header.append(getBaseHeader(context));

        Log.d(TAG, "getServiceHeader() header : " + header);

        String ret;

        try {
            ret = URLEncoder.encode(header.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "getServiceHeader() UnsupportedEncodingException");
            ret = header.toString();
        }
        return ret;
    }

    private String getBaseHeader(Context ctx) throws PackageManager.NameNotFoundException {
        StringBuilder sb = new StringBuilder();
        String packageName = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_META_DATA).packageName;
        int appVer = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_META_DATA).versionCode;
        sb.append("PK=").append(packageName);
        sb.append(";AV=").append(appVer);
        sb.append(";OT=").append(osType);
        sb.append(";OV=").append(osVer);
        sb.append(";RV=").append(resourceVer);
        sb.append(";DW=").append(disWidth );
        sb.append(";DH=").append(disHeight );
        sb.append(";DPI=").append(dpi);
        sb.append(";TD=").append(td);
        sb.append(";UD=").append(uuid);
        sb.append(";UI=").append(userId);
        sb.append(";OC=").append(officeCode);
        sb.append(";OG=").append(officeName);
        sb.append(";MD=").append(modelName);

        return sb.toString();

    }


}
