package kr.go.mobile.common.v3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import kr.co.everspin.eversafe.EversafeHelper;
import kr.go.mobile.common.R;
import kr.go.mobile.common.v3.broker.Caller;
import kr.go.mobile.common.v3.broker.Request;
import kr.go.mobile.common.v3.broker.Response;
import kr.go.mobile.common.v3.broker.SSO;
import kr.go.mobile.common.v3.document.DocConvertManager;
import kr.go.mobile.common.v3.broker.BrokerManager;
import kr.go.mobile.common.v3.document.DefaultDocumentActivity;
import kr.go.mobile.common.v3.utils.TC_LOG;

/**
 * 공통기반 네이티브 API
 * <ol>
 *     <li>공통기반 초기화</li>
 *     <li>인증정보 요청</li>
 *     <li>기관 서비스 연계 요청</li>
 *     <li>문서변환 요청</li>
 *     <li>기본 문서뷰어 연동</li>
 * </ol>
 */
public class CommonBasedAPI {

    private static final String TAG = CommonBasedAPI.class.getSimpleName();
    private static final int STATUS_NONE = 4 >> 3; // 000
    private static final int STATUS_BROKER_READY = 4 >> 2; // 01
    private static final int STATUS_INTEGRITY_APP = 4 >> 1; // 10
    private static final int STATUS_ALL_READY = STATUS_BROKER_READY | STATUS_INTEGRITY_APP; // 4 >> 0; // 11
    private static CommonBasedAPI baseAPI;

    static void initialize(CBApplication api, String packageName, String apiVersion, boolean installedLauncher, boolean grantedPermission, int countStackTrace) {
        CommonBasedAPI common = new CommonBasedAPI(api);
        common.mThisPackageName = packageName;
        common.mApiVersionName = apiVersion;
        common.installedLauncher = installedLauncher;
        common.grantedPermission = grantedPermission;
        common.countStackTrace = countStackTrace;
        CommonBasedAPI.baseAPI = common;
    }

    static CommonBasedAPI getInstance() {
        if(CommonBasedAPI.baseAPI == null) {
            // MegApplication 클래스 또는 MegApplication 을 상속받은 Application 을 Manifest 의 Application 에 선언해야한다.
            throw new IllegalStateException("공통기반 모듈이 초기화되지 않았습니다. 개발 가이드 문서의 <AndroidManifest> 설정을 참고하시기 바랍니다.");
        }
        return CommonBasedAPI.baseAPI;
    }

    /**
     * 행정앱이 실행할 때 최초 실행해야하는 기능으로 이 메소드 호출로 공통기반 서비스의 로그인 기능을 연동할 수 있다.
     *
     * @param callerActivity 공통기반 초기화를 호출하는 Activity 객체
     * @param requestCode 공통기반 초기화 요청시 개발자가 지정하는 코드값 (요청에 대한 응답 확인시 확인함)
     *
     */
    public static void startCommonBaseInitActivityForResult(final Activity callerActivity, final int requestCode) {
        Log.d(TAG, "---- 보안 에이전트 서비스 연결 요청");
        ((CBApplication)callerActivity.getApplication()).bindBrokerService();
        ((CBApplication)callerActivity.getApplication()).bindMonitorService();

        int result_code = CommonBasedConstants.RESULT_OK;
        final AtomicInteger status = new AtomicInteger(STATUS_NONE);
        try {
            if (!getInstance().installedLauncher) {
                TC_LOG.e("", "보안 Agent 설치 확인 : 실패");
                result_code = CommonBasedConstants.RESULT_COMMON_NOT_INSTALLED_AGENT;
//                throw new RuntimeException(callerActivity.getString(R.string.iff_guide_not_installed_and_required_agent));
                return;
            }
            TC_LOG.i("", "보안 Agent 설치 확인 : 정상");

            if (!getInstance().grantedPermission) {
                TC_LOG.e("", "보안 Agent 권한 확인 : 실패");
                result_code = CommonBasedConstants.RESULT_COMMON_DENIED_AGENT_PERMISSION;
//                throw new RuntimeException(callerActivity.getString(R.string.iff_guide_this_app_reinstall));
                return;
            }
            TC_LOG.i("", "보안 Agent 권한 확인 : 정상");
        } catch (Exception e) {
            Log.e(TAG, "GPKI 로그인을 진행할 수 없습니다. (사유 : " + e.getMessage() +")");
            return;
        } finally {
            if (result_code != CommonBasedConstants.RESULT_OK) {
                getInstance().showDialog(callerActivity, result_code);
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                CBApplication app = (CBApplication) callerActivity.getApplication();
                do {
                    // TODO TIMEOUT 설정하여 무한으로 대기하지 않도록 한다.
                } while (app.getBroker(false) == null);
                status.getAndAdd(STATUS_BROKER_READY);
                TC_LOG.i("", "보안 Agent 서비스 연결 확인 : 정상");
            }
        }).start();

