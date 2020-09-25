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
import kr.go.mobile.mobp.iff.R;

public class MonitorService extends Service {

    static final String TAG = "SecureMonitor";//MonitorService.class.getSimpleName();

    public static final String START_SECURE_NETWORK = "kr.go.mobile.command.START_SECURE_NETWORK";
    public static final String FORCE_STOP_SECURE_NETWORK = "kr.go.mobile.command.STOP_SECURE_NETWORK";
    public static final String MONITOR_ADD_ADMIN_PACKAGE = "kr.go.mobile.command.MONITOR_ADD_ADMIN_PACKAGE";
    public static final String MONITOR_REMOVE_ADMIN_PACKAGE = "kr.go.mobile.command.MONITOR_REMOVE_ADMIN_PACKAGE";


    public static final int RESULT_SECURE_NETWORK_FAIL = 3001;
    public static final int RESULT_SECURE_NETWORK_IDLE = 3002;
    public static final int RESULT_SECURE_NETWORK_EXPIRED = 3003;

    // 보안 네트워크를 overtime 동안 사용하지 않음.
    private static final int MONITOR_SECURE_NETWORK_OVERTIME = 0;
    private static final int MONITOR_SECURE_NETWORK_NO_RUNNING_PACKAGE = 1;


    private class LocalMonitorServiceBinder extends Binder implements ILocalMonitorService {

        @Override
        public SecureNetwork.STATUS getSecureNetworkStatus()  {
            return secureNetwork.getStatus();
        }

        @Override
        public void executeSolution(String another) {
            // 행정앱 토큰 저장
            integrityConfirm.setAnotherConfirm(another);
            // 보안 에이전트 무결성 요청
            integrityConfirm.confirm();
            // 악성코드 탐지 요청
            threatDetection.detectedThreats();
        }

        @Override
        public String getErrorMessage(int type) {
            switch (type) {
                case 1: // 무결성 에러
                    return integrityConfirm.getErrorMessage();
                case 2: // 보안 네트워크 연결 에러
                    return secureNetwork.getErrorMessage();

            }
            return null;
        }

        @Override
        public ThreatDetection.STATUS getThreatStatus()  {
            ThreatDetection.STATUS status =  threatDetection.getStatus();
            if (status != null) {
                threatDetection.monitor(true);
            }
            return status;
        }

        @Override
        public String getTokens() {
            return integrityConfirm.getTokens();
        }

        @Override
        public IntegrityConfirm.STATUS getIntegrityStatus() {
            return integrityConfirm.status();
        }

        @Override
        public void reset() {
            resetMonitorSecureNetwork(true);
        }

        @Override
        public void clear() {
            integrityConfirm.clear();
        }
    }

    private ConcurrentMap<String, Bundle> monitorItem = new ConcurrentHashMap<>();

    private SecureNetwork secureNetwork;
    private ThreatDetection threatDetection;
    private IntegrityConfirm integrityConfirm;
    @Deprecated
    private ResultReceiver resultReceiverToApp;

