package kr.go.mobile.agent.v3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import kr.go.mobile.agent.utils.PushMessageDBHelper;
import kr.go.mobile.common.v3.CommonBasedConstants;

import kr.go.mobile.mobp.iff.R;
import kr.go.mobile.support.v2.Utils;

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
public class CommonBasedInitActivity extends AppCompatActivity {

    private static final String TAG = "MobileGov";//CommonBaseInitActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final int STATUS_NONE = 8 >> 4; // 0000
    @Deprecated
    private static final int STATUS_INSTALLED_SOLUTION_APP = 8 >> 3; // 0001
    private static final int STATUS_SAFE_DEVICE = 8 >> 2; // 0010
    private static final int STATUS_INTEGRITY_APP = 8 >> 1; // 0100
    private static final int STATUS_ENABLED_SERVICE = /*8 >> 0*/ 8 ; // 1000
    private static final int STATUS_READY = STATUS_SAFE_DEVICE | STATUS_INTEGRITY_APP | STATUS_ENABLED_SERVICE; // 1110

    public interface LocalService {
        // 행정앱으로부터 전달받은 정보를 설정한다.
        String[] checkSecureSolution();
        void executeMonitor(Bundle extra);
        void clearToken();
        void setStopActivity();
        boolean notReadySecureNetwork();
        Bundle getUserBundle();
        void validSigned() throws SessionManager.SessionException;

        void takeReadyLocalService(TakeListener<Void> listener);
        void takeVerifiedAgent(TakeListener<IntegrityConfirm.STATUS> listener);
        void takeThreatsEnv(TakeListener<ThreatDetection.STATUS> listener);
        void takeGenerateSigned(TakeListener<UserSigned.STATUS> listener, Context context);
        void taskConnectedSecureNetwork(TakeListener<SecureNetwork.STATUS> listener);
        void takeConfirmCertification(TakeListener<String> listener);

        String getErrorMessage(int type);
        void enabledMonitorPackage(String callerPackageName, Bundle extraData);

        void clearSession();

        void exportCrashLog();
    }

    public interface TakeListener<T> {
        void onTake(T takeObject);
    }

