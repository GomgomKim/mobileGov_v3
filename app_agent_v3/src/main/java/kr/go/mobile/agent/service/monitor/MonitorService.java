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
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.Solution.EventListener;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.mobp.iff.R;
import kr.go.mobile.support.v2.Utils;

public class MonitorService extends Service {

    static final String TAG = "SecureMonitor";//MonitorService.class.getSimpleName();

    public static final String MONITOR_REMOVE_ADMIN_PACKAGE = "kr.go.mobile.command.MONITOR_REMOVE_ADMIN_PACKAGE";

    // 보안 네트워크를 overtime 동안 사용하고 있지 않음.
    private static final int MONITOR_SECURE_NETWORK_OVERTIME = 0;
    // 실행 중인 행정앱이 존재하지 않음.
    private static final int MONITOR_SECURE_NETWORK_NO_RUNNING_PACKAGE = 1;

    private class LocalMonitorServiceBinder extends Binder implements ILocalMonitorService {

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

        private ConcurrentMap<Integer, Bundle> monitorItem = new ConcurrentHashMap<>();
        private SecureNetwork secureNetwork;
        private ThreatDetection threatDetection;
        private IntegrityConfirm integrityConfirm;

        LocalMonitorServiceBinder(Context context) {
            try {
                secureNetwork = new SecureNetwork(context, SolutionManager.SSL_VPN, new EventListener<SecureNetwork.STATUS>() {
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

                integrityConfirm = new IntegrityConfirm(context, SolutionManager.EVER_SAFE, new EventListener<String>() {
                    @Override
                    public void onFailure(Context context, String message, Throwable t) {
                        integrityConfirm.setDeny(message);
                    }

                    @Override
                    public void onCompleted(Context context, Solution.Result<String> result) {
                        Log.i(TAG, "무결성 검증 요청 응답 - " + result.getCode() + (result.getErrorMessage().isEmpty() ? "" : ", " +  result.getErrorMessage()));
                        switch (result.getCode()) {
                            case _OK:
                                LocalMonitorServiceBinder.this.integrityConfirm.setConfirm(result.out);
                                break;
                            case _FAIL:
                            case _INVALID:
                            case _CANCEL:
                            case _TIMEOUT:
                                LocalMonitorServiceBinder.this.integrityConfirm.setDeny(result.getErrorMessage());
                                break;
                            default:
                                throw new IllegalStateException("무결성 검증 요청에 대한 응답값이 예상하지 않은 값이 전달되었습니다. value = " + result.getCode());
                        }
                    }
                });

                threatDetection = new ThreatDetection(context, SolutionManager.V_GUARD, new EventListener<ThreatDetection.STATUS>() {
                    @Override
                    public void onFailure(Context context, String message, Throwable t) {
                        Log.e(TAG, message, t);
                    }

                    @Override
                    public void onCompleted(Context context, Solution.Result<ThreatDetection.STATUS> result) {
                        switch (result.getCode()) {
                            case _OK:
                                Log.i(TAG, "악성코드 탐지 완료 - " + result.out.name());
                                LocalMonitorServiceBinder.this.threatDetection.setStatus(result.out);
                                break;
                            case _CANCEL:
                                Log.i(TAG, "악성코드 탐지 실패 - " + result.getErrorMessage());
                                LocalMonitorServiceBinder.this.threatDetection.setStatus(ThreatDetection.STATUS._PERMISSION_NOT_GRANTED);
                                break;
                            case _FAIL:
                            case _INVALID:
                            case _TIMEOUT:
                                Log.i(TAG, "악성코드 탐지 실패 - " + result.getErrorMessage());
                                LocalMonitorServiceBinder.this.threatDetection.setStatus(ThreatDetection.STATUS._ERROR);
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

        private void monitorSecureNetwork(boolean enabled) {
            if (enabled) {
                Log.d(TAG, "보안 네트워크 모니터링 시작");
                secureNetwork.monitor();
            } else {
                Log.d(TAG, "보안 네트워크 모니터링 중지 / 인증세션초기화");

                // 인증 세션 초기화
                Intent i = new Intent("auth_session_clean");
                startService(i);

                // 보안 네트워크 중지
                secureNetwork.disabledMonitor();

                // 구동 중이던 행정앱 종료 요청
                if (!monitorItem.isEmpty()) {
                    // 공통기반 v2.x 호환을 위함.
                    Utils.sendFinishApp(MonitorService.this);

                    Set<Integer> monitorUids = monitorItem.keySet();
                    for (int uid : monitorUids) {
                        Bundle info = monitorItem.get(uid);
                        Message m = Message.obtain(null, CommonBasedConstants.CMD_FORCE_KILL_DISABLED_SECURE_NETWORK);
                        try {
                            ((Messenger)info.get("replyTo")).send(m);
                        } catch (RemoteException e) {
                            Log.e(TAG, "종료 명령을 전달할 수 없습니다.");
                        }
                    }
                }
            }
            resetMonitorSecureNetwork(enabled);
        }

        @Override
        public SecureNetwork.STATUS getSecureNetworkStatus()  {
            return secureNetwork.getStatus();
        }

        @Override
        public void executeSolution(String another) {
            // 행정앱 토큰 저장
            integrityConfirm.setAnotherConfirm(another);
            // 보안 에이전트 무결성 요청
            integrityConfirm.confirm(getBaseContext());
            // 악성코드 탐지 요청
            threatDetection.detectedThreats();
        }

        @Override
        public void startSecureNetwork(Context ctx, String id, String pw) {
            secureNetwork.start(ctx, id, pw);
        }

        @Override
        public void stopSecureNetwork() {
            secureNetwork.stop();
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


        public void monitorTarget(int reqUid, Bundle reqPkgInfo) {
            monitorItem.put(reqUid, reqPkgInfo);
        }

        @Override
        public void addPackage(Bundle info) {
            monitorHandler.removeMessages(MONITOR_SECURE_NETWORK_NO_RUNNING_PACKAGE);

            int uid = info.getInt("req_id");
            if (monitorItem.containsKey(uid)) {
                Messenger replyTo = monitorItem.get(uid).getParcelable("replyTo");
                info.putParcelable("replyTo", replyTo);
                monitorItem.put(uid, info);
                Log.d(TAG, "모니터링 대상 추가 - " + uid);
            } else {
                Log.e(TAG, "모니터링 대상 추가 거부 - " + uid);
            }
        }

        @Override
        public void removePackage(String uid) {
            monitorItem.remove(uid);
            Log.d(TAG, "모니터링 대상 제거 - " + uid);
            if (monitorItem.isEmpty()) {
                Log.d(TAG, "재실행 행정앱 대기");
                monitorHandler.sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_NO_RUNNING_PACKAGE, getResources().getInteger(R.integer.SECURE_NETWORK_WAIT_FOR_NO_RUNNING_PACKAGE) * 1000);
            }
        }

        @Override
        public String getPackageName(int uid) {
            Bundle packageInfo = monitorItem.get(uid);
            return packageInfo.getString("app_id", "/*NO_INFO*/");
        }

        @Override
        public String getVersionCode(int uid) {
            Bundle packageInfo = monitorItem.get(uid);
            return packageInfo.getString("app_code", "/*NO_INFO*/");
        }

        private void resetMonitorSecureNetwork(boolean enabled) {
            monitorHandler.removeMessages(MONITOR_SECURE_NETWORK_OVERTIME);
            if (enabled) {
                monitorHandler.sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_OVERTIME, getResources().getInteger(R.integer.SECURE_NETWORK_IDLE_OVERTIME_SEC) * 1000);
            }
        }

        public void waitAndStop() {
            monitorHandler.sendEmptyMessageDelayed(MONITOR_SECURE_NETWORK_NO_RUNNING_PACKAGE, getResources().getInteger(R.integer.SECURE_NETWORK_WAIT_FOR_NO_RUNNING_PACKAGE) * 1000);
        }

    }

    private LocalMonitorServiceBinder localMonitor;

    final Messenger monitorMessenger = new Messenger(new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            int req_uid = msg.arg1;
            if (req_uid == -1) return;
            if (msg.what == CommonBasedConstants.EVENT_COMMAND_HANDLER_REGISTERED) {
                Bundle baseBundle = new Bundle();
                baseBundle.putParcelable("replyTo", msg.replyTo);
                localMonitor.monitorTarget(req_uid, baseBundle);
            }
        }
    });

    @Override
    public void onCreate() {
        localMonitor = new LocalMonitorServiceBinder(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (Objects.equals(action, "local")) {
            return localMonitor;
        } else {
            return monitorMessenger.getBinder();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        String action = intent.getAction();
        if (Objects.equals(action, "local")) {
            // 로컬 서비스가 언바인딩 되는 경우는 앱 종료 ?
        } else {
            int removeId = intent.getIntExtra("req_id", -1);

            localMonitor.monitorItem.remove(removeId);
            Log.d(TAG, "모니터링 대상 제거 - " + removeId);
            if (localMonitor.monitorItem.isEmpty()) {
                Log.d(TAG, "재실행 행정앱 대기");
                localMonitor.waitAndStop();
            }
        }
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int serviceId) {
        throw new IllegalStateException("Unexpected value: " + intent);
    }

}
