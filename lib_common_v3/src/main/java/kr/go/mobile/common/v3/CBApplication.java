package kr.go.mobile.common.v3;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import kr.co.everspin.eversafe.EversafeHelper;
import kr.go.mobile.agent.service.broker.IBrokerService;
import kr.go.mobile.common.R;
import kr.go.mobile.common.v3.broker.BrokerManager;
import kr.go.mobile.common.v3.utils.PackageUtils;

public class CBApplication extends Application {

    private final String TAG = CBApplication.class.getSimpleName();
    private final String CRASH_TAG = "CrashMonitor";

    private final String PREFIX_LOGFILE = "-crash-log-%s.log";
    private final DateFormat SUFFIX_LOGFILE = new SimpleDateFormat("yyMMddHHmmss", Locale.KOREAN);

    // 에이전트의 서비스와 통신하기 위한 메신져
    private Messenger agentCMDHandler = new Messenger(new Handler(Looper.getMainLooper()) {
        // 보안에이전트의 모니터링 서비스로부터 명령(Command)를 수신하여 처리하기 위한 핸들러
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case CommonBasedConstants.CMD_FORCE_KILL_POLICY_VIOLATION: {
                    int codeCause = message.arg1;
                    Log.e(TAG, String.format("정책 위반으로 앱을 종료합니다. (사유 : %s)", codeCause));
                    finishThisPackage("보안 에이전트에 의한 강제 종료", String.valueOf(codeCause));
                    break;
                }
                case CommonBasedConstants.CMD_FORCE_KILL_DISABLED_SECURE_NETWORK: {
                    Log.e(TAG, "보안 네트워크 비활성화로 앱을 종료합니다.");
                    finishThisPackage("보안 에이전트에 의한 강제 종료", "보안 네트워크 비활성화로 앱을 종료합니다.");
                    break;
                }
            }
            super.handleMessage(message);
        }
    });

    final ServiceConnection agentServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                if (Objects.equals(service.getInterfaceDescriptor(), IBrokerService.class.getCanonicalName())) {
                    brokerManager = BrokerManager.create(service);
                } else  if (Objects.equals(service.getInterfaceDescriptor(), "android.os.IMessenger")) {
                    // 행정앱 --> 보안 Agent 로 Event 를 보내기 위함.
                    monitorManager = MonitorManager.create(service, Process.myUid(), agentCMDHandler);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "***** AGENT SERVICE CONNECTED (from %s) ***** ", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 보안 에이전트로의 서비스가 종료되어 연결이 끊어진 상태로 행정앱이 계속 실행될 필요가 없음.
            Log.e(TAG, "***** AGENT SERVICE DISCONNECTED ***** ");
            finishThisPackage(getString(R.string.iff_disconnection_agent),
                    getString(R.string.iff_guide_this_app_restart));
        }

        @Override
        public void onBindingDied(ComponentName name) {
            // 보안 에이전트의 서비스를 더이상 사용할 수 없음. 즉, 재연결이 필요함.
            Log.e(TAG, "***** AGENT SERVICE Binding Dead! ***** ");
            finishThisPackage(getString(R.string.iff_disconnection_agent),
                    getString(R.string.iff_guide_this_app_restart));
        }
    };

    private BrokerManager brokerManager;
    private MonitorManager monitorManager;
    private Activity foregroundActivity;
    private List<Activity> resumeActivity = Collections.synchronizedList(new ArrayList<Activity>());

    @Override
    public void onCreate() {
        super.onCreate();

        // 보안 에이전트 설치 및 권한 부여 확인
        boolean installedLauncher = PackageUtils.isInstalledPackage(this, getString(R.string.iff_launcher_pkg));
        boolean grantedPermission = (checkCallingOrSelfPermission("kr.go.mobile.permission.ACCESS_RELAY_SERVICE") == PackageManager.PERMISSION_GRANTED);
        String commonApiVersion = getString(R.string.common_based_version);
        final String regex = PREFIX_LOGFILE.replace("%s", "(\\d){12}");
        String[] stackTraces = getFilesDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(regex);
            }
        });
        int countStackTrace = stackTraces == null ? 0 : stackTraces.length;

        // 공통기반 초기화 및 버전 설정
        Log.d(TAG, "공통기반 정보");
        Log.i(TAG, String.format(" - 공통기반 버전 : %s", commonApiVersion));
        Log.i(TAG, String.format(" - 보안 에이전트 : %s", (installedLauncher ? "설치" : "미설치")));
        Log.i(TAG, String.format(" - 서비스 권한 부여 : %s",  (grantedPermission ? "획득" : "미획득")));
        Log.i(TAG, String.format(" - 비정상 종료 횟수 : %d", countStackTrace));
        CommonBasedAPI.initialize(this, getPackageName(), commonApiVersion, installedLauncher, grantedPermission, countStackTrace);

        // MSN 모듈 초기화
        Log.d(TAG, "보안 모듈 초기화");
        EversafeHelper.getInstance().setBackgroundMaintenanceSec(6000);

        // 행정앱의 Activity 라이프 사이클을 감지.
        Log.d(TAG, "행정앱 모니터링 시작");
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                sendEventMonitor(CommonBasedConstants.EVENT_ACTIVITY_STARTED, activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                sendEventMonitor(CommonBasedConstants.EVENT_ACTIVITY_RESUMED, activity);
                addActivity(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                sendEventMonitor(CommonBasedConstants.EVENT_ACTIVITY_PAUSED, activity);
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                sendEventMonitor(CommonBasedConstants.EVENT_ACTIVITY_STOPPED, activity);
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                sendEventMonitor(CommonBasedConstants.EVENT_ACTIVITY_DESTROYED, activity);
                removeActivity(activity);
            }
        });

        // 정상 종료(System.exit(x)) 호출시 처리 코드 설정
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                if (brokerManager != null || monitorManager != null) {
                    brokerManager = null;
                    monitorManager = null;
                    unbindService(agentServiceConnection);
                }
            }
        });

        // 행정앱의 예기치 않은 종료를 감지하여 로그를 생성.
        startCrashMonitor();
    }

    private void addActivity(Activity activity) {
        foregroundActivity = activity;
        resumeActivity.add(activity);
        Log.d("@@@", "--- add Activity : " + activity.getClass().getSimpleName());
    }

    private void removeActivity(Activity activity) {
        if (activity.equals(foregroundActivity)) {
            foregroundActivity = null;
        }
        resumeActivity.remove(activity);
        Log.d("@@@", "--- remove Activity : " + activity.getClass().getSimpleName());
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
            Log.e(CRASH_TAG, String.format("%s 스레드에서 예기치 않은 오류가 발생하였습니다. (PID : %d)",  e.threadName, e.pid), e.getCause());
            if (e.getCause() != null) {
                writeCrashLog(e.getCause());
                cause = e.getMessage();
            }
        } catch (Throwable e) {
            // UI 스레드에서 발생한 예기치 못한 예외사항
            Log.e(CRASH_TAG, "UI 스레드에서 예기치 않은 오류가 발생하였습니다.", e.getCause() == null ? e : e.getCause());
            writeCrashLog(e);
            cause = e.getMessage();
        } finally {
            if (cause != null) {
                Log.d(CRASH_TAG, String.format("앱 종료 이벤트를 보안 에이전트로 전송합니다. (cause = %s)", cause));
//                try {
//                    CommonBaseAPI.getInstance().sendEvent(MobileEGovConstants.EVENT_COMMAND_HANDLER_UNREGISTERED, cause);
//                } catch (CommonBaseAPI.CommonBaseAPIException e) {
//                    e.printStackTrace();
//                }
            }
            // 예기치 않은 에러 발생할 경우 종료!
            Log.e(CRASH_TAG, "예기치 않은 오류로 앱을 종료합니다.");
            finishThisPackage(getString(R.string.iff_uncaught_exception), getString(R.string.iff_uncaught_exception_message));
        }
    }

    private void writeCrashLog(@NonNull Throwable t)  {
        CommonBasedAPI baseAPI = CommonBasedAPI.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append(baseAPI.getBasicInfo());
        sb.append("- Throwable Message: ").append(t.toString()).append("\n\n");

        String exportFileName = String.format(PREFIX_LOGFILE, SUFFIX_LOGFILE.format(new Date()));
        StackTraceElement[] arr = t.getStackTrace();
        sb.append("--------- Stack trace ---------\n\n");
        for (StackTraceElement stackTraceElement : arr) {
            sb.append("\t").append(stackTraceElement.toString()).append("\n");
        }
        sb.append("-------------------------------\n\n");
        String stringSubCause = getCause(t);
        sb.append(stringSubCause);

        FileOutputStream fout = null;
        try {
            fout = openFileOutput(exportFileName, Context.MODE_PRIVATE);
            fout.write(sb.toString().getBytes());
            fout.flush();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "ERROR Write CrashLog - File not create", e);
        } catch (IOException e) {
            Log.d(TAG, "ERROR Write CrashLog - File IO", e);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    Log.e(CRASH_TAG, "IO error : ", e);
                }
            }
        }
    }

    private String getCause(Throwable t) {
        StringBuilder sb = new StringBuilder();

        Throwable cause = t.getCause();
        if(cause != null) {
            sb.append("--------- Cause ---------\n\n");
            sb.append(cause.toString()).append("\n\n");
            StackTraceElement[] arr = cause.getStackTrace();
            for (StackTraceElement stackTraceElement : arr) {
                sb.append("\t").append(stackTraceElement.toString()).append("\n");
            }
            String strCause = getCause(cause);
            sb.append(strCause);
            sb.append("-------------------------------\n\n");
        }

        return sb.toString();
    }

    protected BrokerManager getBroker() {
        return getBroker(true);
    }

    protected BrokerManager getBroker(boolean checkedNull) {
        if (this.brokerManager == null) {
            if (checkedNull)
                throw new IllegalStateException("브로커 매니저가 존재하지 않습니다.");
            return null;
        } else if (this.brokerManager.isAlive()) {
            return this.brokerManager;
        } else {
            BrokerManager.bindService(this, agentServiceConnection);
            return null;
        }
    }

    void bindBrokerService() {
//        int flag = Context.BIND_NOT_FOREGROUND // 포어그라운드 프로세스가 아님.. 즉, 백그라운드 프로세스로 구동되면 메모리 자원이 부족할 때. 강제 종료 대상이 됨.
//                | Context.BIND_AUTO_CREATE // 서비스가 시작되지 않았다면, 서비스를 시작.. (onStartCommand() 를 호출하는 것은 아님), 시스템에 의해 강제 종료된 후 리소스가 충분해지면 다시 Bind 함.
//                | Context.BIND_ADJUST_WITH_ACTIVITY // 런타임시 바인딩된 엑티비티가 포어그라운드에 있지 않아도 서비스의 우선 순위가 높아짐.
//                | Context.BIND_ABOVE_CLIENT // 바인딩된 클라이언트의 중요함. 즉, 메모리 부족시 플랫폼은 바인된 서비스보다 앱을 먼저 종료한다. (100% 보장할수는 없음)
//                | Context.BIND_ALLOW_OOM_MANAGEMENT // 메모리 관리자가 관리하여 시스템에 메모리가 부족한것과 같은 상황에서 일시적으로 중지하고 강제종료 및 재시작을 할 수 있다.
//                | Context.BIND_DEBUG_UNBIND // 비정상적인 연결이 끊어지면 로그를 남김 ?
//                | Context.BIND_EXTERNAL_SERVICE // isolated external 서비스로 선언
//                | Context.BIND_IMPORTANT // 중요한 서비스로 포어그라운드 서비스로 함.
//                | Context.BIND_INCLUDE_CAPABILITIES // 포어그라운드 상태로 특정 기능이 있는 앱에서 바인딩하는 경우  동일한 권한을 허용한다.
//                | Context.BIND_NOT_PERCEPTIBLE // 시스템에서 일시적으로 메모리에서 정리
//                | Context.BIND_WAIVE_PRIORITY; // 메모리 관리 우선 순위에 영향을 미치지 않음.
        if (brokerManager == null) {
            BrokerManager.bindService(this, agentServiceConnection);
        }
    }

    void bindMonitorService() {
        if (monitorManager == null)
            MonitorManager.bindService(this, agentServiceConnection);
    }

    private void finishThisPackage(String title, String message) {
        finishThisPackage(foregroundActivity, title, message);
    }

    void finishThisPackage(final Activity foregroundActivity, String title, String message) {
        if (foregroundActivity != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(foregroundActivity);
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setCancelable(false);
            builder.setNeutralButton(foregroundActivity.getString(R.string.iff_button_close), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, final int which) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // 살아있는 엑티비티 모두 종료 (단, foregroundActivity 는 제외
                            foregroundActivity.moveTaskToBack(true);
                            foregroundActivity.finishAffinity();

                            do {
                                // 모든 Activity 가 종료될때 까지 대기
                            } while (resumeActivity.size() != 0);
                            System.exit(0);
                        }
                    }).start();
                }
            });
            builder.show();
        } else {
            for (Activity activity : resumeActivity) {
                activity.finish();
            }
            do {
                // 모든 Activity 가 종료될때 까지 대기
            } while (resumeActivity.size() != 0);
            System.exit(0);
        }
    }

    private void sendEventMonitor(int event, Activity activity) {
        String activityName = activity.getClass().getCanonicalName();
        // TODO
//        try {
//            Log.d(TAG, "");
//            CommonBaseAPI.getInstance().sendEvent(event, activityName);
//        } catch (CommonBaseAPI.CommonBaseAPIException e) {
//            Log.e(TAG, "", e);
//        }
    }
}
