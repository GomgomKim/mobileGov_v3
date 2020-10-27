package kr.go.mobile.agent.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;

import kr.go.mobile.agent.service.broker.MethodResponse;
import kr.go.mobile.agent.utils.FileUtils;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.mobp.iff.R;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RelayClient {

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
        private String officeCode;

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


        public Builder setOfficeCode(String officeCode) {
            this.officeCode = officeCode;
            return this;
        }

        private Map<String, String> generatorMOBPHeader()
                throws MalformedURLException, PackageManager.NameNotFoundException, UnsupportedEncodingException {
            Map<String, String> defaultHeaders = new HashMap<>();
            URL url = new URL(baseUrl);
            defaultHeaders.put("Host", url.getHost() + (url.getPort() > 0 ? ":"+url.getPort() : ""));
//            defaultHeaders.put("Service-Id", this.serviceId);
            defaultHeaders.put("X-Agent-Detail", URLEncoder.encode(getAgentDetail(), "UTF-8"));
            return defaultHeaders;
        }

        String getAgentDetail() throws PackageManager.NameNotFoundException {
            PackageInfo info = baseContext.getPackageManager().getPackageInfo(baseContext.getPackageName(), PackageManager.GET_META_DATA);
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
            String FORMAT = ";FLD=I;PK=%s;AV=%s;OT=%s;OV=%s;RV=%s;DW=%s;DH=%s;DPI=%s;TD=N;UD=%s,UI=%s;OC=%s;OG=%s;MD=%s";
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

        public RelayClient build() throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException,
                PackageManager.NameNotFoundException, MalformedURLException {
            return build(true);
        }

        public RelayClient build(boolean enabledLog) throws KeyManagementException,
                NoSuchAlgorithmException, UnsupportedEncodingException,
                PackageManager.NameNotFoundException, MalformedURLException {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManager, trustAllCerts, secureRandom);

            RelayClient client = new RelayClient(enabledLog, baseContext, baseUrl, generatorMOBPHeader(),
                    connTimeout, readTimeout,
                    sslContext, trustAllCerts);


            // TODO
            return client;
        }
/*
        generateHeaders = new GenerateHeaders();
//        try {
//            client.setOkHttpClient(nConnectTimeOut, nReadTimeOut);
//            client.buildClient();
//            generateHeaders.setUrl(new URL(baseURL));
//        } catch (MalformedURLException e) {
//            throw new RuntimeException("중계 서버 URL 정보가 잘못되었습니다.", e);
//        } catch (NoSuchAlgorithmException | KeyManagementException e) {
//            throw new RuntimeException("중계 서버와 통신할 클라이언트 초기화를 할 수 없습니다.", e);
//        }
        try {
            generateHeaders.setAgentDetail(this,
                    signed.getUserID(),
                    signed.getOfficeName(),
                    signed.getOfficeCode());

        } catch (UnsupportedEncodingException e){
            throw new RuntimeException("지원하지 않는 인코딩 타입입니다.", e);
        } catch (PackageManager.NameNotFoundException e){
            throw new RuntimeException("단말 정보를 획득할 수 없습니다.", e);
        }
//        client = new RelayClient(getBaseContext(), baseURL);
 */
    }

    /*
    Tom 200914
    Retrofit2 - build 및 인터페이스 연동
    user auth data request, return
     */
    private final static String TAG = RelayClient.class.getSimpleName();
    private final String BROKER_ERROR_PROC_DATA_STRING = "데이터 요청에 대한 응답데이터 처리 중 에러가 발생하였습니다.";
    private final String BROKER_ERROR_HTTP_RESPONSE_STRING = "서버 응답 에러가 발생했습니다. 서비스 관리에게 문의하시기 바랍니다.";
    private final String BROKER_ERROR_RESP_COMMON_DATA_STRING = "공통기반 시스템 에러입니다. 공통기반 운영단에 문의하시기 바랍니다.";
    private final String BROKER_ERROR_REQUEST_FORM_STRING = "요청 데이터 형식 오류입니다.";

    Context context;
    String baseURL;
    Retrofit retrofit;
    RelayInterface relayInterface;
    Map<String, String> defaultMOBPHeader;

    RelayClient(boolean enabledLog, Context context, String baseURL, Map<String, String> header, int connectTimeout, int readTimeout,
                SSLContext sslContext, TrustManager[] trustAllCerts) {

        this.context = context;
        this.baseURL = baseURL;
        this.defaultMOBPHeader = header;

        /*
        okHttp 설정
         */
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

        if (enabledLog) {
            // 통신 중 일어나는 로그를 인터셉트하는 Interceptor
            HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
            logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logInterceptor); // 로그 활성화
    }
        OkHttpClient okHttpClient = builder.build();

        // using Json
        retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(okHttpClient)
                .build();

        // 레트로핏 객체에 관리 인터페이스 연결
        relayInterface = retrofit.create(RelayInterface.class);
        // using Gson
        /*
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();
        */
    }


    /*
   행정 서비스 인터페이스 연결
    */
    public void relayReqData(String serviceId, String body, final IBrokerServiceCallback callback) {
        Log.d(TAG, "[relayReqData] serviceId = " + serviceId + ", Body = " + body);

        byte[] byte_body = body.getBytes(StandardCharsets.UTF_8);
        RequestBody req_body = RequestBody.create(MediaType.parse("application/octet-stream"), byte_body);

        Call<ResponseBody> call = relayInterface.callDefault(serviceId, defaultMOBPHeader, req_body);
        call.enqueue(new RelayClientCallback(context, ResultParser.RESULT_TYPE.DEFAULT, callback));
    }

    /*
    사용자 인증정보 인터페이스 연결
     */
    public void relayUserAuth(String body, final IBrokerServiceCallback callback) {
        Log.d(TAG, "[relayUserAuth] Body = " + body);

        byte[] byte_body = body.getBytes(StandardCharsets.UTF_8);
        RequestBody req_body = RequestBody.create(MediaType.parse("application/octet-stream"), byte_body);

        // 결과 콜백 부분 - using Json
        Call<ResponseBody> call = relayInterface.callAuth(defaultMOBPHeader, req_body);
        call.enqueue(new RelayClientCallback(context, ResultParser.RESULT_TYPE.AUTHENTICATION, callback));
                    }

    /*
    문서 변환 요청
    */
    public void relayLoadConvertedDoc(String body, final IBrokerServiceCallback callback) {
        Log.d(TAG, "[relayLoadConvertedDoc] Body = " + body);

        byte[] byte_body = body.getBytes(StandardCharsets.UTF_8);
        RequestBody req_body = RequestBody.create(MediaType.parse("application/octet-stream"), byte_body);

        // 결과 콜백 부분 - using Json
        Call<ResponseBody> call = relayInterface.callLoadDocument(defaultMOBPHeader, req_body);
        call.enqueue(new RelayClientCallback(context, ResultParser.RESULT_TYPE.DOCUMENT, callback));
                            }

                /*
   파일업로드
    */
    public void relayUploadData(String boundaryId, String body, String fileName, byte[] uploadBytes, final IBrokerServiceCallback callback) {
        Log.d(TAG, "[relayUploadData] fileName = " + fileName + ", fileLength =  " + uploadBytes.length  + ", Body = " + body);
        final long startTime = System.currentTimeMillis();

        MultipartBody multipartBody;
        {
            MultipartBody.Builder builder = new MultipartBody.Builder(boundaryId);
            builder.setType(MultipartBody.FORM);
            // TODO localFile 이 아닌 byte[] 값을 사용하도록 변경 필요 또는 ContentProvider 를 이용하여 파일 접근이 가능한 환경 만들기.
            RequestBody rqFile = RequestBody.create(MediaType.parse("application/octet-stream"), uploadBytes);
            builder.addFormDataPart("file", fileName, rqFile);

            String[] tmp = body.split("[&]");
            for (String bodyParams : tmp) {
                String[] splitData = bodyParams.trim().split("[=]");
                if (splitData.length == 1) {
                    builder.addFormDataPart(splitData[0], "");
                } else {
                    builder.addFormDataPart(splitData[0], splitData[1]);
                }
            }
            multipartBody = builder.build();
        }

        /*
        결과 콜백 부분
         */
        Call<ResponseBody> call = relayInterface.callUpload(boundaryId, defaultMOBPHeader, multipartBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
             /*
               서버로부터 응답 데이터가 왔을 때
              */
                Log.timeStamp("relayReqData");
                Log.d(TAG, "enqueue onResponse : call - "+call+" / response - "+response);
                Log.d(TAG, "Result Message : " + response.message());

                BrokerResponse<?> resp;
                if(response.isSuccessful()) {
                    // http code : 2xx 통신 성공

                    try {
                        // TODO
                        MethodResponse respData = ResultParser.parse(ResultParser.RESULT_TYPE.DEFAULT, response);
                        if (respData.result.equals("1")) {
                            // 데이터가 정상적으로 수신된 경우

                            // 업로드 결과 전송
                            long endTime = System.currentTimeMillis();
                            /*
                            File file = new File(filePath);
                            long size = file.length();
                            String fileName = FileUtils.getFileName(filePath);
                            Log.d(TAG, "파일명 :: " + fileName);
                            String fileExt = FileUtils.getExtension(filePath);
                            Log.d(TAG, "확장자 :: " + fileExt);
                            String data =  kr.go.mobile.agent.utils.FileUtils.makeFileRespData(fileName, fileExt, size + "", startTime, endTime);

                            Log.d(TAG, "Result data : " + data);
                             */
                            // TODO 공통기반 시스템으로 파일 업로드에 대한 레포트 정보를 전달한다.
                            resp = new BrokerResponse<>(respData);

                        } else {
                            Log.e(TAG, context.getString(R.string.BROKER_ERROR_RELAY_SERVER)
                                    + "(message : " + respData.relayServerMessage+ ", code : "+respData.relayServerCode +")");
                            resp = new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_RELAY_SERVER, context.getString(R.string.BROKER_ERROR_RELAY_SERVER)
                                    + "(message : " + respData.relayServerMessage+ ", code : "+respData.relayServerCode +")");
                        }
                    } catch (IOException | JSONException e) {
                        /*
                        1. 응답데이터의 body가 잘못온 경우
                        2. JSON 파싱 중 오류
                         */
                        Log.e(TAG, BROKER_ERROR_PROC_DATA_STRING + "(" + e.getMessage() + ")", e);
                        resp = new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_PROC_DATA,
                                BROKER_ERROR_PROC_DATA_STRING + "(" + e.getMessage() + ")");
                    }

                } else {
                    // http 오류 코드 응답 (ex. 4.. 5..)
                    Log.e(TAG, BROKER_ERROR_HTTP_RESPONSE_STRING + " (HTTP 상태코드 : " + response.code() + ")");
                    resp = new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_HTTP_RESPONSE,
                            BROKER_ERROR_HTTP_RESPONSE_STRING + " (HTTP 상태코드 : " + response.code() + ")");
                }

                try {
                    callback.onResponse(resp);
                } catch (RemoteException e) {
                    //TODO 이부분은 개발자가 정의한 onFailure 에서 데이터를 처리하다가 RemoteException 을 줘야지 받을 수 있는 부분인데
                    //만약, 개발자가 응답 데이터를 받았는데 에러가 발생해서 RemoteException 을 준다고하면, 에이전트에서는 어떻게 처리해야할까요 ?
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                /*
                Exception으로 인해 서버로부터 응답 데이터를 받지 못했을 때
                 */
                Log.e(TAG, "서비스 ID를 이용한 데이터 요청 실패  : call - "+call, t);
                try {
                    if(t instanceof IOException){
                        // 네트워크 오류 - 통신이 불가능한 경우(WIFI미접속 등)
                        Log.e(TAG, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                        callback.onFailure(CommonBasedConstants.BROKER_ERROR_PROC_DATA, BROKER_ERROR_PROC_DATA_STRING);
                    } else {
                        // 요청데이터의 형식이 잘못된 경우
                        Log.e(TAG, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                        callback.onFailure(CommonBasedConstants.BROKER_ERROR_REQUEST_FORM, BROKER_ERROR_REQUEST_FORM_STRING);
                    }
                } catch (RemoteException ignore) {
                    // TODO 이부분은 개발자가 정의한 onFailure 에서 데이터를 처리하다가 RemoteException 을 줘야지 받을 수 있는 부분인데...
                    // 만약, 개발자가 응답 데이터를 받았는데 에러가 발생해서 RemoteException 을 준다고하면, 에이전트에서는 어떻게 처리해야할까요 ?
                }
            }
        });
    }




    // using Gson
        /*
        Call<AuthModel> call = userAuthDataInterface.getReqData(headers, req_body);
        call.enqueue(new Callback<AuthModel>() {
            @Override
            public void onResponse(Call<AuthModel> call, Response<AuthModel> response) {
                Log.d(TAG, "enqueue onResponse : call - "+call+" / response - "+response);

                AuthModel result = response.body();

//                String dn = result.getMethodResponse().getData().getDn();
//                Log.d(TAG, "result : dn - "+dn);

                String message = response.message();
                BrokerResponse resp;
                try {
                    resp = new BrokerResponse(0, message, result);
                } catch (Exception e) {
                    resp = new BrokerResponse(0, "인증서 만료", result);
                }

                try {
                    callback.onResponse(resp);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<AuthModel> call, Throwable t) {
                Log.d(TAG, "enqueue onFailure : code - "+call+" / msg - "+t);
                try {
                    callback.onFailure(-1, "라이브러리 오류");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        */




}
