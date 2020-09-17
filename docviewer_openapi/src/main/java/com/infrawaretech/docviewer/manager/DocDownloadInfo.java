package com.infrawaretech.docviewer.manager;

import android.graphics.Bitmap;

/**
 * 리스너로 전달할 DownloadInfo
 */
public class DocDownloadInfo {

    private int status;
    private int totalPage;
    private int convertedPage;
    private String hashCode;
    private boolean isConverted;
    private byte[] dataArray;
    private Bitmap dataBitmap;
    private String resultMsg;

    DocDownloadInfo(int status, int totalPage, int convertedPage, String hashCode, boolean isConverted, byte[] dataArray, Bitmap dataBitmap, String resultMsg) {
        this.status = status;
        this.totalPage = totalPage;
        this.convertedPage = convertedPage;
        this.hashCode = hashCode;
        this.isConverted = isConverted;
        if (dataArray != null) {
            this.dataArray = dataArray.clone();
        }

        if (dataBitmap != null) {
            this.dataBitmap = Bitmap.createBitmap(dataBitmap);
        }

        this.resultMsg = resultMsg;
    }

    public int getStatus() {
        return this.status;
    }

    public int getTotalPage() {
        return this.totalPage;
    }

    public int getConvertedPage() {
        return this.convertedPage;
    }

    public String getHashCode() {
        return this.hashCode;
    }

    public boolean isConverted() {
        return this.isConverted;
    }

    public byte[] getDataArray() {
        if (dataArray == null) {
            return null;
        }
        return this.dataArray.clone();
    }

    public Bitmap getDataBitmap() {
        return this.dataBitmap;
    }

    public String getResultMsg() {
        return this.resultMsg;
    }
}
