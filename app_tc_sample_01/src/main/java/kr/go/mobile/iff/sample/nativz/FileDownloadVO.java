package kr.go.mobile.iff.sample.nativz;

public class FileDownloadVO {
    String path;
    String url;
    public void setAbsolutePath(String absolutePath) {
        this.path = absolutePath;
    }

    public void setRelayURL(String relayUrl) {
        this.url = relayUrl;
    }
}
