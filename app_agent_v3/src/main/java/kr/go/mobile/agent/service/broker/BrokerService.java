package kr.go.mobile.agent.service.broker;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import kr.go.mobile.agent.app.MonitorManager;
import kr.go.mobile.agent.app.SAGTApplication;
import kr.go.mobile.agent.app.SessionManager;
import kr.go.mobile.agent.network.RelayClient;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.utils.Aria;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.mobp.iff.R;

public class BrokerService extends Service {

    private final static String TAG = BrokerService.class.getSimpleName();

    private final static String CMM_SERVICE_CERT_AUTH = "CMM_CERT_AUTH_MAM";
    private final static String CMM_SERVICE_DOC_IMAGE_LOAD = "CMM_DOC_IMAGE_LOAD";
    private final static String CMM_SERVICE_FILE_UPLOAD = "CMM_FILE_UPLOAD";
    private static final String CMM_SERVICE_FILE_DOWNLOAD = "CMM_FILE_DOWNLOAD";

    public interface IServiceManager {
        SessionManager getSession();
        MonitorManager getMonitor();
    }

    final IBrokerService iBrokerService = new IBrokerService.Stub() {

        @Override
        public UserAuthentication getUserAuth() throws RemoteException {
            try {
                getMonitor().enabledSecureNetwork();
                Log.TC("행정앱 >>브로커(SSO 사용자 인증)>> 보안 에이전트");
                UserAuthentication auth = getSession().getUserAuthentication();
                Log.TC("보안 에이전트 >>브로커(사용자 인증 객체)>> 행정앱");
                return auth;
            } catch (Exception e) {
                throw new RemoteException("사용자 인증 정보를 로드할 수 없습니다. ("+ e.getMessage() + ")");
            }
        }

        @Override
        public BrokerResponse<?> execute(BrokerTask task) throws RemoteException {
            if (getMonitor().enabledSecureNetwork()) {
                switch (task.serviceId) {
                    case CommonBasedConstants.BROKER_ACTION_FILE_UPLOAD: {
                        task.serviceId = CMM_SERVICE_FILE_UPLOAD;
                        break;
                    }
                    case CommonBasedConstants.BROKER_ACTION_FILE_DOWNLOAD: {
                        task.serviceId = CMM_SERVICE_FILE_DOWNLOAD;
                        break;
                    }
                    default:
                        throw new RemoteException("지원하지 않은 서비스 ID 입니다.");
                }
                try {
                    BrokerResponse<?> resp = doSyncWork(task);
                    Log.TC("클라이언트 >>브로커(업로드/다운로드 처리결과)>> 행정앱");
                    task.clear();
                    return resp;
                } catch (IOException e) {
                    Log.e(TAG, "요청 파일을 읽을 수 없습니다.", e);
                    throw new RuntimeException("요청 파일을 읽을 수 없습니다.", e);
                }
            } else {
                throw new RemoteException("보안 네트워크 연결이 종료되었습니다.");
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
                    case CommonBasedConstants.BROKER_ACTION_FILE_UPLOAD:
                    case CommonBasedConstants.BROKER_ACTION_FILE_DOWNLOAD: {
                        throw new RemoteException("지원하지 않는 서비스 입니다.");
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


    @Override
    public IBinder onBind(Intent intent) {
        Log.TC("행정앱 >>(바인딩 요청)>> 브로커");
        return iBrokerService.asBinder();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, " ------------------- BrokerService.onTaskRemoved() -----------------------");
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, " ------------------- BrokerService.onUnbind() -----------------------");
        return super.onUnbind(intent);
    }


    @Override
    public void onDestroy() {
        Log.e(TAG, " ------------------- BrokerService.onDestroy() -----------------------");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, int flag, int startId) {
        UserSigned signed = getSession().getUserSigned();

        try {
            client = builder.setUserId(signed.getUserID())
                    .setOfficeName(signed.getOfficeName())
                    .build();

            BrokerTask task = BrokerTask.obtain(CMM_SERVICE_CERT_AUTH);
            task.serviceParam = signed.getSignedBase64();
            task.serviceCallback = new IBrokerServiceCallback() {
                @Override
                public void onResponse(BrokerResponse response) {
                    if (response.code == CommonBasedConstants.BROKER_ERROR_NONE) {
                        // ou 값은 인증서버에서 획득되는 구조임.
                        client.setOfficeCode(((UserAuthentication) response.getResult()).ouCode);
                        getSession().registerAuthSession((UserAuthentication) response.getResult());
                    } else {
                        throw new RuntimeException(response.getErrorMessage());
                    }
                }

                @Override
                public void onFailure(int failCode, String failMessage)  {
                    throw new RuntimeException(failMessage);
                }

                @Override
                public IBinder asBinder() {
                    return null;
                }
            };

            doAsyncWork(task);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("SSL 초기화를 할 수 없습니다." , e);
        } catch (MalformedURLException e) {
            throw new RuntimeException("중계 서버 URL 주소가 잘못되었습니다." , e);
        }
        return START_STICKY;
    }

    void doAsyncWork(BrokerTask task) {
        String reqPackageInfo = getReqPackageInfo(task.requestUid);
        String serviceId = task.getServiceId();
        try {
            switch (serviceId) {
                case CMM_SERVICE_FILE_UPLOAD:
                case CMM_SERVICE_FILE_DOWNLOAD:{
                    return;
                }
                case CMM_SERVICE_CERT_AUTH: {
                    client.relayUserAuth(reqPackageInfo, task.getOriginalServiceParam(), task.serviceCallback);
                    break;
                }
                case CMM_SERVICE_DOC_IMAGE_LOAD: {
                    Log.TC("행정앱 >>브로커(문서변환)>> 클라이언트");
                    client.relayLoadConvertedDoc(reqPackageInfo, task.getServiceParam(), task.serviceCallback);
                    break;
                }
                default: {
                    Log.d(TAG, "행정 서비스 (ID:" + serviceId + ") 실행");
                    Log.TC("행정앱 >>브로커(기관서비스)>> 클라이언트");
                    client.relayDefault(reqPackageInfo, serviceId, task.getServiceParam(), task.serviceCallback);
                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "요청 데이터를 생성하지 못합니다. (인코딩을 지원하지 않습니다.)", e);
            throw new RuntimeException("요청 데이터를 생성하지 못합니다. (인코딩을 지원하지 않습니다.)", e);
        } catch (JSONException e) {
            Log.e(TAG, "인증 요청 데이터를 생성할 수 없습니다.", e);
            throw new RuntimeException("인증 요청 데이터를 생성할 수 없습니다.", e);
        }
    }

    BrokerResponse<?> doSyncWork(BrokerTask task) throws IOException {
        String reqPackageInfo = getReqPackageInfo(task.requestUid);
        String serviceId = task.getServiceId();
        switch (serviceId) {
            case CMM_SERVICE_FILE_UPLOAD: {
                String boundaryId = "--MOBPMultiPart" + System.currentTimeMillis();
                Log.TC("행정앱 >>브로커(업로드)>> 클라이언트");
                return client.relayUploadData(reqPackageInfo, boundaryId, task.getServiceParam(), task.getTargetRelayUrl(), task.getFileName(), task.getFileBytes());
            }
            case CMM_SERVICE_FILE_DOWNLOAD: {
                Log.TC("행정앱 >>브로커(다운로드)>> 클라이언트");
                return client.relayDownloadData(reqPackageInfo, task.getServiceParam(), task.getTargetRelayUrl(), task.getOutputStream());
            }
            default:
                return null;
        }
    }

    String getReqPackageInfo(int uid) {
        String packageName;
        String versionCode;
        if (uid == Process.myUid()) {
            PackageInfo info = null;
            try {
                info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
                packageName = info.packageName;
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException("");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = Long.toString(info.getLongVersionCode());
            } else {
                versionCode = Integer.toString(info.versionCode);
            }
        } else {
            packageName = getMonitor().getPackageName(uid);
            versionCode = getMonitor().getVersionCode(uid);
        }
        return String.format("PK=%s;AV=%s", packageName, versionCode);
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
