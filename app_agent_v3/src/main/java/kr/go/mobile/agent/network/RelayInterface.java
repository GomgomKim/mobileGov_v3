package kr.go.mobile.agent.network;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

public interface RelayInterface {


    // using Json
    @POST(".")
    @Headers("Content-Type: application/x-www-form-urlencoded; charset=UTF-8")
    Call<ResponseBody> callDefault(
            @Header("Service-Id") String serviceID,
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    /*
    Tom 200914
    사용자 인증정보 인터페이스
     */
    @POST(".")
    @Headers({
            "Content-Type: application/json;charset=utf-8",
            "Service-Id: CMM_CERT_AUTH_MAM"
    })
    Call<ResponseBody> callAuth(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    // using Json
    @POST(".")
    @Headers({
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8",
            "Service-Id: CMM_DOC_IMAGE_LOAD"
    })
    Call<ResponseBody> callLoadDocument(
            @HeaderMap Map<String, String> headers,
            @Body RequestBody body
    );

    // file upload
    @POST(".")
    @Headers("Service-Id: CMM_FILE_UPLOAD")
    Call<ResponseBody> callUpload(
            @Header("Content-Type") String contentType,
            @HeaderMap Map<String, String> headers,
            @Body MultipartBody body
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
}
