package com.infrawaretech.docviewer;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.sds.mobile.servicebrokerLib.HeaderUtil;
import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.ServiceBrokerLib.ServiceBrokerCB;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import kr.go.mobile.mobp.mff.lib.util.E2ESetting;

/**
 * 중계 서버와 통신하기 위한 HEADER 데이터를 생성하기 위한 객체
 * Service Broker 를 이용하여 사용자 정보를 획득하고, 단말 정보를 로드한다.
 *
 * @version 0.0.1
 * @since 2017-01-25
 */
class DOCViewUtil implements ResponseListener, ServiceBrokerCB {

    private static final String TAG = "DOCViewUtil";

    private String osType;
    private String osVer;
    private String resourceVer = "0";
    private int disWidth = 0;
    private int disHeight = 0;
    private String dpi;
    private String td = new E2ESetting().getTestYN();
    private String uuid;
    private String userId = "";
    private String officeCode = "";
    private String officeName = "";
    private String modelName;
    private Context mContext;
    private InitCallback mInitCallback;

    private static final Object LOCK = new Object();
    private static DOCViewUtil mInstance;

    static {
        mInstance = new DOCViewUtil();
    }

    interface InitCallback {
        void onInitResult(boolean result, String msg);
    }

    static DOCViewUtil getInstance() {
        return mInstance;
    }

    private DOCViewUtil() { /* private */ }

    @Override
    public void receive(ResponseEvent paramResponseEvent) {
        // 서비스 브로커 에러 처리
        int errorCode = paramResponseEvent.getResultCode();
        String errorMsg = paramResponseEvent.getResultData();
        Log.d(TAG, "errorMsg :: " + errorMsg + ", errorCode :: " + errorCode);
    }

    @Override
    public void onServiceBrokerResponse(String resultMsg) {
        // 서비스 브로커 응답 처리
        Map<String, String> m = convert2Hash(resultMsg);
        synchronized (LOCK) {
            // 사용자 정보 설정
            userId = m.get("gov:cn");
            officeCode = m.get("gov:oucode");
            officeName = m.get("gov:ou");
        }

        osType = E2ESetting.OSTYPE;
        osVer = E2ESetting.DEVICEVERSION;
        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        disWidth = display.getWidth();
        disHeight = display.getHeight();
        modelName = android.os.Build.DEVICE;
        dpi = getDPI(mContext);
        uuid = getUUID(mContext);

        if (mInitCallback != null) {
            mInitCallback.onInitResult(true, getServiceHeader(mContext));
        }
    }

    // 서비스 브로커를 이용하여 사용자 정보를 요청
    void init(Context context, InitCallback callback) {
        mContext = context;
        mInitCallback = callback;

        ServiceBrokerLib serviceBroker = new ServiceBrokerLib(context,
                this, this);

        Intent intent = new Intent();
        intent.putExtra("dataType", "json");
        intent.putExtra("sCode", "getInfo");
        intent.putExtra("parameter", "[\"gov:cn\",\"gov:ou\",\"gov:oucode\"]");
        serviceBroker.request(intent);
    }

    String getServiceHeader(Context context) {

        StringBuilder header = new StringBuilder();

        header.append(new HeaderUtil(context).getHeader());
        header.append(getBaseHeader());

        Log.d(TAG, "getServiceHeader() header : " + header);

        String ret;

        try {
            ret = URLEncoder.encode(header.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "getServiceHeader() UnsupportedEncodingException");
            ret = header.toString();
        }
        return ret;
    }

    boolean inProgress() {
        synchronized (LOCK) {
            if (userId.equals("") || officeCode.equals("") || officeName.equals("")) {
                return true;
            }
            return false;
        }
    }

    private String getBaseHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append(";OT=").append(osType);
        sb.append(";OV=").append(osVer);
        sb.append(";RV=").append(resourceVer);
        sb.append(";DW=").append(disWidth);
        sb.append(";DH=").append(disHeight);
        sb.append(";DPI=").append(dpi);
        sb.append(";TD=").append(td);
        sb.append(";UD=").append(uuid);
        sb.append(";UI=").append(userId);
        sb.append(";OC=").append(officeCode);
        sb.append(";OG=").append(officeName);
        sb.append(";MD=").append(modelName);

        return sb.toString();

    }

    private Map<String, String> convert2Hash(String jsonString) {
        HashMap<String, String> ret = new HashMap<String, String>();
        try {
            JSONArray jArray = new JSONArray(jsonString);
            JSONObject jObject;
            String keyString;
            for (int i = 0; i < jArray.length(); i++) {
                jObject = jArray.getJSONObject(i);
                keyString = (String) jObject.names().get(0);
                ret.put(keyString, jObject.getString(keyString));
            }
        } catch (Exception e) {
            Log.e(TAG, "getInfo 데이터 파싱 중 에러가 발생하였습니다. (" + jsonString + ")", e);
        }
        return ret;
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

    private String getUUID(Context ctx) {
        String uuid = Settings.Secure.getString(ctx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID); //디바이스 uuid 체크
        return uuid;
    }
}
