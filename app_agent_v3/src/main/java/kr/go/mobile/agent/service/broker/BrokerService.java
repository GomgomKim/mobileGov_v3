package kr.go.mobile.agent.service.broker;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import kr.go.mobile.agent.app.MonitorManager;
import kr.go.mobile.agent.app.SAGTApplication;
import kr.go.mobile.agent.app.SessionManager;
import kr.go.mobile.agent.network.GenerateHeaders;
import kr.go.mobile.agent.network.RelayClient;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.utils.Aria;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.UserAuthenticationUtils;
import kr.go.mobile.common.v3.MobileEGovConstants;
import kr.go.mobile.mobp.iff.R;

public class BrokerService extends Service {

    private final static String TAG = BrokerService.class.getSimpleName();
    private final static String SERVICE_ID_CERT_AUTH = "CMM_CERT_AUTH_MAM";

    private final String BROKER_ERROR_PROC_DATA_STRING = "데이터 요청에 대한 응답데이터 처리 중 에러가 발생하였습니다.";

    public interface IServiceManager {
        SessionManager getSession();
        MonitorManager getMonitor();
    }

    final BlockingQueue<BrokerTask> queueBrokerTask = new ArrayBlockingQueue<>(20);

    final IBrokerService iBrokerService = new IBrokerService.Stub() {
        @Override
        public UserAuthentication getUserAuth() throws RemoteException {
            try {
                return SESSION.getUserAuthentication();
            } catch (Exception e) {
                throw new RemoteException("사용자 인증 정보를 로드할 수 없습니다.");
            }
        }

        @Override
        public BrokerResponse execute(BrokerTask task) throws RemoteException {
            return null;
        }

        @Override
        public void enqueue(BrokerTask task, IBrokerServiceCallback callback) throws RemoteException {
            if (callback == null) {
                throw new RemoteException("요청에 대한 응답을 처리할 callback 이 없습니다.");
            }
            task.serviceCallback = callback;
            queueBrokerTask.offer(task);
        }
    };

    SessionManager SESSION;
    MonitorManager MONITOR;
    GenerateHeaders generateHeaders;
    RelayClient client;

