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
import kr.go.mobile.agent.utils.ReqDataUtils;
import kr.go.mobile.agent.utils.UserAuthenticationUtils;
import kr.go.mobile.common.v3.MobileEGovConstants;
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

                BrokerResponse<UserAuthentication> resp;
                try {
                    String result = response.body().string();
                    UserAuthentication retData = UserAuthenticationUtils.parseUserAuthentication(result);
                    resp = new BrokerResponse<>(MobileEGovConstants.BROKER_ERROR_NONE, retData);
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "사용자 인증 응답데이터 처리중 에러가 발생하였습니다. (" + e.getMessage() + ")", e);
                    // RESULT_OK 가 아닌 값 아무거나  ~
                    resp = new BrokerResponse<>(MobileEGovConstants.BROKER_ERROR_READ,
                            "사용자 인증 응답데이터 처리중 에러가 발생하였습니다. (" + e.getMessage() + ")", null);
                }

                try {
                    callback.onResponse(resp);
                } catch (RemoteException ignored) {
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "enqueue onFailure : code - "+call+" / msg - "+t);
                try {
                    // README : throwable 타입에 따른 분류가 필요할꺼 같아요..
                    // 어떤 Throwable 들이 올 수 있는지 확인 해주세요 !
                    // 객체 타입에 따른 code 값 정의가 필요하고 관련 내용에 대한 설명 문자열 필요해요
                    callback.onFailure(-1, "라이브러리 오류");
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

                BrokerResponse<String>resp;
                try {
                    String result = response.body().string();
                    // README ReqData -> RespData
                    // README 중계 클라이언트 응답데이터를 파싱하는 util 을 하나로 만드는건 어떨까요 ?
                    RespData retData = ReqDataUtils.parseReqData(result);
                    // README result 값에 따른 처리는 안해요 ?? 이 같이 따라서 에러코드로 나눠질텐데 ?
                    /*
                    switch (retData.result) {
                        case "0":
                            break;
                    }
                     */
                    String data = retData.data;
                    // TODO 응답 데이터에따라 parse된 데이터 String으로 묶어주는 작업 필요 - parseReqData해도 될듯
                    resp = new BrokerResponse<>(MobileEGovConstants.BROKER_ERROR_NONE, data);

                } catch (IOException | JSONException e) {
                    Log.e(TAG, "(서비스 ID) 데이터 요청에 대한 응답데이터 처리중 에러가 발생하였습니다. (" + e.getMessage() + ")", e);
                    resp = new BrokerResponse<>(MobileEGovConstants.BROKER_ERROR_READ, "응답 데이터를 읽을 수 없습니다.", null);
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
                Log.e(TAG, "서비스 ID를 이용한 데이터 요청 실패  : call - "+call, t);
                try {
                    // README : throwable 타입에 따른 분류가 필요할꺼 같아요..
                    // 어떤 Throwable 들이 올 수 있는지 확인 해주세요 !
                    // 객체 타입에 따른 code 값 정의가 필요하고 관련 내용에 대한 설명 문자열 필요해요
                    callback.onFailure(-1, "라이브러리 오류");
                } catch (RemoteException e) {
                    // TODO 이부분은 개발자가 정의한 onFailure 에서 데이터를 처리하다가 RemoteException 을 줘야지 받을 수 있는 부분인데...
                    // 만약, 개발자가 응답 데이터를 받았는데 에러가 발생해서 RemoteException 을 준다고하면, 에이전트에서는 어떻게 처리해야할까요 ?
                }
            }
        });
    }

}
