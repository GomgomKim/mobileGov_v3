package kr.go.mobile.agent.service.broker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class DownloadFile extends MethodResponse {

    String filename;
    String contentLength;

    public DownloadFile(String id, String result, String code, String msg) {
        super(id, result, code, msg);
    }

    public DownloadFile(String fileName, String fileSize, byte[] bytes) {
        super();
        int begin = fileName.indexOf("\"") + 1;
        int end = fileName.lastIndexOf("\"");
        this.filename = fileName.substring(begin, end);
        this.contentLength = fileSize;
        this.data = bytes;
    }

    public int getContentLength() {
        return Integer.parseInt(this.contentLength);
    }

    @Override
    public String getServiceServerResponse() {
        JSONObject o = new JSONObject();
        try {
            o.put("filename", this.filename);
            o.put("size", this.contentLength);
            return o.toString();
        } catch (JSONException e) {
            return String.format("filename=%s&filesize=%s", this.filename, this.contentLength);
        }
    }
}
