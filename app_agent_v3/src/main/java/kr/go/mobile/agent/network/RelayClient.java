package kr.go.mobile.agent.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.DownloadFile;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;
import kr.go.mobile.agent.service.broker.MethodResponse;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.mobp.iff.BuildConfig;
import kr.go.mobile.mobp.iff.R;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Retrofit2 을 이용한 중계 클라이언트 구현<br>
 * build 및 인터페이스 연동
 *
 * @author Tom
 * @since 2020.09.14
 * @version 1.0
 */
public class RelayClient {

    // RelayClient 를 생성하기 위한 Builder
    public static class Builder {

        private final Context baseContext;
        private final String baseUrl;
        private int connTimeout;
        private int readTimeout;
        private KeyManager[] keyManager;
        private TrustManager[] trustAllCerts;
        private SecureRandom secureRandom;
        private String userId;
        private String officeName;

        public Builder(Context baseContext, String baseURL) {
            this.baseContext = baseContext;
            this.baseUrl = baseURL;
        }

        public Builder setConnectTimeout(int timeout) {
            this.connTimeout = timeout;
            return this;
        }

        public Builder setReadTimeout(int timeout) {
            this.readTimeout = timeout;
            return this;
        }

        public Builder setKeyManager(KeyManager[] km) {
            this.keyManager = km;
            return this;
        }

        public Builder setTrustManager(TrustManager[] trustAllCerts) {
            this.trustAllCerts = trustAllCerts;
            return this;
        }

        public Builder setSecureRandom(SecureRandom secureRandom) {
            this.secureRandom = secureRandom;
            return this;
        }

        public Builder setUserId(String userID) {
            this.userId = userID;
            return this;
        }

        public Builder setOfficeName(String officeName) {
            this.officeName = officeName;
            return this;
        }

        public RelayClient build() throws KeyManagementException,
                NoSuchAlgorithmException, MalformedURLException {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManager, trustAllCerts, secureRandom);

            return new RelayClient(baseContext, new URL(baseUrl), genDefaultAgentDetail(),
                    connTimeout, readTimeout,
                    sslContext, trustAllCerts);
        }

        private String genDefaultAgentDetail() {

            Display display = ((WindowManager) baseContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

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
            String uuid = Settings.Secure.getString(baseContext.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);


            return String.format(";OT=%s;OV=%s;RV=%s;DW=%s;DH=%s;DPI=%s;TD=N;UD=%s;UI=%s;OC=%s;OG=%s;MD=%s;FLD=I",
                    "A"/*OS_TYPE*/,
                    Build.VERSION.RELEASE/*OS_VERSION*/,
                    "0"/*RESOURCE_VERSION*/,
                    pt.x /*DISPLAY_WIDTH*/,
                    pt.y /*DISPLAY_HEIGHT*/,
                    dpi /*DISPLAY_DENSITY_DPI*/,
                    uuid,
                    userId,
                    "0000000" /*default value*/,
                    officeName,
                    Build.DEVICE);
        }
    }

    private final static String TAG = RelayClient.class.getSimpleName();

    final Context context;
    final String baseHost;
    final RelayClientInterface relayClientInterface;
    String defaultHeaderAgentDetail;

