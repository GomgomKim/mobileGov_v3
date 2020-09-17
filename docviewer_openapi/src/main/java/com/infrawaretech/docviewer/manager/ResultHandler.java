package com.infrawaretech.docviewer.manager;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * DocConvertManager에게 결과 전달 Handler
 */
class ResultHandler extends Handler {

    private DocDownloadManager.DocDownloadListener mListener;

    public ResultHandler(Looper looper) {
        super(looper);
    }

    public void setListener(DocDownloadManager.DocDownloadListener listener) {
        mListener = listener;
    }

    @Override
    public void handleMessage(Message msg) {
        DocDownloadInfo info = (DocDownloadInfo) msg.obj;

        if (mListener != null) {
            mListener.onDownloadResult(info);
        }

        return;

    }
}
