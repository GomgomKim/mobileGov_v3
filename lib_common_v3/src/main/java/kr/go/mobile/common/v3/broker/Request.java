package kr.go.mobile.common.v3.broker;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Iterator;

import kr.go.mobile.agent.service.broker.BrokerTask;
import kr.go.mobile.common.v3.CommonBasedConstants;

public class Request {

    public static Request basic(String serviceID, String serviceParams) {
        return new Request(serviceID, serviceParams);
    }

    @Deprecated
    public static Request upload(String url, String localPath, String extraParam) {
        String params = String.format("path=%s;url=%s;%s", localPath, url, extraParam);
        return new Request(CommonBasedConstants.BROKER_ACTION_FILE_UPLOAD, params);
    }

    @Deprecated
    public static Request download(String url, String localPath, String extraParam) {
        String params = String.format("path=%s;url=%s;%s", localPath, url, extraParam);
        return new Request("download", params);
    }

    BrokerTask task;

    public Request(String serviceID, String serviceParams) {
        this.task = BrokerTask.obtain(serviceID);
        try {
            // JSON 포맷의 문자열을 URL 파라미터 포맷으로 변경?
            JSONObject json = new JSONObject(serviceParams);
            StringBuilder param = new StringBuilder();
            Iterator<?> it = json.keys();
            while (it.hasNext()) {
                if (param.length() != 0) {
                    param.append("&");
                }
                String key = (String) it.next();
                param.append(key).append("=").append(json.getString(key));
            }
            this.task.serviceParam = param.toString();
        } catch (JSONException e) {
            this.task.serviceParam = serviceParams;
        }
    }
}
