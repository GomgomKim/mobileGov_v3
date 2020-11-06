package kr.go.mobile.common.v3.broker;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import kr.go.mobile.agent.service.broker.BrokerTask;
import kr.go.mobile.common.v3.CommonBasedConstants;

public class Request {


    public static Request basic(String serviceID, String serviceParams) {
        return new Request(serviceID, serviceParams);
    }

    @Deprecated
    public static Request upload(String key, String fileName, Uri targetUri, String relayUrl, String extraParam) {
        if (!key.equals(priKey(relayUrl))) {
            return null;
        }
        String params;
        if (extraParam == null || extraParam.isEmpty()) {
            params = String.format("url=%s", relayUrl);
        } else {
            params = String.format("url=%s&%s", relayUrl, extraParam);
        }

        return new Request(CommonBasedConstants.BROKER_ACTION_FILE_UPLOAD, fileName, params, targetUri);
    }

    @Deprecated
    public static Request download(String key, String relayUrl, Uri downloadUri) {
        if (!key.equals(priKey(relayUrl))) {
            return null;
        }
        String params = String.format("url=%s", relayUrl);
        return new Request(CommonBasedConstants.BROKER_ACTION_FILE_DOWNLOAD, null, params, downloadUri);
    }

    private static String priKey(String priUrl) {
        String s = new String(Base64.encode(priUrl.substring(0, 25).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
        System.out.print(s);
        return s;
    }

    BrokerTask task;
    Uri targetUri;
    String fileName;

    private Request(String serviceID, String serviceParams) {
        this(serviceID, null, serviceParams, null);
    }

    public Request(String serviceID, String absolutePath, String serviceParams) {
        this(serviceID, null, serviceParams, null);
    }

    private Request(String serviceID, String fileName, String serviceParams, Uri uri) {
        this.task = BrokerTask.obtain(serviceID);
        this.targetUri = uri;
        this.fileName = fileName;
        try {
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

    public void validTargetUri(Context context) throws IOException {
        validTargetUri(context, true);
    }

    public void validTargetUri(Context context, boolean readable) throws IOException {
        if (readable)
            task.validUriForRead(context, this.targetUri, this.fileName);
        else
            task.validUriForWrite(context, this.targetUri);
    }
}
