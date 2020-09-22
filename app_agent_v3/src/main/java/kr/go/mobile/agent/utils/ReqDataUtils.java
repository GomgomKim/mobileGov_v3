package kr.go.mobile.agent.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import kr.go.mobile.agent.service.broker.ReqData;
import kr.go.mobile.agent.service.broker.UserAuthentication;

public class ReqDataUtils {

    static Calendar CALENDAR = Calendar.getInstance();
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.KOREA);

    /**
       Tom 200914
       Return data Json parsing
    */
    public static ReqData parseReqData(final String resp) {
        ReqData reqData = new ReqData();
        try {
            JSONObject response = new JSONObject(new JSONObject(resp).getString("methodResponse"));
            reqData.result = response.getString("result");

            JSONObject jsonData = new JSONObject(response.getString("data"));
            reqData.data = jsonData.toString();
            // TODO data 내부 json에 오는 형식에 따라 data parsing 필요
//             ex) data.verifyState = jsonData.get("verifyState").toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return reqData;
    }

}
