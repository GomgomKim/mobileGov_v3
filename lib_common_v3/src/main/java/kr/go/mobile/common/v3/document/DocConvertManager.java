package kr.go.mobile.common.v3.document;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import kr.go.mobile.agent.service.broker.Document;
import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.common.v3.broker.Response;

public class DocConvertManager implements DecodeDocTask.ITrimCacheSizeAction {

    static final String TAG = DocConvertManager.class.getSimpleName();

    public static final int MANAGER_CONVERT_COMPLETED = 7000;
    public static final int MANAGER_CONVERT_FAILED = 7001;
    public static final int MANAGER_STATUS_UPDATE = 8000;

    private static final DocConvertManager instance = new DocConvertManager();

    public static DocConvertManager create(String url, String fileName, String createdDate) throws DocConvertException {
        int index = fileName.lastIndexOf(".") + 1;
        if (index < 1) {
            throw new DocConvertException("문서 파일명이 잘못 입력되었습니다.");
        }
        String docExt = fileName.substring(index);
        instance.clear();
        instance.initOption(url, fileName, docExt, (createdDate == null) ? "" : createdDate);
        return instance;
    }



    public interface DocConvertListener {
        void updateConvertedStatus(ConvertStatus status);
        void onConverted(ConvertedDoc convertedDoc);
        void onFailed(int errorCode, String message);
    }

    static class DocConvertOption {
        private AtomicBoolean checkStatus;
        private String docUrl;
        private String docFileName;
        private String docExt;
        private String docCreatedDate;
        private String docHashCode;

        public boolean needStatus() {
            return checkStatus.compareAndSet(true, false);
        }

        String downloadConvertedDoc(int page) throws JSONException {
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("url", docUrl);
            jsonParams.put("fileName", docFileName);
            jsonParams.put("page", page);
            if (docCreatedDate.equals("")) {
                jsonParams.put("ext", docExt);
            } else {
                jsonParams.put("ext", docExt + "@" + docCreatedDate);
            }
            jsonParams.put("requesthashcode", docHashCode);

            return jsonParams.toString();
        }

        String getConvertStatus() throws JSONException {
            return downloadConvertedDoc(1).toString();
        }
    }

    private final Object mLock = new Object();

    private DocConvertOption option;
    private DocConvertListener listener;
    private ThreadPoolExecutor decodePoolExecutor;
    private int sizeCoreThread = 10;
    private int sizeTotalThread = 20;
    private int capacityQueue = 30;
    private int keepAliveTime = 5; // thread 가 full 일 경우 5 초 대기
    private LruCache<Integer, byte[]> encodeDocCache;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            // MainThread (UI 처리 가능)

