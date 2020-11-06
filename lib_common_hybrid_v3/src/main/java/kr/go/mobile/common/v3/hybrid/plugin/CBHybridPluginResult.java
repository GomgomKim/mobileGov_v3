package kr.go.mobile.common.v3.hybrid.plugin;

import org.json.JSONArray;
import org.json.JSONObject;

import kr.go.mobile.common.v3.CommonBasedConstants;

/**
 * Created by ChangBum Hong on 2020-07-23.
 * cloverm@infrawareglobal.com
 * WebView Callback 객체
 */
public class CBHybridPluginResult {

    private int status = CommonBasedConstants.HYBRID_ERROR_NONE; //결과 상태값
    private String retMsg; //전달 메세지
    private boolean keepCallback; //WebView에 callback 유지여부

    public CBHybridPluginResult(String retStr) {
        this.retMsg = JSONObject.quote(retStr);
    }

    public CBHybridPluginResult(String retStr, boolean isJson) {
        this.retMsg = isJson ? retStr : JSONObject.quote(retStr);
    }

    public CBHybridPluginResult(JSONObject jsonObject) {
        this.retMsg = jsonObject.toString();
    }

    public CBHybridPluginResult(JSONArray jsonArray) {
        this.retMsg = jsonArray.toString();
    }

    public CBHybridPluginResult(int retVal) {
        this.retMsg = "" + retVal;
    }

    public CBHybridPluginResult(float retVal) {
        this.retMsg = "" + retVal;
    }

    public CBHybridPluginResult(boolean retVal) {
        this.retMsg = "" + retVal;
    }

    public CBHybridPluginResult(int status, String msg) {
        this.status = status;
        this.retMsg = JSONObject.quote(msg);
    }


    public void setStatus(int status) {
        this.status = status;
    }

    public void setKeepCallback(boolean isKeep) {
        this.keepCallback = isKeep;
    }

    public String toJsonString() {
        return "{status:" + this.status + ",retMsg:" + this.retMsg + ",keepCallback:" + this.keepCallback + "}";
    }

}
