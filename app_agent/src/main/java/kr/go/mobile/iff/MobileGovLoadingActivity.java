package kr.go.mobile.iff;

import com.dreamsecurity.magicline.client.MagicLine;
import com.dreamsecurity.magicline.client.MagicLineType;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.TextView;

import kr.co.everspin.eversafe.EversafeHelper;
import kr.go.mobile.iff.service.ISessionManagerService.ISessionManagerEventListener;
import kr.go.mobile.iff.service.SessionManagerService;
import kr.go.mobile.iff.util.AdminState;
import kr.go.mobile.iff.util.CertManager;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.iff.util.Utils;
import kr.go.mobile.iff.util.Utils.TimeStamp;
import kr.go.mobile.iff.util.VGScanTask;
import kr.go.mobile.mobp.iff.util.NotInstalledRequiredPackagesException;
import kr.go.mobile.mobp.iff.R;

/**
 * 행정앱이 공통기반 라이브러리를 통해서 공통기반 서비스를 초기화할때 호출되는 Activity 로, 다음과 같은 기능을 수행한다.<br>
 * <ul>
 * <li>필수앱 설치 여부 확인</li>
 * <li>위변조 모듈 초기화 및 실행(?)</li>
 * <li>백신 모듈 실행</li>
 * <li>인증서 모듈 실행 및 인증서 로그인 시도</li>
 * <li>VPN 연결 시도</li>
 * </ul>
 *
 * @author 윤기현
 */
public class MobileGovLoadingActivity extends Activity {

    private static final int AUTHORIZATION_FAILED = -999;

    private static final String TAG = MobileGovLoadingActivity.class.getSimpleName();
    private static final boolean LOG_ENABLE = true;

    private static final int PERMISSION_REQUEST_CODE = 1000;

    private final Object mLock = new Object();

    private AdminState mAdminState;

    // 보안 토큰 획득 여부
    private boolean isGetToken = false;
    // AntiVirus OK
    private boolean enabledAntivirus = false;
    // 서비스 동작 준비 여부
    private boolean readyService = false;
    // 바이러스 검사 Task
    private VGScanTask mVGTask;

    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TimeStamp.startTime("estimate second init time");

