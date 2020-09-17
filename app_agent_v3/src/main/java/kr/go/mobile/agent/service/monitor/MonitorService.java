package kr.go.mobile.agent.service.monitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.Solution.EventListener;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.v3.solution.SecuwizVPNSolution;
import kr.go.mobile.mobp.iff.R;

public class MonitorService extends Service {

    static final String TAG = MonitorService.class.getSimpleName();

    @Deprecated
    public static final int RESULT_MALWARE_SCAN_OK = 2000;
    @Deprecated
    public static final int RESULT_MALWARE_SCAN_FAIL = 2001;
    public static final int RESULT_SECURE_NETWORK_OK = 3000;
    public static final int RESULT_SECURE_NETWORK_FAIL = 3001;
    public static final int RESULT_SECURE_NETWORK_IDLE = 3002;
    public static final int RESULT_SECURE_NETWORK_EXPIRED = 3003;

    // 보안 네트워크를 overtime 동안 사용하지 않음. 
    private static final int MONITOR_SECURE_NETWORK_OVERTIME = 0;
    // 보안 네트워크를 반복하여 확인
    private static final int MONITOR_SECURE_NETWORK_REPEAT_CHECK = 1;

    private class LocalMonitorServiceBinder extends Binder implements ILocalMonitorService {
        @Override
        public boolean enabledSecureNetwork()  {
            if (secureNetworkData == null ||
                    secureNetworkData.getStatus() == SecureNetworkData.STATUS._CONNECTING)
                throw new NullPointerException();

            return secureNetworkData.getStatus() == SecureNetworkData.STATUS._CONNECTED;
        }

        @Override
        public String getErrorMessage()  {
            return secureNetworkData.getFailMessage();
        }

        @Override
        public String getThreatMessage()  {
            if (threatData == null)
                throw new NullPointerException();

            return threatData.threatMessage;
        }

        @Override
        public void startSecureNetwork(SecureNetworkData secureNetworkData) {
            try {
                Solution<?, ?> secureNetworkSolution = SolutionManager.getSolutionModule(SolutionManager.SSL_VPN);
                if (secureNetworkSolution instanceof SecuwizVPNSolution) {
                    ((SecuwizVPNSolution)secureNetworkSolution).execute(secureNetworkData);
                }
            } catch (SolutionManager.ModuleNotFoundException e) {
                throw new RuntimeException(e.getMessage() + " 솔루션 모듈을 찾을 수 없습니다.", e);
            }

        }
    }

    private ConcurrentMap<String, Long> monitorPackages = new ConcurrentHashMap<>();

    private volatile SecureNetworkData secureNetworkData;
    private volatile ThreatData threatData;
    private ResultReceiver resultReceiver;

    private Handler idleTimeSecureNetworkHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MONITOR_SECURE_NETWORK_OVERTIME:
                    // TODO 보안 네트워크 연결 종료
//                    solutionSecureNetwork.execute();
                    break;
                case MONITOR_SECURE_NETWORK_REPEAT_CHECK:
                    Long baseTime = System.currentTimeMillis();
                    for (String adminPackage : monitorPackages.keySet()) {
                        Long timeUsed = monitorPackages.get(adminPackage);
                        if (baseTime - timeUsed > getResources().getInteger(R.integer.OVERTIME_SECOND_SECURE_NETWORK)) {
                            monitorPackages.remove(adminPackage);
                        }
                    }

