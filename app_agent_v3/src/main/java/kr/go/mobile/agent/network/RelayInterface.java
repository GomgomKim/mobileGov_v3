package kr.go.mobile.agent.network;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.Multipart;
import retrofit2.http.POST;

public interface RelayInterface {

    /*
    Tom 200914
    사용자 인증정보 인터페이스
     */

    // using Json
    @POST(".")
    Call<ResponseBody> getAuthReqData(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    // using Gson
    /*
    @POST(".")
    Call<AuthModel> getReqData(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
//            @Body byte[] body
//            @Body String body
    );
     */

    // using Json
    @POST(".")
    Call<ResponseBody> getReqData(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );




}
