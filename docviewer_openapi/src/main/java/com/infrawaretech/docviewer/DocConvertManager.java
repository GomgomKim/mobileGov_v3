package com.infrawaretech.docviewer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.infrawaretech.docviewer.manager.DocDownloadInfo;
import com.infrawaretech.docviewer.manager.DocDownloadManager;
import com.infrawaretech.docviewer.manager.DocRequestTask;
import com.infrawaretech.docviewer.utils.VpnStatus;

import java.io.FileNotFoundException;

/**
 * 문서 변환 요청 처리 클래스
 */
public class DocConvertManager implements DocDownloadManager.DocDownloadListener, DOCViewUtil.InitCallback {

    private static final Object LOCK = new Object();

    private final int MAX_TIMEOUT = 10 * 1000;
    private final int ONCE_TIME = 3 * 1000;

    private static DocConvertManager sInstance;
    private String mHeader;
    private DocInfoFactory.Option mDocOpt;//saveHash Code 호출 부분 필요
    private ReqCallback mReqCallback;

    static {
        sInstance = new DocConvertManager();
    }

    private BroadcastReceiver mVpnBroadcastRecv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Agent의 SessionManagerService.java 코드와 동일한 데이터를 사용해야함.
            String EXTRA_STATUS = "STATUS";
            VpnStatus.setStatus(intent.getIntExtra(EXTRA_STATUS, 0 /**/));
/*
            switch (VpnStatus.getStatus()) {
                case VpnStatus.ERROR: // error ?
                    break;
                case VpnStatus.CONNECTION: // connection
                    break;
                case VpnStatus.CONNECTING: // connecting
                    break;
            }
 */
        }
    };

    public static DocConvertManager getInstance() {
        return sInstance;
    }

    /**
     * DownloadManager 결과 수신 리스너
     */
    @Override
    public void onDownloadResult(DocDownloadInfo info) {

        DocConvertedData docConvertedData;
        DocConvertStatus docConvertStatus;

        int status;
        String hashCode = info.getHashCode();

        switch (info.getStatus()) {
            case DocDownloadManager.STATUS_REQUEST_BEGIN:
                return;
            case DocDownloadManager.STATUS_CONVERT_STATE:
                status = DocConvertStatus.STATUS_CONVERT;
                break;
            case DocDownloadManager.STATUS_REQUEST_COMPLETE:
                status = DocConvertStatus.STATUS_REQ_COMPLETE;
                break;
            case DocDownloadManager.STATUS_REQUEST_FAILED:
                status = DocConvertStatus.STATUS_REQ_FAILED;
                break;
            case DocDownloadManager.STATUS_DECODE_FAILED:
            default:
                status = DocConvertStatus.STATUS_DECODE_FAILED;
        }

        if (hashCode != null) {
            mDocOpt.saveHashCode(hashCode);
        }

        if (info.getDataArray() != null) {
            docConvertedData = new DocConvertedData(info.getConvertedPage(), info.getDataArray(), info.getDataBitmap());
        } else {
            docConvertedData = new DocConvertedData(info.getConvertedPage(), new byte[]{}, null);
        }

        docConvertStatus = new DocConvertStatus(status, info.getTotalPage(), info.getConvertedPage(), hashCode, info.isConverted(), info.getResultMsg());

        if (mReqCallback != null) {
            mReqCallback.onReqCallback(docConvertStatus, docConvertedData);
        }
    }

    /**
     * 사용자 정보 요청 결과 수신 콜백
     */
    @Override
    public void onInitResult(boolean result, String msg) {
        synchronized (LOCK) {
            if (result == true) {
                mHeader = msg;
            }
            LOCK.notify();
        }
    }

    public interface ReqCallback {
        void onReqCallback(DocConvertStatus status, DocConvertedData data);
    }

    private DocConvertManager() { }

    /**
     * 문서 변환 서비스 헤더 생성
     *
     * @param url      문서 URL
     * @param fileName 문서 파일명
     * @return 문서 정보
     * @throws DocConvertException 파라미터가 정상적이지 않은 경우 발생
     */
    private DocInfoFactory.Option initDocInfoOption(String url, String fileName) throws DocConvertException {
        // intent로 전달받은 문서 정보 -> DocInfoFactory.Option 생성
        DocInfoFactory.Option opts = new DocInfoFactory.Option();
        opts.InServiceId = "";
        opts.InTargetUrl = url;
        String tmpFileName = fileName;

        if (tmpFileName.contains(";")) {
            tmpFileName = tmpFileName.replace(";", "");
        }
        opts.InFileName = tmpFileName;

        // - 확장자 파라미터 값 제거.
        int index = tmpFileName.lastIndexOf(".") + 1;
        if (index < 0) {
            throw new DocConvertException(DocConvertException.CODE_INVALID_PARAM, DocConvertException.MSG_INVALID_PARAM);
        } else {
            opts.InFileExt = tmpFileName.substring(index, tmpFileName.length());
        }

        opts.InCreated = "";
        opts.mHashCode = "";
        opts.InServiceHeader = mHeader;

        try {
            opts.valid();
        } catch (FileNotFoundException e) {
            throw new DocConvertException(DocConvertException.CODE_INVALID_PARAM, DocConvertException.MSG_INVALID_PARAM);
        }

        return opts;
    }

    /**
     * 문서변환매니저 초기화
     *
     * @param ctx Context
     */
    public void init(Context ctx) {
        //사용자 정보 요청
        DOCViewUtil.getInstance().init(ctx, this);
        //DownloadManager 리스너 등록
        DocDownloadManager.getInstance().setListener(this);
        // TODO Agent의 SessionManagerService.java 코드와 동일한 데이터를 사용해야함.
        String ACTION_VPN_STATUS = "kr.go.mobile.docView.vpn.STATUS";
        // VPN 상태값 리시버 등록
        ctx.registerReceiver(mVpnBroadcastRecv, new IntentFilter(ACTION_VPN_STATUS));
    }

    /**
     * 문서 파일이름과 URL, 콜백 설정
     *
     * @param reqDocFileName 문서 파일명
     * @param reqDocFileURL  문서 URL
     * @param callback       문서변환 요청 결과 수신 콜백
     * @throws DocConvertException 초기화가 정상적으로 되지 않은 경우 발생
     */
    public void setTargetDoc(String reqDocFileName, String reqDocFileURL, ReqCallback callback) throws DocConvertException {
        validHeader();

        if (reqDocFileName == null || reqDocFileURL == null || callback == null) {
            throw new DocConvertException(DocConvertException.CODE_INVALID_PARAM, DocConvertException.MSG_INVALID_PARAM);
        }

        if (reqDocFileName.isEmpty() || reqDocFileURL.isEmpty() ) {
            throw new DocConvertException(DocConvertException.CODE_INVALID_PARAM, DocConvertException.MSG_INVALID_PARAM);
        }

        mDocOpt = initDocInfoOption(reqDocFileURL, reqDocFileName);
        mReqCallback = callback;
    }

    /**
     * 입력 페이지 변환 요청
     *
     * @param reqPage 요청 페이지 번호
     * @throws DocConvertException 초기화가 정상적으로 되지 않은 경우 발생
     */
    public void requestConvertedData(int reqPage) throws DocConvertException {
        validHeader();

        if (mDocOpt == null || mReqCallback == null) {
            throw new DocConvertException(DocConvertException.CODE_NOT_SET, DocConvertException.MSG_NOT_SET);
        }
        DocRequestTask task = DocDownloadManager.getInstance().startDownload(reqPage, mHeader, mDocOpt.genParamenter(reqPage));
        if (task == null) {
            throw new DocConvertException(DocConvertException.CODE_NOT_SET, DocConvertException.MSG_NOT_SET);
        }
    }

    /**
     * 문서 변환 요청
     *
     * @throws DocConvertException 초기화가 정상적으로 되지 않은 경우 발생
     */
    public void beginConvertDoc() throws DocConvertException {
        validHeader();

        if (mDocOpt == null || mReqCallback == null) {
            throw new DocConvertException(DocConvertException.CODE_NOT_SET, DocConvertException.MSG_NOT_SET);
        }

        if (DocDownloadManager.getInstance().startDownload(1, mHeader, mDocOpt.genParamenter(1)) == null) {
            throw new DocConvertException(DocConvertException.CODE_NOT_SET, DocConvertException.MSG_NOT_SET);
        }
    }

    /**
     * 문서 변환 정보 요청
     *
     * @throws DocConvertException 초기화가 정상적으로 되지 않은 경우 발생
     */
    public void requestConvertStatus() throws DocConvertException {
        validHeader();

        if (mDocOpt == null || mReqCallback == null) {
            throw new DocConvertException(DocConvertException.CODE_NOT_SET, DocConvertException.MSG_NOT_SET);
        }

        DocDownloadManager.getInstance().reqDocConvertState(mHeader, mDocOpt.genParamenter(1, true));
    }



    private void validHeader() throws DocConvertException {
        synchronized (LOCK) {
            int waitTime = 0;
            while ((DOCViewUtil.getInstance().inProgress() || mHeader == null)
                    && waitTime < MAX_TIMEOUT) {
                try {
                    LOCK.wait(ONCE_TIME);
                    waitTime += ONCE_TIME;
                } catch (InterruptedException e) {
                    if (mHeader == null) {
                        throw new DocConvertException(DocConvertException.CODE_NOT_INIT, DocConvertException.MSG_NOT_INIT);
                    }
                }
            }
        }
    }

    /**
     * 요청 처리중인 작업 모두 취소
     */
    public void cancelAll(Context context) {
        DocDownloadManager.getInstance().cancelAll();
        if (context != null) {
            context.unregisterReceiver(mVpnBroadcastRecv);
        }
    }

}
