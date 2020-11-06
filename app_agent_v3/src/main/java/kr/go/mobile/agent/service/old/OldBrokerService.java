package kr.go.mobile.agent.service.old;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.sds.mobile.servicebrokerLib.aidl.ByteArray;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteService;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceCallback;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceExitCallback;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.BrokerService;
import kr.go.mobile.agent.service.broker.BrokerTask;
import kr.go.mobile.agent.service.broker.IBrokerService;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.servicebrokerLib.aidl.StringCryptoUtil;

public class OldBrokerService extends Service {

    private static String TAG = "OldService";//OldServiceV2.class.getSimpleName();

    private IBrokerService realBrokerService;
    private ServiceConnection brokerSerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            realBrokerService = (IBrokerService) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            realBrokerService = null;
        }
    };

    public void onCreate() {
        Intent intent = new Intent(this, BrokerService.class);
        bindService(intent, brokerSerConnection, BIND_AUTO_CREATE | BIND_IMPORTANT);
    }

    @Override
    public void onDestroy() {
        unbindService(brokerSerConnection);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new IRemoteService.Stub() {

            IBrokerServiceCallback getDataCallback(final IRemoteServiceCallback callback) {
                return new IBrokerServiceCallback() {
                    @Override
                    public void onResponse(BrokerResponse response) throws RemoteException {
                        Log.i(TAG, "<<<< data to \'NET\' (data)");
                        // TODO BrokerResponse.getCode() 값에 따른 처리 분기 (참고. HttpTask.java line 44)
                        int code = response.getCode();


                        if(code == CommonBasedConstants.BROKER_ERROR_NONE){
                            String respMessage = response.getResult().getServiceServerResponse();
                            // 데이터 들어왔을 때
                            if (respMessage.length() > 20000 ) {
                                // largeResult 일 경우 파일로 전달.
                                BufferedWriter out = null;
                                try {
                                    String fileName = System.currentTimeMillis() + ".txt";
                                    String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/.MFF/";
                                    String filePath = folderPath + fileName;
                                    File folder = new File(folderPath);
                                    if( ! folder.exists()){
                                        folder.mkdirs();
                                    }
                                    folder.mkdirs();
                                    File file = new File(filePath);
                                    if (!file.createNewFile()) {
                                        throw new IOException("대용량 처리 공간을 확보할 수 없습니다.");
                                    }
                                    out = new BufferedWriter(new FileWriter(filePath));

                                    Calendar cal = Calendar.getInstance();
                                    String key = new SimpleDateFormat("yyyyMMddHHmmssss").format(cal.getTime());
                                    String encryptResult = new StringCryptoUtil().encryptAES(respMessage, key);
                                    out.write(encryptResult);
                                    Log.e(TAG, "data successBigData :: " + filePath);
                                    callback.successBigData(key, filePath);
                                } catch (Exception e) {
                                    Log.e(TAG, "대용량 응답 데이터 처리 중 에러가 발생하였습니다. - " + e.getMessage(), e);
                                    code = -109; // E2ESetting.DATA_ERROR_CODE;
                                    callback.fail(code, "대용량 응답 데이터 처리 중 에러가 발생하였습니다. - " + e.getMessage());
                                } finally {
                                    try {
                                        if (out != null)
                                            out.close();
                                    } catch (IOException ignored) {
                                    }
                                }
                            } else {
                                callback.success(respMessage);
                            }
                        } else {
                            String errMessage = response.getErrorMessage();
                            callback.fail(code, errMessage);
                        }

                        /*
                        // TODO 에러별로 분기해야 할까요 ? - Tom
                        else if(code == MobileEGovConstants.BROKER_ERROR_PROC_DATA) {
                            callback.fail(code, errMessage);
                        }
                         */


                    }

                    @Override
                    public void onFailure(int code, String msg) throws RemoteException {
                        Log.e(TAG, "data to \'NET\' (data) >>>> ERROR");
                        callback.fail(code, msg);
                    }

                    @Override
                    public IBinder asBinder() {
                        return null;
                    }
                };
            }

            @Override
            public String getInfo(String list) throws RemoteException {
                Log.i(TAG, ">>>> SSO getInfo");
                Log.d(TAG, " - params :: " + list);
                String result = "";

                try {
                    UserAuthentication auth = realBrokerService.getUserAuth();
                    JSONArray reqJsonarray = (JSONArray)(new JSONTokener(list)).nextValue();
                    JSONArray jArray = new JSONArray();

                    for(int i = 0; i<reqJsonarray.length(); i++){
                        String key = reqJsonarray.get(i).toString();
                        String value;
                        switch (key) {
                            case "gov:nickname":
                                value = auth.getNickName();
                                break;
                            case "gov:dn":
                                value = auth.getUserDN();
                                break;
                            case "gov:cn":
                                value = auth.getUserID();
                                break;
                            case "gov:ou":
                                value = auth.getOuName();
                                break;
                            case "gov:oucode":
                                value = auth.getOuCode();
                                break;
                            case "gov:department":
                                value = auth.getDepartmentName();
                                break;
                            case "gov:departmentnumber":
                                value = auth.getDepartmentCode();
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + key);
                        }
                        JSONObject jobj = new JSONObject();
                        jobj.put(key, value);
                        jArray.put(i, jobj);
                    }

                    result = jArray.toString();
                    Log.d(TAG, "<<<< SSO load info : " + jArray.toString());
                } catch (RemoteException e) {
                    throw e;
                } catch (Exception e) {
                    Log.e(TAG, "SSO load error : " + e.getMessage());
                }
                return result;
            }

            @Override
            public boolean data(String header, String hostUrl, String serviceId, String dataType, String parameter, int timeOut, IRemoteServiceCallback callback) throws RemoteException {
                Log.i(TAG, ">>>> data from \'APP\' (data)");
                Log.d(TAG, " - hostUrl (ignore) :: " + hostUrl);
                Log.d(TAG, " - header (ignore) :: " + header);
                Log.d(TAG, " - dataType (ignore) :: " + dataType);
                Log.d(TAG, " - serviceId :: " + serviceId);
                Log.d(TAG, " - parameters :: " + parameter);
                BrokerTask task = BrokerTask.obtain(serviceId);
                task.serviceParam = parameter;
                realBrokerService.enqueue(task, getDataCallback(callback));
                return true;
            }

            private ByteArrayOutputStream bigDataByteOS = null;
            private byte[] bigDataBuffer = null;
            @Override
            public boolean bigData(String header, String hostUrl, String serviceId, String dataType, ByteArray parameter, int timeOut, IRemoteServiceCallback callback) throws RemoteException {
                byte[] inByteBuffer = parameter.getBytes();

                if("bigData".equals(dataType)) {
                    Log.d(TAG, "** request data length is big. - init");
                    if (bigDataByteOS != null) {
                        try {
                            bigDataByteOS.close();
                        } catch (Exception e) {
                            throw new RemoteException("대용량 데이터 전달 중 에러가 발생하였습니다.");
                        }
                    }
                    bigDataByteOS = new ByteArrayOutputStream();
                    bigDataBuffer = null;

                } else if (inByteBuffer == null || "".equals(new String(inByteBuffer))) {
                    Log.d(TAG, "** request data length is big. - send");
                    Log.i(TAG, "data from \'APP\' (bigData) >>> ");
                    Log.d(TAG, " - hostUrl (ignore) :: " + hostUrl);
                    Log.d(TAG, " - header (ignore) :: " + header);
                    Log.d(TAG, " - dataType (ignore) :: " + dataType);
                    Log.d(TAG, " - serviceId :: " + serviceId);
                    Log.d(TAG, " - parameters :: " + parameter);

                    BrokerTask task = BrokerTask.obtain(serviceId);
                    task.serviceParam = new String(bigDataBuffer).trim();
                    realBrokerService.enqueue(task, getDataCallback(callback));
                } else {
                    Log.d(TAG, "** request data length is big. - load");
                    bigDataByteOS.write(inByteBuffer, 0, inByteBuffer.length);
                    bigDataBuffer = bigDataByteOS.toByteArray();
                    Log.d(TAG, "size >>>>>> :: " + bigDataBuffer.length);
                }
                return true;
            }

            @Override
            public String uploadWithCB(String header, String serviceID, String filePath, String parameter, IRemoteServiceCallback callback) throws RemoteException {
                Log.i(TAG, ">>>> data from \'APP\' (upload)");
                Log.d(TAG, " - header (ignore) :: " + header);
                Log.d(TAG, " - filePath :: " + filePath);
                Log.d(TAG, " - parameters :: " + parameter);
                throw new RemoteException("지원하지 않는 기능입니다.");
            }

            @Override
            public void download(String header, String filePath, String fileName, IRemoteServiceCallback callback) throws RemoteException {
                // 네트워크 모니터링 리셋
            }

            @Override
            public String upload(String header, String filePath, String parameter) throws RemoteException {
                return null;
            }

            @Override
            public boolean document(String header, String url, IRemoteServiceCallback callback) throws RemoteException {
                return false;
            }

            ////////////////////// DEPRECATED ////////////////////////////


            @Override
            public boolean documentWithExitCB(String header, String url, IRemoteServiceCallback callback, IRemoteServiceExitCallback exitcallback) throws RemoteException {
                return false;
            }

            @Override
            public boolean displayMailViewer(String header, String hostUrl, String serviceId, String dataType, String parameter, int timeOut, IRemoteServiceCallback callback, IRemoteServiceExitCallback exitcallback) throws RemoteException {
                return false;
            }

            @Override
            public boolean registerCallback(IRemoteServiceCallback callback) throws RemoteException {
                return false;
            }

            @Override
            public boolean unregisterCallback(IRemoteServiceCallback callback) throws RemoteException {
                return false;
            }

            @Override
            public boolean zipList(String header, String url, IRemoteServiceCallback callback, IRemoteServiceExitCallback exitcallback) throws RemoteException {
                return false;
            }
        };
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return super.onUnbind(intent);
    }
}
