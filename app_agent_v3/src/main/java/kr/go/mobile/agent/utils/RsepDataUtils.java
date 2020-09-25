package kr.go.mobile.agent.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import kr.go.mobile.agent.service.broker.RespData;
import kr.go.mobile.agent.service.broker.UserAuthentication;

public class RsepDataUtils {

    static Calendar CALENDAR = Calendar.getInstance();
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.KOREA);

    /**
     Tom 200914
     Return data Json parsing
     */
    public static UserAuthentication parseUserAuthentication(final String resp) throws JSONException {
        UserAuthentication auth = new UserAuthentication();
        JSONObject response = new JSONObject(new JSONObject(resp).getString("methodResponse"));
        auth.result = response.getString("result");

        JSONObject jsonData = new JSONObject(response.getString("data"));
        auth.verifyState = jsonData.get("verifyState").toString();
        auth.verifyStateCert = jsonData.get("verifyStateCert").toString();
        auth.verifyStateLDAP = jsonData.get("verifyStateLDAP").toString();
        auth.userID = jsonData.get("cn").toString();
        auth.userDN = jsonData.get("dn").toString();
        auth.ouName = jsonData.get("ou").toString();
        auth.ouCode = jsonData.get("oucode").toString();
        auth.departmentName = jsonData.get("companyName").toString();
        auth.departmentCode = jsonData.get("topOuCode").toString();
        auth.nickName = jsonData.get("displayName").toString();
        auth.code = response.getString("code");
        auth.msg = response.getString("msg");

        return auth;
    }

    public static RespData parseReqData(final String resp) throws JSONException {
        RespData respData = new RespData();
        JSONObject response = new JSONObject(new JSONObject(resp).getString("methodResponse"));
        respData.id = response.getString("id");
        respData.result = response.getString("result");

        JSONObject jsonData = new JSONObject(response.getString("data"));
        respData.data_result = jsonData.get("result").toString();
        respData.data_data = jsonData.get("%").toString();
        respData.data_data2 = jsonData.get("aaadasdsa2").toString();

        return respData;
    }

    public static String makeRespData(final RespData respData){
        // String 데이터로 보내는 것 같아서 생성한 함수
        // json으로 보낸다면 삭제해도 됨
        StringBuilder respStringdata = new StringBuilder();
        respStringdata.append("result : ").append(respData.data_result);
        respStringdata.append("  % : ").append(respData.data_data);
        respStringdata.append("  aaadasdsa2 : ").append(respData.data_data2);
        return respStringdata.toString();
    }

}
