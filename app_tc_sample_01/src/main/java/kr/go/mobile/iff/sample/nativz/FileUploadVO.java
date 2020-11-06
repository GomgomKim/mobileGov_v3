package kr.go.mobile.iff.sample.nativz;

public class FileUploadVO {
    String path;
    String url;
    String params;

    public FileUploadVO(){

    }

    public FileUploadVO(String path, String url, String params){
        this.path = path;
        this.url = url;
        this.params = params;
    }

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
