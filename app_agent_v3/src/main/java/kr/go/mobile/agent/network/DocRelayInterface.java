package kr.go.mobile.agent.network;

import java.util.Map;

import kr.go.mobile.agent.utils.DocumentData;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface DocRelayInterface {

    /*
    Document 인터페이스
     */
    @FormUrlEncoded
    @POST("")
    Call<DocumentData> getDocReqData(@HeaderMap Map<String, String> headers, @Body String body);

}
