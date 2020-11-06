package com.sds.mobile.servicebrokerLib;

import android.app.Activity;
import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.common.v3.broker.Response;
import kr.go.mobile.common.v3.broker.SSO;

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

    public ServiceBrokerLib(Context context, ResponseListener listenerObj, ServiceBrokerCB cb) {
        this.mContext = context;
        this.mResponseListener = listenerObj;
        this.mSvcBrokerCB = cb;
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

        if (serviceID.equals(ServiceBrokerLib.SERVICE_GET_INFO)) {
            try {
                SSO sso = CommonBasedAPI.getSSO();
                String info = "";
                try {
                    info = sso.toJsonString();
                    Log.d("@@@TOM", sso.toJsonString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mSvcBrokerCB.onServiceBrokerResponse(info);
            } catch (CommonBasedAPI.CommonBaseAPIException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            CommonBasedAPI.call(serviceID, parameter, getDefaultListener(parameter));
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

    Response.Listener getDefaultListener(final String param) {
        return new Response.Listener() {
            @Override
            public void onSuccess(Response resp) {
                int code = resp.getErrorCode();
                StringBuilder sb = new StringBuilder();

                if(resp.OK()) {
                    String message = resp.getResponseString();
                    sb.append("Result :: ")
                            .append("code = ").append(code)
                            .append(", result = ").append(message);
                    Log.d("Result : ", sb.toString());
                    Log.d("Result : ", "(size = " + message.length() +")");
                } else {
                    String title;
                    String message = resp.getErrorMessage();
                    switch (resp.getErrorCode()) {
                        case CommonBasedConstants.BROKER_ERROR_RELAY_SYSTEM: // 공통기반 시스템에서 확인된 에러 (중계 서버에서 처리 중 발생한 에러)
                            title = "서비스 연계 에러";
                            break;
                        case CommonBasedConstants.BROKER_ERROR_SERVICE_SERVER: // 서비스 제공 서버에서 발생한 HTTP 에러 (행정 서비스 서버 접속시 발생함)
                            title = "서비스 제공 서버 HTTP 응답 에러";
                            break;
                        case CommonBasedConstants.BROKER_ERROR_FAILED_REQUEST: // 서비스 요청 실패 (네트워크 유실로 발생할 수 있음)
                            title = "서비스 요청 실패";
                            break;
                        case CommonBasedConstants.BROKER_ERROR_INVALID_RESPONSE: // 서비스 응답 메시지 처리 에러 (네트워크 유실로 발생할 수 있음)
                            title = "서비스 응답 처리 실패";
                            break;
                        default:
                            title = "정의되지 않음.";
                            message = "알수없음.";

                    }

                    sb.append("ERROR :: ").append("[").append(title).append("]\n").append(message);
                    Log.e("ERROR : ",  sb.toString());
                }

                Log.d(TAG, "response (sucess) <<<<<< ");
                Log.d(TAG, " - data : " + sb.toString());
                final ResponseEvent re = new ResponseEvent(code, sb.toString());
                if (mResponseListener != null) {
                    if (mContext instanceof Activity) {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, " - CallBack (UI enabled)");
                                mResponseListener.receive(re);
                            }
                        });

                    }else{
                        Log.d(TAG, " - CallBack (UI disabled)");
                        mResponseListener.receive(re);
                    }
                }else{
                    Log.w(TAG, " - CallBack (LIstener is null)");
                }

//                Toast.makeText(mContext, sb.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int errorCode, String errMessage, Throwable t) {
                Log.e(TAG, errMessage + "(code : " + errorCode + ")", t);
                // fail
                Log.e(TAG, "response (error) <<<<<< ");
                Log.e(TAG, " - errorCode : " + errorCode);
                Log.e(TAG, " - data : " + errMessage);
                final ResponseEvent re = new ResponseEvent(errorCode, errMessage);
                if (mResponseListener != null) {
                    if (mContext instanceof Activity) {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, " - CallBack (UI enabled)");
                                mResponseListener.receive(re);
                            }
                        });

                    }else{
                        Log.d(TAG, " - CallBack (UI disabled)");
                        mResponseListener.receive(re);
                    }
                } else {
                    Log.w(TAG, " - CallBack (LIstener is null)");
                }
