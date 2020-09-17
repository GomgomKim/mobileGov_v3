package com.infrawaretech.docviewer;

import android.graphics.Bitmap;

/**
 * 문서 변환 데이터 클래스
 */
public class DocConvertedData {

    private byte[] bitmapByteArray;
    private int pageNum;
    private Bitmap bitmap;

    DocConvertedData(int pageNum, byte[] dataArray, Bitmap bitmap) {
        if (dataArray != null) {
            this.bitmapByteArray = dataArray.clone();
        }

        this.pageNum = pageNum;

        if (bitmap != null) {
            this.bitmap = Bitmap.createBitmap(bitmap);
        }
    }

    /**
     * 페이지 번호
     *
     * @return 페이지 번호
     */
    public int getPageNum() {
        return this.pageNum;
    }

    /**
     * 페이지 byte array
     *
     * @return 페이지 byte array
     */
    public byte[] getBytes() {
        if (this.bitmapByteArray == null) {
            return null;
        }
        return this.bitmapByteArray.clone();
    }

    /**
     * 페이지 Bitmap
     *
     * @return 페이지 Bitmap
     */
    public Bitmap getBitmap() {
        return this.bitmap;
    }
}
