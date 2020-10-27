package kr.go.mobile.agent.network;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;

import kr.go.mobile.agent.service.broker.Document;
import kr.go.mobile.agent.service.broker.MethodResponse;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ResultParser {

    public enum RESULT_TYPE {
        DEFAULT,
        AUTHENTICATION,
        DOCUMENT
    }

    public static MethodResponse parse(RESULT_TYPE parseType, Response<ResponseBody> respRelay) throws JSONException, IOException {
        switch (parseType) {
            case AUTHENTICATION:
                return parseUserAuthentication(respRelay.body().string());
            case DOCUMENT:
                return parseConvertDocData(respRelay.headers(), respRelay.body());
            case DEFAULT:
            default:
                return parseDefault(respRelay.body().string());
        }
    }

    private static JSONObject getMethodResponse(String strMethodResponse) throws JSONException {
        JSONObject o = (JSONObject) new JSONTokener(strMethodResponse).nextValue();
        return o.getJSONObject("methodResponse");
    }

    /**
     Tom 200914
     Return data Json parsing
     */
    static MethodResponse parseDefault(final String stringBody) throws JSONException {
        JSONObject response = getMethodResponse(stringBody);
        MethodResponse methodResponse = new MethodResponse();
        methodResponse.id = response.getString("id"); // requestId or responseId ?
        methodResponse.relayServerCode = response.getString("code"); // 처리에 대한 응답 코드 (중계 서버의 코드)
        methodResponse.relayServerMessage = response.getString("msg"); // 처리에 대한 응답 메시지 (중계 서버의 메시지)
        methodResponse.result = response.getString("result"); // 처리 대한 결과 1(성공) or 0(실패)
        if (methodResponse.relayServerOK()) {
            methodResponse.data = response.getString("data"); // 실제 서버가 전달한 값.
        }
        return methodResponse;
    }

    static UserAuthentication parseUserAuthentication(final String stringBody) throws JSONException {
        JSONObject response = getMethodResponse(stringBody);

        UserAuthentication auth = new UserAuthentication();
        auth.relayServerCode = response.getString("code");
        auth.relayServerMessage = response.getString("msg");
        auth.result = response.getString("result");
        auth.parseData(response.getString("data"));
        return auth;
    }

    static Document parseConvertDocData(Headers headers, ResponseBody body) throws JSONException, IOException {

        Document document = new Document();
        if (headers.get("Content-Type").equals("application/json;charset=UTF-8")) {
            JSONObject response = getMethodResponse(body.string());
            // 기본 메시지 리턴.. 에러인가 ?
            document.result = response.getString("result");
            document.relayServerCode = response.getString("code");
            document.relayServerMessage = response.getString("msg");
        } else {
            document.errorCode = headers.get("MO_ERRCODE"); // 문서변환서버 에러코드
            document.hash = headers.get("MO_HASHCODE");
            document.pageWidth = headers.get("MO_PAGEWIDTH");
            document.pageHeight = headers.get("MO_PAGEHEIGHT");
            document.pageCount = headers.get("MO_PAGECOUNT"); // 변환된 전체 페이지
            document.state = headers.get("MO_STATE"); //  변환 상태 ?
            document.converted = headers.get("MO_CONVERTING"); // 변환 완료 ?
            document.contentType = headers.get("Content-Type");
            document.contentLength = headers.get("Content-Length");
            document.contentDisposition = headers.get("Content-Disposition");
            document.byteImage = body.bytes();
        }

        return document;
    }
}
