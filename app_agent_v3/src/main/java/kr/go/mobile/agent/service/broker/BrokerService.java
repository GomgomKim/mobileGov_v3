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
        public SessionManager getSessionManager();
        public MonitorManager getMonitorManager();
    }

    public static final int RESULT_AUTHENTICATION_OK = 1000;
    public static final int RESULT_AUTHENTICATION_FAIL = 1001;

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

    @Override
    public void onCreate() {
        Application app = getApplication();
        if (app instanceof SAGTApplication) {
            serviceManager = (IServiceManager) app;
        } else {
            throw new RuntimeException("보안 에이전트의 서비스 매니저를 로드할 수 없습니다.");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBrokerService.asBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        final ResultReceiver resultReceiver = intent.getParcelableExtra("extra_receiver");
        final UserSigned signed = intent.getParcelableExtra("auth_data");

        if (resultReceiver == null || signed == null) {
            throw new IllegalStateException("BrokerService 를 실행할 수 없습니다.");
        }

        String baseURL = new Aria(BrokerService.this.getString(R.string.MagicMRSLicense))
                .decrypt(BrokerService.this.getString(R.string.agenturl))
                +"/";

        /*
        헤더 제작
         */
        final GenerateHeaders generateHeaders = new GenerateHeaders(this);
        generateHeaders.setContentType("application/json;charset=utf-8");
        try {
            generateHeaders.setAgentDetail(signed.getUserID(), signed.getOfficeName(), signed.getOfficeCode());
            URL url = new URL(baseURL);
            generateHeaders.setUrl(url);
        } catch (UnsupportedEncodingException | PackageManager.NameNotFoundException | MalformedURLException e) {
            // TODO
            e.printStackTrace();
        }

        /*
        클라이언트의 기본 설정 하기
        url, 타임아웃설정
         */
        final int nConnectTimeOut = this.getResources().getInteger(R.integer.HttpConnectionTimeOut);
        final int nReadTimeOut = this.getResources().getInteger(R.integer.HttpReadTimeOut);

        Log.d(TAG, "baseURL : " + baseURL);
        final RelayClient client = new RelayClient(baseURL);
        client.setOkHttpClient(nConnectTimeOut, nReadTimeOut);
        client.buildClient();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO BrokerTask 객체 정보를 이용하여 retrofit 으로 구현된 RelayClient 로 요청 및 응답 콜백
                while (true) {
                    try {
                        BrokerTask task = queueBrokerTask.take();

                        // BrokerTask 객체의 serviceId 를 이용하여 클라이언트 header 값 세팅
                        // BrokerTask 객체의 serviceParams 은 body 값으로 사용
                        // 클라이언트 요청에 대한 응답은 serviceCallback 함수를 이용하여 리턴.
                        generateHeaders.setServiceId(task.getServiceId());

                        Log.d(TAG, "serviceID : " + task.getServiceId());

                        if (task.serviceId.equals(BrokerTask.SERVICE_ID_CERT_AUTH)) {

                            // Retrofit 통신
                            String body = task.serviceParam;
                            Log.d(TAG, "task body : " + body);
                            body = URLEncoder.encode(body, "UTF-8");
                            generateHeaders.setContentLength(body.length());
                            Map<String, String> headers = generateHeaders.getHeaders();
                            Log.d(TAG, "task body : " + body);

                            client.relayUserAuth(task.serviceCallback, headers, body);

                        } else {
                            Log.i(TAG, "행정 서비스 ID : " + task.serviceId);
                            // TODO 중계 서버와 통신, 기존 코드 HttpService.java - line 146, HttpTask.java - line 39
                            // Retrofit 통신
                            String body = task.serviceParam;
                            Log.d(TAG, "task body : " + body);

                            body = URLEncoder.encode(body, "UTF-8");
                            generateHeaders.setContentLength(body.length());

                            // req data에선 application/x-www-form-urlencoded; charset=utf-8 사용 (default)
                            generateHeaders.setContentType(null);

                            Map<String, String> headers = generateHeaders.getHeaders();
                            Log.d(TAG, "task body : " + body);

                            client.relayReqData(task.serviceCallback, headers, body);

                        }
                    } catch (InterruptedException ignored) {
                    } catch (UnsupportedEncodingException e) {
                        // TODO
                        e.printStackTrace();
                    }
                }
            }
        }, "broker queue").start();

        try {
            Log.d(TAG, "auth-task enqueue");
            String authParams = UserAuthenticationUtils.generateBody(signed.getSignedBase64());
            BrokerTask task = BrokerTask.generateAuthTask(authParams);
            task.setCallback(new IBrokerServiceCallback() {
                @Override
                public void onResponse(BrokerResponse response) throws RemoteException {
                    Log.d(TAG, "response code : " + response.code);
                    Log.d(TAG, "response obj : " + response.obj.toString());
                    switch (response.code) {
                        case 0: {
                            // 세션 서비스로 인증 세션 생성(등록)
                            Bundle bundle = new Bundle();

                            /*
                            Tom 200914
                            Json, Gson 별로 분기
                             */
                            // Json
                            bundle.putParcelable("user_auth", (UserAuthentication) response.obj);
                            // gson
//                            bundle.putParcelable("user_auth", (UserAuthentication.AuthModel) response.obj);

                            resultReceiver.send(BrokerService.RESULT_AUTHENTICATION_OK, bundle);
                        }
                        break;
                        case 200: {
                            // TODO 인증 실패 
                        }
                        break;
                        default:
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
            });
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
