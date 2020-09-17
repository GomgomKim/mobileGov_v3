package kr.go.mobile.agent.service.old;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
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

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.BrokerService;
import kr.go.mobile.agent.service.broker.BrokerTask;
import kr.go.mobile.agent.service.broker.IBrokerService;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.utils.Log;

public class OldServiceV2 extends Service {

    private static String TAG = "CommonBaseAPI_v2";

    private IBrokerService realBrokerService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
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
        bindService(intent, serviceConnection, BIND_AUTO_CREATE | BIND_IMPORTANT);
    }

    @Override
    public void onDestroy() {
        unbindService(serviceConnection);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new IRemoteService.Stub() {

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
            public boolean data(String header, String hostUrl, String serviceId, String dataType, String parameter, int timeOut, final IRemoteServiceCallback callback) throws RemoteException {
                Log.i(TAG, ">>>> data from \'APP\' (data)");
                Log.d(TAG, " - hostUrl (ignore) :: " + hostUrl);
                Log.d(TAG, " - header (ignore) :: " + header);
                Log.d(TAG, " - dataType (ignore) :: " + dataType);
                Log.d(TAG, " - serviceId :: " + serviceId);
                Log.d(TAG, " - parameters :: " + parameter);
                BrokerTask task = BrokerTask.obtain();
                task.serviceId = serviceId;
                task.serviceParam = parameter;
                IBrokerServiceCallback serviceCallback = new IBrokerServiceCallback() {
                    @Override
                    public void onResponse(BrokerResponse response) throws RemoteException {
                        Log.i(TAG, "<<<< data to \'NET\' (data)");
                        Log.d(TAG, "respon data :"+(String)response.getResult());
                        callback.success((String)response.getResult());
                    }

                    @Override
                    public void onFailure(int code, String msg) throws RemoteException {
                        Log.i(TAG, "<<<< data to \'NET\' (data)");
                        callback.fail(code, msg);
                    }

                    @Override
                    public IBinder asBinder() {
                        return null;
                    }
                };

                // BrokerService와 통신하기 위한 callback 추가
                task.setCallback(serviceCallback);

                realBrokerService.enqueue(task, serviceCallback);
                return false;
            }

            @Override
            public boolean bigData(String header, String hostUrl, String serviceId, String dataType, ByteArray parameter, int timeOut, IRemoteServiceCallback callback) throws RemoteException {
                Log.i(TAG, "data from \'APP\' (bigData) >>> ");
                Log.d(TAG, " - hostUrl :: " + hostUrl);
                Log.d(TAG, " - header :: " + header);
                Log.d(TAG, " - serviceId :: " + serviceId);
                Log.d(TAG, " - dataType :: " + dataType);
                Log.d(TAG, " - parameters :: " + parameter);
                return false;
            }

            @Override
            public void download(String header, String filePath, String fileName, IRemoteServiceCallback callback) throws RemoteException {

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
            public String uploadWithCB(String header, String serviceID, String filePath, String parameter, IRemoteServiceCallback callback) throws RemoteException {
                return null;
            }

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
}
