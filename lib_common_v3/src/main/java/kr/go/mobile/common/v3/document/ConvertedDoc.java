package kr.go.mobile.common.v3.document;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import kr.go.mobile.agent.service.broker.Document;

public class ConvertedDoc {

    int pageDoc;
    Bitmap bitmapDoc;

    protected ConvertedDoc(int pageDocImage, Bitmap bitmap) {
        this.pageDoc = pageDocImage;
        this.bitmapDoc = bitmap;
    }

    public Bitmap getBitmapDoc() {
        return bitmapDoc;
    }

    public int getPageDoc() { return this.pageDoc; }
}
