package kr.go.mobile.agent.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import kr.go.mobile.agent.service.broker.RespData;

public class ReqDataUtils {

    static Calendar CALENDAR = Calendar.getInstance();
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.KOREA);

    /**
       Tom 200914
       Return data Json parsing
    */
    public static RespData parseReqData(final String resp) throws JSONException {
        RespData respData = new RespData();
       // try {
            JSONObject response = new JSONObject(new JSONObject(resp).getString("methodResponse"));
            respData.result = response.getString("result");

            JSONObject jsonData = new JSONObject(response.getString("data"));
            respData.data = jsonData.toString();
            // TODO data 내부 json에 오는 형식에 따라 data parsing 필요
//             ex) data.verifyState = jsonData.get("verifyState").toString();

       // } catch (JSONException e) {
            // README 여기서 예외사항을 처리하지 않고 .. 넘기면 ?? 다음에는 어떻게 될까요 ?
            // e.printStackTrace();
        //}
        return respData;
    }

}
