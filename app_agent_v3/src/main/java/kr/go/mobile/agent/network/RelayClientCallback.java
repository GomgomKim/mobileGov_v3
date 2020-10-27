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
    private final String TAG = RelayClientCallback.class.getSimpleName();

    private final ResultParser.RESULT_TYPE parseType;
    private final IBrokerServiceCallback brokerCallback;
    private final Context context;

    public RelayClientCallback(Context context, ResultParser.RESULT_TYPE type, IBrokerServiceCallback brokerCallback) {
        this.context = context;
        this.parseType = type;
        this.brokerCallback = brokerCallback;
    }

    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        Log.d(TAG, "enqueue onResponse : call - " + call + " / response - " + response);
        Log.d(TAG, "Result Message : " + response.message());

        BrokerResponse<?> resp;

        if (response.isSuccessful()) {
            // http code : 2xx 통신 성공
            try {
                MethodResponse respData = ResultParser.parse(parseType, response);
                if (respData.relayServerOK()) {
                    resp = new BrokerResponse<>(respData);
                } else {
                    Log.e(TAG, context.getString(R.string.BROKER_ERROR_RELAY_SERVER)
                            + "(message : " + respData.relayServerMessage+ ", code : "+respData.relayServerCode +")");
                    resp = new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_RELAY_SERVER, context.getString(R.string.BROKER_ERROR_RELAY_SERVER)
                            + "(message : " + respData.relayServerMessage+ ", code : "+respData.relayServerCode +")");
                }
            } catch (IOException | JSONException e) {
                /*
                1. 응답데이터의 body가 잘못온 경우
                2. JSON 파싱 중 오류
                 */
                Log.e(TAG, context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + e.getMessage() + ")", e);
                resp = new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_PROC_DATA,
                        context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + e.getMessage() + ")");
            }

        } else {
            // http 오류 코드 응답 (ex. 4.. 5..)
            Log.e(TAG, context.getString(R.string.BROKER_ERROR_HTTP_RESPONSE_STRING) + " (HTTP 상태코드 : " + response.code() + ")");
            resp = new BrokerResponse<>(CommonBasedConstants.BROKER_ERROR_HTTP_RESPONSE,
                    context.getString(R.string.BROKER_ERROR_HTTP_RESPONSE_STRING) + " (HTTP 상태코드 : " + response.code() + ")");
        }

        try {
            brokerCallback.onResponse(resp);
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
                brokerCallback.onFailure(CommonBasedConstants.BROKER_ERROR_PROC_DATA, context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + t.getMessage() + ")");
            } else {
                // 그 외 ?
                Log.e(TAG, context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + t.getMessage() + ")");
                brokerCallback.onFailure(CommonBasedConstants.BROKER_ERROR_PROC_DATA, context.getString(R.string.BROKER_ERROR_PROC_DATA_STRING) + "(" + t.getMessage() + ")");
            }
        } catch (RemoteException ignored) {
        }
    }
}