//                Toast.makeText(mContext, errMessage + "(code : " + errorCode + ")", Toast.LENGTH_LONG).show();
            }
        };
    }

    /*
    private IRemoteServiceCallback mRemoteSVCCallBack = new IRemoteServiceCallback.Stub() {
        @Override
        public void success(String data) throws RemoteException {
            Log.d(TAG, "response (sucess) <<<<<< ");
            Log.d(TAG, " - data : " + data);
            int resultCode = 0;
            final ResponseEvent re = new ResponseEvent(resultCode, data);
            if (mResponseListener != null) {
                if (mContext instanceof Activity) {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, " - CallBack (UI enabled)");
                            mResponseListener.receive(re);
                        }
                    });

                }else{
                    Log.d(TAG, " - CallBack (UI disabled)");
                    mResponseListener.receive(re);
                }
            }else{
                Log.w(TAG, " - CallBack (LIstener is null)");
            }
        }

        @Override
        public void successBigData(String code, String data) throws RemoteException {
            Log.d(TAG, "response (sucess - bigData) <<<<<< ");
            Log.d(TAG, " - code : " + code);
            Log.d(TAG, " - data : " + data);

            int resultCode = 0;
            String result = "";
            FileInputStream fis = null;
            Reader reader = null;
            BufferedReader br = null;
            try {
                fis = new FileInputStream(data);
                reader = new InputStreamReader(fis,"UTF-8");
                br = new BufferedReader(reader);

                StringBuffer builder = new StringBuffer();

                String readLine = null;
                while ((readLine = br.readLine()) != null) {
                    builder.append(readLine);
                }

                resultCode = RESPONSE_OK;
                result = new StringCryptoUtil().decryptAES(builder.toString(), code);

                File file = new File(data);
                if (file.exists()) {
                    file.delete();
                    Log.d(TAG, "file deleted. (" + file.exists() + ")");
                }

            } catch (NullPointerException e) {
                Log.e(TAG, "data is null. :: " + e.getMessage());
                resultCode = RESPONSE_DATA_FORMAT_INVALID;
                result = "데이터 처리 중 오류가 발생했습니다.";
            } catch (Exception e) {
                Log.e(TAG, "unknow error  :: " + e.getMessage());
                resultCode = RESPONSE_DATA_FORMAT_INVALID;
                result = "데이터 처리 중 오류가 발생했습니다.";
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                    if (reader != null) {
                        reader.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                } catch (Exception e) {
                }
            }

            final ResponseEvent re = new ResponseEvent(resultCode, result);

            if (mResponseListener != null) {
                if (mContext instanceof Activity) {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, " - CallBack (UI enabled)");
                            mResponseListener.receive(re);
                        }
                    });

                }else{
                    Log.d(TAG, " - CallBack (UI disabled)");
                    mResponseListener.receive(re);
                }
            } else {
                Log.w(TAG, " - CallBack (LIstener is null)");
            }
        }

        @Override
        public void fail(int errorCode, String data) throws RemoteException {
            Log.e(TAG, "response (error) <<<<<< ");
            Log.e(TAG, " - errorCode : " + errorCode);
            Log.e(TAG, " - data : " + data);
            final ResponseEvent re = new ResponseEvent(errorCode, data);
            if (mResponseListener != null) {
                if (mContext instanceof Activity) {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, " - CallBack (UI enabled)");
                            mResponseListener.receive(re);
                        }
                    });

                }else{
                    Log.d(TAG, " - CallBack (UI disabled)");
                    mResponseListener.receive(re);
                }
            } else {
                Log.w(TAG, " - CallBack (LIstener is null)");
            }
        }
    };

     */
}
