package kr.go.mobile.agent.v3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import kr.go.mobile.agent.app.SessionManager;
import kr.go.mobile.agent.service.monitor.IntegrityConfirm;
import kr.go.mobile.agent.service.monitor.SecureNetwork;
import kr.go.mobile.agent.service.monitor.ThreatDetection;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.common.v3.MobileEGovConstants;

import kr.go.mobile.mobp.iff.R;

/**
 * 행정앱이 공통기반 라이브러리를 통해서 공통기반 서비스를 초기화할때 호출되는 Activity 로, 다음과 같은 기능을 수행한다.<br>
 * <ul>
 * <li>필수앱 설치 여부 확인</li>
 * <li>위변조 모듈 초기화 및 실행</li>
 * <li>백신 모듈 실행</li>
 * <li>인증서 모듈 실행 및 인증서 로그인 시도</li>
 * <li>VPN 연결 시도</li>
 * </ul>
 *
 */
public class CommonBaseInitActivity extends AppCompatActivity {

    public static final String ACTION_EVENT = "go.kr.mobile.agent.action.ACTION_EVENT";
    public static final String ACTION_EXTRA_TYPE = "receiver_extra_event";
    public static final String ACTION_EXTRA_MESSAGE = "receiver_extra_message";
    public static final int EVENT_TYPE_SIGNED_REGISTERED_OK = 3;
    public static final int EVENT_TYPE_AUTH_REGISTERED_OK = 5;
    public static final int EVENT_TYPE_USER_CANCELED = 0;
    public static final int EVENT_TYPE_SOLUTION_ERROR = 2;
    public static final int EVENT_TYPE_SIGNED_REGISTERED_ERROR = 4;
    public static final int EVENT_TYPE_AUTH_REGISTERED_ERROR = 6;

    private static final String TAG = CommonBaseInitActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final int STATUS_NONE = 8 >> 4; // 0000

    private static final int STATUS_INSTALLED_SOLUTION_APP = 8 >> 3; // 0001
    private static final int STATUS_SAFE_DEVICE = 8 >> 2; // 0010
    private static final int STATUS_INTEGRITY_APP = 8 >> 1; // 0100
    private static final int STATUS_READY_SERVICE = /*8 >> 0*/ 8 ; // 1000
    private static final int STATUS_READY = STATUS_INSTALLED_SOLUTION_APP | STATUS_SAFE_DEVICE | STATUS_INTEGRITY_APP | STATUS_READY_SERVICE; // 1111

    public interface LocalService {
        void setExtraData(Bundle extra);
        void clearToken();
        void setStopActivity();
        boolean notReadySecureNetwork();
        Bundle getResultBundle();
        void validSigned() throws SessionManager.SessionException;

        void takeReadyLocalService(TakeListener<Void> listener);
        void takeInstalledApps(TakeListener<String[]> listener);
        void takeVerifiedAgent(TakeListener<IntegrityConfirm.STATUS> listener);
        void takeThreatsEnv(TakeListener<ThreatDetection.STATUS> listener);
        void takeGenerateSigned(Context context, TakeListener<UserSigned.STATUS> listener);

        void taskConnectedSecureNetwork(TakeListener<SecureNetwork.STATUS> listener);
        void takeConfirmCertification(TakeListener<String> listener);
    }

    public interface TakeListener<T> {
        void onTake(T takeObject);
    }

    boolean aboveAPIVer3;
    boolean showFinishDialog;

    LocalService localService;
    ProgressDialog dialogProgress;
    AtomicInteger status = new AtomicInteger(STATUS_NONE);