    LocalService localService;
    ProgressDialog dialogProgress;
    AtomicInteger status = new AtomicInteger(STATUS_NONE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.timeStamp("activity-init");
        setContentView(R.layout.activity_mobile_gov_loading);

        try {
            validAdminPackageInfo();
            String apiVersion = getCommonBasedAPI();
            displayVersion(apiVersion);
        } catch (NullPointerException e) {
            String message = e.getMessage();
            Log.e(TAG, "필수정보 누락 : " + message);
            showFinishDialog(CommonBasedConstants.RESULT_AGENT_INVALID, "접근 에러", message);
            return;
        }
        Log.TC("필수 정보 유효성 체크 - 정상");

        localService = (LocalService) getApplication();

        // 필수앱(보안 솔루션 앱) 설치 유무 체크
        String[] ret = localService.checkSecureSolution();

        if(ret.length == 0) {
            Log.TC("필수 앱 체크 - 정상");
            //doNextStep(STATUS_INSTALLED_SOLUTION_APP);
            localService.takeReadyLocalService(new TakeListener<Void>() {
                public void onTake(Void v) {
                    Log.TC("서비스 바인딩 - 정상");
                    localService.executeMonitor(getIntent().getExtras());
                    doNextStep(STATUS_ENABLED_SERVICE);
                }
            });

            // Agent 가 사용하는 퍼미션 확인 / 퍼미션이 없을 경우 요청시도
            if (grantedPermission(true)) {
                checkStatus();
            }
            // else { 권한이 없을 경우 권한 요청을 하였기때문에 요청처리 이후 onRequestPermissionsResult() 에서 처리함. }
        } else {
            int result;
            String realMessage = ret[1];
            Log.TC(realMessage);
            switch (ret[0]) {
                case "REMOVE":
                    Log.TC("라이센스 만료 솔루션 앱 삭제 필요");
                    result = CommonBasedConstants.RESULT_AGENT_EXIST_LICENSE_EXPIRED_PACKAGE;
                    break;
                case "INSTALL":
                    Log.TC("공통기반 서비스 지원 솔루션 앱 설치 필요");
                    result = CommonBasedConstants.RESULT_AGENT_INSTALL_REQUIRED_PACKAGE;
                    break;
                case "ERROR":
                    result = CommonBasedConstants.RESULT_AGENT_INTERNAL_ERROR;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + ret[0]);
            }
            showFinishDialog(result, "필수앱 검사 에러", realMessage, true);
        }
    }


    private boolean isAboveAPIVer3() {
        return Objects.equals(getIntent().getAction(), CommonBasedConstants.ACTION_LAUNCH_SECURITY_AGENT);
    }

    private String getCommonBasedAPI() {
        String ret = getIntent().getStringExtra("api_version");
        return ret == null ? "2.x.x" : ret;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showFinishDialog(CommonBasedConstants.RESULT_AGENT_INVALID,
                "보안 에이전트 종료",
                intent.getStringExtra("message"), true);
    }

    void displayVersion(String apiVersion) {
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "test";
        }

        TextView versionName = findViewById(R.id.version_name);
        String displayVersion = String.format("Ver.%s / API %s", version, apiVersion);
        versionName.setText(displayVersion);
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
        checkStatus(true);
    }

    void checkStatus(boolean flag) {
        if (flag /* 공지 메시지 확인 */) {
            List<String> newNotice;
            if ((newNotice = PushMessageDBHelper.unreadNotice()).size() > 0) {
                showExistNoticeDialog(newNotice);
                return;
            }
        }
        // 보안 검사 중 다이얼로그 보이기.
        showProgressDialog(getString(R.string.security_msg));

        // 무결성 검증 확인
        localService.takeVerifiedAgent(new TakeListener<IntegrityConfirm.STATUS>() {
            @Override
            public void onTake(IntegrityConfirm.STATUS takeObject) {
                switch (takeObject) {
                    case _VERIFIED:
                        Log.TC("보안 에이전트 무결성 검증 - 정상");
                        doNextStep(STATUS_INTEGRITY_APP);
                        break;
                    case _NOT_VERIFIED:
                    default:
                        showFinishDialog(CommonBasedConstants.RESULT_AGENT_UNSAFE_DEVICE,
                                "무결성 검증 에러",
                                "보안 에이전트 무결성 검증에 실패하였습니다. (사유 : " + localService.getErrorMessage(1) + ")");
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
                        threatMessage = "단말에 악성코드(앱)가 존재합니다. 치료 후 다시 실행해주시기 바랍니다.";
                        break;
                    case _ROOTING_DEVICE:
                        threatMessage = "루팅된 단말입니다. 보안을 위하여 루팅 단말에서 실행할 수 없습니다.";
                        break;
                    case _PERMISSION_NOT_GRANTED:
                        threatMessage = "권한 허용을 적용하기 위하여 앱을 다시 실행하시기 바랍니다.";
                        break;
                    case _ERROR:
                        threatMessage = "단말 보안 솔루션 연동 중 에러가 발생하였습니다.";
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + takeObject);
                }
                if (threatMessage.isEmpty()) {
                    Log.TC("단말 보안 상태 - 안전");
                    doNextStep(STATUS_SAFE_DEVICE);
                } else {
                    Log.TC("단말 보안 상태 - 위협");
                    showFinishDialog(CommonBasedConstants.RESULT_AGENT_UNSAFE_DEVICE,
                            "단말 보안 상태",
                            threatMessage);
                }
            }
        });
    }

    @NotNull
    private void validAdminPackageInfo() throws NullPointerException {
        String tmp;
        Bundle extraData = getIntent().getExtras();
        if(extraData == null) {
            throw new NullPointerException("행정앱 정보가 존재하지 않습니다.");
        }
        tmp = extraData.getString("extra_token") ;
        if (tmp == null || tmp.equals("")) {
            throw new NullPointerException("행정앱의 보안 토큰값이 존재하지 않습니다.");
        }

        if (isAboveAPIVer3()) {
            int i = extraData.getInt("req_id", -1) ;
            if (i < 0) {
                throw new NullPointerException("행정앱 고유정보가 존재하지 않습니다.");
            }
            Log.d(TAG, "- 초기화 요청 ID  : " + i);

            tmp = extraData.getString("api_version") ;
            if (tmp == null || tmp.equals("")) {
                throw new NullPointerException("공통기반 라이브러리 버전값이 존재하지 않습니다.");
            }
            Log.d(TAG, "- 초기화 요청 API : " + tmp);
            extraData.getInt("stack_trace_count", 0) ;
        } else {
            tmp = extraData.getString("extra_package");
            if (tmp == null || tmp.equals("")) {
                throw new NullPointerException("행정앱 고유정보가 존재하지 않습니다.");
            }
            // 3.x 타입으로 변경
            // TODO tmp 값을 임의의 integer 값으로 변경 ?
            extraData.putInt("req_id", Integer.MAX_VALUE);
            Log.d(TAG, "- 초기화 요청 ID (임시)  : " + tmp);

            tmp = "2.x.x";
            Log.d(TAG, "- 초기화 요청 API : " + tmp);
        }
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
                showFinishDialog(CommonBasedConstants.RESULT_AGENT_INVALID,
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
        if (newStatus > -1) {
            status.addAndGet(newStatus);
        }

        if (status.compareAndSet(STATUS_READY, STATUS_READY)) {
            Log.timeStamp("activity-init");
            if (newStatus != -1) {
            } else {
                hideProgressDialog();
            }

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
                Log.d(TAG, "보안 네트워크 상태 - 미연결");

                try {
                    localService.validSigned();
                    Log.TC("보안 네트워크 - 연결 요청");
                    // 서명값이 존재하므로 보안 네트워크 연결 요청
                    showProgressDialog(getString(R.string.loading_secure_network_msg));

                    Log.timeStamp("secure-network");
                    // 모니터 서비스를 이용하여 보안 네트워크 연결 요청
                    localService.taskConnectedSecureNetwork(new TakeListener<SecureNetwork.STATUS>() {
                        @Override
                        public void onTake(SecureNetwork.STATUS takeObject) {
                            Log.timeStamp("secure-network");
                            switch (takeObject) {
                                case _CONNECTED:
                                    Log.TC("보안 네트워크 - 연결 성공");
                                    doNextStep();
                                    break;
                                case _ERROR:
                                    Log.TC("보안 네트워크 - 연결 실패");
                                    String message = localService.getErrorMessage(2);
                                    if (message == null) {
                                        message = "로그인 정보가 존재하지 않습니다.";
                                    }
                                    Log.e(TAG, "보안 네트워크 연결 실패 : " + message);
                                    showFinishDialog(CommonBasedConstants.RESULT_AGENT_SOLUTION_ERROR,
                                            "보안 네트워크 연결 실패",
                                            message);
                                    break;
                                default:
                                    throw new IllegalStateException("Unexpected value: " + takeObject);
                            }
                        }
                    });
                } catch (SessionManager.SessionException e) {
                    Log.TC("서명 세션 - 만료 (" + e.getMessage() +")");
                    // 서명값이 존재하지 않으므로 인증서 로그인 솔루션 연계
                    localService.takeGenerateSigned(new TakeListener<UserSigned.STATUS>() {
                        @Override
                        public void onTake(UserSigned.STATUS takeObject) {
                            switch (takeObject) {
                                case _OK:
                                    Log.TC("인증서 로그인 - 성공");
                                    doNextStep();
                                    break;
                                case _USER_CANCEL:
                                    Log.TC("인증서 로그인 - 사용자 취소");
                                    showFinishDialog(CommonBasedConstants.RESULT_USER_CANCELED,
                                            "인증서 로그인 취소",
                                            "사용자에 의하여 인증서 로그인이 취소되었습니다.");
                                    break;

                                case _ERROR:
                                    showFinishDialog(CommonBasedConstants.RESULT_AGENT_SOLUTION_ERROR,
                                            "인증서 로그인 에러",
                                            "인증서 로그인 솔루션 연계 중 에러가 발생하였습니다.");
                                    break;
                            }

                        }
                    }, this);
                }
            } else {
                Log.TC("서명 세션 - 등록");
                Log.timeStamp("certification-confirm");
                localService.takeConfirmCertification(new TakeListener<String>() {
                    @Override
                    public void onTake(String takeObject) {
                        hideProgressDialog();
                        if (takeObject.isEmpty()) {
                            Log.timeStamp("certification-confirm");
                            Log.TC("인증 세션 - 유효");
                            Bundle extra = localService.getUserBundle();

                            // 공통기반 라이브러리 2.x 지원. 2.x 버전을 지원하지 않을 경우 삭제.
                            extra = Utils.insertV2Type(extra);
                            localService.enabledMonitorPackage(getCallingActivity().getPackageName(), getIntent().getExtras());
                            ////////////////////////////////////////////////////////////////////////
                            finishResult(CommonBasedConstants.RESULT_OK, extra);
                        } else {
                            Log.TC("인증 세션 - 생성 실패");
                            showFinishDialog(CommonBasedConstants.RESULT_AGENT_FAILURE_USER_AUTHENTICATION,
                                    "사용자 인증 실패", takeObject);
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
                if (dialogProgress != null) {
                    dialogProgress.dismiss();
                    dialogProgress = null;
                }
                dialogProgress = ProgressDialog.show(CommonBasedInitActivity.this, "", message, true);
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

    private void showExistNoticeDialog(final List<String> newNotice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(CommonBasedInitActivity.this)
                        .setCancelable(false)
                        .setTitle("공지가 존재합니다.")
                        .setMessage("모바일 전자정부 지원센터로부터 수신된 공지사항이 있습니다.")
                        .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkStatus(false);
                            }
                        })
                        .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Iterator<String> iterator = newNotice.iterator();
                                if (iterator.hasNext()) {
                                    showNoticeDialog(iterator);
                                } else {
                                    checkStatus(false);
                                }
                            }
                        }).show();
            }
        });
    }

    private void showNoticeDialog(final Iterator<String> newNotice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String reqId = newNotice.next();
                String[] item = PushMessageDBHelper.unreadNotice(reqId);
                new AlertDialog.Builder(CommonBasedInitActivity.this)
                        .setCancelable(false)
                        .setTitle(item[0])
                        .setMessage(item[1])
                        .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkStatus(false);
                            }
                        })
                        .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PushMessageDBHelper.checkedNotice(reqId);
                                if (newNotice.hasNext()) {
                                    showNoticeDialog(newNotice);
                                } else {
                                    checkStatus(false);
                                }
                            }
                        }).show();
            }
        });
    }

    public void showFinishDialog(final int result_code, final String title, final String message) {
        showFinishDialog(result_code, title, message, false);
    }

    public void showFinishDialog(final int result_code, final String title, final String message, final boolean finishApp) {
        hideProgressDialog();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(CommonBasedInitActivity.this)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setNeutralButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                moveTaskToBack(true);
                                finishResult(result_code, null);

                                if (localService != null) {
                                    localService.clearSession();
                                }
                                if (finishApp) {
                                    moveTaskToBack(true);
                                    System.exit(0);
                                }
                            }
                        }).show();
            }
        });
    }

    void finishResult(int result_code, Bundle extra) {
        hideProgressDialog();
        Intent result = null;
        if (extra != null) {
            result = new Intent();
            result.putExtras(extra);
        }
        Log.i(TAG, "------- 공통기반 초기화 결과 -------");
        Log.d(TAG, String.format("Code = %s, Data = %s", result_code, extra == null ? "N/A" : extra.toString()));
        setResult(result_code, result);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 앱 실행시 볼륨키 다운 이벤트를 입력하고 있다면.
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            localService.exportCrashLog();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        hideProgressDialog();
        localService.setStopActivity();
        localService.clearToken();
        super.onDestroy();
    }
}