            switch (msg.what) {
                case MANAGER_CONVERT_COMPLETED:
                    listener.onConverted((ConvertedDoc)msg.obj);
                    break;
                case MANAGER_STATUS_UPDATE:
                    listener.updateConvertedStatus((ConvertStatus) msg.obj);
                    break;
                case MANAGER_CONVERT_FAILED:
                    listener.onFailed(msg.arg1, msg.obj == null ? "decode bytes is null." : (String) msg.obj);
                default:
            }
        }
    };

    DocConvertManager() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / (1024.0f * 1024.0f));
        int cache_size = 4194304;
        if (maxMemory > 64) {
            cache_size = 104857600;
        }
        encodeDocCache = new LruCache<Integer, byte[]>(cache_size) {
            @Override
            protected int sizeOf(Integer paeg, byte[] data) {
                return data.length;
            }
        };
        decodePoolExecutor = new ThreadPoolExecutor(sizeCoreThread, sizeTotalThread,
                keepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(capacityQueue)) {

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (t != null) {
                    Log.e("", t.getMessage(), t);
                }
            }
        };
    }

    private void initOption(String url, String fileName, String docExt, String createdDate) {
        option = new DocConvertOption();
        option.checkStatus = new AtomicBoolean(true);
        option.docUrl = url;
        option.docFileName = fileName;
        option.docExt = docExt;
        option.docCreatedDate = (createdDate == null ? "" : createdDate);
        option.docHashCode = "";
    }

    public void clear() {
        encodeDocCache.evictAll();
    }

    public void setConvertedListener(DocConvertListener docConvertListener) {
        this.listener = docConvertListener;
    }

    private void requestConvertStatus() throws DocConvertException {
        Log.d(TAG, "request convert status");
        try {
            CommonBasedAPI.call(CommonBasedConstants.BROKER_ACTION_CONVERT_STATUS_DOC,
                    option.getConvertStatus(),
                    new Response.Listener() {
                @Override
                public void onSuccess(Response response) {
                    if (response.getErrorCode() == CommonBasedConstants.BROKER_ERROR_NONE) {
                        try {
                            Document doc = response.getResponse();
                            ConvertStatus status = ConvertStatus.parse(doc);
                            updateDocStatus(status);
                        } catch (ClassCastException e) {
                            throw new RuntimeException("문서변환 중 전달받은 데이터가 문서 타입이 아닙니다.");
                        }
                    } else {
                        Log.e(TAG, "onSuccess : " + response.getErrorCode() + ",  " + response.getErrorMessage());
                        Message m = handler.obtainMessage(MANAGER_CONVERT_FAILED, response.getErrorCode(), 0, response.getErrorMessage());
                        m.sendToTarget();
                    }
                }

                @Override
                public void onFailure(int errorCode, String errMessage, Throwable t) {
                    Log.d(TAG, "onFailure : " + errMessage);
                    Message m = handler.obtainMessage(MANAGER_CONVERT_FAILED, errorCode, 0, errMessage);
                    m.sendToTarget();
                }
            });
        } catch (CommonBasedAPI.CommonBaseAPIException e) {
            e.printStackTrace();
            throw new DocConvertException("브로커 서비스 연계 중 에러가 발생하였습니다.", e);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new DocConvertException("요청 데이터 생성 중 에러가 발생하였습니다.", e);
        }
    }

    void updateDocStatus(ConvertStatus status) {
        option.docHashCode = status.getConvertDocHash();
        try {
            if (!status.isConverted()) {
                requestConvertStatus();
            }
            Message m = handler.obtainMessage(DocConvertManager.MANAGER_STATUS_UPDATE, status);
            m.sendToTarget();
        } catch (DocConvertException e) {
            Log.w(TAG, "문서 변환 상태 조회를 할 수 없습니다.", e);
            listener.onFailed(CommonBasedConstants.CONVERT_DOC_ERROR_CAN_NOT_STATUS, "문서 변환 상태 조회를 할 수 없습니다.");
        }
    }

    public void requestConvertedData(final int page) throws DocConvertException {

        Log.d(TAG, "request page = " + page);
        // cache 에 존재하면 바로 리턴.
        byte[] convertedDoc;
        synchronized (mLock) {
            convertedDoc = encodeDocCache.get(page);
        }
        if (convertedDoc == null) {
            // 캐쉬에 변환된 문서 정보가 존재하지 않으므로, 다운로드 요청
            Log.d(TAG, "no docCache. request BrokerManager (page = " + page + ")");

            try {
                CommonBasedAPI.call(CommonBasedConstants.BROKER_ACTION_LOAD_DOCUMENT,
                        option.downloadConvertedDoc(page),
                        new Response.Listener() {
                    @Override
                    public void onSuccess(Response response) {

                        if (response.getErrorCode() == CommonBasedConstants.BROKER_ERROR_NONE) {
                            try {
                                Document doc = response.getResponse();
                                DecodeDocTask task = DecodeDocTask.obtain(page, doc.getResponseBytes(), DocConvertManager.this, handler);
                                decodePoolExecutor.execute(task);
                                synchronized (mLock) {
                                    encodeDocCache.put(page, doc.getResponseBytes());
                                    if (option.needStatus()) {
                                        Log.d(TAG, "required StatusCheck");
                                        ConvertStatus status = ConvertStatus.parse(doc);
                                        updateDocStatus(status);
                                    }
                                }
                            } catch (ClassCastException e) {
                                throw new RuntimeException("문서변환 중 전달받은 데이터가 문서 타입이 아닙니다.");
                            }
                        } else {
                            Log.e(TAG, "onSuccess : " + response.getErrorCode() + ",  " + response.getErrorMessage());
                            Message m = handler.obtainMessage(MANAGER_CONVERT_FAILED, response.getErrorCode(), 0, response.getErrorMessage());
                            m.sendToTarget();
                        }
                    }

                    @Override
                    public void onFailure(int errorCode, String errMessage, Throwable t) {
                        Log.d(TAG, "onFailure : " + errMessage);
                        Message m = handler.obtainMessage(MANAGER_CONVERT_FAILED, errorCode, 0, errMessage);
                        m.sendToTarget();
                    }
                });
            } catch (CommonBasedAPI.CommonBaseAPIException e) {
                e.printStackTrace();
                throw new DocConvertException("브로커 서비스 연계 중 에러가 발생하였습니다.", e);
            } catch (JSONException e) {
                e.printStackTrace();
                throw new DocConvertException("요청 데이터 생성 중 에러가 발생하였습니다.", e);
            }
        } else {
            Log.d(TAG, "exist docCache. request BrokerManager (page = " + page + ")");
            DecodeDocTask task = DecodeDocTask.obtain(page, convertedDoc, this, handler);
            decodePoolExecutor.execute(task);
        }
    }

    @Override
    public void onTrim() {
        synchronized (mLock) {
            int max = encodeDocCache.maxSize();
            max = max / 2;
            encodeDocCache.trimToSize(max);
        }
    }

    protected void failedRequest(DocConvertException e) {
        listener.onFailed(CommonBasedConstants.CONVERT_DOC_ERROR_CAN_NOT_CONVERT, e.getMessage());
    }

    public static class DocConvertException extends Exception {

        public DocConvertException(String s, Throwable e) {
            super(s, e);
        }

        public DocConvertException(String s) {
            super(s);
        }
    }
}
