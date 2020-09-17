package com.infrawaretech.docloader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class DocLoadManager {
    private static final String TAG = "DocLoadManager" ;
    /**
     * 2017-12-12 지원센터의 요청으로 추가. 기존의 IP:PORT 값을 개선후에서는 ServiceID 값으로 전달하도록 함. 문서변환을 요청할 문서의 SERVICE ID
     */
    public static final String EXTRA_SERVICE_ID = "serviceId";
    /**
     * 문서변환을 요청할 문서의 URL
     */
    public static final String EXTRA_URL = "url";
    /**
     * 문서변환을 요청할 문서의 파일명
     */
    public static final String EXTRA_FILE_NAME = "fileName";
    /**
     * 문서변환을 요청할 문서의 생성날짜
     */
    public static final String EXTRA_CREATED = "createDate";

    static int VIEWPAGER_OFF_SCREEN_PAGE_LIMIT = 1;

    private Context mContext;

    public void init(Context ctx) {
        this.mContext = ctx;
        /*
         * [2017-06-13][YOONGI][ISSUE #02] 저사양한 단말에서는 동시에 많은 이미지 작업을 요청할 경우 out of memory 발생으로 정상동작을 할
         * 수 없다. 이로 인한 비정상 실행을 해결하기 위하여 하나의 작업만 요청하도록 한다.
         */
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / (1024.0f * 1024.0f));
        if (maxMemory > 128) {
            VIEWPAGER_OFF_SCREEN_PAGE_LIMIT = 2;
        }
        DocUtils.getInstance().init(ctx);
    }

    public void setDocInfo( Bundle extra) throws IllegalArgumentException {
        Log.ENABLE = extra.getBoolean("LOG", false);
        android.util.Log.d(TAG, "Document Loader Manager : Log Enable :: " + Log.ENABLE);

        // intent로 전달받은 문서 정보 -> DocInfoFactory.Option 생성
        DocInfoFactory.Option opts = new DocInfoFactory.Option(this.mContext);
        opts.InServiceId = extra.getString(DocLoadManager.EXTRA_SERVICE_ID, "");
        opts.InTargetUrl = extra.getString(DocLoadManager.EXTRA_URL);
        String tmpFileName = extra.getString(DocLoadManager.EXTRA_FILE_NAME);

        if (tmpFileName.contains(";")) {
            tmpFileName = tmpFileName.replace(";", "");
        }
        opts.InFileName = tmpFileName;

        // 2017-12-07 지원센터 (임석일)로 부터 요청사항 적용
        // 확장자 파라미터 값 제거.
        int index = tmpFileName.lastIndexOf(".") + 1;
        if (index < 0) {
            throw new IllegalArgumentException("잘못된 아큐먼트값을 입력하였습니다.");
        } else {
            opts.InFileExt = tmpFileName.substring(index, tmpFileName.length());
        }

        opts.InCreated = extra.getString(DocLoadManager.EXTRA_CREATED, "");
        try {
            opts.InServiceHeader = DocUtils.getInstance().getServiceHeader(this.mContext);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("패키지 정보를 획득할 수 없습니다.");
        }

        Log.d(TAG, "DocInfoFactory.Option :: " + opts.toString());

        opts.valid();
    }

}
