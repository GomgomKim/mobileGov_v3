package kr.go.mobile.agent.network;

import android.os.RemoteException;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.ReqData;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;

import kr.go.mobile.agent.utils.ReqDataUtils;
import kr.go.mobile.agent.utils.UserAuthenticationUtils;
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

//        Log.d(TAG, "userAuthRelay : headers - "+headers+" / body - "+body);

        byte[] byte_body = body.getBytes(StandardCharsets.UTF_8);

//        Log.d(TAG, "byte_body - "+ Arrays.toString(byte_body));

        RequestBody req_body = RequestBody.create(MediaType.parse("application/octet-stream"), byte_body);

        /*
        레트로핏 객체에 관리 인터페이스 연결
         */
        RelayInterface userAuthDataInterface = retrofit.create(RelayInterface.class);
        /*
        결과 콜백 부분
         */
        SimpleDateFormat mFormat = new SimpleDateFormat("hh:mm:ss.SSS");
        String curTime = mFormat.format(new Date(System.currentTimeMillis()));
        Log.d("estimate_time", "Time : start - "+curTime);

        // using Json
        Call<ResponseBody> call = userAuthDataInterface.getAuthReqData(headers, req_body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "enqueue onResponse : call - "+call+" / response - "+response);
                String message = response.message();
                Log.d(TAG, "Result Message : " + message);

                BrokerResponse resp;
                try {
                    String result = response.body().string();
                    UserAuthentication retData = UserAuthenticationUtils.parseUserAuthentication(result);
                    UserAuthenticationUtils.confirm(retData);
                    resp = new BrokerResponse(0, retData);

                } catch (UserAuthenticationUtils.InvalidatedAuthException e) {
                    // TODO 에러 코드 분류
                    resp = new BrokerResponse(0, e.getMessage());
                } catch (IOException e) {
                    // TODO 에러 코드 분류
                    e.printStackTrace();
                    resp = new BrokerResponse(0, "응답 데이터를 읽을 수 없습니다.");
                }

                try {
                    callback.onResponse(resp);
                } catch (RemoteException e) {
                    // TODO 응답 받는 클라이언트 쪽에서 에러 발생.. 어떻게 처리할까 ?
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "enqueue onFailure : code - "+call+" / msg - "+t);
                try {
                    callback.onFailure(-1, "라이브러리 오류");
                } catch (RemoteException e) {
                    // TODO 응답 받는 클라이언트 쪽에서 에러 발생.. 어떻게 처리할까 ?
                    e.printStackTrace();
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
   사용자 인증정보 인터페이스 연결
    */
    public void relayReqData(final IBrokerServiceCallback callback, Map<String, String> headers, String body){

        Log.d(TAG, "userAuthRelay : headers - "+headers+" / body - "+body);

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
                String message = response.message();
                Log.d(TAG, "Result Message : " + message);

                BrokerResponse resp;
                try {
                    String result = response.body().string();
                    Log.d(TAG, "Result Message : " + result);
                    ReqData retData = ReqDataUtils.parseReqData(result);
                    String data = retData.data;
                    // TODO 응답 데이터에따라 parse된 데이터 String으로 묶어주는 작업 필요 - parseReqData해도 될듯

                    resp = new BrokerResponse(0, data);

                } catch (IOException e) {
                    // TODO 에러 코드 분류
                    e.printStackTrace();
                    resp = new BrokerResponse(0, "응답 데이터를 읽을 수 없습니다.");
                }


                try {
                    callback.onResponse(resp);
                } catch (RemoteException e) {
                    // TODO 응답 받는 클라이언트 쪽에서 에러 발생.. 어떻게 처리할까 ?
                    e.printStackTrace();
                }


            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "enqueue onFailure : code - "+call+" / msg - "+t);
                try {
                    callback.onFailure(-1, "라이브러리 오류");
                } catch (RemoteException e) {
                    // TODO 응답 받는 클라이언트 쪽에서 에러 발생.. 어떻게 처리할까 ?
                    e.printStackTrace();
                }
            }
        });
    }

}
