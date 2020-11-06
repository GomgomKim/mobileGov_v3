package com.sds.mobile.servicebrokerLib;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import java.io.ByteArrayOutputStream;
import java.io.File;

import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.broker.Response;

public class ServiceBrokerLib {

    private static final String TAG = ServiceBrokerLib.class.getSimpleName();

    private static final int SERVICE_BROKER_ERROR_CODE = -1000;
    public static final int INVALID_ARGUMENT = SERVICE_BROKER_ERROR_CODE - 1;
    public static final int STRANGE_STATE_CONTEXT = SERVICE_BROKER_ERROR_CODE - 2;
    public static final int REMOTE_SERVICE_NOT_CONNTETION = SERVICE_BROKER_ERROR_CODE - 3;
    public static final int REMOTE_SERVICE_EXCEPTION = SERVICE_BROKER_ERROR_CODE - 4;
    public static final int UNDEFINED_EXCEPTION = SERVICE_BROKER_ERROR_CODE - 5;
    public static final int RESPONSE_OK = 0;
    public static final int RESPONSE_DATA_FORMAT_INVALID = SERVICE_BROKER_ERROR_CODE - 9;

    @Deprecated
    public static final String SERVICE_DOCUMENT = "document";
    @Deprecated
    public static final String ZIP_LIST = "ziplist";
    //	@Deprecated
    public static final String SERVICE_UPLOAD = "upload";
    // 기존 upload 방식용으로 처리할때 upload2사용
    @Deprecated
    public static final String SERVICE_UPLOAD2 = "upload2";
    @Deprecated
    public static final String SERVICE_DOWNLOAD = "download";

    @Deprecated
    private long mSvcTaskStartTime = 0;
    @Deprecated
    private String mResultMsg = "-1004";
    @Deprecated
    private ByteArrayOutputStream lByteArrayOutputStream;
    @Deprecated
    private byte[] lDataBytes;
    @Deprecated
    private boolean mNeeedExitCB = false;


    static final String KEY_HOST = "host";
    static final String KEY_HEADER = "header";
    static final String KEY_TIMEOUT = "timeoutInterval";
    static final String KEY_DATA_TYPE = "dataType";

    /**
     *
     */
    public static final String KEY_SERVICE_ID = "sCode";
    /**
     *
     */
    public static final String KEY_PARAMETER = "parameter";
    /**
     *
     */
    public static final String KEY_FILE_PATH = "filePath";
    /**
     *
     */
    public static final String KEY_FILE_NAME = "fileName";

    /**
     *
     */
    public static final String SERVICE_GET_INFO = "getInfo";

    private Context mContext;
    private ServiceBrokerCB mSvcBrokerCB;
    private ResponseListener mResponseListener;

    public ServiceBrokerLib(Context context, ServiceBrokerCB cb) {
        this.mContext = context;
        this.mSvcBrokerCB = cb;
    }

    public ServiceBrokerLib(Context context, ResponseListener listener) {
        this.mContext = context;
        this.mResponseListener = listener;
    }


    public void request(Intent intent) {
        Bundle extraData = intent.getExtras();

        // 기본 정보`
        String hostURL = getHostURL(extraData);
        String serviceID = extraData.getString(KEY_SERVICE_ID, "");
        String parameter = extraData.getString(KEY_PARAMETER, "");
        // 파일 정보
        String filePath = extraData.getString(KEY_FILE_PATH, "");
        String fileName = extraData.getString(KEY_FILE_NAME, "");

        Log.d(TAG, "request >>>>> ");
        Log.d(TAG, " - HOST_URL :  " + hostURL);
        Log.d(TAG, " - SERVICE_ID :  " + serviceID);
        Log.d(TAG, " - PARAMETER :  " + parameter);
        if (filePath.length() > 0 && fileName.length() > 0) {
            Log.d(TAG, "* FILE SERVICE INFO *");
            Log.d(TAG, " - FILE_NAME :  " + fileName);
            Log.d(TAG, " - FILE_PATH :  " + filePath);
            Log.d(TAG, "* ********************* *");
        }

        if (serviceID.length() == 0) {
            ResponseEvent re = new ResponseEvent(INVALID_ARGUMENT, "ServiceID가 존재하지 않습니다.");
            mResponseListener.receive(re);
            return;
        }

        if ((serviceID.compareToIgnoreCase("download") != 0)
                && (filePath != null && filePath.length() > 0)) {
            File file = new File(filePath);
            if (file.exists() == false) {
                Log.e(TAG, "첨부파일(" + filePath + ")이 존재하지 않습니다.");
                ResponseEvent re = new ResponseEvent(STRANGE_STATE_CONTEXT, "첨부파일(" + filePath + ")이 존재하지 않습니다.");
                mResponseListener.receive(re);
                return;
            }
        }

        try {
            CommonBasedAPI.call(serviceID, parameter, (Response.Listener) this.mResponseListener);
        } catch (CommonBasedAPI.CommonBaseAPIException e) {
            Log.e("@@@", e.getMessage(), e);
            Toast.makeText(this.mContext, "요청 실패 : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public interface ServiceBrokerCB  {
        void onServiceBrokerResponse(String retMsg);
    }

    private String getHostURL(Bundle data) {
        String ret = "__DEFAULT__";
        // 별도의 서비스 IP를 사용할 경우로 현재는 가이드 되지 않고 있음.
        String connectionType = data.getString("connectionType", "");
        String ipAddress = data.getString("ipAddress", "");
        String portNumber = data.getString("portNumber", "");
        String contextUrl = data.getString("contextUrl", "");

        if (ipAddress.length() > 0
                && portNumber.length() > 0
                && connectionType.length() > 0
                && contextUrl.length() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(connectionType).append("://")
                    .append(ipAddress).append(":")
                    .append(portNumber)	.append("/")
                    .append(contextUrl);
            ret = sb.toString();
        }
        data.putString(KEY_HOST, ret);
        return ret;
    }
}
