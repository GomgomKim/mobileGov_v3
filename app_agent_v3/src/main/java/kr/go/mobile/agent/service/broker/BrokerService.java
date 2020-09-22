package kr.go.mobile.agent.service.broker;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;

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
import kr.go.mobile.agent.utils.UserAuthenticationUtils;
import kr.go.mobile.mobp.iff.R;

public class BrokerService extends Service {

    private final static String TAG = BrokerService.class.getSimpleName();

    public interface IServiceManager {
        SessionManager getSessionManager();
        MonitorManager getMonitorManager();
    }

    final BlockingQueue<BrokerTask> queueBrokerTask = new ArrayBlockingQueue<>(20);

    final IBrokerService iBrokerService = new IBrokerService.Stub() {
        @Override
        public UserAuthentication getUserAuth() throws RemoteException {
            try {
                return serviceManager.getSessionManager().getUserAuthentication();
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

    IServiceManager serviceManager;
    GenerateHeaders generateHeaders;
    RelayClient client;

    @Override
    public void onCreate() {
        Application app = getApplication();
        if (app instanceof SAGTApplication) {
            serviceManager = (IServiceManager) app;
            generateHeaders = new GenerateHeaders(this);

        } else {
            throw new IllegalStateException("보안 에이전트의 서비스 매니저를 로드할 수 없습니다.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serviceManager = null;
    }

    public SessionManager getSession() {
        return serviceManager.getSessionManager();
    }

    public MonitorManager getMonitor() {
        return serviceManager.getMonitorManager();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBrokerService.asBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        kr.go.mobile.agent.utils.Log.timeStamp("startCert");
        UserSigned signed = getSession().getUserSigned();
        int nConnectTimeOut = getResources().getInteger(R.integer.HttpConnectionTimeOut);
        int nReadTimeOut = getResources().getInteger(R.integer.HttpReadTimeOut);
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

        } catch (UnsupportedEncodingException | PackageManager.NameNotFoundException | MalformedURLException e) {
            // TODO 에러 코드 정의..
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        BrokerTask task = queueBrokerTask.take();

                        if (getMonitor().enabledSecureNetwork()) {
                            String serviceId = task.getServiceId();
                            String serviceParams = task.getServiceParam();

                            Log.d(TAG, "serviceID : " + serviceId);
                            Log.d(TAG, "serviceParams : " +  serviceParams);

                            generateHeaders.setServiceId(task.getServiceId());
                            generateHeaders.setContentLength(serviceParams.length());

                            if (task.serviceId.equals(BrokerTask.SERVICE_ID_CERT_AUTH)) {
                                generateHeaders.setContentType("application/json;charset=utf-8");
                                Map<String, String> headers = generateHeaders.getHeaders();
                                try {
                                    client.relayUserAuth(task.serviceCallback, headers, URLEncoder.encode(serviceParams, "UTF-8"));
                                } catch (UnsupportedEncodingException e) {
                                    // TODO 에러코드값 정의 필요
                                    task.serviceCallback.onFailure(0, "사용자 인증 요청을 할 수 없는 단말 환경입니다. (인코딩 미지원)");
                                    Log.e(TAG, "사용자 인증 요청을 할 수 없는 단말 환경입니다. (인코딩 미지원)" , e);
                                }
                            } else {
                                Log.i(TAG, "행정 서비스 ID : " + task.serviceId);
                                generateHeaders.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                                Map<String, String> headers = generateHeaders.getHeaders();
                                client.relayReqData(task.serviceCallback, headers, serviceParams);
                            }
                        } else {
                            // TODO 네트워크 오류 코드 정의
                            task.serviceCallback.onFailure(0, "보안 네크워크 연결되지 않았습니다.");
                        }
                    } catch (InterruptedException | RemoteException ignored) {
                    }
                } while (serviceManager != null);
            }
        }, "broker queue").start();

        try {
            Log.d(TAG, "auth-task enqueue");
            String authParams = UserAuthenticationUtils.generateBody(signed.getSignedBase64());
            BrokerTask task = BrokerTask.generateAuthTask(authParams);
            task.serviceCallback = new IBrokerServiceCallback() {
                @Override
                public void onResponse(BrokerResponse response) throws RemoteException {
                    Log.d(TAG, "response code : " + response.code);
                    Log.d(TAG, "response obj : " + response.obj.toString());
                    if (response.code == 0) {// 세션 서비스로 인증 세션 생성(등록)
                        serviceManager.getSessionManager().registerAuthSession((UserAuthentication) response.obj);
                    } else if (response.code == 1) {
                        // TODO 응답 데이터 처리 오류
                    } else {
                        throw new IllegalStateException("Unexpected value: " + response.code);
                    }
                }

                @Override
                public void onFailure(int failCode, String failMessage) throws RemoteException {
                    Log.d(TAG, "failCode : " + failCode + " failMessage : "+ failMessage);
                }

                @Override
                public IBinder asBinder() {
                    return null;
                }
            };
            queueBrokerTask.offer(task);
        } catch (JSONException e) {
            // TODO 서비스 실행 실패 처리.. 인증 실패...
            Log.d(TAG, "서비스 실행 실패");
        }
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }



}
