package kr.go.mobile.agent.service.monitor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
        public SecureNetwork.STATUS getSecureNetworkStatus()  {
            return secureNetwork.getStatus();
        }

        @Override
        public void registerPackage(Bundle extra) {
            if (integrityConfirm == null)
                throw new RuntimeException("!!!!!!!!!!!!!!!!!!!!!!!!!");

            integrityConfirm.setAnotherConfirm(extra.getString("extra_token", null));
        }

        @Override
        public ThreatDetection.STATUS getThreatStatus()  {
            return threatDetection.getStatus();
        }

        @Override
        public String getTokens() {
            return integrityConfirm.getTokens();
        }
    }

    private ConcurrentMap<String, Long> monitorPackages = new ConcurrentHashMap<>();

    private SecureNetwork secureNetwork;
    private ThreatDetection threatDetection;
    private IntegrityConfirm integrityConfirm;
    private ResultReceiver resultReceiverToApp;

    private Handler idleTimeSecureNetworkHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MONITOR_SECURE_NETWORK_OVERTIME:
                    Log.d(TAG, "보안 네트워크 모니터링 - 대기 시간을 초과했습니다.");
                    // TODO 보안 네트워크 연결 종료
                    stopSecureNetwork();
                    break;
                case MONITOR_SECURE_NETWORK_REPEAT_CHECK:
                    Long baseTime = System.currentTimeMillis();
                    for (String adminPackage : monitorPackages.keySet()) {
                        Long timeUsed = monitorPackages.get(adminPackage);
                        // TODO
//                        if (baseTime - timeUsed > getResources().getInteger(R.integer.OVERTIME_SECOND_SECURE_NETWORK)) {
//                            monitorPackages.remove(adminPackage);
//                        }
                    }

                    if (monitorPackages.isEmpty()) {
                        // TODO 보안 네트워크 연결 종료
                        stopSecureNetwork();
                    } else {
                        sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_REPEAT_CHECK, getResources().getInteger(R.integer.REPEAT_CHECK_SECOND_SECURE_NETWORK) * 1000);
                    }
                    break;
            }
        }

        void stopSecureNetwork() {
            secureNetwork.stop(new EventListener<SecureNetwork.STATUS>() {
                @Override
                public void onFailure(Context context, String message, Throwable t) {

                }

                @Override
                public void onCompleted(Context context, Solution.Result<SecureNetwork.STATUS> out) {

                }
            });
        }
    };

    @Override
    public void onCreate() {
        try {
            secureNetwork = new SecureNetwork(this, SolutionManager.SSL_VPN);
            integrityConfirm = new IntegrityConfirm(this, SolutionManager.EVER_SAFE);
            threatDetection = new ThreatDetection(this, SolutionManager.V_GUARD);
        } catch (SolutionManager.ModuleNotFoundException e) {
            throw new RuntimeException("보안 솔루션 모듈 로드를 실패하였습니다. (솔루션 : " + e.getNotFoundSolutionSimpleName()  +")", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (Objects.equals(action, "local")) {
            this.resultReceiverToApp = intent.getParcelableExtra("result");
            return new LocalMonitorServiceBinder();
        } else {
            return new IMonitorService.Stub() {
                // TODO 원격 서비스 바인딩 요청 처리
            };
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int serviceId) {
        String command = intent.getAction();
        if (command == null) throw new NullPointerException("서비스 커멘드가 존재하지 않습니다.");
        switch (command) {
            case "kr.go.mobile.command.START_MONITOR":
                integrityConfirm.confirm(new EventListener<String>() {
                    @Override
                    public void onFailure(Context context, String message, Throwable t) {
                        // TODO !!
                    }

                    @Override
                    public void onCompleted(Context context, Solution.Result<String> result) {
                        Log.i(TAG, " 무결성 검증 요청 응답 - " + result.getCode());
                        switch (result.getCode()) {
                            case _OK:
                                MonitorService.this.integrityConfirm.setConfirm(result.out);
                                break;
                            case _FAIL:
                            case _INVALID:
                            case _CANCEL:
                            case _TIMEOUT:
                                MonitorService.this.integrityConfirm.setDeny(result.getErrorMessage());
                            default:
                                throw new IllegalStateException("Unexpected value: " + result.getCode());
                        }
                    }
                });
                threatDetection.detectedThreats(new EventListener<ThreatDetection.STATUS>() {
                    @Override
                    public void onFailure(Context context, String message, Throwable t) {
                        integrityConfirm.setConfirm(message);
                        Log.e(TAG, message, t);
                    }

                    @Override
                    public void onCompleted(Context context, Solution.Result<ThreatDetection.STATUS> result) {
                        switch (result.getCode()) {
                            case _OK:
                                Log.i(TAG, "악성코드 탐지 완료하였습니다. - " + result.out.name());
                                MonitorService.this.threatDetection.setStatus(result.out);
                                break;
                            case _FAIL:
                            case _INVALID:
                            case _CANCEL:
                            case _TIMEOUT:
                                Log.i(TAG, "악성코드 탐지에 실패하였습니다 : " + result.getErrorMessage());
                                MonitorService.this.threatDetection.setStatus(ThreatDetection.STATUS._ERROR);
                                // TODO
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + result.getCode());
                        }
                    }
                });
                break;
            case "kr.go.mobile.command.START_SECURE_NETWORK":
                String userId = intent.getStringExtra("id");
                String userPw = intent.getStringExtra("pw");
                secureNetwork.start(userId, userPw, new EventListener<SecureNetwork.STATUS>() {
                    @Override
                    public void onFailure(Context context, String message, Throwable t) {
                        MonitorService.this.secureNetwork.status = null;
                        // TODO 에러 데이터를 Application.resultReceiver.send
                    }

                    @Override
                    public void onCompleted(Context context, Solution.Result<SecureNetwork.STATUS> result) {
                        switch (result.getCode()) {
                            case _OK:
                                MonitorService.this.secureNetwork.status = result.out;
                                break;
                            case _FAIL:
                                // TODO 에러 데이터를 Application.resultReceiver.send
                            case _CANCEL:
                            case _TIMEOUT:
                            case _INVALID:
                            default:
                                throw new IllegalStateException("Unexpected value: " + result.getCode());
                        }

                    }
                });
                break;
            case "kr.go.mobile.command.MONITOR_START_SECURE_NETWORK":
                monitorSecureNetwork();
                break;
            case "kr.go.mobile.command.MONITOR_START_ADMIN_PACKAGE":
                // TODO 행정앱의 어플리케이션 클래스 생성시 호출하도록 변경
                addMonitorAdminPackage(intent.getStringExtra("extra_admin_package"));
                break;
        }
        return START_NOT_STICKY;
    }

    void monitorSecureNetwork() {
        Log.d(TAG, "보안 네트워크 모니터링 시작.");
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
