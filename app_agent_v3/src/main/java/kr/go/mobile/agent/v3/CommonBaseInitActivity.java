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
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import kr.go.mobile.agent.app.NotResponseServiceException;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.ResourceUtils;
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
    private static final int STATUS_RUNNING = /*8 >> 0*/ 8 ; // 1000
    private static final int STATUS_SERVICE_READY = 8 >> 3; // 0001
    private static final int STATUS_SAFE_DEVICE = 8 >> 2; // 0010
    private static final int STATUS_INTEGRITY_APP = 8 >> 1; // 0100
    private static final int STATUS_READY = STATUS_SERVICE_READY | STATUS_INTEGRITY_APP | STATUS_SAFE_DEVICE; // 0111

    public interface IPublicAPI {
        void setExtraData(Bundle extra);
        boolean verifyIntegrityApp();
        void startSecureNetwork();
        boolean enabledSecureNetwork() throws NotResponseServiceException;
        String getErrorMessage() throws NotResponseServiceException;
        String getThreatMessage() throws NotResponseServiceException;
        boolean validSignedSession();
        void registeredSignedSession(UserSigned signed);
        boolean readyService();
        void startBrokerService();
        Bundle getResultBundle();
        void clearToken();
    }

    boolean aboveAPIVer3;
    boolean flagShowDialog;

    IPublicAPI publicAPI;
    ProgressDialog mProgressDialog;
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
                        Log.timeStamp("register Signed");
                        doNextStep(-1);
                        break;
                    }
                    case EVENT_TYPE_AUTH_REGISTERED_OK: {
                        Log.i(TAG, "인증 세션 생성 - 정상");
                        Bundle extra = publicAPI.getResultBundle();
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
        Log.timeStamp("init");
        setContentView(R.layout.activity_mobile_gov_loading);
        try {
            initSolutionModule();
        } catch (SolutionManager.ModuleNotFoundException e) {
            Log.e(TAG, "인증서 로그인 요청이 실패하였습니다. - " + e.getNotFoundSolutionName(), e);
            showFinishDialog(MobileEGovConstants.RESULT_AGENT_SOLUTION_ERROR,
                    "솔루션 에러", String.format("%s 솔루션을 로드할 수 없습니다.", e.getNotFoundSolutionSimpleName()));
        }

        Bundle extraData = getIntent().getExtras();

        aboveAPIVer3 = Objects.equals(getIntent().getAction(), MobileEGovConstants.ACTION_LAUNCH_SECURITY_AGENT);
        try {
            // 행정앱에서 공통기반 서비스 초기화 요청시 에러 다이얼로그를 비활성화 하기 위해서는 flagShowDialog 갓이 false 값을 가져야 한다.
            flagShowDialog = extraData.getBoolean("extra_show_dialog", !aboveAPIVer3 /*FIXME API 3.x 이상일 경우 기본적으로 보안에이전트에서 다이얼로그를 생성하지 않는다.*/);
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

            publicAPI = (IPublicAPI) getApplication();
            publicAPI.setExtraData(extraData);
            checkStatus();
        }
        // else { 권한이 없을 경우 권한 요청을 하였기때문에 요청처리 이후 onRequestPermissionsResult() 에서 처리함. }
    }

    void initSolutionModule() throws SolutionManager.ModuleNotFoundException {
        SolutionManager.initSolutionModule(SolutionManager.DREAM_SECURITY_GPKI_LOGIN, new Solution.EventListener<UserSigned>() {
            @Override
            public void onCancel(Context context) {
                showAlertDialog(context, MobileEGovConstants.RESULT_AGENT_INVALID,
                        "인증서 로그인 모듈",
                        "사용자에 의하여 취소되어 앱을 종료합니다.");
            }

            @Override
            public void onFailure(Context context, String message, Throwable t) {

            }

            @Override
            public void onError(Context context, Solution.RESULT_CODE errorCode, String message) {
                if (errorCode == Solution.RESULT_CODE._INVALID) {
                    showAlertDialog(context, MobileEGovConstants.RESULT_AGENT_INVALID,
                            "인증서 로그인 모듈 에러",
                            "인증서 로그인에 실패하였습니다. (이유 : " + message + ")");
                }
            }

            @Override
            public void onCompleted(Context context, UserSigned userSigned) {
                Log.i(TAG, "GPKI 인증서 로그인 성공");
                Log.timeStamp("register Signed");
                publicAPI.registeredSignedSession(userSigned);
            }

            void showAlertDialog(final Context context, final int result_code, final String title, final String message) {
                final CommonBaseInitActivity activity = (CommonBaseInitActivity) context;
                if(activity.flagShowDialog) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(context)
                                    .setTitle(title)
                                    .setMessage(message)
                                    .setCancelable(false)
                                    .setNeutralButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            activity.finishResult(result_code, null);
                                        }
                                    }).show();
                        }
                    });
                } else {
                    activity.finishResult(result_code, null);
                }
            }
        });
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

    void checkPackages() throws NotInstalledRequiredPackagesException, UninstallExpiredPackagesException, Exception {
        // 라이선스 만료된 앱 체크
        validPackages("license_expired_package", false);
        // 존재해야 하는 앱 체크
        validPackages();
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

        // 앱 무결성 체크
        try {
            SolutionManager.getSolutionModule(SolutionManager.EVER_SAFE).execute(); // -> EVENT_TYPE_INTEGRITY_APP_OK or EVENT_TYPE_INTEGRITY_APP_FAIL
        } catch (SolutionManager.ModuleNotFoundException e) {
            Log.e(TAG, "앱 무결성 솔루션 연계 에러가 발생하였습니다. -  " + e.getNotFoundSolutionName(), e);

            showFinishDialog(MobileEGovConstants.RESULT_AGENT_SOLUTION_ERROR,
                    "솔루션 에러",
                    String.format("%s 솔루션을 로드할 수 없습니다.", e.getNotFoundSolutionSimpleName()));
            return;
        }

        // 필수앱(보안 솔루션 앱) 설치 유무 체크
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    checkPackages();
                } catch (UninstallExpiredPackagesException e) {
                    showFinishDialog(MobileEGovConstants.RESULT_AGENT_EXIST_LICENSE_EXPIRED_PACKAGE,
                            "앱 삭제 요청",
                            String.format("라이선스 만료로 %s 앱을 삭제해야 합니다. 삭제 후 다시 실행해주시기 바랍니다.", e.getPackageLabel()));
                } catch (NotInstalledRequiredPackagesException e) {
                    showFinishDialog(MobileEGovConstants.RESULT_AGENT_INSTALL_REQUIRED_PACKAGE,
                            "앱 설치 요청",
                            String.format("공통기반 서비스 지원을 위한 %s 앱을 설치해야 합니다. 설치 후 다시 실행해주시기 바립니다.", e.getPackageLabel()));
                } catch (Exception e) {
                    showFinishDialog(MobileEGovConstants.RESULT_AGENT_INTERNAL_ERROR,
                            "",
                            "앱 검사를 진행할 수 없습니다. 종료 후 다시 실행해주시기 바랍니다.");
                }
            }
        }, "check installed package").start();

        IntentFilter filter = new IntentFilter(ACTION_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(this.eventReceiver, filter);

        // 무결성 검증 확인
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean notReady;
                do {
                    notReady = !publicAPI.verifyIntegrityApp();
                }  while (notReady);
                Log.i(TAG, "보안 에이전트 무결성 상태 - 정상");
                doNextStep(STATUS_INTEGRITY_APP);
            }
        }, "wait integrity status").start();


        // 악성 코드 검사 결과 대기
        new Thread(new Runnable() {
            @Override
            public void run() {
                String threatMessage = "";
                do {
                    try {
                        threatMessage = publicAPI.getThreatMessage();
                        if (threatMessage.isEmpty()) {
                            Log.i(TAG, "단말 위협 상태 - 정상");
                            doNextStep(STATUS_SAFE_DEVICE);
                        } else {
                            showFinishDialog(MobileEGovConstants.RESULT_AGENT_UNSAFE_DEVICE, "악성코드 탑지 솔루션", threatMessage);
                        }
                        break;
                    } catch (NotResponseServiceException ignored) {
                    }
                } while (true);
            }
        }, "check device status").start();

        // 사용자 세션관리 서비스 준비 대기
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    if(publicAPI.readyService()) {
                        Log.i(TAG, "서비스 준비 상태 - 정상");
                        doNextStep(STATUS_SERVICE_READY);
                        break;
                    }
                } while (true);

            }
        }, "wait service ready status").start();
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

    void doNextStep(int status) {
        Log.calling();

        if (status > -1) {
            this.status.addAndGet(status);
        }

        if (this.status.compareAndSet(STATUS_READY, STATUS_RUNNING) ||
                this.status.compareAndSet(STATUS_RUNNING, STATUS_RUNNING) ) {
            Log.timeStamp("init");
            hideProgressDialog();

            // VPN 이 연결되어 있는지 확인 ?
            // --> 연결되어 있다면, 인증서버 요청
            // -----> 인증서버 인증 결과 는 ?
            // ---------> 성공이면, 서명값과 일치하는지 확인
            // -------------> 일치하면, 초기화 성공
            // -------------> 불일치하면, 인증서 로그인 요청
            // ---------> 실패하면, 인증 실패처리 및 종료
            // --> 안되어 있다면, 서명값이 존재하는가 ?
            // -----> 존재하면 VPN 연결 요청
            // -----> 존재하지 않으면 인증서 로그인 요청

            try {
                if (publicAPI.enabledSecureNetwork()) {
                    showProgressDialog(getString(R.string.user_authentication_msg));
                    publicAPI.startBrokerService(); // ----> SESSION_AUTH_REGISTERED_OK or SESSION_AUTH_REGISTERED_ERROR
                    return;
                }
            } catch (NotResponseServiceException ignored) {
                Log.i(TAG, "보안 네트워크가 연결되어 있지 않습니다.");
            }

            if(publicAPI.validSignedSession()) {
                Log.i(TAG, "보안 네트워크가 연결을 시도합니다.");
                // 서명값이 존재하므로 보안 네트워크 연결 요청
                showProgressDialog(getString(R.string.loading_secure_network_msg));
                // 모니터 서비스를 이용하여 보안 네트워크 연결 요청
                publicAPI.startSecureNetwork();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            try {
                                if (publicAPI.enabledSecureNetwork()) {
                                    doNextStep(-1);
                                } else {
                                    try {
                                        showFinishDialog(MobileEGovConstants.RESULT_AGENT_SOLUTION_ERROR,
                                                "보안 네트워크 연결", publicAPI.getErrorMessage());
                                    } catch (NotResponseServiceException e) {
                                        showFinishDialog(MobileEGovConstants.RESULT_AGENT_INTERNAL_ERROR,
                                                "보안 네트워크 연결", "실패 메시지를 확인할 수 없습니다.");
                                    }
                                }
                                break;
                            } catch (NotResponseServiceException ignored) {
                            }
                        } while (true);
                    }
                }, "wait connected secure network").start();
            } else {
                // 보안네크워크 미연결 & 서명값이 존재하지 않으므로 인증서 로그인
                Log.i(TAG, "서명값이 존재하지 않거나 만료되었습니다. 인증서 로그인이 필요합니다.");
                try {
                    SolutionManager.getSolutionModule(SolutionManager.DREAM_SECURITY_GPKI_LOGIN).execute(false);
                } catch (SolutionManager.ModuleNotFoundException e) {
                    Log.e(TAG, "인증서 로그인 요청이 실패하였습니다. - " + e.getNotFoundSolutionName(), e);
                    showFinishDialog(MobileEGovConstants.RESULT_AGENT_SOLUTION_ERROR,
                            "솔루션 에러", String.format("%s 솔루션을 로드할 수 없습니다.", e.getNotFoundSolutionSimpleName()));
                }
            }
        }
    }

    @Deprecated
    void showProgressDialog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog = ProgressDialog.show(CommonBaseInitActivity.this, "", message, true);
                mProgressDialog.show();
            }
        });

    }

    @Deprecated
    void hideProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }
        });
    }

    public void showFinishDialog(final int result_code, final String title, final String message) {
        if (flagShowDialog) {
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
        SolutionManager.finishSolution(SolutionManager.DREAM_SECURITY_GPKI_LOGIN);
        SolutionManager.finishSolution(SolutionManager.EVER_SAFE);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(eventReceiver);
        publicAPI.clearToken();
        super.onDestroy();
    }
}
