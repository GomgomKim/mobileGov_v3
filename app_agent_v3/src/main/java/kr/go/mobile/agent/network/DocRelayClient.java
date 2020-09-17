package kr.go.mobile.agent.network;

import android.os.RemoteException;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.DocumentResponse;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;
import kr.go.mobile.agent.service.broker.IDocumentCallback;
import kr.go.mobile.agent.utils.DocumentData;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DocRelayClient {

    private final static String TAG = DocRelayClient.class.getSimpleName();

    String baseURL;

    Retrofit retrofit;
    OkHttpClient okHttpClient;

    private DocumentData DocumentData;

    public DocRelayClient(String baseURL) {
        this.baseURL = baseURL;
    }

    public void setTimeout(int nConnectTimeOut, int nReadTimeOut){
        /*
        timeout 설정
         */
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(nConnectTimeOut, TimeUnit.MILLISECONDS)
                .readTimeout(nReadTimeOut, TimeUnit.MILLISECONDS)
                .build();
    }

    public void buildClient(){
        /*
        서버 url설정
        데이터 파싱 설정
        객체정보 반환
         */
        retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    /*
    doucment 인터페이스 연결
     */
    public void docRelay(final IDocumentCallback callback, Map<String, String> headers, String body){
        /*
        레트로핏 객체에 관리 인터페이스 연결
         */
        DocRelayInterface docInterface = retrofit.create(DocRelayInterface.class);
        /*
        결과 콜백 부분
         */
        Call<DocumentData> call = docInterface.getDocReqData(headers, body);
        call.enqueue(new Callback<DocumentData>() {
            @Override
            public void onResponse(Call<DocumentData> call, Response<DocumentData> response) {
                Log.d(TAG, "enqueue onResponse : call - "+call+" / response - "+response);
                int doc_code = response.code();
                DocumentData doc_data = response.body();
                String message = response.message();
                DocumentResponse resp;
                try {
                    resp = new DocumentResponse(doc_code, message, doc_data);
                } catch (Exception e) {
                    resp = new DocumentResponse(doc_code, "데이터 전송 오류", doc_data);
                }

                try {
                    callback.onResponse(resp);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<DocumentData> call, Throwable t) {
                Log.d(TAG, "enqueue onFailure : code - "+call+" / msg - "+t);
                try {
                    callback.onFailure(-1, "라이브러리 오류");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