    @Override
    public void onCreate() {
        Application app = getApplication();
        if (app instanceof SAGTApplication) {
            SESSION = ((IServiceManager) app).getSession();
            MONITOR = ((IServiceManager) app).getMonitor();
            generateHeaders = new GenerateHeaders(this);
        } else {
            throw new IllegalStateException("보안 에이전트의 서비스 매니저를 로드할 수 없습니다.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        generateHeaders.clear();
        generateHeaders = null;
        SESSION = null;
        MONITOR = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBrokerService.asBinder();
    }

    @Override
    public int onStartCommand(final Intent intent, int flag, int startId) {
        UserSigned signed = SESSION.getUserSigned();
        int nConnectTimeOut = getResources().getInteger(R.integer.RelayClientConnectionTimeOut);
        int nReadTimeOut = getResources().getInteger(R.integer.RelayClientReadTimeOut);
        String baseURL = new Aria(getString(R.string.MagicMRSLicense))
                .decrypt(getString(R.string.agenturl))
                +"/";

        try {
            URL url = new URL(baseURL);
            client = new RelayClient(baseURL);
            client.setOkHttpClient(nConnectTimeOut, nReadTimeOut);
            client.buildClient();

            generateHeaders.setUrl(url);
            generateHeaders.setAgentDetail(signed.getUserID(), signed.getOfficeName(), signed.getOfficeCode());
        } catch (MalformedURLException e) {
            // README : 여기서 에러가 발생하면 중계 클라이언트 서비스를 제공할 수 없습니다. 즉, 앱이 종료되어야 합니다.
            throw new RuntimeException("중계 서버 URL 정보가 잘못되었습니다.", e);
        } catch (UnsupportedEncodingException e){
            // README : 여기서 에러가 발생하면 중계 클라이언트 서비스를 제공할 수 없습니다. 즉, 앱이 종료되어야 합니다.
            throw new RuntimeException("지원하지 않는 인코딩 타입입니다.", e);
        } catch (PackageManager.NameNotFoundException e){
            // README : 여기서 에러가 발생하면 중계 클라이언트 서비스를 제공할 수 없습니다. 즉, 앱이 종료되어야 합니다.
            throw new RuntimeException("단말 정보를 획득할 수 없습니다.", e);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        BrokerTask task = queueBrokerTask.take();

                        if (MONITOR.enabledSecureNetwork()) {
                            String serviceId = task.getServiceId();
                            String serviceParams = task.getServiceParam();
                            Log.d(TAG, "- serviceID : " + serviceId );
                            Log.d(TAG, "- serviceParams : " +  serviceParams);

                            generateHeaders.setServiceId(task.getServiceId());
                            generateHeaders.setContentLength(serviceParams.length());

                            if (task.serviceId.equals(SERVICE_ID_CERT_AUTH)) {
                                generateHeaders.setContentType("application/json;charset=utf-8");
                                Map<String, String> headers = generateHeaders.getHeaders();
                                try {
                                    client.relayUserAuth(task.serviceCallback, headers, URLEncoder.encode(serviceParams, "UTF-8"));
                                } catch (UnsupportedEncodingException e) {
                                    // README : 에러 코드만 출력하면 나중에 로그로 확인할 경우 코드 값의 의미를 또 찾아야 해요. 그래서 그냥 출력해주는게 좋아요
                                    Log.e(TAG, "사용자 인증 요청을 할 수 없는 단말 환경입니다. (인코딩 미지원)" , e);
                                    // README : 여기서 에러가 발생하면 중계 클라이언트 서비스를 제공할 수 없습니다. 즉, 앱이 종료되어야 합니다.
                                    throw new RuntimeException("사용자 인증 요청을 할 수 없는 단말 환경입니다. (인코딩 미지원)" , e);
                                }
                            } else {
                                Log.d(TAG, "행정 서비스 ID : " + task.serviceId);
                                generateHeaders.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                                Map<String, String> headers = generateHeaders.getHeaders();
                                client.relayReqData(task.serviceCallback, headers, serviceParams);
                            }
                        } else {
                            // README : 에러 코드만 출력하면 나중에 로그로 확인할 경우 코드 값의 의미를 또 찾아야 해요. 그래서 그냥 출력해주는게 좋아요
                            Log.i(TAG, "보안 네트워크가 연결되지 않아 요청을 처리할 수 없습니다.");
                            task.serviceCallback.onFailure(MobileEGovConstants.BROKER_ERROR_SECURE_NETWORK_DISCONNECTION, "보안 네크워크 연결되지 않았습니다.");
                        }
                    } catch (InterruptedException | RemoteException ignored) {
                    }
                } while (SESSION != null && MONITOR != null);
            }
        }, "broker queue").start();

        try {
            Log.d(TAG, "auth-task enqueue");
            BrokerTask task = BrokerTask.obtain(SERVICE_ID_CERT_AUTH);
            task.serviceParam = UserAuthenticationUtils.generateBody(signed.getSignedBase64());
            task.serviceCallback = new IBrokerServiceCallback() {
                @Override
                public void onResponse(BrokerResponse response) throws RemoteException {
                    Log.d(TAG, "response code : " + response.code);
                    Log.d(TAG, "response obj : " + response.obj.toString());
                    if (response.code == MobileEGovConstants.BROKER_ERROR_NONE) {
                        SESSION.registerAuthSession((UserAuthentication) response.obj);
                    } else {
                        // README : 여기서 에러가 발생하면 중계 클라이언트 서비스를 제공할 수 없습니다. 즉, 앱이 종료되어야 합니다.
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
            queueBrokerTask.offer(task);
        } catch (JSONException e) { ;
            // README : 여기서 에러가 발생하면 중계 클라이언트 서비스를 제공할 수 없습니다. 즉, 앱이 종료되어야 합니다.
            throw new RuntimeException(""+BROKER_ERROR_PROC_DATA_STRING + e);
        }
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}
