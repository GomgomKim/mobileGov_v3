package kr.go.mobile.iff.sample.hybrid;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import kr.go.mobile.common.v3.hybrid.CBHybridException;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPlugin;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPluginResult;

public class HybridCustomAPI extends CBHybridPlugin {

    String TAG = HybridCustomAPI.class.getSimpleName();

    @Override
    public void init(Context context) {

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
}
