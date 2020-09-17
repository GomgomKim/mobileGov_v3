package com.infrawaretech.docviewer.manager;

import android.os.Build;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;

import com.infrawaretech.docviewer.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 문서변환서버에 요청하는 데이터를 관리하는 클래스.
 */
public class DocDownloadManager implements ConvertStateTask.ConvertTaskFinishListener, DocRequestTask.DocReqTaskFinishListener {

    private static final String TAG = DocDownloadManager.class.getSimpleName();

    public static final int STATUS_DECODE_FAILED = -2;
    public static final int STATUS_VPN_DISCONNECTION = -1;
    public static final int STATUS_REQUEST_FAILED = 0;
    public static final int STATUS_REQUEST_BEGIN = 1;
    public static final int STATUS_DOWNLOAD_COMPLETE = 2;
    public static final int STATUS_REQUEST_COMPLETE = 3;
    public static final int STATUS_CONVERT_STATE = 4; //신규추가

    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    private static final int DOWNLOAD_SIZE = 4;
    private static DocDownloadManager sInstance;

    static {
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        sInstance = new DocDownloadManager();
    }

    public static DocDownloadManager getInstance() {
        return sInstance;
    }

    private LinkedBlockingDeque<Runnable> mReqConvertStateWorkQueue = new LinkedBlockingDeque<Runnable>();
    private LinkedBlockingDeque<Runnable> mDownloadWorkQueue = new LinkedBlockingDeque<Runnable>();
    private LinkedBlockingDeque<Runnable> mDecodeWorkQueue = new LinkedBlockingDeque<Runnable>();
    private LinkedBlockingDeque<DocRequestTask> mDocReqTaskWorkQueue = new LinkedBlockingDeque<DocRequestTask>();
    private ConvertStateTask mConvertStateTask;

    private ThreadPoolExecutor mReqConvertStateThreadPool;
    private ThreadPoolExecutor mDownloadThreadPool;
    private ThreadPoolExecutor mDecodeThreadPool;
    protected LruCache<Integer, byte[]> mDocCache;
    private ResultHandler mHandler;
    private int mDecodePoolSize;
    private List<Integer> mTaskHashList;
    private List<Future> mFutureList;

