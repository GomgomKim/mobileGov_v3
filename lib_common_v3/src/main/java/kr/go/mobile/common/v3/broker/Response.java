package kr.go.mobile.common.v3.broker;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.MethodResponse;
import kr.go.mobile.common.v3.CommonBasedConstants;

public class Response {


    public interface Listener {
        /**
         * 요청 처리가 성공한 경우 호출됨.
         */
        void onSuccess(Response response);

        /**
         * 요청 처리 중 에러가 발생한 경우 호출됨.
         */
        void onFailure(int errorCode, String errMessage, Throwable t);
    }

    public static Response convert(BrokerResponse<?> brokerResponse) {
        if (brokerResponse.getCode() == CommonBasedConstants.BROKER_ERROR_NONE) {
            MethodResponse data = brokerResponse.getResult();
            return new Response(data);
        }
        return new Response(brokerResponse.getCode(), brokerResponse.getErrorMessage());
    }

    public static Response makeError(int error, String cause) {
        return new Response(error, cause);
    }

    int errorCode;
    String errorMessage;
    Throwable errorThrowable;

    MethodResponse resp;

    private Response(MethodResponse resp) {
        this.errorCode = CommonBasedConstants.BROKER_ERROR_NONE;
        this.resp = resp;
    }

    private Response(int errorCode, String resp) {
        this(errorCode, resp, null);
    }

    private Response(int errorCode, String resp, Throwable t) {
        this.errorCode = errorCode;
        this.errorMessage = resp;
        this.errorThrowable = t;
    }

    public boolean OK() {
        return this.getErrorCode() == CommonBasedConstants.BROKER_ERROR_NONE;
    }

    public <T extends MethodResponse> T getResponse() {
        return (T) resp;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getResponseString() {
        return this.resp.getServiceServerResponse();
    }
}
