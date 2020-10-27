package kr.go.mobile.agent.service.broker;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import kr.go.mobile.agent.app.MonitorManager;
import kr.go.mobile.agent.app.SAGTApplication;
import kr.go.mobile.agent.app.SessionManager;
import kr.go.mobile.agent.network.GenerateHeaders;
import kr.go.mobile.agent.network.RelayClient;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.utils.Aria;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.UserAuthenticationUtils;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.mobp.iff.R;

public class BrokerService extends Service {

    private final static String TAG = BrokerService.class.getSimpleName();

    private final static String CMM_SERVICE_CERT_AUTH = "CMM_CERT_AUTH_MAM";
    public final static String CMM_SERVICE_FILE_UPLOAD = "CMM_FILE_UPLOAD";
    public final static String CMM_SERVICE_DOC_IMAGE_LOAD = "CMM_DOC_IMAGE_LOAD";

    public interface IServiceManager {
        SessionManager getSession();
        MonitorManager getMonitor();
    }

    final IBrokerService iBrokerService = new IBrokerService.Stub() {
        @Override
        public UserAuthentication getUserAuth() throws RemoteException {
            try {
                getMonitor().enabledSecureNetwork();
                return getSession().getUserAuthentication();
            } catch (Exception e) {
                throw new RemoteException("사용자 인증 정보를 로드할 수 없습니다. ("+ e.getMessage() + ")");
            }
        }

        @Override
        public BrokerResponse execute(BrokerTask task) throws RemoteException {
            // TODO 제공할까 ?
            try {
                return doSyncWork(task);
            } catch (UnsupportedEncodingException | InterruptedException | ExecutionException e) {
                throw new RemoteException("브로커 서비스를 이용하여 요청을 실행할 수 없습니다. (" + e.getMessage() + ")");
            }
        }

        @Override
        public void enqueue(BrokerTask task, IBrokerServiceCallback callback) throws RemoteException {
            if (callback == null) {
                throw new RemoteException("요청에 대한 응답을 처리할 callback 이 없습니다.");
            }
            if (getMonitor().enabledSecureNetwork()) {
            task.serviceCallback = callback;
                // 공통기반 API 에서 사용하는 serviceID 와 공통기반 시스템에서 사용하는 ServiceID 는 다름.
                // 공통기반 시스템 ServiceID로 변환.
                String param = task.getOriginalServiceParam();
                switch (task.serviceId) {
                    case CommonBasedConstants.BROKER_ACTION_CONVERT_STATUS_DOC:
                        task.serviceParam = param + "&action=empty";
                    case CommonBasedConstants.BROKER_ACTION_LOAD_DOCUMENT:
                        task.serviceId = CMM_SERVICE_DOC_IMAGE_LOAD;
                        break;
                    case CommonBasedConstants.BROKER_ACTION_FILE_UPLOAD: {
                        task.serviceId = CMM_SERVICE_FILE_UPLOAD;
                        String[] extraParams = task.serviceParam.split(";");
                        StringBuilder sb = new StringBuilder();
                        for (String tmp : extraParams) {
                            if (tmp.startsWith("path")) {
                                task.serviceLocalPath = tmp.split("=")[1];
                            } else if (sb.length() > 0) {
                                sb.append(";").append(tmp);
                            } else {
                                sb.append(tmp);
                            }
                        }
                        task.serviceParam = sb.toString();
                        break;
                    }
                }
            doAsyncWork(task);
            } else {
                throw new RemoteException("보안 네트워크 연결이 종료되었습니다.");
        }
        }
    };

    RelayClient.Builder builder;
    RelayClient client;

    @Override
    public void onCreate() {
        Log.e(TAG, " ------------------- BrokerService.onCreate() -----------------------");
        String baseURL = new Aria(getString(R.string.MagicMRSLicense))
                .decrypt(getString(R.string.agenturl));

        builder = new RelayClient.Builder(getBaseContext(), baseURL)
                .setConnectTimeout(getResources().getInteger(R.integer.RelayClientConnectionTimeOut))
                .setReadTimeout(getResources().getInteger(R.integer.RelayClientReadTimeOut))
                .setKeyManager(null)
                .setTrustManager(new TrustManager[] { new X509TrustManager() {
                    // Create a trust manager that does not validate certificate chains
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[] {};
            }
                }})
                .setSecureRandom(new java.security.SecureRandom());
            }
