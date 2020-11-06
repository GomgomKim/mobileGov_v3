package kr.go.mobile.agent.app;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import kr.go.mobile.agent.service.broker.BrokerService;
import kr.go.mobile.agent.service.monitor.ILocalMonitorService;
import kr.go.mobile.agent.service.monitor.IntegrityConfirm;
import kr.go.mobile.agent.service.monitor.SecureNetwork;
import kr.go.mobile.agent.service.monitor.ThreatDetection;
import kr.go.mobile.agent.service.session.ILocalSessionService;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.agent.utils.Aria;
import kr.go.mobile.agent.utils.HardwareUtils;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.PushMessageDBHelper;
import kr.go.mobile.agent.utils.ResourceUtils;
import kr.go.mobile.agent.v3.CommonBasedInitActivity;
import kr.go.mobile.agent.v3.NotInstalledRequiredPackagesException;
import kr.go.mobile.agent.v3.UninstallExpiredPackagesException;
import kr.go.mobile.agent.v3.solution.DKI_LocalPushSolution;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.mobp.iff.R;

/**
 * 모바일 전자정부에서 사용하는 보안 에이전트 (Security Agent) 의 Application 객체
 * 객체 생성시
 */
public class SAGTApplication extends Application implements CommonBasedInitActivity.LocalService,
        BrokerService.IServiceManager {

    private final String TAG = "MobileGov-Core"; //SAGTApplication.class.getSimpleName();
    private final String PREFIX_LOGFILE = "crash-log-%s.log";
    private final DateFormat SUFFIX_LOGFILE = new SimpleDateFormat("yyMMddHHmmss", Locale.KOREAN);

    private ServiceConnection localServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof ILocalSessionService) {
                Log.d(TAG, "(세션) 로컬 서비스에 연결되었습니다. - " + service.hashCode());
                SESSION = SessionManager.create(service);
            } else if (service instanceof ILocalMonitorService) {
                Log.d(TAG, "(모니터) 로컬 서비스에 연결되었습니다. - " + service.hashCode());
                MONITOR = MonitorManager.create(service);
            } else {
                throw new IllegalStateException("정의되지 않은 로컬서비스에 연결되었습니다.");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "로컬 서비스 연결이 해제되어 강제종료 합니다.");
            throw new RuntimeException("로컬 서비스 연결이 해제되어 강제종료 합니다.");
        }
    };

    private boolean forceStopLoadingActivity;
    private SessionManager SESSION;
    private MonitorManager MONITOR;

    String[] resultCheckPackages;
    private boolean printTimeStamp = true;
    private boolean exportLog = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.DEBUG = getResources().getBoolean(R.bool.config_log_debug);
        Log.TC = getResources().getBoolean(R.bool.config_log_tc);
        Log.TIMESTAMP = getResources().getBoolean(R.bool.config_log_timestamp);

        try {
            checkPackages();
            resultCheckPackages = new String[0];
            SessionManager.bindService(this, localServiceConnection);
            MonitorManager.bindService(this, localServiceConnection);

            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run(){
                    Log.v(TAG, "----- Application.onShutdownHook ----- ");
                }
            });

            registerActivityLifeCycle(false /*현재는 사용 안함*/);
            enableLocalPush();
            startCrashMonitor();
        } catch (UninstallExpiredPackagesException e) {
            resultCheckPackages = new String[] {
                    "REMOVE",
                    String.format("라이선스 만료로 %s 앱을 삭제해야 합니다. 삭제 후 다시 실행해주시기 바랍니다.", e.getPackageLabel())
            };
        } catch (NotInstalledRequiredPackagesException e) {
            resultCheckPackages = new String[] {
                    "INSTALL",
                    String.format("공통기반 서비스 지원을 위한 %s 앱을 설치해야 합니다. 설치 후 다시 실행해주시기 바립니다.", e.getPackageLabel())
            };
        } catch (Exception e) {
            resultCheckPackages = new String[] {
                    "ERROR",
                    "앱 검사를 진행할 수 없습니다. 종료 후 다시 실행해주시기 바랍니다."
            };
        }
    }

    void enableLocalPush() {
        try {
            Solution<String, Bundle> pushSolution = SolutionManager.initSolutionModule(getApplicationContext(), SolutionManager.LOCAL_PUSH);
            pushSolution.setDefaultEventListener(new Solution.EventListener<Bundle>() {
                @Override
                public void onCompleted(Context context, Solution.Result<Bundle> result) {
                    Bundle bundle = (Bundle) result.out;
                    String code = bundle.getString("RT", "/*정의되지 않음*/");
                    String msg = bundle.getString("RT_MSG", "/*알수없음*/");
                    if (code.equals("0000")) {
                        PushMessageDBHelper.newInstance(context);
                        // TEST CODE - BEGIN //
                        try {
                            JSONObject oo = new JSONObject();
                            oo.put("type", "1");
                            oo.put("msg", "공통기반 시스템 점검으로 인하여 서비스 제공이 불안정할 수 있습니다.");
                            Bundle b = new Bundle();
                            b.putString("requestid", "124536");
                            b.putString("alert", "공통기반 시스템 점검");
                            b.putString("message", oo.toString());
                            DKI_LocalPushSolution.PushMessage m = DKI_LocalPushSolution.PushMessage.create(b);
                            PushMessageDBHelper.insertNotice(m);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // TEST CODE - END //
                        Log.d(DKI_LocalPushSolution.TAG, "로컬 푸시 등록 성공");
                    } else {
                        Log.e(DKI_LocalPushSolution.TAG, "로컬 푸시 등록 실패 ("  + msg + ")");
                    }
                }

                @Override
                public void onFailure(Context context, String message, Throwable t) {
                    Log.e(DKI_LocalPushSolution.TAG, "");
                }
            });
            pushSolution.execute(getApplicationContext(), HardwareUtils.getAndroidID(this));
            ////////////
        } catch (SolutionManager.ModuleNotFoundException e) {
            throw new RuntimeException("로컬 푸시 솔루션을 연계할 수 없습니다.");
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e(TAG, "----- onTerminate ----- ");
    }

    public void setStopActivity() {
        forceStopLoadingActivity = true;
        SESSION.getUserSigned().stopLoginActivity();
    }

    @Override
    public void executeMonitor(@NotNull Bundle extra) {
        MONITOR.execute(extra);
    }

    @Override
    public void enabledMonitorPackage(String packageName, Bundle pkgInfo) {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
            String versionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = String.valueOf(packageInfo.getLongVersionCode());
            } else {
                versionCode = String.valueOf(packageInfo.versionCode);
            }
            pkgInfo.putString("app_id", packageInfo.packageName);
            pkgInfo.putString("app_version", packageInfo.versionName + "(" + versionCode + ")");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("실행중인 행정앱 정보를 획득할 수 없습니다.");
        }
        MONITOR.addMonitorPackage(pkgInfo);
    }


    @Override
    public void validSigned() throws SessionManager.SessionException {
        SESSION.validSignedSession();
    }

    @Override
    public String[] checkSecureSolution() {
        return resultCheckPackages;
    }

    @Override
    public void takeReadyLocalService(final CommonBasedInitActivity.TakeListener<Void> listener) {
        this.forceStopLoadingActivity = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if (forceStopLoadingActivity) break;
                    if (SESSION == null || MONITOR == null) continue;
                    listener.onTake(null);
                    break;
                } while (true);
            }
        }, "wait for certification login status").start();
    }

    @Override
    public void takeVerifiedAgent(final CommonBasedInitActivity.TakeListener<IntegrityConfirm.STATUS> listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if (forceStopLoadingActivity) break;
                    if (MONITOR == null) continue;;

                    if (MONITOR.getIntegrityStatus() == IntegrityConfirm.STATUS._UNKNOWN) continue;

                    listener.onTake(MONITOR.getIntegrityStatus());
                    break;
                }  while (true);
            }
        }, "wait for integrity status").start();
    }

    @Override
    public void takeThreatsEnv(final CommonBasedInitActivity.TakeListener<ThreatDetection.STATUS> listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if (forceStopLoadingActivity) break;
                    if (MONITOR == null) continue;

                    ThreatDetection.STATUS status = MONITOR.getThreatStatus();
                    if (status == null) continue;

                    listener.onTake(status);
                    break;
                } while (true);
            }
        }, "wait for safe device status").start();
    }

    @Override
    public void takeGenerateSigned(final CommonBasedInitActivity.TakeListener<UserSigned.STATUS> listener, Context context) {
        if(printTimeStamp) {
            Log.timeStamp("application-init");
            printTimeStamp = false;
        }

        UserSigned signed = SESSION.getUserSigned();
        signed.startLoginActivityForResult(context, new Solution.EventListener<UserSigned>() {
            @Override
            public void onFailure(Context context, String message, Throwable t) {
                Log.e(TAG, "인증서 로그인 모듈을 실행할 수 없습니다. (이유 : " + message + ")", t);
                listener.onTake(UserSigned.STATUS._ERROR);
            }

            @Override
            public void onCompleted(Context context, Solution.Result<UserSigned> result) {
                switch (result.getCode()) {
                    case _OK:
                        Log.d(TAG, "GPKI 인증서 로그인 성공");
                        SESSION.registerSigned(result.out);
                        result.out = null;
                        listener.onTake(UserSigned.STATUS._OK);
                        break;
                    case _CANCEL:
                        listener.onTake(UserSigned.STATUS._USER_CANCEL);
                        break;
                    default:
                        Log.e(TAG, result.getErrorMessage());
                        throw new IllegalStateException("인증서 로그인 요청시 예상되지 않는 값이 전달되었습니다. value = " + result.getCode());
                }
            }
        });
    }

    @Override
    public void taskConnectedSecureNetwork(final CommonBasedInitActivity.TakeListener<SecureNetwork.STATUS> listener) {
        ////// SSL-VPN 솔루션에 종속적인 코드임. (SSL-VPN 에 로그인하기 위하여 사전에 정의된 ID 생성) //////////
        String hardwareID = HardwareUtils.getAndroidID(this);
        String signedUserDN = SESSION.getUserDN();
        String confirmTokens = MONITOR.getConfirm();
        if (confirmTokens == null) {
            listener.onTake(SecureNetwork.STATUS._ERROR);
            return;
        }
        String loginId = String.format("%s,deviceID=%s|%s", signedUserDN, hardwareID, confirmTokens);
        String loginPw = "";
        ////// ///////////////////////////////////////////////////////////////////////// //////////
        MONITOR.startSecureNetwork(this, loginId, loginPw);
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if (forceStopLoadingActivity) break;

                    SecureNetwork.STATUS status = MONITOR.getSecureNetworkStatus();
                    // 보안 네트워크 연결 요청을 했기 때문에 연결과 오류 상태만 필요하다.
                    if (status == SecureNetwork.STATUS._UNKNOWN || status == SecureNetwork.STATUS._DISCONNECTED) continue;

                    listener.onTake(status);
                    break;
                } while (true);
            }

        }, "wait for connection secure network").start();
    }

    @Override
    public void takeConfirmCertification(final CommonBasedInitActivity.TakeListener<String> listener) {
        Intent intent = new Intent(this, BrokerService.class);
        startService(intent);
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if(forceStopLoadingActivity) break;
                    try {
                        SESSION.validSession();
                        listener.onTake("");
                        break;
                    } catch (SessionManager.SessionException e) {
                        if (e.getExpiredType() != SessionManager.SessionException.AUTH_NO_SESSION) {
                            // SessionManager.SessionException.AUTH_MISMATCH_USER_DN:
                            // SessionManager.SessionException.AUTH_FAILED:
                            listener.onTake(e.getMessage());
                            break;
                        }
                    }
                } while (true);
            }
        }, "wait for confirm certification").start();
    }

    @Override
    public String getErrorMessage(int type) {
        return MONITOR.getErrorMessage(type);
    }

    @Override
    public void clearToken() {
        if (MONITOR != null)
            MONITOR.clear();
    }

    @Override
    public void clearSession() {
        if (SESSION != null)
            SESSION.clear();
    }

    @Override
    public boolean notReadySecureNetwork() {
        SecureNetwork.STATUS status = MONITOR.getSecureNetworkStatus();
        return status == null || !status.equals(SecureNetwork.STATUS._CONNECTED);
    }

    @Override
    public Bundle getUserBundle() {
        Bundle extra = new Bundle();
        extra.putString(CommonBasedConstants.EXTRA_KEY_USER_ID, SESSION.getUserId());
        extra.putString(CommonBasedConstants.EXTRA_KEY_DN, SESSION.getUserDN());
        return extra;
    }

    @Override
    public void exportCrashLog() {
        if (exportLog) {
            return;
        }
        exportLog = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String regex = PREFIX_LOGFILE.replace("%s", "(\\d){12}");
                String[] logFileNames = getFilesDir().list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.matches(regex);
                    }
                });

                File externalPath = getExternalFilesDir("log");

                if (logFileNames == null || externalPath == null) return;

                if (externalPath.mkdirs()) {
                    Log.d(TAG, "export 경로 생성 완료");
                }

                for(String logFileName : logFileNames) {
                    String source = getFilesDir().getAbsolutePath() + File.separator + logFileName;
                    String dest = externalPath.getAbsolutePath() + File.separator + logFileName;
                    FileInputStream fis = null;
                    FileOutputStream fos = null;
                    FileChannel fic = null;
                    FileChannel foc = null;
                    try {
                        fis = new FileInputStream(source);
                        fos = new FileOutputStream(dest);
                        fic = fis.getChannel();
                        foc = fos.getChannel();
                        foc.transferFrom(fic, 0, fic.size());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (foc != null) {
                            try {
                                foc.close();
                            } catch (IOException ignored) {
                            }
                        }
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException ignored) {
                            }
                        }
                        if (fic != null) {
                            try {
                                fic.close();
                            } catch (IOException ignored) {
                            }
                        }
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                    new File(source).delete();
                }
            }
        }, "export to ExternalPath").start();
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
            cause = e.getMessage();
            try {
                if (e.getCause() != null) {
                    writeCrashLog(e.getCause());
                }
            } catch (Exception ex) {
                Log.d(TAG, "로그 생성 실패", ex);
            }
        } catch (Throwable e) {
            // UI 스레드에서 발생한 예기치 못한 예외사항
            Log.e(TAG, "UI 스레드에서 예기치 않은 오류가 발생하였습니다.", e.getCause());
            try {
                writeCrashLog(e);
            } catch (Exception ex) {
                Log.e(TAG, "로그 생성 실패", ex);
            }
            cause = e.getMessage();
        } finally {
            forceStopLoadingActivity = true;

            // 예기치 않은 에러 발생할 경우 종료!
            Log.e(TAG, "예기치 않은 오류로 앱을 종료합니다. " + cause);
            if (MONITOR.getSecureNetworkStatus() == SecureNetwork.STATUS._CONNECTED) {
                MONITOR.stopSecureNetwork();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            SecureNetwork.STATUS status = MONITOR.getSecureNetworkStatus();
                            if (status == SecureNetwork.STATUS._CONNECTED) continue;
                            // TODO 종료 코드 다시 !
                            if (getTopActivity() != null) {
                                // 살아있는 엑티비티 모두 종료 (단, foregroundActivity 는 제외
                                getTopActivity().moveTaskToBack(true);
                                getTopActivity().finishAffinity();

                                do {

                                } while (!getTopActivity().isDestroyed());
                            }
                            System.exit(0);
                            break;
                        } while (true);
                    }

                }, "wait for disconnection secure network").start();
            }

        }
    }

    private void writeCrashLog(Throwable t) throws Exception {
        String cert = null;
        if (!Log.DEBUG) {
            Signature certSignature;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                certSignature = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES).signingInfo.getApkContentsSigners()[0];
            } else {
                certSignature = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).signatures[0];
            }
            MessageDigest msgDigest = MessageDigest.getInstance("SHA1");
            msgDigest.update(certSignature.toByteArray());
            cert = Base64.encodeToString(msgDigest.digest(), Base64.DEFAULT);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("- Throwable Message: ").append(t.toString()).append("\n\n");
        String exportFileName = String.format(PREFIX_LOGFILE, SUFFIX_LOGFILE.format(new Date()));
        StackTraceElement[] arr = t.getStackTrace();
        sb.append("--------- Stack trace ---------\n\n");
        for (StackTraceElement stackTraceElement : arr) {
            String encrypt = stackTraceElement.toString();
            if (cert != null) {
                encrypt = new Aria(cert).encrypt(encrypt);
            }
            sb.append("\t").append(encrypt).append("\n");
        }
        sb.append("-------------------------------\n\n");
        String stringSubCause = getCauseMessage(t);
        sb.append(stringSubCause);

        FileOutputStream fout = null;
        try {
            fout = openFileOutput(exportFileName, Context.MODE_PRIVATE);
            fout.write(sb.toString().getBytes());
            fout.flush();
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

    @Override
    public SessionManager getSession() {
        return this.SESSION;
    }

    @Override
    public MonitorManager getMonitor() {
        return this.MONITOR;
    }

    ////////////// Activity Lifecycle Callback /////////////////
    // TODO 공통기반 초기화 화면에서 사용자가 홈키로 나갈 경우 초기화 처리를 강제 중지해야 함.
    final ConcurrentHashMap<Activity, Boolean> resumeActivities = new ConcurrentHashMap<>();
    final AtomicReference<Activity> topActivity = new AtomicReference<>();

    void registerActivityLifeCycle(boolean enabled) {
        if (!enabled) return;

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NotNull Activity activity, @Nullable Bundle savedInstanceState) {
                Log.d(TAG, "onActivityCreated - " + activity.toString());
            }

            @Override
            public void onActivityStarted(@NotNull Activity activity) {
                Log.d(TAG, "onActivityStarted - " + activity.toString());
            }

            @Override
            public void onActivityResumed(Activity activity) {
                setTopActivity(activity);
                Log.d(TAG, "onActivityResumed - " + activity.toString());
            }

            @Override
            public void onActivityPaused(@NotNull Activity activity) {
                changeStatus(activity);
                Enumeration<Activity> keys = resumeActivities.keys();
                int resumeCount = 0;
                while(keys.hasMoreElements()) {
                    Activity a = keys.nextElement();
                    if (a == null) continue;
                    boolean isResume = resumeActivities.get(a);

                    if (isResume) {
                        resumeCount++;
                    }
                    Log.d(TAG, a.toString() + " status(resume) = " + isResume);
                }
                if (resumeCount == 0) {
                    Log.e(TAG, "force finish");
                }
            }

            @Override
            public void onActivityStopped(@NotNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NotNull Activity activity, @NotNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NotNull Activity activity) {
                deleteTopActivity(activity);
            }
        });
    }

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



}
