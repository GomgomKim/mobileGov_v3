package kr.go.mobile.iff.sample.hybrid;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import kr.go.mobile.common.v3.hybrid.CBHybridException;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPlugin;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPluginResult;
import kr.go.mobile.iff.sample.LoadingActivity;

public class HybridCustomAPI extends CBHybridPlugin {

    String TAG = HybridCustomAPI.class.getSimpleName();

    protected HybridCustomAPI(Context context) {
        super(context);
    }

    public CBHybridPluginResult echo(String jsonArgs) throws CBHybridException {
        Log.d(TAG, "echo service : " + jsonArgs);
        return new CBHybridPluginResult(jsonArgs);
    }

    public CBHybridPluginResult getMyMessage(String jsonArgs) throws CBHybridException {
        Log.d(TAG, "getMyMessage service : " + jsonArgs);
        String message = "N/A";
        try {
            JSONObject o = new JSONObject(jsonArgs);
            message = o.getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new CBHybridPluginResult(message);
    }

    public CBHybridPluginResult toJsonMyMessage(String jsonArgs) throws CBHybridException {
        Log.d(TAG, "toJsonMyMessage service : " + jsonArgs);
        String message = "N/A";
        try {
            JSONObject o = new JSONObject(jsonArgs);
            message = o.getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject retObject = new JSONObject();
        try {
            retObject.put("retMessage", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new CBHybridPluginResult(retObject.toString());
    }

    public CBHybridPluginResult startActivityForResult(String json) {
        Intent intent = new Intent(getContext(), LoadingActivity.class);
        startActivityForResult(intent, 10);
        return new CBHybridPluginResult("startActivityForResult()");
    }

    @Override
    public String getVersionName() {
        return "1.0.0";
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
        sendAsyncResult(new CBHybridPluginResult("onActivityForResult()"));
    }
}
