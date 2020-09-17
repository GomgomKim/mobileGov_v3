package kr.go.mobile.agent.network;

import android.content.Context;
import android.provider.Settings;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DocGenerateHeaders {

    Map<String, String> defaultHeaders = new HashMap<String,String>();
    URL url;
    String serviceId;
    String agentDetail;
    String contentType;

    private final static String TAG = DocGenerateHeaders.class.getSimpleName();

    public DocGenerateHeaders(){

    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setAgentDetail(String header) {
        agentDetail = header;
    }

    /*
   헤더 제작
    */
    public Map<String, String> initDefaultHeader() {
        defaultHeaders.put("Host", url.getHost() + (url.getPort() > 0 ? ":"+url.getPort() : ""));
        defaultHeaders.put("Service-Id", this.serviceId);
        defaultHeaders.put("X-Agent-Detail", this.agentDetail);

        return defaultHeaders;
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = initDefaultHeader();

        if(this.contentType == null) {
            headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        }
        else {
            headers.put("Content-Type", this.contentType);
        }

        return headers;
    }

}