        // 상단 타이틀바 제거
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_intro);

        // 내부 런처 화면에 버전 표시
        TextView versionName = (TextView) findViewById(R.id.version_name);
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
            versionName.setText("Ver." + version);
        } catch (NameNotFoundException e) {
            versionName.setText("Ver.x.x.x-test");
            LogUtil.e(getClass(), "", e);
        }

        try {
            // 행정앱 정보가 정상인지 확인
            final Bundle extra = getIntent().getExtras();
            if (extra.getString(AdminState.EXTRA_TOKEN) == null) {
                throw new Exception("TOKEN is null");
            }
            if (extra.getString(AdminState.EXTRA_PACKAGE) == null) {
                throw new Exception("PACKAGE_NAME is null");
            }
            mAdminState = new AdminState(extra);
        } catch (NullPointerException e) {
            LogUtil.e(getClass(), getString(R.string.not_exist_required_info), e);
            showFinishDialog(getString(R.string.not_normal_approach),
                    getString(R.string.not_exist_required_info));
            return;
        } catch (Exception e) {
            LogUtil.e(getClass(), getString(R.string.not_exist_required_info), e);
            showFinishDialog(getString(R.string.not_normal_approach), getString(R.string.not_exist_required_info));
            return;
        }

        // V3 엔터프라이즈 앱 확인
        {
            PackageManager pm = getPackageManager();
            String v3EnterprisePkg = "com.ahnlab.v3mobileenterprise";
            boolean flag = true;
            try {
                pm.getApplicationInfo(v3EnterprisePkg, PackageManager.GET_META_DATA);
            } catch (NameNotFoundException e) {
                flag = false;
            }
            if (flag) {
                showDialogAndGuide("라이센스 만료", "V3 엔터프라이즈의 라이센스가 만료되어 앱을 삭제해야 합니다.\n삭제 후 다시 실행해 주시기 바랍니다.", GuideActivity.GUIDE_TYPE_V3_REMOVE);
                return;
            }
        }

        try {
            // REQ 행정앱의 요청이 있을때 마다, 필수앱 설치 여부와 백신 빠른 검사를 실행한다.
            TimeStamp.startTime("checkRequiredApp");
            Utils.checkRequiredApp(this);
            TimeStamp.endTime("checkRequiredApp");
        } catch (NotInstalledRequiredPackagesException e) {
            final String label = e.getPackageLabel();
            final String pkgName = e.getNotInstalledPackageName();

            Utils.showAlertDialog(this, getString(R.string.not_installed_required_apps),
                    String.format(getString(R.string.not_installed_required_apps_details), label),
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sendResult(null);
                            if (label.equals("SecureHub")) {
                                String url = "market://details?id=" + pkgName;
                                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(i);
                            } else {
                                Intent intent = getPackageManager().getLaunchIntentForPackage("com.zenprise");
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }
                    });
            // }
            return;
        }

        ServiceWaitHandler.getInstance().addEventListener(new ISessionManagerEventListener() {

            @Override
            public void timeout() {
                showFinishDialog(getString(R.string.init_error_msg), getString(R.string.init_error_msg_details));
            }

            @Override
            public void ready() {

                if (deniedPermission(true)) {
                    return;
                }

                boolean enableVPN = SAGTApplication.getSessionManagerService().enableVpn();
                if (enableVPN) {
                    // 이미 서명값과 VPN 연결이 정상인 상태
                    // 서명값은 메모리에 존재하는 값으로 보안에이전트 재실행시 또는 지정된 시간이후에는 초기화된다.
                    return;
                }
                // 2016.11.22 윤기현 - 보안 환경 설정 중입니다. 다이얼로그 상자 보이기
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                if (mProgressDialog == null && !(MobileGovLoadingActivity.this.isFinishing() || MobileGovLoadingActivity.this.isDestroyed())) {
                    mProgressDialog = ProgressDialog.show(MobileGovLoadingActivity.this, "", getString(R.string.security_msg), true);
                    mProgressDialog.show();
                }

                scanAntivirusTask();
                executeGetTokenTask();
            }

            @Override
            public void onChangedStatus(int eventType, Bundle data) {
                LogUtil.d(TAG, "SessionManager --> onChangedStatus (event = " + eventType + ")", LOG_ENABLE);

                switch (eventType) {
                    case SIGN_EVENT:
                        synchronized (mLock) {
                            readyService = true;
                        }
                        checkSignedInfo();
                        break;
                    case VPN_EVENT:
                        String result = data == null ? null : data.getString(EXTRA_RESULT);
                        if (result == null) {
                            // connecting...
                            if (mProgressDialog == null && !(MobileGovLoadingActivity.this.isFinishing() || MobileGovLoadingActivity.this.isDestroyed())) {
                                mProgressDialog = ProgressDialog.show(MobileGovLoadingActivity.this, "", getString(R.string.loading_msg), true);
                                mProgressDialog.show();
                            }
                        } else {
                            LogUtil.d(getClass(), "extra: " + result);
                            /**
                             *  ---------------------------------- BEGIN
                             * 요청일: 2018-05-01
                             * 처리일: 2018-05-08
                             * 요청내용: 보안에이전트 메시지 수정요청
                             * "VPN 연결 에러"를  "보안접속 종료"로 메시지 변경
                             */
                            showFinishDialog("", "보안접속 중 문제가 발생하여 앱을 종료합니다.");
                            /* ---------------------------------- END */
                        }
                        break;
                    case CERT_EVENT:
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                            mProgressDialog = null;
                        }

                        if (data.getBoolean(EXTRA_IS_CERT, false)) {
                            String dn = data.getString(EXTRA_SIGNED_DN);
                            LogUtil.d(MobileGovLoadingActivity.class, "extra: " + dn);
                            if (dn.equals(SessionManagerService.NOT_EXIST_SIGNED)) {
                                // 서명값이 존재 하지 않는 상태로 서명값을 재 요청한다.
                                checkSignedInfo();
                            } else {
                                sendResult(dn);
                            }
                        } else {
                            showFinishDialog("인증에러", data.getString(EXTRA_RESULT));
                        }
                        break;
                    default:
                        break;
                }
            }

        });
        final Handler H = ServiceWaitHandler.getInstance();
        Message m = H.obtainMessage(ServiceWaitHandler.MESSAGE_CATEGORY_LOADING_ACTIVITY, ServiceWaitHandler.READY_BIND_SERVICE, 0);
        H.sendMessage(m);
    }

    /**
     * 백신 모듈을 이용하여 악성코드를 스캔
     */
    private void scanAntivirusTask() {
        /*
         * if (SAGTApplication.getBoolean(SAGTApplication.PREFS_KEY_SET_PROFILE)) return;
         *
         * mV3Task = new V3ScanTask() {
         */

        mVGTask = new VGScanTask() {
            @Override
            protected void onError(int errorCode) {
                // REQ VG 실행 중 에러가 발생하였음 -> 런처 종료.
                showFinishDialog("VG 연동 알림", VGScanTask.getErrorMessage(errorCode));
            }

            @Override
            protected void onCompletedScan(int infectedCount, boolean enableRealtime) {
                if (enableRealtime) {
                    LogUtil.d(getClass(), String.format("Realtime Scan: enabled"), LOG_ENABLE);
                } else {
                    LogUtil.d(getClass(), String.format("scanAntiVirus (%s)  - End", infectedCount), LOG_ENABLE);
                    if (infectedCount == -1) {
                        LogUtil.w(getClass(), "백신 검사가 비정상적으로 종료되었습니다.");
                    } else if (infectedCount > 0) {
                        // REQ 악성코드가 발견되었음 -> 런처 종료
                        showFinishDialog("안전하지 않은 환경입니다", VGScanTask.getErrorMessage(VGScanTask.ERROR_INFECTED_PACKAGE));
                        return;
                    }
                }

                TimeStamp.endTime("scanAntivirus");
                enabledAntivirus = true;
                checkSignedInfo();
            }
        };

        LogUtil.i(getClass(), "scanAntivirusTask()");
        TimeStamp.startTime("scanAntivirus");
        mVGTask.start(this);
    }

    /**
     * MSM 보안 토큰을 취득하기 위한 코드
     */
    private void executeGetTokenTask() {
        LogUtil.i(getClass(), "executeGetTokenTask()");
        TimeStamp.startTime("executeGetTokenTask");
        EversafeHelper.getInstance().setBackgroundMaintenanceSec(600);
        EversafeHelper.getInstance().initialize(getString(R.string.msmurl));

        new EversafeHelper.GetVerificationTokenTask() {
            @Override
            protected void onCompleted(byte[] verificationToken, String verificationTokenAsByte64, boolean isEmergency) {
                TimeStamp.endTime("executeGetTokenTask");
                LogUtil.d(MobileGovLoadingActivity.class, "verification: " + verificationTokenAsByte64, LOG_ENABLE);
                synchronized (mLock) {
                    isGetToken = true;
                    // 런처의 보안 토큰 취득이 정상적으로 수행되면 설정.
                    mAdminState.putThisToken(verificationTokenAsByte64);
                }
                checkSignedInfo();
            }
        }.setTimeout(-1).execute();
    }

    private void checkSignedInfo() {
        // REQ 필수앱 검사 / V3 검사 / 보안토큰 취득이 정상적일 경우 GPKI 인증서 로그인 단계로 넘어간다.
        // if (isGetToken && SAGTApplication.getBoolean(SAGTApplication.PREFS_KEY_SET_PROFILE)) {
        if (isGetToken && enabledAntivirus) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }

            // 서비스가 시작되지 않은 상태일때는 인증정보가 없는 상태로 간주..
            if (readyService || SAGTApplication.getSessionManagerService().existSigned()) {
                LogUtil.i(getClass(), "startVPN");
                TimeStamp.startTime("startVPN");
                // connecting...
                if (mProgressDialog == null && !(MobileGovLoadingActivity.this.isFinishing() || MobileGovLoadingActivity.this.isDestroyed())) {
                    mProgressDialog = ProgressDialog.show(MobileGovLoadingActivity.this, "", getString(R.string.loading_msg), true);
                    mProgressDialog.show();
                }
                SAGTApplication.getSessionManagerService().startVPN(mAdminState);
            } else {
                readyService = false;
                // 서명값이 존재하지 않으면 서명값을 요청. GPKI 인증서 로그인
                TimeStamp.endTime("estimate init time");
                TimeStamp.endTime("estimate second init time");
                LogUtil.d(getClass(), "show CertList", LOG_ENABLE);
                CertManager.showCertList(this);
                // -->> 리턴값은 onActivityResult() 메소드를 통해서 받음
            }
        }
    }

    // BUILD SDK android 6.0 (API 23) 이상에서 부터 사용가능한 코드 - BEGIN
    @TargetApi(23)
    private boolean deniedPermission(boolean flag) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return false;
        }
        String[] requiredPermissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
        if ((requiredPermissions = Utils.checkPermission(MobileGovLoadingActivity.this, requiredPermissions)) == null) {
            return false;
        } else if (flag) {
            requestPermissions(requiredPermissions, PERMISSION_REQUEST_CODE);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (deniedPermission(false)) {
                    showFinishDialog(getString(R.string.denied_permission), getString(R.string.denied_permission_details));
                } else {
                    final Handler H = ServiceWaitHandler.getInstance();
                    Message m = H.obtainMessage(ServiceWaitHandler.MESSAGE_CATEGORY_LOADING_ACTIVITY, ServiceWaitHandler.READY_BIND_SERVICE, 0);
                    H.sendMessage(m);
                }
                break;

            default:
                break;
        }
    }

    // BUILD SDK android 6.0 (API 23) 이상에서 부터 사용가능한 코드 - END

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case CertManager.REQUEST_CODE_SIGNSHOW:
                handleSingedInfo(resultCode, intent);
                break;
            case CertManager.REQUEST_CODE_CERTMANAGER:
                CertManager.showCertList(this);
                break;
        }
    }

    /*
     * 인증서모듈을 이용한 결과를 처리한다.
     */
    private void handleSingedInfo(int resultCode, Intent intent) {

        if (resultCode == RESULT_OK) {
            // GPKI 인증이 성공하면 인증 정보를 Intent에 넣어서 서비스를 시작한다.
            String signedDataBase64 = MagicLine.getSignedDataBase64(intent);
            String subjectDN = MagicLine.getSignerSubjectDN(intent);
            String userDNWithDeviceID = subjectDN + ",deviceID=" + Utils.getDeviceID(this);

            // REQ 인증서 로그인이 성공하면 서명 정보를 유지한다.
            // 서명정보를 유지하기 위한 서비스를 실행한다.
            Intent startSessManaService = new Intent(this, SessionManagerService.class);
            startSessManaService.putExtra(SessionManagerService.EXTRA_SIGNED_SUCCESS_TIME, System.currentTimeMillis());
            startSessManaService.putExtra(SessionManagerService.EXTRA_SIGNED_MAINTAIN_TIME, getResources().getInteger(R.integer.SignedSessionTimeoutSec));
            startSessManaService.putExtra(SessionManagerService.EXTRA_USER_DN, subjectDN);
            startSessManaService.putExtra(SessionManagerService.EXTRA_USER_DN_WITH_DEVICE_ID, userDNWithDeviceID);
            startSessManaService.putExtra(SessionManagerService.EXTRA_SIGNED_BASE64, signedDataBase64);

            startService(startSessManaService);
        } else {
            int errorCode = MagicLine.getErrorCode(intent);
            LogUtil.w(getClass(), "인증서 ERROR : " + errorCode);
            switch (errorCode) {
                case MagicLineType.MAGICLINE_SIGN_EMPTY_CERT:
                case MagicLineType.LAUNCHER_GO_MANAGE:
                    // 인증서가 없음 -> 인증서 관리 페이지 호출
                    CertManager.showCertManager(this);
                    break;
                case MagicLineType.MAGICLINE_SIGN_USER_CANCEL:
                    // 사용자 취소 -> 앱 종료
                    showFinishDialog(getString(R.string.cancel_login_certificate), String.format(getString(R.string.cancel_login_certificate_details)));
                    break;
                default:
                    // 인증서 모듈 연동 에러 -> 앱 종료
                    showFinishDialog(getString(R.string.error_certificate_module), String.format(getString(R.string.error_certificate_module_details), errorCode));
                    break;
            }
        }
    }

    /**
     * 다이얼로그를 보여주고 "확인"을 누르면 앱을 종료한다.
     *
     * @param title
     * @param message
     */
    private void showFinishDialog(String title, String message) {
        // REQ 런처 종료시 종료 사유에 대한 팝업창 뷰
        Utils.showAlertDialog(this, title, message, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendResult(null);
            }
        });
    }

    /**
     * 다이얼로그를 보여주고 "확인"을 누르면 앱을 종료 후 가이드 화면을 보여줌.
     *
     * @param title
     * @param message
     */
    private void showDialogAndGuide(String title, String message, final byte guideType) {
        Utils.showAlertDialog(this, title, message, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendResult(null, guideType);
            }
        });
    }

    private void sendResult(String dn) {
        sendResult(dn, (byte) 0);
    }

    private void sendResult(String dn, byte guideType) {
        // REQ 런처 종료시 행정앱에 리턴값 전달
        Intent data = new Intent();
        if (dn == null || dn.isEmpty()) {
            LogUtil.e(getClass(), "dn is " + (dn == null ? "null" : "empty"));
            setResult(AUTHORIZATION_FAILED, data);
        } else {
            data.putExtra("userId", CertManager.getUserID(dn));
            data.putExtra("dn", dn);
            setResult(RESULT_OK, data);
        }
        if (guideType != 0) {
            Intent guide = new Intent(this, GuideActivity.class);
            guide.putExtra(GuideActivity.KEY_GUIDE_TYPE, guideType);
            startActivity(guide);
        }
        LogUtil.d(getClass(), "call GMobileActivity.finish()", LOG_ENABLE);
        finish();
        // -->> 행정앱의 Activity에서 처리함.
    }

    @Override
    protected void onDestroy() {
        if (mVGTask != null) {
            mVGTask.destroy();
            mVGTask = null;
        }

        ServiceWaitHandler.getInstance().removeEventListener();
        super.onDestroy();
    }
}
