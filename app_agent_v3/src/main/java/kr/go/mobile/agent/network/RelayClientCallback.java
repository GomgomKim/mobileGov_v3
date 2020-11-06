package kr.go.mobile.agent.network;

import android.content.Context;
import android.os.RemoteException;

import org.json.JSONException;

import java.io.IOException;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;
import kr.go.mobile.agent.service.broker.MethodResponse;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.mobp.iff.R;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RelayClientCallback implements Callback<ResponseBody> {
    private final String TAG = "RelayClient-asyncHandler"; //RelayClientCallback.class.getSimpleName();

    private final RelayResponseParser.RESULT_TYPE parseType;
    private final IBrokerServiceCallback brokerCallback;
    private final Context context;

    public RelayClientCallback(Context context, RelayResponseParser.RESULT_TYPE type, IBrokerServiceCallback brokerCallback) {
        this.context = context;
        this.parseType = type;
        this.brokerCallback = brokerCallback;
    }

    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        BrokerResponse<?> resp;

        if (response.isSuccessful()) {
            // http code : 2xx 통신 성공
            try {
                MethodResponse respData = RelayResponseParser.parse(parseType, response);
                if (respData.relayServerOK()) {
                    resp = new BrokerResponse<>(respData);
                    Log.TC("서버 >> 응답데이터 -> 객체변환");
                } else {
                    Log.e(TAG, context.getString(R.string.BROKER_ERROR_RELAY_SERVER)
                            + "(message : " + respData.getRelayServerMessage()+ ", code : "+respData.getRelayServerCode() +")");
                    resp = new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_RELAY_SYSTEM, context.getString(R.string.BROKER_ERROR_RELAY_SERVER)
                            + "(message : " + respData.getRelayServerMessage()+ ", code : "+respData.getRelayServerCode() +")");
                    Log.TC("서버 >> 공통기반 시스템 에러 --> 객체변환");
                }
            } catch (IOException | JSONException e) {
                /*
                1. 응답데이터의 body가 잘못온 경우
                2. JSON 파싱 중 오류
                 */
                Log.e(TAG, context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + e.getMessage() + ")", e);
                resp = new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_INVALID_RESPONSE,
                        context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + e.getMessage() + ")");
                Log.TC("서버 >> 응답 데이터 생성 및 처리 에러 --> 객체변환");
            }
        } else {
            // http 오류 코드 응답 (ex. 4.. 5..)
            Log.e(TAG, context.getString(R.string.BROKER_ERROR_HTTP_RESPONSE_STRING) + " (HTTP 상태코드 : " + response.code() + ")");
            resp = new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_SERVICE_SERVER,
                    context.getString(R.string.BROKER_ERROR_HTTP_RESPONSE_STRING) + " (HTTP 상태코드 : " + response.code() + ")");
            Log.TC("서버 >> 기관 서버 에러 --> 객체변환");
        }

        try {
            Log.TC("클라이언트 >>(응답객체)>> 브로커");
            brokerCallback.onResponse(resp);
            if (parseType == RelayResponseParser.RESULT_TYPE.DEFAULT) {
                Log.TC("클라이언트 >>브로커(기관 서비스 처리결과)>> 행정앱");
            }
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void onFailure(Call<ResponseBody> call, Throwable t) {
        Log.d(TAG, "enqueue onFailure : code - " + call + " / msg - " + t);
        try {
            // retrofit, okhttp, adapter, converter 중에서 에러가 난 상황
            if (t instanceof IOException) {
                // 네트워크 오류  통신이 불가능한 상황(WIFI미접속 등)
                Log.e(TAG, context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + t.getMessage() + ")");
                brokerCallback.onFailure(CommonBasedConstants.BROKER_ERROR_FAILED_REQUEST, context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + t.getMessage() + ")");
            } else {
                // 그 외 ?
                Log.e(TAG, context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + t.getMessage() + ")");
                brokerCallback.onFailure(CommonBasedConstants.BROKER_ERROR_FAILED_REQUEST, context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + t.getMessage() + ")");
            }
        } catch (RemoteException ignored) {
        }
    }
}
