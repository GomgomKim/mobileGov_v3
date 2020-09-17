package kr.go.mobile.agent.app;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import kr.go.mobile.agent.service.broker.BrokerService;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.service.monitor.ILocalMonitorService;
import kr.go.mobile.agent.service.monitor.MonitorService;
import kr.go.mobile.agent.service.monitor.SecureNetworkData;
import kr.go.mobile.agent.service.monitor.ValidateToken;
import kr.go.mobile.agent.service.session.ILocalSessionService;
import kr.go.mobile.agent.service.session.SessionService;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.agent.utils.HardwareUtils;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.v3.CommonBaseInitActivity;
import kr.go.mobile.common.v3.MobileEGovConstants;

/**
 * 모바일 전자정부에서 사용하는 보안 에이전트 (Security Agent) 의 Application 객체
 * 객체 생성시
 */
public class SAGTApplication extends Application implements Application.ActivityLifecycleCallbacks,
        CommonBaseInitActivity.IPublicAPI, BrokerService.IServiceManager {

    private static String TAG = SAGTApplication.class.getSimpleName();
    private final DateFormat SUFFIX_LOGFILE = new SimpleDateFormat("ddMMyyHHmmss", Locale.KOREAN);

    // Service 로 부터 실시간 데이터를 전달 받는다. (Service --> Application)
    private ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            Log.i(TAG, "서비스 응답 결과 : " + resultCode);
            switch (resultCode) {
                case MonitorService.RESULT_SECURE_NETWORK_OK: {
                    // TODO
                    break;
                }
                case SessionService.RESULT_SIGNED_REGISTER_OK: {
                    sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_SIGNED_REGISTERED_OK);
                    break;
                }
                case BrokerService.RESULT_AUTHENTICATION_OK: {
                    try {
                        // 세션 서비스에 인증 세션 값 등록
                        UserAuthentication authentication = resultData.getParcelable("user_auth");
                        sessionManager.registerAuthSession(authentication);
                        break;
                    } catch (SessionManager.SessionException e) {
                        e.printStackTrace();
                    }
                }
                case BrokerService.RESULT_AUTHENTICATION_FAIL:
                case SessionService.RESULT_SIGNED_REGISTER_FAIL:
                case MonitorService.RESULT_SECURE_NETWORK_FAIL: {
                    handleFailMessage(resultData);
                    break;
                }
            }
        }

        private void handleFailMessage(Bundle resultData) {
            String message = resultData.getString("message", "알수없음");
            Log.d(TAG, "실패 메시지 - " + message);
        }
    };

    private ServiceConnection localServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected - " + service.toString());
            if (service instanceof ILocalSessionService) {
                createSessionServiceManager(service);
            } else if (service instanceof ILocalMonitorService) {
                createMonitorServiceManager(service);
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            SessionManager.destroy();
            sessionManager = null;
        }

        void createSessionServiceManager(IBinder service) {
            Log.concurrency(Thread.currentThread(), "createSessionServiceManager");
            sessionManager = SessionManager.create(service);
        }

        void createMonitorServiceManager(IBinder service) {
            Log.concurrency(Thread.currentThread(), "createMonitorServiceManager");
            monitorManager = MonitorManager.create(service);
            monitorManager.start(SAGTApplication.this);
        }
    };

    private volatile SessionManager sessionManager;
    private volatile MonitorManager monitorManager;
    private ValidateToken validateToken  = new ValidateToken();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.ENABLE = true;
        Log.i(TAG, "보안 Agent 를 시작합니다.");

        bindServices(new Class[]{MonitorService.class, SessionService.class});

        try {
            initSolutionModules();
        } catch (SolutionManager.ModuleNotFoundException e) {
            throw new RuntimeException(e);
        }

        registerActivityLifecycleCallbacks(this);
        startCrashMonitor();
    }

    void bindServices(Class<?>[] initServiceClasses) {
        for (Class<?> clazz : initServiceClasses) {
            Intent bindIntent = new Intent(this, clazz);
            bindIntent.setAction("local");
            bindIntent.putExtra("result", resultReceiver);
            bindService(bindIntent, localServiceConnection,
                    Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
        }
    }

    void initSolutionModules() throws SolutionManager.ModuleNotFoundException {
        SolutionManager.initSolutionModule(SolutionManager.EVER_SAFE, new Solution.EventListener<String>() {
            @Override
            public void onCancel(Context context) {
                Log.w(TAG, "사용자에 의하여 취소되었습니다.");
                sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_USER_CANCELED);
            }

            @Override
            public void onFailure(Context context, String message, Throwable t) {
                Log.e(TAG, message + ", throw message : " +  t.getMessage(), t);
                sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_SOLUTION_ERROR, "무결성 체크 솔루션 연동 에러 - " + message);
            }

            @Override
            public void onError(Context context, Solution.RESULT_CODE errorCode, String message) {
                Log.e(TAG, "무결성 체크 중 에러가 발생하였습니다. : " + message);
                sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_SOLUTION_ERROR, "무결성 체크 솔루션 실행 에러 - " + message);
            }

            @Override
            public void onCompleted(Context context, String verificationToken) {
                if (Objects.equals(verificationToken, "")) {
                    sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_SOLUTION_ERROR, "무결성 체크 솔루션 실행 에러 - 필수 정보 획득 실패");
                    return;
                }
                Log.calling();
                validateToken.setAgent(verificationToken);
            }
        });
    }

    @Override
    public boolean verifyIntegrityApp(){
        return validateToken.existAgentToken();
    }

    @Override
    public String getThreatMessage() throws NotResponseServiceException {
        if (monitorManager == null) {
            throw new NotResponseServiceException();
        }
        Log.concurrency(Thread.currentThread(), "getThreatMessage");
        return monitorManager.getThreatMessage();
    }

    @Override
    public void setExtraData(@NotNull Bundle extra) {
        String token = extra.getString("extra_token");
        validateToken.setExtra(token);
    }

    @Override
    public void clearToken() {
        validateToken.clearAgentToken();
    }

    @Override
    public boolean readyService() {
        Log.concurrency(Thread.currentThread(), "readyService");
        return (sessionManager != null && monitorManager != null);
    }

    @Override
    public boolean validSignedSession()  {
        try {
            sessionManager.validSignedSession();
        } catch (SessionManager.SessionException e) {
            switch (e.getExpiredType()) {
                case SessionManager.SessionException.NO_SIGNED_SESSION: // 사용자 세션 없음.
                    Log.d(TAG, "서명 세션이 존재하지 않습니다.");
                    break;
                case SessionManager.SessionException.EXPIRED_SIGNED_SESSION:  // 사용자 세션 정보 만료
                    Log.d(TAG, "서명 세션이 만료되었습니다.");
                    break;
            }
            return false;
        }
        return true;
    }

    @Override
    public void registeredSignedSession(UserSigned signed) {
        Intent intent = new Intent(this, SessionService.class);
        intent.putExtra("signed_data", signed);
        intent.putExtra("extra_receiver", resultReceiver);
        startService(intent);
    }

    @Override
    public void startSecureNetwork() {
        String hardwareID = HardwareUtils.getAndroidID(this);
        ////// SSL-VPN 솔루션에 종속적인 코드임. (SSL-VPN 에 로그인하기 위하여 약속된 ID 생성) //////////
        String loginId = String.format("%s,deviceID=%s|%s",
                sessionManager.getUserDN(), hardwareID, validateToken.getTokens());
        ///////////////////////////////////////////////////////////////////////////////////////////
        Log.concurrency(Thread.currentThread(), "startSecureNetwork");
        monitorManager.startSecureNetwork(new SecureNetworkData(loginId, ""));
    }

    @Override
    public boolean enabledSecureNetwork() throws NotResponseServiceException {
        Log.concurrency(Thread.currentThread(), "enabledSecureNetwork");
        return monitorManager.enabledSecureNetwork();
    }

    @Override
    public String getErrorMessage() throws NotResponseServiceException {
        Log.concurrency(Thread.currentThread(), "getErrorMessage");
        return monitorManager.getErrorMessage();
    }

    @Override
    public void startBrokerService() {
        Intent intent = new Intent(this, BrokerService.class);
        intent.putExtra("auth_data", sessionManager.getUserSigned());
        intent.putExtra("extra_receiver", resultReceiver);
        startService(intent); // -----> RESULT_AUTHENTICATION_OK
    }

    @Override
    public Bundle getResultBundle() {
        Bundle extra = new Bundle();
        extra.putString(MobileEGovConstants.EXTRA_KEY_USER_ID, sessionManager.getUserId());
        extra.putString(MobileEGovConstants.EXTRA_KEY_DN, sessionManager.getUserDN());
        return extra;
    }

    public void sendBroadcastToActivity(int event) {
        sendBroadcastToActivity(event, "");
    }

    public void sendBroadcastToActivity(int event, String message) {
        Intent i = new Intent(CommonBaseInitActivity.ACTION_EVENT);
        i.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        i.putExtra(CommonBaseInitActivity.ACTION_EXTRA_TYPE, event);
        i.putExtra(CommonBaseInitActivity.ACTION_EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void startCrashMonitor() {
        Thread.UncaughtExceptionHandler _DEFAULT_HANDLER = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(new Handler()));
        String cause = null;
        try {
            Looper.loop();
            // Looper.loop() 에서 벗어나는 경우는 MessageQueue 가 종료되고 있을 경우..
            // 즉, Application 이 종료되고 있을 경우 DefaultUncaughtExceptionHandler 를 기존 Handler 로 설정한다.
            Thread.setDefaultUncaughtExceptionHandler(_DEFAULT_HANDLER);
        } catch (CrashHandler.MegRuntimeException e) {
            // runtime 중 백그라운드 스레드에서 발생한 예기치 못한 예외사항
            Log.e(TAG, String.format("%s 스레드에서 예기치 않은 오류가 발생하였습니다. (PID : %d)",  e.threadName, e.pid), e.getCause());
            if (e.getCause() != null) {
                writeCrashLog(e.getCause());
                cause = e.getMessage();
            }
        } catch (Throwable e) {
            // UI 스레드에서 발생한 예기치 못한 예외사항
            Log.e(TAG, "UI 스레드에서 예기치 않은 오류가 발생하였습니다.", e);
            writeCrashLog(e);
            cause = e.getMessage();
        } finally {
            // 예기치 않은 에러 발생할 경우 종료!
            Log.e(TAG, "예기치 않은 오류로 앱을 종료합니다. " + cause);
            finishApplication();
        }
    }

    private void writeCrashLog(@NonNull Throwable t)  {
        StringBuilder sb = new StringBuilder();
        sb.append("- Throwable Message: ").append(t.toString()).append("\n\n");
        String PREFIX_LOGFILE = "crash-log-%s.log";
        String exportFileName = String.format(PREFIX_LOGFILE, SUFFIX_LOGFILE.format(new Date()));
        StackTraceElement[] arr = t.getStackTrace();
        sb.append("--------- Stack trace ---------\n\n");
        for (StackTraceElement stackTraceElement : arr) {
            sb.append("\t").append(stackTraceElement.toString()).append("\n");
        }
        sb.append("-------------------------------\n\n");
        String stringSubCause = getCauseMessage(t);
        sb.append(stringSubCause);

        FileOutputStream fout = null;
        try {
            fout = openFileOutput(exportFileName, Context.MODE_PRIVATE);
            fout.write(sb.toString().getBytes());
            fout.flush();
        } catch (FileNotFoundException e) {
            Log.w(TAG, "ERROR Write CrashLog - File not create", e);
        } catch (IOException e) {
            Log.w(TAG, "ERROR Write CrashLog - File IO", e);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    Log.e(TAG, "IO error : ", e);
                }
            }
        }
    }

    @NotNull
    private String getCauseMessage(Throwable t) {
        StringBuilder sb = new StringBuilder();

        Throwable cause = t.getCause();
        if(cause != null) {
            sb.append("--------- Cause ---------\n\n");
            sb.append(cause.toString()).append("\n\n");
            StackTraceElement[] arr = cause.getStackTrace();
            for (StackTraceElement stackTraceElement : arr) {
                sb.append("\t").append(stackTraceElement.toString()).append("\n");
            }
            String strCause = getCauseMessage(cause);
            sb.append(strCause);
            sb.append("-------------------------------\n\n");
        }

        return sb.toString();
    }

    private void finishApplication() {
        if (topActivity.get() != null) {
            // 마지막 resume activity 가 존재하면
            topActivity.get().moveTaskToBack(true);
            topActivity.get().finish();
        }
        System.exit(0);
    }


    @Override
    public SessionManager getSessionManager() {
        return this.sessionManager;
    }

    @Override
    public MonitorManager getMonitorManager() {
        return this.monitorManager;
    }

    ////////////// Activity Lifecycle Callback /////////////////
    // TODO 공통기반 초기화 화면에서 사용자가 홈키로 나갈 경우 초기화 처리를 강제 중지해야 함.
    final ConcurrentHashMap<Activity, Boolean> resumeActivities = new ConcurrentHashMap<>();
    final AtomicReference<Activity> topActivity = new AtomicReference<>();

    public Activity getTopActivity() {
        if(this.topActivity.get() != null && this.topActivity.get().isDestroyed()) {
            this.topActivity.getAndSet(null);
        }
        return this.topActivity.get();
    }
    private void deleteTopActivity(Activity activity) {
        resumeActivities.remove(activity);
        if (topActivity.get().equals(activity)) {
            this.topActivity.getAndSet(null);
        }
    }
    private void setTopActivity(@NotNull  Activity activity) {
        resumeActivities.put(activity, true);
        topActivity.getAndSet(activity);
    }
    private void changeStatus(@NotNull  Activity activity) {
        resumeActivities.put(activity, false);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
//        Log.d(TAG, "onActivityCreated - " + activity.toString());
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
//        Log.d(TAG, "onActivityStarted - " + activity.toString());
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        setTopActivity(activity);
//        Log.d(TAG, "onActivityResumed - " + activity.toString());
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
//        Log.d(TAG, "onActivityPaused - " +activity.toString());
        changeStatus(activity);
        Enumeration<Activity> keys = resumeActivities.keys();
        int resumeCount = 0;
        while(keys.hasMoreElements()) {
            Activity a = keys.nextElement();
            boolean isResume = resumeActivities.get(a);
            if (isResume) {
                resumeCount++;
            }
//            Log.d(TAG, a.toString() + " status(resume) = " + isResume);
        }
        if (resumeCount == 0) {
//            Log.e(TAG, "force finish");
//            finishApplication();
            // TODO 사용자가 홈키로 나갈 경우 종료 ?
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        deleteTopActivity(activity);
    }

}
