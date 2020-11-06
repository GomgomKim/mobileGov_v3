package kr.go.mobile.common.v3.hybrid.plugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Iterator;
import java.util.Objects;

import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.common.v3.broker.Request;
import kr.go.mobile.common.v3.broker.Response;
import kr.go.mobile.common.v3.broker.SSO;
import kr.go.mobile.common.v3.hybrid.CBHybridException;

public abstract class CBHybridBrokerPlugin extends CBHybridPlugin  {

    final String DOC_URL = "docUrl";
    final String DOC_NAME = "docName";
    final String DOC_CREATED_DATE = "docCreatedDate";

    protected CBHybridBrokerPlugin(Context context) {
        super(context);
    }

    @Override
    public String getVersionName() {
        return "3.0.0";
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

    }

    public CBHybridPluginResult getSSO() throws CBHybridException {
        try {
            SSO sso = CommonBasedAPI.getSSO();
            return new CBHybridPluginResult(sso.toJsonString());
        } catch (CommonBasedAPI.CommonBaseAPIException e) {
            e.printStackTrace();
            throw new CBHybridException(0, e.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR, e.getMessage());
        }
    }

    public void asyncRequest(final String callbackID, String jsonArgs) throws CBHybridException {

        try {
            JSONObject o = new JSONObject(jsonArgs);

            String serviceID = o.getString("serviceId");
            String serviceParams = o.getString("serviceParams");

            CommonBasedAPI.call(serviceID, serviceParams, new Response.Listener() {
                @Override
                public void onSuccess(Response response) {
                    int code = response.getErrorCode();
                    String result = response.getResponseString();
                    sendAsyncResult(callbackID, new CBHybridPluginResult(result));
                }

                @Override
                public void onFailure(int errorCode, String errMessage, Throwable t) {
                    t.printStackTrace();
                    sendAsyncResult(callbackID, new CBHybridPluginResult(errMessage));
                }
            });
        } catch (CommonBasedAPI.CommonBaseAPIException e) {
            e.printStackTrace();
            throw new CBHybridException(0, e.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR, e.getMessage());
        }
    }

    public void startDefaultDocumentView(String jsonArgs) throws CBHybridException {
        Log.e("@@", jsonArgs);
        if (Objects.equals(jsonArgs, "undefined")) {
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_INVALID_PARAMETER, "입력된 값이 없습니다.");
        }

        String reqDocFileURL;
        String reqDocFileName;
        String reqDocCreatedDate;
        try {
            Object o = new JSONTokener(jsonArgs).nextValue();
            if (o instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) o;
                reqDocFileURL = jsonObject.getString(DOC_URL);
                reqDocFileName = jsonObject.getString(DOC_NAME);
                reqDocCreatedDate = jsonObject.getString(DOC_CREATED_DATE);

                CommonBasedAPI.startDefaultDocViewActivity(getContext(), reqDocFileURL, reqDocFileName, reqDocCreatedDate);
            } else {
                throw new JSONException("");
            }
        } catch (JSONException e) {
            // TODO Exception 에 대한 내용을 모두 로깅 ?
            e.printStackTrace();
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_INVALID_PARAMETER, "정의되지 않은 형태로 입력되었거나 필수 값이 존재하지 않습니다.");
        }
    }


}