                    if (monitorPackages.isEmpty()) {
                        // TODO 보안 네트워크 연결 종료
                    } else {
                        sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_REPEAT_CHECK, getResources().getInteger(R.integer.REPEAT_CHECK_SECOND_SECURE_NETWORK) * 1000);
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        try {
            SolutionManager.initSolutionModule(SolutionManager.SSL_VPN, new EventListener<SecureNetworkData>() {
                @Override
                public void onCancel(Context context) {

                }

                @Override
                public void onFailure(Context context, String message, Throwable t) {

                }

                @Override
                public void onError(Context context, Solution.RESULT_CODE errorCode, String errorMessage) {
                    SecureNetworkData error = new SecureNetworkData("", "");
                    error.status = SecureNetworkData.STATUS._DISCONNECTED;
                    error.failMessage = errorMessage;
                    MonitorService.this.secureNetworkData = error;

                    Bundle bundle = new Bundle();
                    bundle.putString("message", errorMessage);
                    resultReceiver.send(RESULT_SECURE_NETWORK_FAIL, bundle);
                }

                @Override
                public void onCompleted(Context context, SecureNetworkData secureNetworkData) {
                    Log.concurrency(Thread.currentThread(), "secureNetwork set");
                    MonitorService.this.secureNetworkData = secureNetworkData;

                    Bundle resultData = new Bundle();
                    resultData.putParcelable("secure_network", secureNetworkData);
                    resultReceiver.send(RESULT_SECURE_NETWORK_OK, resultData);
                }
            });
            SolutionManager.initSolutionModule(SolutionManager.V_GUARD, new EventListener<ThreatData>() {

                @Override
                public void onCancel(Context context) {
                    Log.w(TAG, "사용자에 의하여 취소되었습니다.");
                }

                @Override
                public void onFailure(Context context, String message, Throwable t) {
                    Log.e(TAG, message + ", throw message : " +  t.getMessage(), t);
                    Bundle extra = new Bundle();
                    extra.putString("error_cause", message);
                    resultReceiver.send(RESULT_MALWARE_SCAN_FAIL, extra);
                }

                @Override
                public void onError(Context context, Solution.RESULT_CODE errorCode, String message) {
                    Log.e(TAG, "악성코드 탐지 중 에러가 발생하였습니다. : " + message);
                    Bundle extra = new Bundle();
                    switch (errorCode) {
                        case _INVALID:
                            extra.putString("error_cause", message);
                            resultReceiver.send(RESULT_MALWARE_SCAN_FAIL, extra);
                            break;
                        case _FAIL:
                        case _CANCEL:
                        case _TIMEOUT:
                        default:
                            throw new IllegalStateException("Unexpected value: " + errorCode);
                    }
                }

                @Override
                public void onCompleted(Context context, ThreatData threatData) {
                    Log.i(TAG, "악성코드 탐지 완료하였습니다. - " + threatData.threatMessage);
                    Log.concurrency(Thread.currentThread(), "threatData set");
                    MonitorService.this.threatData = threatData;

                    Bundle resultData = new Bundle();
                    resultData.putParcelable("thread_data", threatData);
                    resultReceiver.send(RESULT_MALWARE_SCAN_OK, resultData);
                }

            });
        } catch (SolutionManager.ModuleNotFoundException e) {
            throw new RuntimeException("솔루션을 연계할 수 없습니다.", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (Objects.equals(action, "local")) {
            this.resultReceiver = intent.getParcelableExtra("result");
            return new LocalMonitorServiceBinder();
        } else {
            // TODO 원격 서비스 바인딩 요청 처리
            return new IMonitorService.Stub() {
            };
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int serviceId) {
        try {
            String command = intent.getAction();
            if (command.equals("kr.go.mobile.command.START_MONITOR")) {
                SolutionManager.getSolutionModule(SolutionManager.V_GUARD).execute();
                SolutionManager.getSolutionModule(SolutionManager.SSL_VPN).execute();
            } else if (command.equals("kr.go.mobile.command.MONITOR_START_SECURE_NETWORK")) {
                monitorSecureNetwork();
            } else if (command.equals("kr.go.mobile.command.MONITOR_START_ADMIN_PACKAGE")) {
                // TODO 행정앱의 어플리케이션 클래스 생성시 호출하도록 변경
                addMonitorAdminPackage(intent.getStringExtra("extra_admin_package"));
            }
        } catch (SolutionManager.ModuleNotFoundException | NullPointerException e) {
            throw new RuntimeException(e);
        }

        return START_NOT_STICKY;
    }

    void monitorSecureNetwork() {
        idleTimeSecureNetworkHandler.sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_OVERTIME, getResources().getInteger(R.integer.OVERTIME_SECOND_SECURE_NETWORK) * 1000);
    }

    void resetMonitorSecureNetwork() {
        idleTimeSecureNetworkHandler.removeMessages(MONITOR_SECURE_NETWORK_OVERTIME);
        idleTimeSecureNetworkHandler.sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_OVERTIME, getResources().getInteger(R.integer.OVERTIME_SECOND_SECURE_NETWORK) * 1000);
    }

    void addMonitorAdminPackage(String adminPackage) {
        idleTimeSecureNetworkHandler.sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_REPEAT_CHECK, getResources().getInteger(R.integer.REPEAT_CHECK_SECOND_SECURE_NETWORK) * 1000);
        monitorPackages.put(adminPackage, System.currentTimeMillis());
    }

    void updateMonitorAdminPackage(String adminPackage) {
        monitorPackages.put(adminPackage, System.currentTimeMillis());
    }

    void removeMonitorAdminPackage(String adminPackage) {
        monitorPackages.remove(adminPackage);
    }
}
