package kr.go.mobile.common.v3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
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
    private static final int STATUS_NONE = 4 >> 3; // 0 (000)
    private static final int STATUS_ERROR = 4 >> 0; // 4 (100)
    private static final int STATUS_BROKER_READY = 4 >> 1; // 2 (010)
    private static final int STATUS_INTEGRITY_APP = 4 >> 2; // 1 (001)
    private static final int STATUS_ALL_READY = STATUS_BROKER_READY | STATUS_INTEGRITY_APP; // 3 (011)
    private static boolean isFinish = false;
    private static CommonBasedAPI baseAPI;

    private static Thread t;
    private static Thread w;

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
    public static void startInitActivityForResult(final Activity callerActivity, final int requestCode) {
        isFinish = true;
        if (t != null) {
            t.interrupt();
            t = null;
        }
        if (w != null) {
            w.interrupt();
            w = null;
        }

        ((CBApplication)callerActivity.getApplication()).bindBrokerService();

        int result_code = CommonBasedConstants.RESULT_OK;
        final AtomicInteger status = new AtomicInteger(STATUS_NONE);
        try {
            if (!getInstance().installedLauncher) {
                result_code = CommonBasedConstants.RESULT_COMMON_NOT_INSTALLED_AGENT;
                Utils.TC("보안 에이전트 : 미설치");
                return;
            }
            Utils.TC("보안 에이전트 : 설치");

            if (!getInstance().grantedPermission) {
                Utils.TC("브로커 서비스 사용 권한: 거부");
                result_code = CommonBasedConstants.RESULT_COMMON_DENIED_AGENT_PERMISSION;
                return;
            }
            Utils.TC("브로커 서비스 사용 권한: 획득");
        } catch (Exception e) {
            Log.e(TAG, "GPKI 로그인을 진행할 수 없습니다. (사유 : " + e.getMessage() +")");
            result_code = CommonBasedConstants.RESULT_COMMON_BIND_FAILED_SECURE_SERVICE;
        } finally {
            if (result_code != CommonBasedConstants.RESULT_OK) {
                getInstance().showDialog(callerActivity, result_code);
                return;
            }
        }

        // 지정시간동안 브로커 서비스 바인딩이 안되면 앱 종료
        t = new Thread(new Runnable() {
            int checkMillis = 100;
            @Override
            public void run() {
                int waitMillis = 0;
                while(waitMillis < checkMillis * 100 /*10초*/) {
                    try {
                        Thread.sleep(checkMillis);
                        waitMillis += checkMillis;
                    } catch (InterruptedException ignored) {
                    }
                    if (isFinish ||
                            (status.get() & STATUS_BROKER_READY) == STATUS_BROKER_READY) {
                        return;
                    }
                }
                if ((status.get() & STATUS_BROKER_READY) != STATUS_BROKER_READY ) {
                    status.getAndSet(STATUS_ERROR);

                }
            }
        });

        w = new Thread(new Runnable() {
            @Override
            public void run() {
                CBApplication app = (CBApplication) callerActivity.getApplication();
                do {
                    if (status.get() == STATUS_ERROR) {
                        getInstance().showDialog(callerActivity, CommonBasedConstants.RESULT_COMMON_BIND_FAILED_SECURE_SERVICE);
                        isFinish = true;
                        return;
                    }
                    if (isFinish) {
                        return;
                    }
                } while (app.getBroker() == null);
                status.getAndAdd(STATUS_BROKER_READY);
            }
        });

        isFinish = false;
        t.start();
        w.start();

        // 보안 토큰 획득 -> 보안토큰 미획득시 팝업 후 앱 종료
        EversafeHelper.getInstance().initialize(Utils.getMsmURL(callerActivity));
        new EversafeHelper.GetVerificationTokenTask() {
            @Override
            protected void onCompleted(byte[] verificationToken, String verificationTokenAsByte64, boolean isEmergency) {
                if (isEmergency) {
                    Log.e(TAG, callerActivity.getString(R.string.iff_guide_check_network));
                    if(callerActivity.isDestroyed() || callerActivity.isFinishing()) {
                        return;
                    }
                    getInstance().showDialog(callerActivity, CommonBasedConstants.RESULT_COMMON_INTEGRITY_APP_INVALID_URL);
                } else {
                    status.getAndAdd(STATUS_INTEGRITY_APP);
                    Utils.TC("무결성 검증 : 토큰 획득");
                    Log.d(TAG, String.format("보안토큰을 획득하였습니다. (%s)", verificationTokenAsByte64));
                    while (status.compareAndSet(STATUS_ALL_READY, STATUS_NONE)) {
                        getInstance().requestLaunchSecurityAgent(callerActivity, requestCode, verificationTokenAsByte64);
                    }
                }
            }

            @Override
            protected void onTimeover() {
                super.onTimeover();
                if(callerActivity.isDestroyed() || callerActivity.isFinishing()) {
                    return;
                }
                getInstance().showDialog(callerActivity, CommonBasedConstants.RESULT_COMMON_INTEGRITY_APP_TIMEOUT);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                if(callerActivity.isDestroyed() || callerActivity.isFinishing()) {
                    return;
                }
                getInstance().showDialog(callerActivity, CommonBasedConstants.RESULT_USER_CANCELED);
            }

            @Override
            protected void onTerminated() {
                super.onTerminated();
                if(callerActivity.isDestroyed() || callerActivity.isFinishing()) {
                    return;
                }
                getInstance().showDialog(callerActivity, CommonBasedConstants.RESULT_COMMON_NOT_READY_INTEGRITY_APP);
            }
        }.setTimeout(60000).execute();
    }

    @Deprecated
    public static String handleInitActivityResult(Activity callerActivity, int resultCode, Intent intent) throws CommonBaseAPIException {
        switch (resultCode) {
            case CommonBasedConstants.RESULT_OK: {
                Utils.TC("공통기반 서비스 : 초기화");
                // TODO 사용자 DN
                return "";
            }
            case CommonBasedConstants.RESULT_AGENT_EXIST_LICENSE_EXPIRED_PACKAGE:
            case CommonBasedConstants.RESULT_AGENT_FAILURE_USER_AUTHENTICATION:
            case CommonBasedConstants.RESULT_AGENT_INSTALL_REQUIRED_PACKAGE:
            case CommonBasedConstants.RESULT_AGENT_INTERNAL_ERROR:
            case CommonBasedConstants.RESULT_AGENT_INVALID:
            case CommonBasedConstants.RESULT_AGENT_SOLUTION_ERROR:
            case CommonBasedConstants.RESULT_AGENT_UNSAFE_DEVICE:
            default:
                // TODO 메시지 정의
                throw new CommonBaseAPIException("");
        }
    }


    /**
     * 공통기반 시스템의 인증 서버로부터 획득한 인증정보를 공유한다.
     *
     * @return SSO -
     * @throws CommonBaseAPIException
     */
    public static SSO getSSO() throws CommonBaseAPIException {
        Utils.TC("공통기반 서비스: SSO 사용자 인증");
        return getInstance().getUserAuth();
    }

    /**
     * 비동기 방식의 호출 방식으로 입력되는 request 에 대한 응답은 같이 입력된 callback 을 통해서 리턴한다.
     */
    public static void call(String serviceId, String serviceParams, Response.Listener listener) throws CommonBaseAPIException {
        switch (serviceId) {
            case CommonBasedConstants.BROKER_ACTION_LOAD_DOCUMENT:
            case CommonBasedConstants.BROKER_ACTION_CONVERT_STATUS_DOC:
                break;
            default:
                Utils.TC("공통기반 서비스: 기관서비스");
        }
        Request request = Request.basic(serviceId, serviceParams);
        getInstance().enqueue(request, listener);
    }

    /**
     * 동기 호출 방식으로 입력된 request 에 대한 응답을 response 로 응답한다. 만약, timeout 시간동안 응답이 없을 경우 exception 이 발생한다.
     *
     * @param request
     * @return Response
     */
    protected static Response execute(Request request) throws CommonBaseAPIException {
        Caller caller = Caller.obtain();
        try {
            return caller.execute(request);
        } catch (ExecutionException | InterruptedException e) {
            throw new CommonBaseAPIException("동기 요청 (" + request.toString() + ") 시도를 할 수 없습니다.", e);
        } finally {
            caller.recycle();
        }
    }

    // 제한적인 기능으로 RestrictedAPI 클래스를 이용해서 접근해야 함.
    protected static Response executeUpload(String fileName, String absolutePath, String relayUrl, String extraParams) throws CommonBaseAPIException {
        return executeUpload(fileName, Uri.parse(absolutePath), relayUrl, extraParams);
    }

    // 제한적인 기능으로 RestrictedAPI 클래스를 이용해서 접근해야 함.
    protected static Response executeUpload(String fileName, Uri targetUri, String relayUrl, String extraParams) throws CommonBaseAPIException {
        Request req = Request.upload(RestrictedAPI.key, fileName, targetUri, relayUrl, extraParams);
        if (req == null) {
            throw new CommonBasedAPI.CommonBaseAPIException("제한된 기능으로 사용할 수 없습니다.");
        }
        Utils.TC("공통기반 서비스: 업로드");
        return getInstance().doUpload(req);
    }

    // 제한적인 기능으로 RestrictedAPI 클래스를 이용해서 접근해야 함.
    protected static Response executeDownload(String relayUrl, String absolutePath) throws CommonBaseAPIException {
        Request req = Request.download(RestrictedAPI.key, relayUrl, Uri.parse(absolutePath));
        if (req == null) {
            throw new CommonBasedAPI.CommonBaseAPIException("제한된 기능으로 사용할 수 없습니다.");
        }
        Utils.TC("공통기반 서비스: 다운로드");
        return getInstance().doDownload(req);
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
        context.startActivity(i);
        Utils.TC("공통기반 서비스: 기본 문서뷰어");
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
                    case CommonBasedConstants.RESULT_COMMON_BIND_FAILED_SECURE_SERVICE: {
                        titleId = R.string.iff_not_ready_service;
                        messageId = R.string.iff_failed_secure_service;
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
        i.setPackage(Utils.getLauncherName(callerActivity));

        // 공통기반 정보
        i.putExtra("req_id", Process.myUid());
        i.putExtra("extra_token", verificationTokenByte64);
        i.putExtra("api_version", mApiVersionName);
        i.putExtra("stack_trace_count", countStackTrace);

        try {
            int labelRes = callerActivity.getPackageManager()
                    .getPackageInfo(callerActivity.getPackageName(), 0)
                    .applicationInfo.labelRes;

            i.putExtra("app_label", callerActivity.getString(labelRes));
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("실행중인 행정앱 정보를 획득할 수 없습니다.");
        }

        ((CBApplication)callerActivity.getApplication()).bindMonitorService();
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

    SSO getUserAuth() throws CommonBaseAPIException {
        try {
            return SSO.create();
        } catch (NullPointerException e) {
            throw new CommonBaseAPIException("브로커 서비스를 재연결 중입니다.", e);
        } catch (RemoteException e) {
            throw new CommonBaseAPIException("사용자 정보 요청 중 에러가 발생하였습니다.", e);
        }
    }

    void enqueue(Request request, Response.Listener listener) throws CommonBaseAPIException {
        Caller caller = Caller.obtain();
        try {
            caller.enqueue(request, listener);
        } catch (RuntimeException e) {
            caller.recycle();
            throw new CommonBaseAPIException("비동기 요청 (" + request.toString() + ") 시도를 할 수 없습니다.", e);
        }
    }

    /**
     * 단말의 데이터를 공통기반을 이용하여 기관서버로 전달
     */
    Response doUpload(Request request) throws CommonBaseAPIException {
        try {
            request.validTargetUri(api.getBaseContext());
        } catch (IOException e) {
            throw new CommonBaseAPIException("해당 파일을 읽을 수 없습니다 (" + e.getMessage() +")", e);
        }
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
     * 기관 서버의 데이터를 공통기반을 이용하여 단말로 가져옴
     */
    Response doDownload(Request request) throws CommonBaseAPIException {
        try {
            request.validTargetUri(api.getBaseContext(), false);
        } catch (IOException e) {
            throw new CommonBaseAPIException("다운로드 위치를 읽을 수 없습니다 (" + e.getMessage() +")", e);
        }
        Caller caller = Caller.obtain();
        try {
            return caller.execute(request);
        } catch (ExecutionException | InterruptedException e) {
            throw new CommonBaseAPIException("동기 요청 (" + request.toString() + ") 시도를 할 수 없습니다.", e);
        } finally {
            caller.recycle();
        }
    }

    public void destroy() {
        BrokerManager.removeAll();
    }

    public static class CommonBaseAPIException extends Exception {
        public CommonBaseAPIException(String s) {
            super(s);
        }
        public CommonBaseAPIException(String s, Throwable e) {
            super(s, e);
        }
    }

    static class Initializer {
        private CommonBasedAPI common;
        public Initializer(CBApplication api) {
            common = new CommonBasedAPI(api);
            common.mThisPackageName = api.getPackageName();
        }

        public Initializer setCommonBasedApiVersion(String apiVersion) {
            common.mApiVersionName = apiVersion;
            return this;
        }

        public Initializer setInstalledLauncher(boolean installedLauncher) {
            common.installedLauncher = installedLauncher;
            return this;
        }

        public Initializer setGrantedPermission(boolean grantedPermission) {
            common.grantedPermission = grantedPermission;
            return this;
        }

        public Initializer setCountStackTrace(int countStackTrace) {
            common.countStackTrace = countStackTrace;
            return this;
        }

        public void init() {
            CommonBasedAPI.baseAPI = common;
        }
    }
}