    private Handler monitorHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MONITOR_SECURE_NETWORK_OVERTIME:
                    Log.i(TAG, "보안 네트워크 모니터링 - 대기 시간을 초과했습니다.");
                    stopSecureNetwork();
                    break;
                case MONITOR_SECURE_NETWORK_NO_RUNNING_PACKAGE:
                    Log.i(TAG, "보안 네트워크 모니터링 - 재실행 대기 시간을 초과하였습니다.");
                    stopSecureNetwork();
                    break;
            }
        }

    };


    @Override
    public void onCreate() {
        try {
            secureNetwork = new SecureNetwork(this, SolutionManager.SSL_VPN, new EventListener<SecureNetwork.STATUS>() {
                @Override
                public void onFailure(Context context, String message, Throwable t) {
                    // TODO 에러 데이터를 Application.resultReceiver.send
                }

                @Override
                public void onCompleted(Context context, Solution.Result<SecureNetwork.STATUS> result) {
                    switch (result.getCode()) {
                        case _OK:
                            Log.i(TAG, "보안 네트워크 연결 - " + result.out);
                            if (result.out == SecureNetwork.STATUS._CONNECTED) {
                                monitorSecureNetwork(true);
                            } else if (result.out == SecureNetwork.STATUS._DISCONNECTED) {
                                monitorSecureNetwork(false);
                            }
                            break;
                        case _FAIL:
                            secureNetwork.errorMessage = result.getErrorMessage();
                            break;
                        case _CANCEL:
                            Log.w(TAG, "요청이 취소되었습니다. " + result.getErrorMessage());
                            break;
                        case _TIMEOUT:
                        case _INVALID:
                        default:
                            throw new IllegalStateException("Unexpected value: " + result.getCode());
                    }

                }


            });
            integrityConfirm = new IntegrityConfirm(this, SolutionManager.EVER_SAFE, new EventListener<String>() {
                @Override
                public void onFailure(Context context, String message, Throwable t) {
                    // TODO !!
                }

                @Override
                public void onCompleted(Context context, Solution.Result<String> result) {
                    Log.i(TAG, "무결성 검증 요청 응답 - " + result.getCode() + (result.getErrorMessage().isEmpty() ? "" : ", " +  result.getErrorMessage()));
                    switch (result.getCode()) {
                        case _OK:
                            MonitorService.this.integrityConfirm.setConfirm(result.out);
                            break;
                        case _FAIL:
                        case _INVALID:
                        case _CANCEL:
                        case _TIMEOUT:
                            MonitorService.this.integrityConfirm.setDeny(result.getErrorMessage());
                            break;
                        default:
                            throw new IllegalStateException("무결성 검증 요청에 대한 응답값이 예상하지 않은 값이 전달되었습니다. value = " + result.getCode());
                    }
                }
            });
            threatDetection = new ThreatDetection(this, SolutionManager.V_GUARD, new EventListener<ThreatDetection.STATUS>() {
                @Override
                public void onFailure(Context context, String message, Throwable t) {
                    integrityConfirm.setConfirm(message);
                    Log.e(TAG, message, t);
                }

                @Override
                public void onCompleted(Context context, Solution.Result<ThreatDetection.STATUS> result) {
                    switch (result.getCode()) {
                        case _OK:
                            Log.i(TAG, "악성코드 탐지 완료 - " + result.out.name());
                            MonitorService.this.threatDetection.setStatus(result.out);
                            // TODO 행정앱한테 알려줘야지!!
                            break;
                        case _FAIL:
                        case _INVALID:
                        case _CANCEL:
                        case _TIMEOUT:
                            Log.i(TAG, "악성코드 탐지 실패 - " + result.getErrorMessage());
                            MonitorService.this.threatDetection.setStatus(ThreatDetection.STATUS._ERROR);
                            // TODO
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + result.getCode());
                    }
                }
            });
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
            case START_SECURE_NETWORK:
                String userId = intent.getStringExtra("id");
                String userPw = intent.getStringExtra("pw");
                secureNetwork.start(this, userId, userPw);
                break;
            case FORCE_STOP_SECURE_NETWORK:
                stopSecureNetwork();
                break;
            case MONITOR_ADD_ADMIN_PACKAGE:
                Bundle extraAdmin = intent.getBundleExtra("admin_info");
                if (extraAdmin == null) {
                    throw new RuntimeException("실행되는 행정앱의 정보가 존재하지 않습니다.");
                }
                String addId = extraAdmin.getString("req_id_base64");
                monitorHandler.removeMessages(MONITOR_SECURE_NETWORK_NO_RUNNING_PACKAGE);
                monitorItem.put(addId, extraAdmin);
                Log.d(TAG, "모니터링 대상 추가 - " + addId);
                break;
            case MONITOR_REMOVE_ADMIN_PACKAGE:
                String removeId = intent.getStringExtra("req_id_base64");
                monitorItem.remove(removeId);
                Log.d(TAG, "모니터링 대상 제거 - " + removeId);
                if (monitorItem.isEmpty()) {
                    Log.d(TAG, "재실행 행정앱 대기");
                    monitorHandler.sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_NO_RUNNING_PACKAGE, getResources().getInteger(R.integer.SECURE_NETWORK_WAIT_FOR_NO_RUNNING_PACKAGE) * 1000);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + command);
        }

        return START_NOT_STICKY;
    }

    private void stopSecureNetwork() {
        secureNetwork.stop();
    }

    private void monitorSecureNetwork(boolean enabled) {
        if (enabled) {
            Log.d(TAG, "보안 네트워크 모니터링 시작");
            secureNetwork.monitor();
        } else {
            Log.d(TAG, "보안 네트워크 모니터링 중지");
            secureNetwork.disabledMonitor();
            if (!monitorItem.isEmpty()) {
                // 공통기반 v2.x 호환을 위함.
                Intent intent = new Intent("kr.go.mobile.ACTION_CONTROL");
                intent.putExtra("extra_type", 100);
                sendBroadcast(intent);
                // TODO 공통기반 3.0 코드 추가
            }
        }
        resetMonitorSecureNetwork(enabled);
    }

    private void resetMonitorSecureNetwork(boolean enabled) {
        monitorHandler.removeMessages(MONITOR_SECURE_NETWORK_OVERTIME);
        if (enabled) {
            monitorHandler.sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_OVERTIME, getResources().getInteger(R.integer.SECURE_NETWORK_IDLE_OVERTIME_SEC) * 1000);
        }
    }
}
