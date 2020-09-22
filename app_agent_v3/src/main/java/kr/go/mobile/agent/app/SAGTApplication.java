package kr.go.mobile.agent.app;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import kr.go.mobile.agent.service.broker.BrokerService;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.service.monitor.ILocalMonitorService;
import kr.go.mobile.agent.service.monitor.IntegrityConfirm;
import kr.go.mobile.agent.service.monitor.MonitorService;
import kr.go.mobile.agent.service.monitor.SecureNetwork;
import kr.go.mobile.agent.service.monitor.ThreatDetection;
import kr.go.mobile.agent.service.session.ILocalSessionService;
import kr.go.mobile.agent.service.session.SessionService;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.utils.HardwareUtils;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.ResourceUtils;
import kr.go.mobile.agent.v3.CommonBaseInitActivity;
import kr.go.mobile.agent.v3.NotInstalledRequiredPackagesException;
import kr.go.mobile.agent.v3.UninstallExpiredPackagesException;
import kr.go.mobile.common.v3.MobileEGovConstants;
import kr.go.mobile.mobp.iff.R;

/**
 * 모바일 전자정부에서 사용하는 보안 에이전트 (Security Agent) 의 Application 객체
 * 객체 생성시
 */
public class SAGTApplication extends Application implements Application.ActivityLifecycleCallbacks,
        CommonBaseInitActivity.LocalService, BrokerService.IServiceManager {

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
                    monitorManager.monitorNetwork(getApplicationContext());
                    break;
                }
                case MonitorService.RESULT_SECURE_NETWORK_EXPIRED: {
                    Log.d(TAG, "보안 네트워크 대기 시간을 초과하였습니다. 실행 중인 행정앱을 종료합니다.");
                    break;
                }
                case SessionService.RESULT_SIGNED_REGISTER_OK: {
                    sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_SIGNED_REGISTERED_OK);
                    break;
                }
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
            MonitorManager.start(SAGTApplication.this);
        }
    };

    private boolean forceStopActivity;
    private SessionManager sessionManager;
    private MonitorManager monitorManager;
    private Bundle tmpExtra;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.timeStamp("init");
        Log.ENABLE = true;
        Log.i(TAG, "보안 Agent 를 시작합니다.");

        bindServices(new Class[]{MonitorService.class, SessionService.class});
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

    public void setStopActivity() {
        forceStopActivity = true;
        sessionManager.finishLoginActivity();
    }

    @Override
    public void setExtraData(@NotNull Bundle extra) {
        if (monitorManager == null) {
            tmpExtra = extra;
        } else {
            monitorManager.monitorPackage(extra);
        }
    }

    @Override
    public void takeGenerateSigned(Context context, final CommonBaseInitActivity.TakeListener<UserSigned.STATUS> listener) {
        UserSigned signed = sessionManager.getUserSigned();
        signed.startLoginActivityForResult(context, new Solution.EventListener<UserSigned>() {
            @Override
            public void onFailure(Context context, String message, Throwable t) {
                Log.e(TAG, "인증서 로그인 모듈을 실행할 수 없습니다. (이유 : " + message + ")", t);
                listener.onTake(UserSigned.STATUS._ERROR);
                sessionManager.finishLoginActivity();
            }

            @Override
            public void onCompleted(Context context, Solution.Result<UserSigned> result) {
                switch (result.getCode()) {
                    case _OK:
                        Log.i(TAG, "GPKI 인증서 로그인 성공");
                        sessionManager.registerSigned(result.out);
                        listener.onTake(UserSigned.STATUS._OK);
                        break;
                    case _CANCEL:
                        listener.onTake(UserSigned.STATUS._USER_CANCEL);
                        break;
                    case _INVALID:
                    case _TIMEOUT:
                    case _FAIL:
                        Log.e(TAG, result.getErrorMessage());
                        throw new IllegalStateException("Unexpected value: " + result.getCode());
                }
                sessionManager.finishLoginActivity();
            }
        });
    }

    @Override
    public void validSigned() throws SessionManager.SessionException {
        sessionManager.validSignedSession();
    }

    @Override
    public void takeInstalledApps(final CommonBaseInitActivity.TakeListener<String[]> listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] ret = null;
                try {

                    checkPackages();
                    ret = new String[0];
                } catch (UninstallExpiredPackagesException e) {
                    ret = new String[] {
                            "REMOVE",
                            String.format("라이선스 만료로 %s 앱을 삭제해야 합니다. 삭제 후 다시 실행해주시기 바랍니다.", e.getPackageLabel())
                    };
                } catch (NotInstalledRequiredPackagesException e) {
                    ret = new String[] {
                            "INSTALL",
                            String.format("공통기반 서비스 지원을 위한 %s 앱을 설치해야 합니다. 설치 후 다시 실행해주시기 바립니다.", e.getPackageLabel())
                    };
                } catch (Exception e) {
                    ret = new String[] {
                            "ERROR",
                            "앱 검사를 진행할 수 없습니다. 종료 후 다시 실행해주시기 바랍니다."
                    };
                } finally {
                    if(forceStopActivity) return;

                    listener.onTake(ret);
                }
            }
        }, "check installed package").start();
    }

    @Override
    public void takeReadyLocalService(final CommonBaseInitActivity.TakeListener<Void> listener) {
        this.forceStopActivity = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if (forceStopActivity) break;
                    if (sessionManager == null || monitorManager == null) continue;
                    listener.onTake(null);
                    break;
                } while (true);
                Log.concurrency(Thread.currentThread(), "쓰레드 종료");
            }
        }, "wait for certification login status").start();
    }

    @Override
    public void takeVerifiedAgent(final CommonBaseInitActivity.TakeListener<IntegrityConfirm.STATUS> listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if (forceStopActivity) break;
                    if (monitorManager == null) continue;;

                    String confirm = monitorManager.getConfirm();
                    if (confirm.isEmpty()) continue;

                    listener.onTake(IntegrityConfirm.STATUS._VERIFIED);
                    break;
                }  while (true);
                Log.concurrency(Thread.currentThread(), "쓰레드 종료");
            }
        }, "wait for integrity status").start();
    }

    @Override
    public void takeThreatsEnv(final CommonBaseInitActivity.TakeListener<ThreatDetection.STATUS> listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if (forceStopActivity) break;
                    if (monitorManager == null) continue;

                    ThreatDetection.STATUS status = monitorManager.getThreatStatus();
                    if (status == null) continue;

                    listener.onTake(status);
                    break;
                } while (true);
                Log.concurrency(Thread.currentThread(), "쓰레드 종료");
            }
        }, "wait for safe device status").start();
    }

    @Override
    public void taskConnectedSecureNetwork(final CommonBaseInitActivity.TakeListener<SecureNetwork.STATUS> listener) {
        ////// SSL-VPN 솔루션에 종속적인 코드임. (SSL-VPN 에 로그인하기 위하여 약속된 ID 생성) //////////
        String hardwareID = HardwareUtils.getAndroidID(this);
        String signedUserDN = sessionManager.getUserDN();
        String confirmTokens = monitorManager.getConfirm();
        String loginId = String.format("%s,deviceID=%s|%s", signedUserDN, hardwareID, confirmTokens);
        String loginPw = "";
        ////// ///////////////////////////////////////////////////////////////////////// //////////
        MonitorManager.startSecureNetwork(this, loginId, loginPw);
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if (forceStopActivity) break;

                    SecureNetwork.STATUS status = monitorManager.getSecureNetworkStatus();
                    if (status == null) continue;

                    listener.onTake(status);
                    break;
                } while (true);
                Log.concurrency(Thread.currentThread(), "쓰레드 종료");
            }

        }, "wait for connection secure network").start();
    }

    @Override
    public void takeConfirmCertification(final CommonBaseInitActivity.TakeListener<String> listener) {
        Intent intent = new Intent(this, BrokerService.class);
        startService(intent);
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if(forceStopActivity) break;
                    try {
                        sessionManager.validSession();
                        listener.onTake("");
                        break;
                    } catch (SessionManager.SessionException e) {
                        if (e.getExpiredType() == SessionManager.SessionException.AUTH_NO_SESSION) {
                            continue;
                        } else {
                            // SessionManager.SessionException.AUTH_MISMATCH_USER_DN:
                            // SessionManager.SessionException.AUTH_FAILED:
                            listener.onTake(e.getMessage());
                            break;
                        }
                    }
                } while (true);
                Log.concurrency(Thread.currentThread(), "쓰레드 종료");
            }
        }, "wait for confirm certification").start();
    }

    @Override
    public void clearToken() {
        // TODO
    }

    @Override
    public boolean notReadySecureNetwork() {
        SecureNetwork.STATUS status = monitorManager.getSecureNetworkStatus();
        if (status == null || !status.equals(SecureNetwork.STATUS._CONNECTED)) {
            return true;
        }
        return false;
    }

    @Override
    public Bundle getResultBundle() {
        Bundle extra = new Bundle();
        extra.putString(MobileEGovConstants.EXTRA_KEY_USER_ID, sessionManager.getUserId());
        extra.putString(MobileEGovConstants.EXTRA_KEY_DN, sessionManager.getUserDN());
        return extra;
    }

    void checkPackages() throws NotInstalledRequiredPackagesException, UninstallExpiredPackagesException, Exception {
        // 라이선스 만료된 앱 체크
        validPackages("license_expired_package", false);
        // 존재해야 하는 앱 체크
        validPackages();
    }

    private void validPackages() throws NotInstalledRequiredPackagesException, UninstallExpiredPackagesException, Exception {
        validPackages("required_packages", true);
    }

    private void validPackages(String targetKeyword, boolean requiredPackages) throws NotInstalledRequiredPackagesException, UninstallExpiredPackagesException, Exception {
        PackageManager pm = this.getPackageManager();
        String appLabel = null;
        String packageName = null;
        try {
            String jsonPackageList = ResourceUtils.loadResourceRaw(this, R.raw.valid_packages);
            JSONObject jsonObj = (JSONObject) new JSONTokener(jsonPackageList).nextValue();
            JSONArray jsonArr = jsonObj.getJSONArray(targetKeyword);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject object = jsonArr.getJSONObject(i);
                packageName = object.getString("package");
                appLabel = object.getString("label");
                int thisApi = Build.VERSION.SDK_INT;
                int minApi, maxApi;
                try {
                    minApi = object.getInt("min.api");
                } catch (JSONException e) {
                    minApi = Build.VERSION.SDK_INT;
                }
                try {
                    maxApi = object.getInt("max.api");
                } catch (JSONException e) {
                    maxApi = Build.VERSION.SDK_INT;
                }

                if (minApi <= thisApi && thisApi <= maxApi) {
                    pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                    if (!requiredPackages) {
                        throw new UninstallExpiredPackagesException(appLabel, packageName);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (requiredPackages) {
                //  지정된 필수앱이 없을때 Exception 발생.
                throw new NotInstalledRequiredPackagesException(appLabel, packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "필수앱 목록을 읽을 수 없습니다. (message : " + e.getMessage() + ")", e);
            throw e;
        }
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

    @Deprecated
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
