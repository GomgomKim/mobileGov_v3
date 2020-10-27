package kr.go.mobile.common.v3.broker;

import android.os.RemoteException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import kr.go.mobile.agent.service.broker.UserAuthentication;

public class SSO {

    public static SSO create() throws RemoteException {
        UserAuthentication authentication = BrokerManager.getUserAuth();
        return new SSO(authentication);
    }

    private UserAuthentication authentication;

    SSO(UserAuthentication authentication) {
        this.authentication = authentication;
    }

    public String getUserDN() {
        return authentication.getUserDN();
    }

    public String getUserID() {
        return authentication.getUserID();
    }

    public String getOuName() {
        return authentication.getOuName();
    }

    public String getOuCode() {
        return authentication.getOuCode();
    }

    public String getNickName() {
        return authentication.getNickName();
    }

    public String getDepartmentName() {
        return authentication.getDepartmentName();
    }

    public String getDepartmentCode() {
        return authentication.getDepartmentCode();
    }

    public String toJsonString() throws JSONException {
        String [] keys = {
            "gov:nickname",
            "gov:cn",
            "gov:ou",
            "gov:oucode",
            "gov:department",
            "gov:departmentnumber"
        };
        JSONObject jobj = new JSONObject();

        int i = 0;
        for (String key : keys) {
            switch (key) {
                case "gov:nickname":
                    jobj.put(key, getNickName());
                    break;
                case "gov:cn":
                    jobj.put(key, getUserID());
                    break;
                case "gov:ou":
                    jobj.put(key, getOuName());
                    break;
                case "gov:oucode":
                    jobj.put(key, getOuCode());
                    break;
                case "gov:department":
                    jobj.put(key, getDepartmentName());
                    break;
                case "gov:departmentnumber":
                    jobj.put(key, getDepartmentCode());
                    break;
            }
        }
        return jobj.toString();
    }
}
