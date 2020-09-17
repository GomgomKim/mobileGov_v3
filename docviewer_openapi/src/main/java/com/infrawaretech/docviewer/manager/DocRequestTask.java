package com.infrawaretech.docviewer.manager;

import android.graphics.Bitmap;

import com.infrawaretech.docviewer.manager.DocDecodeRunnable.TaskRunnableDecodeMethod;
import com.infrawaretech.docviewer.manager.DocDownloadRunnable.TaskRunnableDownloadMethod;
import com.infrawaretech.docviewer.utils.Log;

public class DocRequestTask implements TaskRunnableDownloadMethod, TaskRunnableDecodeMethod {

    private static final String TAG = "DocRequestTask";

    private int mPage;
    private Runnable mDownloadRunnable;
    private Runnable mDecodeRunnable;
    private byte[] mDocBuffer;
    private boolean isConverted;
    private Bitmap mDecodedDoc;
    private Thread mCurrentThead;

    private DocReqTaskFinishListener mListener;

    interface DocReqTaskFinishListener {
        void onReqTaskFinish(DocRequestTask task, int state);

        void onTrim();
    }


    public DocRequestTask(DocReqTaskFinishListener listener) {
        mDownloadRunnable = new DocDownloadRunnable(this);
        mDecodeRunnable = new DocDecodeRunnable(this);
        mListener = listener;
    }

    public void initialize(int page) {
        mPage = page;
    }

    String header;
    String param;

    boolean flag = false;
    private int mTotalPage;
    private String mDocHashCode;

    public void immediatelyCancel() {
        flag = true;
    }

    @Override
    public boolean cancel() {
        return flag;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setParam(String param) {
        this.param = param;
    }


    public void setByteBuffer(byte[] bs) {
        this.mDocBuffer = bs;
    }

    @Override
    public byte[] getByteBuffer() {
        if (this.mDocBuffer == null)
            return null;
        return this.mDocBuffer.clone();
    }

    public Runnable getDownloadRunnable() {
        return this.mDownloadRunnable;
    }

    public Runnable getDecodeRunnable() {
        return this.mDecodeRunnable;
    }

    public String getServiceHeader() {
        return this.header;
    }

    public String getParameter() {
        return this.param;
    }

    String mResultMessage;

    @Override
    public void setResultMessage(String result) {
        this.mResultMessage = result;
    }

    public String getResultMessage() {
        return this.mResultMessage;
    }


    @Override
    public void setDownloadThread(Thread thread) {
        setCurrentThread(thread);
    }

    @Override
    public void setDecodeThread(Thread thread) {
        setCurrentThread(thread);
    }

    public void setCurrentThread(Thread thread) {
        this.mCurrentThead = thread;
    }

    public Thread getCurrentThread() {
        return this.mCurrentThead;
    }

    @Override
    public void handleRequestState(int state) {

        switch (state) {
            case DocDownloadRunnable.TASK_BEGIN:
                handleState(DocDownloadManager.STATUS_REQUEST_BEGIN);
                break;
            case DocDownloadRunnable.TASK_COMPLETED:
                handleState(DocDownloadManager.STATUS_DOWNLOAD_COMPLETE);
                break;
            case DocDownloadRunnable.TASK_FAILED:
                handleState(DocDownloadManager.STATUS_REQUEST_FAILED);
                break;
            default:
                Log.e(TAG, "처리할 수 없는 상태값입니다. (state = " + state + ")");
                break;
        }
    }

    @Override
    public void handleDecodeState(int state) {
        switch (state) {
            case DocDecodeRunnable.TASK_FAILED:
                handleState(DocDownloadManager.STATUS_DECODE_FAILED);
                break;
            case DocDecodeRunnable.TASK_COMPLETED:
                handleState(DocDownloadManager.STATUS_REQUEST_COMPLETE);
                break;
            case DocDecodeRunnable.TASK_BEGIN:
            default:
                break;
        }
    }

    @Override
    public void setTotalPage(int totalPage) {
        this.mTotalPage = totalPage;
    }

    @Override
    public void setDocHashcode(String hashcode) {
        this.mDocHashCode = hashcode;
    }

    @Override
    public void setDocPage(Bitmap decodedDocPage) {
        this.mDecodedDoc = decodedDocPage;
    }

    public Bitmap getDocPage() {
        return this.mDecodedDoc;
    }

    public int getPage() {
        return this.mPage;
    }

    public int getTotalPage() {
        return this.mTotalPage;
    }

    public String getDocHashcode() {
        return this.mDocHashCode;
    }

    private void handleState(int state) {
        if (mListener != null) {
            mListener.onReqTaskFinish(this, state);
        }
    }

    public void recycle() {
        mDocBuffer = null;
        mDecodedDoc = null;
        mCurrentThead = null;
        param = null;
        flag = false;
    }

    @Override
    public void trimCacheSize() {
        if (mListener != null) {
            mListener.onTrim();
        }
    }

    @Override
    public void setConverted(boolean isConverted) {
        this.isConverted = isConverted;
    }

    public boolean isConverted() {
        return this.isConverted;
    }
}
