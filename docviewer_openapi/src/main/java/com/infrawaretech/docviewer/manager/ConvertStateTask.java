package com.infrawaretech.docviewer.manager;

import com.infrawaretech.docviewer.manager.ReqConvertStateRunnable.TaskRunnableConvertStateMethod;

class ConvertStateTask implements TaskRunnableConvertStateMethod {

    private String mServiceHeader;
    private String mParam;
    private ReqConvertStateRunnable mReqConvertStateRunnable;
    private Thread mThread;
    private boolean isOperate;
    private boolean isConverted;
    private int mTotalPage;
    private int mDelay;

    private ConvertTaskFinishListener mListener;

    interface ConvertTaskFinishListener {
        void onConvertTaskFinish(ConvertStateTask task);
    }

    public ConvertStateTask(ConvertTaskFinishListener listener) {
        mReqConvertStateRunnable = new ReqConvertStateRunnable(this);
        mListener = listener;
    }

    public ReqConvertStateRunnable getRunnable() {
        if (isOperate || isConverted)
            return null;
        isOperate = true;
        return mReqConvertStateRunnable;
    }

    public void initialize(String serviceHeader, String param) {
        //   sManager = manager;
        mServiceHeader = serviceHeader;
        mParam = param;
    }

    public void recycle() {
        isOperate = false;
        isConverted = false;
        mTotalPage = 0;
        mThread = null;
        mDelay = 0;
    }

    public String getServiceHeader() {
        return this.mServiceHeader;
    }

    public String getParam() {
        return this.mParam;
    }

    @Override
    public void setTotalPage(int totalPage) {
        this.mTotalPage = totalPage;
    }

    @Override
    public void setConverted(boolean isConverted) {
        this.isConverted = isConverted;
    }

    public boolean isConverted() {
        return this.isConverted;
    }

    public int getTotalPage() {
        return this.mTotalPage;
    }

    @Override
    public void handleState() {
        if (mListener != null) {
            mListener.onConvertTaskFinish(this);
        }
    }

    @Override
    public void setThread(Thread thread) {
        this.mThread = thread;
    }

    public Thread getThread() {
        return this.mThread;
    }

    public void setDelay(int delay) {
        this.mDelay = delay;
    }

    @Override
    public int getDelay() {
        return this.mDelay;
    }

}
