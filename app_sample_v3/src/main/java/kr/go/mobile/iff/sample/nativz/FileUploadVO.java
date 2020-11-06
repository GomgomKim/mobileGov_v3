package kr.go.mobile.iff.sample.nativz;

public class FileUploadVO {
    String path;
    String url;
    String params;
    public void setAbsolutePath(String absolutePath) {
        this.path = absolutePath;
    }

    public void setRelayURL(String relayUrl) {
        this.url = relayUrl;
    }

    public void setExtraParams(String extraParams) {
        this.params = extraParams;
    }
}