    // Application 에 등록된 Service 로부터 수신된 Event 를 Activity 로 전달한다.
    final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ACTION_EVENT)) {
                final int event = intent.getIntExtra(ACTION_EXTRA_TYPE, -1);
                switch (event) {
                    case EVENT_TYPE_SIGNED_REGISTERED_OK: {
                        Log.i(TAG, "서명 세션 생성 - 정상");
                        doNextStep(-1);
                        break;
                    }
                    case EVENT_TYPE_AUTH_REGISTERED_OK: {
                        Log.i(TAG, "인증 세션 생성 - 정상");
                        Bundle extra = localService.getResultBundle();
                        // FIXME 공통기반 라이브러리 2.x 지원. 2.x 버전을 지원하지 않을 경우 삭제. /////
                        if (!aboveAPIVer3) {
                            extra.putString("userId", extra.getString(MobileEGovConstants.EXTRA_KEY_USER_ID));
                            extra.putString("dn", extra.getString(MobileEGovConstants.EXTRA_KEY_DN));
                        }
                        ////////////////////////////////////////////////////////////////////////
                        finishResult(MobileEGovConstants.RESULT_OK, extra);
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unexpected value: " + event);
                }
              
            } else {
                Log.w(TAG, String.format("정의되지 않은 action 입니다. (action = %s)", action));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.timeStamp("second init");
        setContentView(R.layout.activity_mobile_gov_loading);

        final Bundle extraData = getIntent().getExtras();
        aboveAPIVer3 = Objects.equals(getIntent().getAction(), MobileEGovConstants.ACTION_LAUNCH_SECURITY_AGENT);
        try {
            // 행정앱에서 공통기반 서비스 초기화 요청시 에러 다이얼로그를 비활성화 하기 위해서는 flagShowDialog 갓이 false 값을 가져야 한다.
            showFinishDialog = extraData.getBoolean("extra_show_dialog", !aboveAPIVer3 /*FIXME API 3.x 이상일 경우 기본적으로 보안에이전트에서 다이얼로그를 생성하지 않는다.*/);
            String apiVersion = validAdminPackageInfo(extraData);
            displayVersion(apiVersion);
        } catch (NullPointerException e) {
            String message = e.getMessage();
            Log.e(TAG, "필수정보 누락 : " + message);
            showFinishDialog(MobileEGovConstants.RESULT_AGENT_INVALID, "접근 에러", message);
            return;
        }

        // Agent 가 사용하는 퍼미션 확인 / 퍼미션이 없을 경우 요청시도
        if (grantedPermission(true)) {
            localService = (LocalService) getApplication();
            localService.takeReadyLocalService(new TakeListener<Void>() {
                public void onTake(Void takeObject) {
                    Log.i(TAG, "로컬 서비스 준비 상태 - 정상");
                    localService.setExtraData(extraData);
                    doNextStep(STATUS_READY_SERVICE);
                }
            });
            checkStatus();
        }
        // else { 권한이 없을 경우 권한 요청을 하였기때문에 요청처리 이후 onRequestPermissionsResult() 에서 처리함. }
    }

    void displayVersion(String apiVersion) {
        // 버전 표시
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "test";
        }

        TextView versionName = findViewById(R.id.version_name);
        versionName.setText(String.format("Ver.%s / API %s", version, apiVersion));
    }

    boolean grantedPermission(boolean requestDeniedPermission) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            // M 미안일 경우 체크할 필요 없음
            return true;
        }
        String[] requiredPermissions = checkPermission(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE});
        if (requiredPermissions == null) {
            return true;
        } else if (requestDeniedPermission) {
            requestPermissions(requiredPermissions, PERMISSION_REQUEST_CODE);
            // --> onRequestPermissionsResult()
        }
        return false;
    }

    void checkStatus() {
        // 보안 검사 중 다이얼로그 보이기.
        showProgressDialog(getString(R.string.security_msg));

        // BroadcastReceiver 보다 Thread wait 가 응답 속도가 빠름.
//        IntentFilter filter = new IntentFilter(ACTION_EVENT);
//        LocalBroadcastManager.getInstance(this).registerReceiver(this.eventReceiver, filter);

        // 필수앱(보안 솔루션 앱) 설치 유무 체크
        localService.takeInstalledApps(new TakeListener<String[]>() {
            @Override
            public void onTake(String[] takeObject) {
                if(takeObject.length == 0) {
                    Log.i(TAG, "필수 앱 체크 상태 - 정상");
                    doNextStep(STATUS_INSTALLED_SOLUTION_APP);
                } else {
                    int result = -1;
                    String realMessage = takeObject[1];
                    switch (takeObject[0]) {
                        case "REMOVE":
                            result = MobileEGovConstants.RESULT_AGENT_EXIST_LICENSE_EXPIRED_PACKAGE;
                            break;
                        case "INSTALL":
                            result = MobileEGovConstants.RESULT_AGENT_INSTALL_REQUIRED_PACKAGE;
                            break;
                        case "ERROR":
                            result = MobileEGovConstants.RESULT_AGENT_INTERNAL_ERROR;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + takeObject[0]);
                    }
                    showFinishDialog(result, "필수앱 검사", realMessage);
                }
            }
        });



        // 무결성 검증 확인
        localService.takeVerifiedAgent(new TakeListener<IntegrityConfirm.STATUS>() {
            @Override
            public void onTake(IntegrityConfirm.STATUS takeObject) {
                switch (takeObject) {
                    case _VERIFIED:
                        Log.i(TAG, "보안 에이전트 무결정 검증 상태 - 정상");
                        doNextStep(STATUS_INTEGRITY_APP);
                        break;
                    case _NOT_VERIFIED:
                    case _ERROR:
                        showFinishDialog(MobileEGovConstants.RESULT_AGENT_UNSAFE_DEVICE,
                                "무결성 검증 솔루션",
                                "보안 에이전트 무결설 검증에 실패하였습니다.");
                }
            }
        });

        // 악성 코드 검사 결과 대기
        localService.takeThreatsEnv(new TakeListener<ThreatDetection.STATUS>() {
            @Override
            public void onTake(ThreatDetection.STATUS takeObject) {
                String threatMessage;
                switch (takeObject) {
                    case _SAFE:
                        threatMessage = "";
                        break;
                    case _DISABLED_REAL_TIME_SCAN:
                        threatMessage = "보안을 위하여 실시간 검사가 활성화되어 있어야 합니다. V-Guard 환경설정에서 실시간 검사를 활성화해주시기 바랍니다.";
                        break;
                    case _EXIST_MALWARE:
                        threatMessage = "단말에 악성코드(앱)이 존재합니다. 치료 후 다시 실행해주시기 바랍니다.";
                        break;
                    case _ROOTING_DEVICE:
                        threatMessage = "루팅된 단말입니다. 보안을 위하여 루팅 단말에서 실행할 수 없습니다.";
                        break;
                    case _ERROR:
                        threatMessage = "단말 보안 솔루션 연동 중 에러가 발생하였습니다.";
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + takeObject);
                }
                if (threatMessage.isEmpty()) {
                    Log.i(TAG, "단말 보안 상태 - 정상");
                    doNextStep(STATUS_SAFE_DEVICE);
                } else {
                    showFinishDialog(MobileEGovConstants.RESULT_AGENT_UNSAFE_DEVICE,
                            "악성코드 탑지 솔루션",
                            threatMessage);
                }
            }
        });
    }

    @NotNull
    private String validAdminPackageInfo(Bundle extraData) throws NullPointerException {
        String tmp;
        if(extraData == null) {
            throw new NullPointerException("행정앱 정보가 존재하지 않습니다.");
        }
        tmp = extraData.getString("extra_token") ;
        if (tmp == null || tmp.equals("")) {
            throw new NullPointerException("행정앱의 보안 토큰값이 존재하지 않습니다.");
        }
        if (aboveAPIVer3) {
            tmp = extraData.getString("req_id_base64") ;
            if (tmp == null || tmp.equals("")) {
                throw new NullPointerException("행정앱 고유정보가 존재하지 않습니다.");
            }
            tmp = extraData.getString("api_version") ;
            if (tmp == null || tmp.equals("")) {
                throw new NullPointerException("공통기반 라이브러리 버전값이 존재하지 않습니다.");
            }
            extraData.getInt("stack_trace_count", 0) ;
        } else {
            tmp = extraData.getString("extra_package");
            if (tmp == null || tmp.equals("")) {
                throw new NullPointerException("행정앱 고유정보가 존재하지 않습니다.");
            }
            tmp = "2.x.x";
        }

        return tmp;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SolutionManager.onActivityResult(this, requestCode, resultCode, data);
    }

    private String[] checkPermission(String[] requiredPermissions) {
        Set<String> set = new HashSet<>();
        for (String reqPermission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, reqPermission) == PackageManager.PERMISSION_DENIED) {
                set.add(reqPermission);
            }
        }
        return set.isEmpty() ? null : set.toArray(new String[]{});
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantedPermission(false)) {
                checkStatus();
            } else {
                showFinishDialog(MobileEGovConstants.RESULT_AGENT_INVALID,
                        getString(R.string.denied_permission_title),
                        getString(R.string.denied_permission_details));
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }

    void doNextStep() {
        doNextStep(-1);
    }

    void doNextStep(int newStatus) {
        Log.calling();

        if (newStatus > -1) {
            status.addAndGet(newStatus);
        }

        if (status.compareAndSet(STATUS_READY, STATUS_READY)) {

            if (newStatus != -1) {
                Log.timeStamp("init");
                Log.timeStamp("second init");
            }

            hideProgressDialog();

            // VPN 이 연결되어 있는지 확인 ?
            // --> 안되어 있다면, 서명값이 존재하는가 ?
            // -----> 존재하면 VPN 연결 요청
            // -----> 존재하지 않으면 인증서 로그인 요청
            // --> 연결되어 있다면, 인증서 검증 요청
            // -----> 인증서버 인증 결과 처리
            // ---------> 성공이면, 서명값과 일치하는지 확인
            // -------------> 일치하면, 초기화 성공
            // -------------> 불일치하면, 인증서 로그인 요청
            // ---------> 실패하면, 인증 실패처리 및 종료

            if (localService.notReadySecureNetwork()) {
                Log.i(TAG, "보안 네트워크가 연결되어 있지 않습니다.");

                try {
                    localService.validSigned();
                    
                    Log.i(TAG, "보안 네트워크가 연결을 시도합니다.");
                    Log.timeStamp("startVpn");
                    // 서명값이 존재하므로 보안 네트워크 연결 요청
                    showProgressDialog(getString(R.string.loading_secure_network_msg));
                    // 모니터 서비스를 이용하여 보안 네트워크 연결 요청
                    localService.taskConnectedSecureNetwork(new TakeListener<SecureNetwork.STATUS>() {
                        @Override
                        public void onTake(SecureNetwork.STATUS takeObject) {
                            switch (takeObject) {
                                case _CONNECTED:
                                    Log.timeStamp("startVpn");
                                    Log.i(TAG, "보안 네트워크 연결 성공");
                                    doNextStep();
                                    Log.timeStamp("startCert");
                                    break;
                                case _CONNECTING:
                                case _DISCONNECTED:
                                    Log.e(TAG, "보안 네트워크 연결 실패");
                                    break;
                                default:
                                    throw new IllegalStateException("Unexpected value: " + takeObject);
                            }
                        }
                    });
                } catch (SessionManager.SessionException e) {
                    Log.i(TAG, "서명 값이 존재하지 않거나 만료되었습니다. 인증서 로그인이 필요합니다.");
                    // 서명값이 존재하지 않으므로 인증서 로그인 솔루션 연계
                    localService.takeGenerateSigned(this, new TakeListener<UserSigned.STATUS>() {
                        @Override
                        public void onTake(UserSigned.STATUS takeObject) {
                            switch (takeObject) {
                                case _OK:
                                    doNextStep();
                                    break;
                                case _USER_CANCEL:
                                    showFinishDialog(MobileEGovConstants.RESULT_AGENT_INVALID,
                                            "인증서 로그인 취소",
                                            "사용자에 의하여 인증서 로그인이 취소되었습니다.");
                                    break;
                                case _ERROR:
                                    showFinishDialog(MobileEGovConstants.RESULT_AGENT_INVALID,
                                            "인증서 로그인 에러",
                                            "인증서 로그인 솔루션 연계 중 에러가 발생하였습니다.");
                                    break;
                            }

                        }
                    });
                }
            } else {
                showProgressDialog(getString(R.string.user_authentication_msg));
                localService.takeConfirmCertification(new TakeListener<String>() {
                    @Override
                    public void onTake(String takeObject) {
                        hideProgressDialog();
                        if (takeObject.isEmpty()) {
                            Log.i(TAG, "인증 세션 생성 - 정상");
                            Bundle extra = localService.getResultBundle();
                            // FIXME 공통기반 라이브러리 2.x 지원. 2.x 버전을 지원하지 않을 경우 삭제. /////
                            if (!aboveAPIVer3) {
                                extra.putString("userId", extra.getString(MobileEGovConstants.EXTRA_KEY_USER_ID));
                                extra.putString("dn", extra.getString(MobileEGovConstants.EXTRA_KEY_DN));
                            }
                            ////////////////////////////////////////////////////////////////////////
                            Log.timeStamp("startCert");
                            finishResult(MobileEGovConstants.RESULT_OK, extra);
                        } else {
                            showFinishDialog(MobileEGovConstants.RESULT_AGENT_FAILURE_USER_AUTHENTICATION,
                                    "사용자 인증 실패",
                                    takeObject);
                        }
                    }
                });
            }
        }
    }

    @Deprecated
    void showProgressDialog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialogProgress = ProgressDialog.show(CommonBaseInitActivity.this, "", message, true);
                dialogProgress.show();
            }
        });

    }

    @Deprecated
    void hideProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialogProgress != null) {
                    dialogProgress.dismiss();
                    dialogProgress = null;
                }
            }
        });
    }

    public void showFinishDialog(final int result_code, final String title, final String message) {

        if (showFinishDialog) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(CommonBaseInitActivity.this)
                            .setTitle(title)
                            .setMessage(message)
                            .setCancelable(false)
                            .setNeutralButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finishResult(result_code, null);
                                }
                            }).show();
                }
            });
        } else {
            finishResult(result_code, null);
        }
    }

    void finishResult(int result_code, Bundle extra) {
        hideProgressDialog();

        Intent result = null;
        if (extra != null) {
            result = new Intent();
            result.putExtras(extra);
        }
        setResult(result_code, result);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 앱 실행시 볼륨키 다운 이벤트를 입력하고 있다면.
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // TODO 다운키가 입력된 상태라면 로그를 sdcard 로 복사시킨다.
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        hideProgressDialog();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(eventReceiver);
        localService.setStopActivity();
        localService.clearToken();

        super.onDestroy();
    }
}
