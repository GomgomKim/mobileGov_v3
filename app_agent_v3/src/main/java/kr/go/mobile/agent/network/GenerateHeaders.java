package kr.go.mobile.agent.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class GenerateHeaders {

    Context context;
    Map<String, String> defaultHeaders = new HashMap<String,String>();
    URL url;
    String serviceId;
    String encodeAgentDetail;
    String contentType;
    int contentLength;

    private final static String TAG = GenerateHeaders.class.getSimpleName();

    public GenerateHeaders(Context context){
        this.context = context;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setAgentDetail(String userId, String officeName, String officeCode) throws UnsupportedEncodingException, PackageManager.NameNotFoundException {
        String agentDetail = getAgentDetail(context, userId, officeName, officeCode);
        encodeAgentDetail = URLEncoder.encode(agentDetail, "UTF-8");
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    /*
   헤더 제작
    */
    public Map<String, String> initDefaultHeader() {
        defaultHeaders.put("Host", url.getHost() + (url.getPort() > 0 ? ":"+url.getPort() : ""));
        defaultHeaders.put("Service-Id", this.serviceId);
        defaultHeaders.put("X-Agent-Detail", this.encodeAgentDetail);

        return defaultHeaders;
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = initDefaultHeader();

        if(this.contentType == null) {
            throw new IllegalArgumentException("Content-Type 이 지정되어 있지 않습니다.");
        } else {
            headers.put("Content-Type", this.contentType);
        }

        headers.put("Content-Length", Integer.toString(contentLength));

        return headers;
    }

    private static String getAgentDetail(Context context, String userId, String officeName, String officeCode) throws PackageManager.NameNotFoundException {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        Point pt = new Point();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getSize(pt);
        display.getMetrics(metrics);

        int densityDpi = metrics.densityDpi; // 단말의 density. 120 dpi 해상도의 경우 120 값
        String dpi = "HDPI";
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
        }
        String uuid = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        String FORMAT = ";FLD=I;PK=%s;AV=%s;OT=%s;OV=%s;RV=%s;DW=%s;DH=%s;DPI=%s;TD=N;UD=%s,UI=%s;OC=%s;OG=%s;MD=%s";

//        String userId = userSession.getUserID();
//        String officeName = userSession.getOfficeName();
//        String officeCode = userSession.getOfficeCode();
        String versionCode;
        StringBuilder agentDetail = new StringBuilder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            versionCode = Long.toString(info.getLongVersionCode());
        } else {
            versionCode = Integer.toString(info.versionCode);
        }
        agentDetail.append(String.format(FORMAT,
                info.packageName,
                versionCode,
                "A"/*OS_TYPE*/,
                android.os.Build.VERSION.RELEASE/*OS_VERSION*/,
                "0"/*RESOURCE_VERSION*/,
                pt.x /*DISPLAY_WIDTH*/,
                pt.y /*DISPLAY_HEIGHT*/,
                dpi /*DISPLAY_DENSITY_DPI*/,
                uuid,
                userId,
                officeCode,
                officeName,
                android.os.Build.DEVICE));
        return agentDetail.toString();
    }

}
