package kr.go.mobile.agent.network;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;

import kr.go.mobile.agent.service.broker.Document;
import kr.go.mobile.agent.service.broker.DownloadFile;
import kr.go.mobile.agent.service.broker.MethodResponse;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class RelayResponseParser {

    public enum RESULT_TYPE {
        DEFAULT,
        AUTHENTICATION,
        DOCUMENT,
        REPORT,
        DOWNLOAD
    }

    public static <T extends MethodResponse> T parse(RESULT_TYPE parseType, Response<ResponseBody> respRelay) throws JSONException, IOException {
        switch (parseType) {
            case AUTHENTICATION:
                return (T) parseUserAuthentication(respRelay.body().string());
            case DOCUMENT:
                return (T) parseConvertDocData(respRelay.headers(), respRelay.body());
            case DOWNLOAD:
                return (T) parseConvertDownload(respRelay.headers(), respRelay.body());
            case DEFAULT:
            case REPORT:
                return (T) parseDefault(respRelay.body().string());
            default:
                throw new IllegalStateException("Unexpected value: " + parseType);
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
    private static MethodResponse parseDefault(final String stringBody) throws JSONException {
        JSONObject response = getMethodResponse(stringBody);
        MethodResponse methodResponse = new MethodResponse(response.getString("id"), // requestId or responseId ?
                response.getString("result"), // 처리 대한 결과 1(성공) or 0(실패)
                response.getString("code"), // 처리에 대한 응답 코드 (중계 서버의 코드)
                response.getString("msg") // 처리에 대한 응답 메시지 (중계 서버의 메시지)
                );

        if (methodResponse.relayServerOK()) {
            // TODO 대용량 데이터 테스트 코드. 과제 완료 시 제거
//            StringBuilder sb = new StringBuilder();
//            for (int i = 0 ; i < 6 ; i++) {
//                sb.append(response.getString("data"));
//            }
//            if (response.getString("data").length() > 60000) {
//                sb.append(response.getString("data").substring(0, 60000));
//            }
//            methodResponse.setResponse(sb.toString());
            methodResponse.setServiceServerResponse(response.getString("data")); // 실제 서버가 전달한 값.
        }
        return methodResponse;
    }

    private static UserAuthentication parseUserAuthentication(final String stringBody) throws JSONException {
        JSONObject response = getMethodResponse(stringBody);

        UserAuthentication auth = new UserAuthentication(response.getString("id"),
                response.getString("result"),
                response.getString("code"),
                response.getString("msg"));
        auth.parseData(response.getString("data"));
        return auth;
    }

    private static DownloadFile parseConvertDownload(Headers headers, ResponseBody body) throws IOException, JSONException {
        if (headers.get("Content-Type").equals("application/json;charset=UTF-8")) {
            // 기본 메시지 리턴.. 에러인가 ?
            JSONObject response = getMethodResponse(body.string());
            return new DownloadFile(response.getString("id"),
                    response.getString("result"),
                    response.getString("code"),
                    response.getString("msg"));
        } else {
            // 성공
            return new DownloadFile(headers.get("Content-Disposition"),
                    headers.get("Content-Length"),
                    body.bytes());
        }
    }

    private static Document parseConvertDocData(Headers headers, ResponseBody body) throws JSONException, IOException {

        if (headers.get("Content-Type").equals("application/json;charset=UTF-8")) {
            JSONObject response = getMethodResponse(body.string());
            // 기본 메시지 리턴.. 에러인가 ?
            return  new Document(response.getString("id"),
                    response.getString("result"),
                    response.getString("code"),
                    response.getString("msg"));
        } else {
            Document document = new Document();
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
            document.setServiceServerResponse(body.bytes());
            return document;
        }
    }
}
