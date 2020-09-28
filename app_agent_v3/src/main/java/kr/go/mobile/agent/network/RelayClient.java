package kr.go.mobile.agent.network;

import android.os.RemoteException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.RespData;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;

import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.RsepDataUtils;
import kr.go.mobile.agent.utils.UserAuthenticationUtils;
import kr.go.mobile.common.v3.MobileEGovConstants;
import kr.go.mobile.mobp.iff.R;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RelayClient {

    /*
    Tom 200914
    Retrofit2 - build 및 인터페이스 연동
    user auth data request, return
     */

    private final static String TAG = RelayClient.class.getSimpleName();
    private final String BROKER_ERROR_PROC_DATA_STRING = "데이터 요청에 대한 응답데이터 처리 중 에러가 발생하였습니다.";
    private final String BROKER_ERROR_CONNECT_HTTP_STRING = "서버 응답 에러가 발생했습니다. 행정서버에 문의하세요.";
    private final String BROKER_ERROR_RESP_COMMON_DATA_STRING = "공통기반 시스템 에러입니다. 행정기관에 문의하세요.";

    String baseURL;
    Retrofit retrofit;
    OkHttpClient okHttpClient;

    public RelayClient(String baseURL) {
        this.baseURL = baseURL;
    }

    public void setOkHttpClient(int nConnectTimeOut, int nReadTimeOut){
        /*
        SSL 설정
         */
        // Create a trust manager that does not validate certificate chains
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

        SSLContext sslContext = null;
        SSLSocketFactory sslSocketFactory = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        /*
        okHttp 설정
         */
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(nConnectTimeOut, TimeUnit.MILLISECONDS)
                .readTimeout(nReadTimeOut, TimeUnit.MILLISECONDS)
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .build();
    }

    public void buildClient(){
        /*
        서버 url설정
        데이터 파싱 설정
        객체정보 반환
         */
//        Log.d(TAG, "in");
//        Log.d(TAG, "base url" + baseURL);

        // using Json

        retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(okHttpClient)
                .build();

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
        Log.d(TAG, "build");
    }

    /*
    사용자 인증정보 인터페이스 연결
     */
    public void relayUserAuth(final IBrokerServiceCallback callback, Map<String, String> headers, String body){
        Log.timeStamp("relayUserAuth");

        byte[] byte_body = body.getBytes(StandardCharsets.UTF_8);
        RequestBody req_body = RequestBody.create(MediaType.parse("application/octet-stream"), byte_body);

        // 레트로핏 객체에 관리 인터페이스 연결
        RelayInterface userAuthDataInterface = retrofit.create(RelayInterface.class);

        // 결과 콜백 부분
        // using Json
        Call<ResponseBody> call = userAuthDataInterface.getAuthReqData(headers, req_body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "enqueue onResponse : call - "+call+" / response - "+response);
                Log.d(TAG, "Result Message : " + response.message());

                if(response.isSuccessful()) { // http code : 2xx 통신 성공
                    BrokerResponse<UserAuthentication> resp;
                    try {
                        String result = response.body().string();
                        UserAuthentication respData = RsepDataUtils.parseUserAuthentication(result);
                        resp = new BrokerResponse<>(MobileEGovConstants.BROKER_ERROR_NONE, respData);
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, BROKER_ERROR_PROC_DATA_STRING + "(" + e.getMessage() + ")", e);
                        resp = new BrokerResponse<>(MobileEGovConstants.BROKER_ERROR_PROC_DATA,
                                BROKER_ERROR_PROC_DATA_STRING +  "(" + e.getMessage() + ")", null);
                    }

                    try {
                        callback.onResponse(resp);
                    } catch (RemoteException ignored) {
                    }

                } else { // http 통신 실패
                    Log.e(TAG, BROKER_ERROR_CONNECT_HTTP_STRING + " (HTTP 상태코드 : " + response.code() + ")");
                    try {
                        callback.onFailure(MobileEGovConstants.BROKER_ERROR_CONNECT_HTTP, BROKER_ERROR_CONNECT_HTTP_STRING + " (HTTP 상태코드 : " + response.code() + ")");
                    } catch (RemoteException ignored) {
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "enqueue onFailure : code - "+call+" / msg - "+t);
                try {
                    // retrofit, okhttp, adapter, converter 중에서 에러가 난 상황
                    if(t instanceof IOException){
                        // 네트워크 오류 - header, body 형식이 맞지 않거나 통신이 불가능한 상황(WIFI미접속 등)
                        Log.e(TAG, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                        callback.onFailure(MobileEGovConstants.BROKER_ERROR_PROC_DATA, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                    } else {
                        // 그 외 converter, baseurl, SSL등을 잘못 설정한 경우
                        Log.e(TAG, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                        callback.onFailure(MobileEGovConstants.BROKER_ERROR_PROC_DATA, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                    }
                } catch (RemoteException ignored) {
                }
            }
        });

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


    /*
   행정 서비스 인터페이스 연결
    */
    public void relayReqData(final IBrokerServiceCallback callback, Map<String, String> headers, String body){
        Log.timeStamp("relayReqData");
        Log.d(TAG, "relayReqData : headers - "+headers+" / body - "+body);

        byte[] byte_body = body.getBytes(StandardCharsets.UTF_8);

        RequestBody req_body = RequestBody.create(MediaType.parse("application/octet-stream"), byte_body);

        /*
        레트로핏 객체에 관리 인터페이스 연결
         */
        RelayInterface reqDataInterface = retrofit.create(RelayInterface.class);
        /*
        결과 콜백 부분
         */
        Call<ResponseBody> call = reqDataInterface.getReqData(headers, req_body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                Log.d(TAG, "enqueue onResponse : call - "+call+" / response - "+response);
                Log.d(TAG, "Result Message : " + response.message());

                if(response.isSuccessful()) {
                    // http code : 2xx 통신 성공
                    BrokerResponse<String>resp;
                    try {
                        String result = response.body().string();
                        RespData respData = RsepDataUtils.parseReqData(result);
                        if (respData.result.equals("1")) {
                            // 데이터가 정상적으로 수신된 경우
                            String data = RsepDataUtils.makeRespData(respData);
                            resp = new BrokerResponse<>(MobileEGovConstants.BROKER_ERROR_NONE, data);
                        } else {
                            // 행정기관에서 정상적인 데이터가 아닌 오류메세지를 보낼 경우
                            Log.e(TAG, BROKER_ERROR_RESP_COMMON_DATA_STRING + "(result: " + respData.result + ", code: "+ respData.code +")");
                            resp = new BrokerResponse<>(MobileEGovConstants.BROKER_ERROR_RESP_COMMON_DATA ,
                                    BROKER_ERROR_RESP_COMMON_DATA_STRING + "(result: " + respData.result + ", code: "+ respData.code +")", null);
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG,"err code : "+MobileEGovConstants.BROKER_ERROR_PROC_DATA+ BROKER_ERROR_PROC_DATA_STRING+ " (" + e.getMessage() + ")", e);
                        resp = new BrokerResponse<>(MobileEGovConstants.BROKER_ERROR_PROC_DATA, BROKER_ERROR_PROC_DATA_STRING+ " (" + e.getMessage() + ")", null);
                    }

                    try {
                        callback.onResponse(resp);
                    } catch (RemoteException e) {
                        //TODO 이부분은 개발자가 정의한 onFailure 에서 데이터를 처리하다가 RemoteException 을 줘야지 받을 수 있는 부분인데
                        //만약, 개발자가 응답 데이터를 받았는데 에러가 발생해서 RemoteException 을 준다고하면, 에이전트에서는 어떻게 처리해야할까요 ?
                    }
                }
                else {
                    // http 통신 실패
                    Log.e(TAG, BROKER_ERROR_CONNECT_HTTP_STRING + " (HTTP 상태코드 : " + response.code() + ")");
                    try {
                        callback.onFailure(MobileEGovConstants.BROKER_ERROR_CONNECT_HTTP, BROKER_ERROR_CONNECT_HTTP_STRING + " (HTTP 상태코드 : " + response.code() + ")");
                    } catch (RemoteException ignored) {
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "서비스 ID를 이용한 데이터 요청 실패  : call - "+call, t);
                try {
                    // retrofit, okhttp, adapter, converter 중에서 에러가 난 상황
                    if(t instanceof IOException){
                        // 네트워크 오류 - header, body 형식이 맞지 않거나 통신이 불가능한 상황(WIFI미접속 등)
                        Log.e(TAG, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                        callback.onFailure(MobileEGovConstants.BROKER_ERROR_PROC_DATA, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                    } else {
                        // 그 외 converter, baseurl, SSL등을 잘못 설정한 경우
                        // TODO 상수를 추가하거나 별도의 에러 처리 작업이 필요할듯 합니다.
                        Log.e(TAG, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                        callback.onFailure(MobileEGovConstants.BROKER_ERROR_PROC_DATA, BROKER_ERROR_PROC_DATA_STRING +  "(" + t.getMessage() + ")");
                    }
                } catch (RemoteException ignore) {
                    // TODO 이부분은 개발자가 정의한 onFailure 에서 데이터를 처리하다가 RemoteException 을 줘야지 받을 수 있는 부분인데...
                    // 만약, 개발자가 응답 데이터를 받았는데 에러가 발생해서 RemoteException 을 준다고하면, 에이전트에서는 어떻게 처리해야할까요 ?
                }
            }
        });
    }

}
