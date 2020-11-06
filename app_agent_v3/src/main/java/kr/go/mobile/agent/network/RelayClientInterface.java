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

public interface RelayClientInterface {


    // using Json
    @POST(".")
    @Headers("Content-Type: application/x-www-form-urlencoded; charset=UTF-8")
    Call<ResponseBody> callDefault(
            @Header("Service-Id") String serviceID,
            @Header("Host") String hostURL,
            @Header("X-Agent-Detail") String agentDetail,
            @Body RequestBody body
    );

    /*
    Tom 200914
    사용자 인증정보 인터페이스
     */
    @POST(".")
    @Headers({
            "Content-Type: application/json; charset=UTF-8",
            "Service-Id: CMM_CERT_AUTH_MAM"
    })
    Call<ResponseBody> callAuth(
            @Header("Host") String hostURL,
            @Header("X-Agent-Detail") String encAgentDetail,
            @Body RequestBody body
    );

    // using Json
    @POST(".")
    @Headers({
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8",
            "Service-Id: CMM_DOC_IMAGE_LOAD"
    })
    Call<ResponseBody> callLoadDocument(
            @Header("Host") String hostURL,
            @Header("X-Agent-Detail") String encAgentDetail,
            @Body RequestBody body
    );

    // file upload
    @POST(".")
    @Headers("Service-Id: CMM_FILE_UPLOAD")
    Call<ResponseBody> callUpload(
            @Header("Content-Type") String multipartContent,
            @Header("Host") String hostURL,
            @Header("X-Agent-Detail") String encAgentDetail,
            @Body MultipartBody body
    );

    // file upload
    @POST(".")
    @Headers({
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8",
            "Service-Id: CMM_FILE_DOWNLOAD"
    })
    Call<ResponseBody> callDownload(
            @Header("Host") String hostURL,
            @Header("X-Agent-Detail") String encAgentDetail,
            @Body RequestBody body
    );

    // report upload
    @POST(".")
    @Headers({
            "Content-Type: application/json; charset=UTF-8",
            "Service-Id: CMM_REPORT_FILE_UPLOAD"
    })
    Call<ResponseBody> callReportUpload(
            @Header("Host") String hostURL,
            @Header("X-Agent-Detail") String encAgentDetail,
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
}