    private RelayClient(Context context, URL baseURL, String headerAgentDetail,
                        int connectTimeout, int readTimeout, SSLContext sslContext, TrustManager[] trustAllCerts) {

        this.context = context;
        this.baseHost = baseURL.getHost() + (baseURL.getPort() > 0 ? ":"+baseURL.getPort() : "");
        this.defaultHeaderAgentDetail = headerAgentDetail;

        // okHttp 설정
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });

        if (BuildConfig.DEBUG) {
            // 통신 중 일어나는 로그를 인터셉트하는 Interceptor
            HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(@NotNull String log) {
                    Log.d("RelayClientHttp-Log", log);
                }
            });
            logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logInterceptor); // 로그 활성화
        }

        // using Json
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURL.toString())
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(builder.build())
                .build();

        // 레트로핏 객체에 관리 인터페이스 연결
        relayClientInterface = retrofit.create(RelayClientInterface.class);
    }


    // 인증서버로부터 획득한 기관코드 값을 설정한다.
    public void setOfficeCode(String officeCode) {
        // ex. OC=0000000 -> OC=1741000
        defaultHeaderAgentDetail = defaultHeaderAgentDetail.replace("OC=0000000", "OC=" + officeCode);
        Log.TC("공통기반 시스템 통신 준비");
    }

    // 행정 서비스 인터페이스 연결 (공통기반 라이브러리의 기본 기능)
    public void relayDefault(String reqPackageInfo, String serviceId, String body, final IBrokerServiceCallback callback) throws UnsupportedEncodingException {
        Log.i(TAG, "행정 서비스 요청 : " + reqPackageInfo + ", ServiceId : " + serviceId);
        String encAgentDetail = getHeaderAgentDetail(reqPackageInfo);;
        RequestBody requestBody = new RelayRequestBody.DefaultBuilder()
                .setServiceParams(body)
                .build();

        Call<ResponseBody> call = relayClientInterface.callDefault(serviceId, baseHost, encAgentDetail, requestBody);
        Log.TC("클라이언트 >>(기관 서비스 요청)>> 서버");
        call.enqueue(new RelayClientCallback(context, RelayResponseParser.RESULT_TYPE.DEFAULT, callback));
    }

    // 사용자 인증정보 요청 (공통기반 라이브러리에서는 사용 못함)
    public void relayUserAuth(String reqPackageInfo, String base64, final IBrokerServiceCallback callback) throws UnsupportedEncodingException, JSONException {
        Log.i(TAG, "사용자 인증 요청 : " + reqPackageInfo);
        String encAgentDetail = getHeaderAgentDetail(reqPackageInfo);
        RequestBody requestBody = new RelayRequestBody.AuthBuilder()
                .setSigned(base64)
                .build();

        Call<ResponseBody> call = relayClientInterface.callAuth(baseHost, encAgentDetail, requestBody);
        Log.TC("클라이언트 >>(사용자 인증 요청)>> 서버");
        call.enqueue(new RelayClientCallback(context, RelayResponseParser.RESULT_TYPE.AUTHENTICATION, callback));
    }

    // 문서 변환 요청
    public void relayLoadConvertedDoc(String reqPackageInfo, String body, final IBrokerServiceCallback callback) throws UnsupportedEncodingException {
        Log.i(TAG, "문서변환 요청 : " + reqPackageInfo);

        String encodeHeader = getHeaderAgentDetail(reqPackageInfo);
        RequestBody requestBody = new RelayRequestBody.DefaultBuilder()
                .setServiceParams(body)
                .build();

        Call<ResponseBody> call = relayClientInterface.callLoadDocument(baseHost, encodeHeader, requestBody);
        Log.TC("클라이언트 >>(문서변환)>> 서버");
        call.enqueue(new RelayClientCallback(context, RelayResponseParser.RESULT_TYPE.DOCUMENT, callback));
    }

    // 파일업로드
    public BrokerResponse<?> relayUploadData(String reqPackageInfo, String boundaryId, String body,
                                          String relayUrl, String fileName, byte[] uploadBytes) throws IOException {
        Log.i(TAG, "업로드 요청 : " + reqPackageInfo);

        RelayRequestBody.MultiPartBuilder builder = new RelayRequestBody.MultiPartBuilder(boundaryId)
                .setUploadURL(relayUrl)
                .addFile(fileName, uploadBytes)
                .addParam("file", fileName);

        String[] tmp = body.split("[&]");
        for (String bodyParams : tmp) {
            String[] splitData = bodyParams.trim().split("[=]");
            if (splitData.length == 1) {
                builder.addParam(splitData[0]);
            } else {
                builder.addParam(splitData[0], splitData[1]);
            }
        }

        String encAgentDetail = getHeaderAgentDetail(reqPackageInfo);
        MultipartBody multipartBody = builder.build();
        String multipartContent = builder.getContentType();

        Call<ResponseBody> call = relayClientInterface.callUpload(multipartContent, baseHost, encAgentDetail, multipartBody);

        long beginTime = System.currentTimeMillis();
        Log.TC("클라이언트 >>(업로드 요청)>> 서버");
        Response<ResponseBody> response = call.execute();
        long endTime = System.currentTimeMillis();

        BrokerResponse<?> brokerResponse = handleResponse(response);
        Log.TC("서버 >> 응답데이터 -> 객체변환");
        try {
            sendReport(encAgentDetail, fileName, uploadBytes.length, beginTime, endTime);
        } catch (JSONException e) {
            Log.e(TAG, "업로드 결과 보고서를 생성할 수 없습니다.", e);
        }

        return brokerResponse;
    }

    // 파일 다운로드
    public BrokerResponse<?> relayDownloadData(String reqPackageInfo, String body,
                                               String relayUrl, OutputStream outputStream) throws IOException {
        Log.i(TAG, "다운로드 요청 : " + reqPackageInfo);

        RequestBody requestBody = new RelayRequestBody.DownloadBuilder()
                .setDownloadURL(relayUrl)
                .setServiceParams(body)
                .build();

        String encAgentDetail = getHeaderAgentDetail(reqPackageInfo);

        Call<ResponseBody> call = relayClientInterface.callDownload(baseHost, encAgentDetail, requestBody);

        long beginTime = System.currentTimeMillis();
        Log.TC("클라이언트 >>(다운로드 요청)>> 서버");
        Response<ResponseBody> response = call.execute();
        long endTime = System.currentTimeMillis();

        BrokerResponse<?> brokerResponse = handleResponse(response, outputStream);
        Log.TC("서버 >> 응답데이터 -> 객체변환");
        try {
            JSONObject o = new JSONObject(brokerResponse.getResult().getServiceServerResponse());
            sendReport(encAgentDetail, o.getString("filename"), o.getInt("size"), beginTime, endTime);
        } catch (JSONException e) {
            Log.e(TAG, "다운로드 결과 보고서를 생성할 수 없습니다.", e);
        }

        return brokerResponse;
    }

    private String getHeaderAgentDetail(String reqPackageInfo) throws UnsupportedEncodingException {
        return URLEncoder.encode(reqPackageInfo + defaultHeaderAgentDetail, "UTF-8");
    }

    private BrokerResponse<?> handleResponse(Response<ResponseBody> response) {
        return handleResponse(response, null);
    }

    private BrokerResponse<?> handleResponse(Response<ResponseBody> response, OutputStream out) {
        if(response.isSuccessful()) {
            try {
                MethodResponse respData;
                if (out == null) {
                    // upload (default)
                    respData = RelayResponseParser.parse(RelayResponseParser.RESULT_TYPE.DEFAULT, response);
                } else {
                    // download
                    respData = RelayResponseParser.parse(RelayResponseParser.RESULT_TYPE.DOWNLOAD, response);
                }
                if (respData.relayServerOK()) {
                    if (respData instanceof DownloadFile) {
                        if (out != null) {
                            DownloadFile downloadFile = (DownloadFile) respData;
                            try {
                                out.write(respData.getResponseBytes(), 0, downloadFile.getContentLength());
                                out.flush();
                            } catch (IOException e) {
                                Log.e(TAG+"-syncHandler", "응답 데이터를 이용하여 파일 생성 중 에러가 발생하였습니다." + "(" + e.getMessage() + ")", e);
                                return new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_INVALID_RESPONSE,
                                        "응답 데이터를 이용하여 파일 생성 중 에러가 발생하였습니다." + "(" + e.getMessage() + ")");
                            } finally {
                                try {
                                    out.close();
                                } catch (IOException ignored) {
                                }
                            }
                            MethodResponse tmpResp = new MethodResponse();
                            tmpResp.setServiceServerResponse(downloadFile.getServiceServerResponse());
                            return new BrokerResponse<>(tmpResp);
                        } else {
                            Log.e(TAG+"-syncHandler", "응답 데이터를 이용하여 파일 생성 중 에러가 발생하였습니다. (output = null)");
                            return new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_INVALID_RESPONSE,
                                    "응답 데이터를 이용하여 파일 생성 중 에러가 발생하였습니다. (output = null)");
                        }
                    } else {
                        return new BrokerResponse<>(respData);
                    }
                } else {
                    Log.e(TAG+"-syncHandler", context.getString(R.string.BROKER_ERROR_RELAY_SERVER)
                            + "(message : " + respData.getRelayServerMessage() + ", code : " + respData.getRelayServerCode() + ")");
                    return new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_RELAY_SYSTEM, context.getString(R.string.BROKER_ERROR_RELAY_SERVER)
                            + "(message : " + respData.getRelayServerMessage() + ", code : " + respData.getRelayServerCode() + ")");
                }
            } catch (JSONException | IOException e) {
                 /*
                1. 응답데이터의 body가 잘못온 경우
                2. JSON 파싱 중 오류
                 */
                Log.e(TAG+"-syncHandler", context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + e.getMessage() + ")", e);
                return new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_INVALID_RESPONSE,
                        context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + e.getMessage() + ")");
            }
        } else {
            // http 오류 코드 응답 (ex. 4.. 5..)
            Log.e(TAG+"-syncHandler", context.getString(R.string.BROKER_ERROR_HTTP_RESPONSE_STRING) + " (HTTP 상태코드 : " + response.code() + ")");
            return new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_SERVICE_SERVER,
                    context.getString(R.string.BROKER_ERROR_HTTP_RESPONSE_STRING) + " (HTTP 상태코드 : " + response.code() + ")");
        }
    }

    // 파일 다운로드 / 업로드에 대한 결과 보고서 전송
    private void sendReport(String encAgentDetail, String fileName, int fileSize, long beginTime, long endTime) throws JSONException {
        RequestBody requestBody = new RelayRequestBody.ReportBuilder()
                .setFileName(fileName)
                .setFileSize(fileSize)
                .setBeginTime(beginTime)
                .setEndTime(endTime)
                .build();

        Call<ResponseBody> call = relayClientInterface.callReportUpload(baseHost, encAgentDetail, requestBody);
        call.enqueue(new RelayClientCallback(context, RelayResponseParser.RESULT_TYPE.REPORT, new IBrokerServiceCallback() {

            @Override
            public IBinder asBinder() {
                return null;
            }

            @Override
            public void onResponse(BrokerResponse response) {
                if(response.ok()) {
                    Log.d(TAG+"-Report", "업로드 레포트 업데이트 성공");
                } else {
                    Log.w(TAG+"-Report", "업로드 레포트 업데이트 실패");
                }
            }

            @Override
            public void onFailure(int code, String msg) {
                Log.e(TAG+"-Report", "업로드 레포트 업데이트시 에러: " + msg);
            }
        }));
    }
}