        TC_LOG.d("", "보안 토큰 획득");
        // 보안 토큰 획득 -> 보안토큰 미획득시 팝업 후 앱 종료
        EversafeHelper.getInstance().initialize(callerActivity.getString(R.string.iff_msm_url));
        new EversafeHelper.GetVerificationTokenTask() {
            @Override
            protected void onCompleted(byte[] verificationToken, String verificationTokenAsByte64, boolean isEmergency) {
                if (isEmergency) {
                    TC_LOG.e("", "보안 토큰 획득 : 실패 (Emergency) " + verificationTokenAsByte64);
                    Log.e(TAG, callerActivity.getString(R.string.iff_guide_check_network));
                    if(callerActivity.isDestroyed() || callerActivity.isFinishing()) {
                        return;
                    }
                    getInstance().showDialog(callerActivity, CommonBasedConstants.RESULT_COMMON_INTEGRITY_APP_INVALID_URL);
                } else {
                    status.getAndAdd(STATUS_INTEGRITY_APP);
                    TC_LOG.i("", "보안 토큰 획득 : 정상");
                    Log.d(TAG, String.format("보안토큰을 획득하였습니다. (%s)", verificationTokenAsByte64));
                    while (status.compareAndSet(STATUS_ALL_READY, STATUS_NONE)) {
                        getInstance().requestLaunchSecurityAgent(callerActivity, requestCode, verificationTokenAsByte64);
                    }
                }
            }

            @Override
            protected void onTimeover() {
                super.onTimeover();
                TC_LOG.e("", "보안 토큰 획득 : 실패 (타임아웃)");
                if(callerActivity.isDestroyed() || callerActivity.isFinishing()) {
                    return;
                }
                getInstance().showDialog(callerActivity, CommonBasedConstants.RESULT_COMMON_INTEGRITY_APP_TIMEOUT);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                TC_LOG.e("", "보안 토큰 획득 : 실패 (취소)");
                if(callerActivity.isDestroyed() || callerActivity.isFinishing()) {
                    return;
                }
                getInstance().showDialog(callerActivity, CommonBasedConstants.RESULT_USER_CANCELED);
            }

            @Override
            protected void onTerminated() {
                super.onTerminated();
                TC_LOG.e("", "보안 토큰 획득 : 실패");
                if(callerActivity.isDestroyed() || callerActivity.isFinishing()) {
                    return;
                }
                getInstance().showDialog(callerActivity, CommonBasedConstants.RESULT_COMMON_NOT_READY_INTEGRITY_APP);
            }
        }.setTimeout(60000).execute();
    }

    public static void onActivityResult(Activity activity, int resultCode, Intent intent) {
        // TODO
    }

    public static SSO getSSO() throws CommonBaseAPIException {
        return getInstance().getUserAuth();
    }

    /**
     * 동기 호출 방식으로 입력된 request 에 대한 응답을 response 로 응답한다. 만약, timeout 시간동안 응답이 없을 경우 exception 이 발생한다.
     *
     * @param request
     * @return Response
     */
    @Deprecated
    public static Response execute(Request request) throws CommonBaseAPIException {
        Caller caller = Caller.obtain();
        try {
            return caller.execute(request);
        } catch (ExecutionException | InterruptedException e) {
            throw new CommonBaseAPIException("동기 요청 (" + request.toString() + ") 시도를 할 수 없습니다.", e);
        } finally {
            caller.recycle();
        }
    }

    /**
     * 비동기 방식의 호출 방식으로 입력되는 request 에 대한 응답은 같이 입력된 callback 을 통해서 리턴한다.
     *
     * @param request
     * @param listener
     */
    public static void enqueue(Request request, Response.Listener listener) throws CommonBaseAPIException {
        Caller caller = Caller.obtain();
        try {
            caller.enqueue(request, listener);
        } catch (RuntimeException e) {
            caller.recycle();
            throw new CommonBaseAPIException("비동기 요청 (" + request.toString() + ") 시도를 할 수 없습니다.", e);
        }
    }

    /**
     * 공통기반에서 제공하는 기본 문서 뷰어를 호출
     *
     */
    public static void startDefaultDocViewActivity(Context context, String reqDocFileURL, String reqDocFileName, String createdDate) {
        Intent i = new Intent(context, DefaultDocumentActivity.class);
        i.putExtra(CommonBasedConstants.EXTRA_DOC_URL, reqDocFileURL);
        i.putExtra(CommonBasedConstants.EXTRA_DOC_FILE_NAME, reqDocFileName);
        i.putExtra(CommonBasedConstants.EXTRA_DOC_CREATED, createdDate);
        i.putExtra("LOG", true);
        context.startActivity(i);
    }

    public static DocConvertManager createDocConvertManager(String url, String fileName, String createdDate) throws DocConvertManager.DocConvertException {
        return DocConvertManager.create(url, fileName, createdDate);
    }

    private CBApplication api;
    private String mThisPackageName;
    private String mApiVersionName;
    private String mAgentVersionName;
    private boolean installedLauncher = false;
    private boolean grantedPermission = false;
    private int countStackTrace = 0;

    private CommonBasedAPI(CBApplication api) { this.api = api; }

    @Deprecated
    private void showDialog(final Activity callerActivity, final int error_code) {
        callerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int titleId;
                int messageId;

                switch (error_code) {
                    case CommonBasedConstants.RESULT_COMMON_NOT_INSTALLED_AGENT: {
                        titleId = R.string.iff_not_installed_required_package;
                        messageId = R.string.iff_guide_not_installed_and_required_agent;
                        break;
                    }
                    case CommonBasedConstants.RESULT_COMMON_DENIED_AGENT_PERMISSION: {
                        titleId = R.string.iff_denied_permission;
                        messageId = R.string.iff_guide_this_app_reinstall;
                        break;
                    }
                    case CommonBasedConstants.RESULT_COMMON_NOT_READY_INTEGRITY_APP: {
                        titleId = R.string.iff_not_ready_service;
                        messageId = R.string.iff_guide_this_app_restart;
                        break;
                    }
                    case CommonBasedConstants.RESULT_USER_CANCELED: {
                        titleId = R.string.iff_not_ready_service;
                        messageId = R.string.iff_guide_user_cancel;
                        break;
                    }
                    case CommonBasedConstants.RESULT_COMMON_INTEGRITY_APP_TIMEOUT:
                    case CommonBasedConstants.RESULT_COMMON_INTEGRITY_APP_INVALID_URL: {
                        titleId = R.string.iff_not_ready_service;
                        messageId = R.string.iff_guide_check_network;
                        break;
                    }
                    default:{
                        titleId = -1;
                        messageId = -1;
                    }
                }

                if(titleId > 0 && messageId > 0) {
                    api.finishThisPackage(callerActivity,
                            callerActivity.getString(titleId),
                            callerActivity.getString(messageId));
                }

            }
        });
    }

    private void requestLaunchSecurityAgent(Activity callerActivity, int requestCode, String verificationTokenByte64) {
        Intent i = new Intent(CommonBasedConstants.ACTION_LAUNCH_SECURITY_AGENT);

        i.setPackage(callerActivity.getString(R.string.iff_launcher_pkg));

        i.putExtra("extra_token", verificationTokenByte64);
        i.putExtra("api_version", mApiVersionName);
        i.putExtra("app_label", callerActivity.getString(callerActivity.getApplicationContext().getApplicationInfo().labelRes));
        i.putExtra("app_id", callerActivity.getPackageName());

        try {
            PackageInfo packageInfo = callerActivity.getPackageManager().getPackageInfo(callerActivity.getPackageName(), 0);
            i.putExtra("app_version_name", packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            i.putExtra("app_version_name", "unsupported version name");
        }
        i.putExtra("req_id", String.valueOf(Process.myUid()));
        i.putExtra("stack_trace_count", countStackTrace);
        callerActivity.startActivityForResult(i, requestCode);
    }

    String getBasicInfo() {
        return "- 보안Agent 버전: " + this.mAgentVersionName + "\n" +
                "- 공통기반 라이브러리 버전: " + this.mApiVersionName + "\n" +
                "- PACKAGE: " + this.mThisPackageName + "\n" ;
    }

    void sendEvent(int event) throws CommonBaseAPIException {
        sendEvent(event, null);
    }

    void sendEvent(int event, String message) throws CommonBaseAPIException {
//        try {
//            this.mEventMonitorManager.sendEvent(event, message);
//        } catch (RemoteException e) {
//            throw new CommonBaseAPIException("", e);
//        }
    }

    @Deprecated
    void cancel(Request request) {
        // execute 또는 enqueue 로 입력된 caller 의 request 를 비교하여 future 를 획득하여 cancel 해야함.
        // 실행된 caller 를 관리/비교 해야 함.
        // obtainCaller() 를 이용하여 획득한 Caller 를 개발자가 관리하고 필요에 따라 취소하는 것이 좋을 것 같음.
    }

    public SSO getUserAuth() throws CommonBaseAPIException {
        TC_LOG.d("sb-01", "getGPKICert 호출");

        try {
            SSO sso = SSO.create();
            TC_LOG.d("sb-01", "getGPKICert 호출 성공 : " + sso);
            return sso;
        } catch (NullPointerException e) {
            throw new CommonBaseAPIException("브로커 서비스를 재연결 중입니다.", e);
        } catch (RemoteException e) {
            TC_LOG.e("sb-01", "getGPKICert 호출 실패 : " + e.getMessage());
            throw new CommonBaseAPIException("사용자 정보 요청 중 에러가 발생하였습니다.", e);
        }
    }

    /**
     * 기관 서버의 데이터를 공통기반을 이용하여 단말로 가져옴
     */
    public void doDownload() {
        api.getBroker();
    }

    /**
     * 단말의 데이터를 공통기반을 이용하여 기관서버로 전달
     */
    public void doUpload() {
        api.getBroker();
    }

    public void destroy() {
        BrokerManager.removeAll();
    }

    public static class CommonBaseAPIException extends Exception {
        public CommonBaseAPIException(String s, Throwable e) {
            super(s, e);
        }
    }
}
