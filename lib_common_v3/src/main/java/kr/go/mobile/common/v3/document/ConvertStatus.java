package kr.go.mobile.common.v3.document;

import kr.go.mobile.agent.service.broker.Document;

public class ConvertStatus {

    public static ConvertStatus parse(Document resp) {
        ConvertStatus status = new ConvertStatus();
        status.hash = resp.hash;
        status.converted = Boolean.parseBoolean(resp.converted);
        status.convertedPage = Integer.parseInt(resp.pageCount);
        return status;
    }

    private String hash;
    private Boolean converted;
    private int convertedPage;

    private ConvertStatus() { }

    public boolean isConverted() {
        return converted;
    }

    public int getConvertedPageCount() {
        return convertedPage;
    }

    public String getConvertDocHash() {
        return hash;
    }
}
