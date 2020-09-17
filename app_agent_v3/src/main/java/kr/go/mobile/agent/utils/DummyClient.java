package kr.go.mobile.agent.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.service.session.UserSigned;

// 중계 서버를 통해서 인증 서버로 사용자 인증을 요청하기 위한 Dummy 클라이언트
public class DummyClient {

    public static class DummyClientException extends Exception {
        int errorCode;
        DummyClientException(int errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return this.errorCode;
        }
    }

    public static class TransactionIdGenerator {
        private static TransactionIdGenerator generator;

        private long currTimeMillis;
        private long currSeq;
        private TransactionIdGenerator() {
            currTimeMillis = System.currentTimeMillis();
            currSeq = 0;
        }

        public static TransactionIdGenerator getInstance() {
            if (generator == null) {
                generator = new TransactionIdGenerator();
            }

            synchronized(generator){
                return generator;
            }
        }

        public String nextKey() {
            synchronized (generator) {
                long tempTimeMillis = System.nanoTime();
                if (currTimeMillis == tempTimeMillis) {
                    currSeq++;
                } else {
                    currTimeMillis = tempTimeMillis;
                    currSeq = 1;
                }
            }
            return makeTransactionKey(currTimeMillis, currSeq);
        }

        private String makeTransactionKey(long millis, long seq) {
            String sSeq = "00000" + Long.toString(seq);
            sSeq = millis + sSeq;
            if (sSeq.length() > 12) {
                sSeq = sSeq.substring(sSeq.length() - 12, sSeq.length());
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA);

            return sdf.format(Calendar.getInstance().getTime()) + "-" + sSeq;
        }
    }

    public static UserAuthentication parseAuthResponse(String resp) {
        UserAuthentication auth = new UserAuthentication();
        try {
            JSONObject response = new JSONObject(new JSONObject(resp).getString("methodResponse"));
            auth.result = response.getString("result");

            JSONObject jsonData = new JSONObject(response.getString("data"));
            auth.verifyState = jsonData.get("verifyState").toString();
            auth.verifyStateCert = jsonData.get("verifyStateCert").toString();
            auth.verifyStateLDAP = jsonData.get("verifyStateLDAP").toString();
            auth.userID = jsonData.get("cn").toString();
            auth.userDN = jsonData.get("dn").toString();
            auth.ouName = jsonData.get("ou").toString();
            auth.ouCode = jsonData.get("oucode").toString();
            auth.departmentName = jsonData.get("companyName").toString();
            auth.departmentCode = jsonData.get("topOuCode").toString();
            auth.nickName = jsonData.get("displayName").toString();
            auth.code = response.getString("code");
            auth.msg = response.getString("msg");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return auth;
    }

    public static String validateUserCert(Map<String, String> property, String encodeRequestData)
            throws IOException, DummyClientException {



        HttpsURLConnection connection = getConnection();
        connection.setConnectTimeout(60000*2);
        connection.setReadTimeout(60000);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        Set<String> headerKey = property.keySet();
        for (String sKey : headerKey) {
            String sValue = property.get(sKey);
            connection.setRequestProperty(sKey, sValue);
        }

        // execute request
        OutputStream out = connection.getOutputStream();
        if (out != null) {
            out.write(encodeRequestData.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        // read response property
        String strResponseHeader = connection.getHeaderFields().toString();
        int statusCode = connection.getResponseCode();
        int contentLength = Integer.parseInt(connection.getHeaderField("Content-Length"));
        String strContentType = connection.getHeaderField("Content-Type");
        Log.e("@@@", strResponseHeader);
        Log.e("@@@", String.format("statusCode=%s, contentLength=%s, strContentType=%s",
                statusCode,
                contentLength,
                strContentType));

        ///// Read stream
        int respCode = connection.getResponseCode();
        InputStream is;
        if (respCode != 200) {
            is = connection.getErrorStream();
        } else {
            is = connection.getInputStream();
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        char[] buf = new char[10 * 1024];
        int readByte = 0;
        while ((readByte = br.read(buf)) != -1) {
            sb.append(buf, 0, readByte);
        }
        String responseData = sb.toString();
        if (respCode == 200) {
            Log.d("@@@", String.format("Length = %s, RESPONSE = %s", responseData.length(), responseData));
        } else {
            throw new DummyClientException(respCode, responseData);
        }
        return responseData;
    }

    @NotNull
    private static HttpsURLConnection getConnection() throws IOException {
        URL hostURL = new URL("https://10.1.1.40:443/mois/rpc");
        setTrustAllHosts();
        HttpsURLConnection connection = (HttpsURLConnection) hostURL.openConnection();
        connection.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        connection.setRequestMethod("POST");
        return connection;
    }

    private static void setTrustAllHosts() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                    throws java.security.cert.CertificateException {

            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                    throws java.security.cert.CertificateException {

            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }
        } };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Calendar CALENDAR = Calendar.getInstance();
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.KOREA);

    public static String generateBody(@NotNull String signedData, String encode) throws JSONException, UnsupportedEncodingException {
        // STEP 1. 사용자 인증서 검증 요청 값 생성
        JSONObject reqValues = new JSONObject();
        reqValues.put("reqType", "1");
        reqValues.put("transactionId", DATE_FORMAT.format(CALENDAR.getTime()));
        reqValues.put("signedData", signedData);

        // STEP 2. 요청 파라미터 생성
        JSONObject reqParam = new JSONObject();
        // "\" -> "" 으로 변환
        reqParam.put("reqAusData", reqValues.toString().replace("\\", ""));

        // STEP 3. 요청 파라미터 목록 생성.
        JSONArray reqParamArray = new JSONArray();
//        reqParamArray.add(reqParam);
        reqParamArray.put(reqParam);

        // STEP 4. 요청 파라미터 목록 값 등록
        JSONObject reqParams = new JSONObject();
        reqParams.put("params", reqParamArray);
        reqParams.put("id", TransactionIdGenerator.getInstance().nextKey());

        JSONObject reqMethodCall = new JSONObject();
        reqMethodCall.put("methodCall", reqParams);
        Log.e("@@@", reqMethodCall.toString());


        return URLEncoder.encode(reqMethodCall.toString(), encode);
    }

    @NotNull
    public static Map<String, String> generateHeader(Context context, String serviceID, String userId, String officeName, String officeCode, int contentLength)
            throws UnsupportedEncodingException, PackageManager.NameNotFoundException {

        Map<String, String> header = new HashMap<>();

        String agentDetail = getAgentDetail(context, userId, officeName, officeCode);
        String encodeAgentDetail = URLEncoder.encode(agentDetail, "UTF-8");

        header.put("Service-Id", serviceID);
        header.put("Host", "10.1.1.40:443");
        header.put("X-Agent-Detail", encodeAgentDetail);
        header.put("Content-Type", "application/json;charset=utf-8");
        header.put("Content-Length", Integer.toString(contentLength));

        return header;
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