    private DocDownloadManager() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / (1024.0f * 1024.0f));

        mDecodePoolSize = 1;
        int cache_size = 4194304;
        if (maxMemory > 64) {
            mDecodePoolSize = 2;
            cache_size = 104857600;
        }

        mReqConvertStateThreadPool = new ThreadPoolExecutor(1, 1, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mReqConvertStateWorkQueue);
        // 저사양 4/4 1/1 4194304 / 3670016
        // 저사양 4/4 2/2 104857600
        mDownloadThreadPool = new ThreadPoolExecutor(DOWNLOAD_SIZE, DOWNLOAD_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mDownloadWorkQueue);
        mDecodeThreadPool = new ThreadPoolExecutor(mDecodePoolSize, mDecodePoolSize, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mDecodeWorkQueue);
        mDocCache = new LruCache<Integer, byte[]>(cache_size) {
            @Override
            protected int sizeOf(Integer paeg, byte[] data) {
                return data.length;
            }
        };


        mHandler = new ResultHandler(Looper.getMainLooper());
        mTaskHashList = new ArrayList<Integer>();
        mFutureList = new ArrayList<Future>();
    }

    //  변환 상태를 확인하기 위한 요청.
    public static boolean reqDocConvertState(String header, String param) {

        if (sInstance.mConvertStateTask == null) {
            sInstance.mConvertStateTask = new ConvertStateTask(sInstance);
        } else {
            sInstance.mConvertStateTask.setDelay(2500);
        }

        sInstance.mConvertStateTask.initialize(header, param);
        Runnable runnable = sInstance.mConvertStateTask.getRunnable();
        if (runnable != null) {
            sInstance.mFutureList.add(sInstance.mReqConvertStateThreadPool.submit(runnable));
        } else {
            return false;
        }
        return true;
    }

    public static DocRequestTask startDownload(int page, String header, String param) {

        DocRequestTask task = sInstance.mDocReqTaskWorkQueue.poll();
        if (task == null) {
            task = new DocRequestTask(sInstance);

            if (!sInstance.mTaskHashList.contains(task.hashCode())) {
                sInstance.mTaskHashList.add(task.hashCode());
            }
        }

        task.initialize(page);
        task.setByteBuffer(sInstance.mDocCache.get(page));

        if (task.getByteBuffer() == null) {
            // 데이터가 존재하지 않으므로 doc 를 다운로드 함.
            Log.e(TAG, "요청 페이지: " + page);
            task.setHeader(header);
            task.setParam(param);
            sInstance.mFutureList.add(sInstance.mDownloadThreadPool.submit(task.getDownloadRunnable()));
        } else {
            Log.e(TAG, "요청 페이지(디코딩): " + page);
            // 데이터를 가지고 있으므로 decoder 를 진행하도록 함.
            sInstance.mFutureList.add(sInstance.mDecodeThreadPool.submit(task.getDecodeRunnable()));
        }

        return task;
    }

    public void recycleTask(DocRequestTask task) {
        task.recycle();
        mDocReqTaskWorkQueue.offer(task);
    }

    public void cancelAll() {

        synchronized (sInstance) {

            sInstance.mTaskHashList.clear();
            sInstance.mDocReqTaskWorkQueue.clear();
            sInstance.mDecodeWorkQueue.clear();
            sInstance.mReqConvertStateWorkQueue.clear();
            sInstance.mDownloadWorkQueue.clear();

            for (Future future : sInstance.mFutureList) {
                future.cancel(true);
            }

            sInstance.mFutureList.clear();

            if (sInstance.mConvertStateTask != null) {
                Thread t = sInstance.mConvertStateTask.getThread();
                if (t != null) {
                    t.interrupt();
                }
                sInstance.mConvertStateTask.recycle();
            }

            sInstance.mDocCache.evictAll();
        }
    }

    void sendMessage(Message msg) {
        if (msg == null)
            return;

        msg.sendToTarget();
    }

    // 변환 상태 요청에 대한 응답을 처리
    @Override
    public void onConvertTaskFinish(ConvertStateTask task) {
        DocDownloadInfo docDownloadInfo = new DocDownloadInfo(STATUS_CONVERT_STATE, task.getTotalPage(), -1, null, task.isConverted(), null, null, null);
        Message msg = mHandler.obtainMessage(STATUS_CONVERT_STATE, docDownloadInfo);
        sendMessage(msg);
        task.recycle();
    }

    @Override
    public void onReqTaskFinish(DocRequestTask docRequestTask, int state) {

        if (!sInstance.mTaskHashList.contains(docRequestTask.hashCode())) {
            return;
        }

        switch (state) {
            case STATUS_REQUEST_BEGIN: {
                break;
            }
            case STATUS_DOWNLOAD_COMPLETE: {
                if (mDocCache.get(docRequestTask.getPage()) == null) {
                    mDocCache.put(docRequestTask.getPage(), docRequestTask.getByteBuffer());
                }

                //sInstance.mDecodeThreadPool.execute(docRequestTask.getDecodeRunnable());
                sInstance.mFutureList.add(sInstance.mDecodeThreadPool.submit(docRequestTask.getDecodeRunnable()));
                break;
            }
            case STATUS_DECODE_FAILED:
            case STATUS_REQUEST_FAILED:
            case STATUS_REQUEST_COMPLETE: {
                DocDownloadInfo docDownloadInfo = new DocDownloadInfo(state, docRequestTask.getTotalPage(), docRequestTask.getPage(), docRequestTask.getDocHashcode(), docRequestTask.isConverted(), docRequestTask.getByteBuffer(), docRequestTask.getDocPage(), docRequestTask.getResultMessage());
                recycleTask(docRequestTask);

                Message msg = mHandler.obtainMessage(state, docDownloadInfo);
                sendMessage(msg);
                break;
            }

            default:
                break;
        }
    }

    @Override
    public void onTrim() {
        synchronized (sInstance) {
            int max = sInstance.mDocCache.maxSize();
            max = max / 2;

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                sInstance.mDocCache.trimToSize(max);
            }
        }
    }

    /**
     * 결과 전달 리스너
     */
    public interface DocDownloadListener {
        void onDownloadResult(DocDownloadInfo info);
    }

    /**
     * 결과 전달 리스너 등록
     *
     * @param listener
     */
    public void setListener(DocDownloadListener listener) {
        if (mHandler != null) {
            mHandler.setListener(listener);
        }
    }

}