//        Log.TC("", "중계서버 초기화 완료");

    @Override
    public void onDestroy() {
        Log.e(TAG, " ------------------- BrokerService.onDestroy() -----------------------");
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, " ------------------- BrokerService.onTaskRemoved() -----------------------");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, " ------------------- BrokerService.onBind() -----------------------");
        return iBrokerService.asBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.e(TAG, " ------------------- BrokerService.onRebind() -----------------------");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, " ------------------- BrokerService.onUnbind() -----------------------");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(final Intent intent, int flag, int startId) {
        UserSigned signed = getSession().getUserSigned();

        try {
            builder.setUserId(signed.getUserID())
                    .setOfficeName(signed.getOfficeName())
                    .setOfficeCode(signed.getOfficeCode());

            client = builder.build();

            BrokerTask task = BrokerTask.obtain(CMM_SERVICE_CERT_AUTH);
            task.serviceParam = UserAuthenticationUtils.generateBody(signed.getSignedBase64());
            task.serviceCallback = new IBrokerServiceCallback() {
                @Override
                public void onResponse(BrokerResponse response) throws RemoteException {
                    Log.d(TAG, "response code : " + response.code);
                    Log.d(TAG, "response obj : " + response.obj.toString());
                    if (response.code == CommonBasedConstants.BROKER_ERROR_NONE) {
                        getSession().registerAuthSession((UserAuthentication) response.obj);
                    } else {
                        throw new RuntimeException(response.getErrorMessage());
                    }
                }

                @Override
                public void onFailure(int failCode, String failMessage) throws RemoteException {
                    throw new RuntimeException(failMessage);
                }

                @Override
                public IBinder asBinder() {
                    return null;
                }
            };

            doAsyncWork(task);
        } catch (JSONException e) {
            throw new RuntimeException("사용자 인증 요청 데이터를 생성하지 못합니다." , e);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("SSL 초기화를 할 수 없습니다." , e);
        } catch (MalformedURLException e) {
            throw new RuntimeException("중계 서버 URL 주소가 잘못되었습니다." , e);
        } catch (PackageManager.NameNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException("해더를 생성 할 수 없습니다." , e);
        }
        return START_STICKY;
    }

    void doAsyncWork(BrokerTask task) {
        String serviceId = task.getServiceId();
            try {
            switch (serviceId) {
                case CMM_SERVICE_CERT_AUTH: {
                // 인증요청 파라미터 값은 URL 인코딩으로 변환하여 전달한다. (서버와의 정의된 설정)
                    String urlEncodeParams = URLEncoder.encode(task.getOriginalServiceParam(), "UTF-8");
                    client.relayUserAuth(urlEncodeParams, task.serviceCallback);
                    break;
                }
                case CMM_SERVICE_FILE_UPLOAD: {
                    String boundaryId = "--MOBPMultiPart" + System.currentTimeMillis();
                    client.relayUploadData(boundaryId, task.getServiceParam(), task.getFileName(), task.getFileBytes(), task.serviceCallback);
                    break;
                }
                case CMM_SERVICE_DOC_IMAGE_LOAD: {
                    client.relayLoadConvertedDoc(task.getServiceParam(), task.serviceCallback);
                    break;
                }
                default: {
                    Log.d(TAG, "행정 서비스 (ID:" + serviceId + ") 실행");
                    client.relayReqData(serviceId, task.getServiceParam(), task.serviceCallback);
                    break;
                }
            }
            } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "요청 데이터를 생성하지 못합니다. (인코딩을 지원하지 않습니다.)", e);
            throw new RuntimeException("요청 데이터를 생성하지 못합니다. (인코딩을 지원하지 않습니다.)", e);
            }
        }

    @Deprecated
    BrokerResponse doSyncWork(BrokerTask task) throws UnsupportedEncodingException, InterruptedException, ExecutionException{
        return null;
    }

    SessionManager getSession() {
        Application app = getApplication();
        if (app instanceof SAGTApplication) {
            return ((IServiceManager) app).getSession();
        } else {
            throw new IllegalStateException("보안 에이전트의 서비스 매니저를 로드할 수 없습니다.");
        }
    }

    MonitorManager getMonitor() {
        Application app = getApplication();
        if (app instanceof SAGTApplication) {
            return ((IServiceManager) app).getMonitor();
        } else {
            throw new IllegalStateException("보안 에이전트의 서비스 매니저를 로드할 수 없습니다.");
        }
    }


}
