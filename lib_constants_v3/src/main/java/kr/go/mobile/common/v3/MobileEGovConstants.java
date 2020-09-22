package kr.go.mobile.common.v3;

import android.app.Activity;
import android.content.Context;

public class MobileEGovConstants {

    public static final String ACTION_LAUNCH_SECURITY_AGENT = "kr.go.mobile.action.LAUNCH_SECURITY_AGENT";

    public static final int RESPONSE_ERROR_NONE = 0;
    public static final int RESPONSE_ERROR_TIMEOUT = 1;

    // EVENT : 행정앱에서 보안에이전트로 보내는 이벤트
    public static final int EVENT_COMMAND_HANDLER_UNREGISTERED = 0;
    public static final int EVENT_COMMAND_HANDLER_REGISTERED = 1;
    
    public static final int EVENT_ACTIVITY_STOPPED = 10;
    public static final int EVENT_ACTIVITY_PAUSED = 11;
    public static final int EVENT_ACTIVITY_RESUMED = 12;
    public static final int EVENT_ACTIVITY_STARTED = 13;
    public static final int EVENT_ACTIVITY_DESTROYED = 14;

    // CMD : 보안 에이전트에서 행정앱으로 보내는 명령
    public static final int CMD_FORCE_KILL_POLICY_VIOLATION = 100;
    public static final int CMD_SECURE_AGENT_VERSION = 101;

    // 정상
    public static final int RESULT_OK = Activity.RESULT_OK;
    // 사용자 취소
    public static final int RESULT_USER_CANCELED = Activity.RESULT_CANCELED;
    // 공통기반 라이브러리에서 러틴하는 실패 값 /////////////////////////
    public static final int RESULT_COMMON_NOT_INSTALLED_AGENT = 20;
    public static final int RESULT_COMMON_DENIED_AGENT_PERMISSION = 21;
    public static final int RESULT_COMMON_NOT_READY_INTEGRITY_APP = 22;
    public static final int RESULT_COMMON_INTEGRITY_APP_TIMEOUT = 23;
    public static final int RESULT_COMMON_INTEGRITY_APP_INVALID_URL = 24;
    /////////////////////////////////////////////////////////////////

    /**
     * 보안 Agent 내부 에러로 인한 실패
     */
    public static final int RESULT_AGENT_INTERNAL_ERROR = 49000;
    /**
     * 공통기반 초기화 요청시 잘못된 접근
     *  - 필수 정보 에러
     *  - 필수 권한 거부
     */
    public static final int RESULT_AGENT_INVALID = 49001;
    /**
     * 단말의 상태가 안전하지 않습니다.
     *  - 루팅 단말
     *  - 단말내에 악성코드 존재
     */
    public static final int RESULT_AGENT_UNSAFE_DEVICE = 49002;
    /**
     * 공통기반 보안 솔루션 중 라이센스가 만료되어 삭제해야 하는 앱이 존재합니다.
     */
    public static final int RESULT_AGENT_EXIST_LICENSE_EXPIRED_PACKAGE = 49003;
    /**
     * 공통기반 보안 솔루션의 앱을 설치해야합니다.
     */
    public static final int RESULT_AGENT_INSTALL_REQUIRED_PACKAGE = 49004;
    /**
     * 공통기반 보안 솔루션 연계 실패
     */
    public static final int RESULT_AGENT_SOLUTION_ERROR = 49005;
    /**
     * 인증 실패
     */
    public static final int RESULT_AGENT_FAILURE_USER_AUTHENTICATION = 49005;

    public static final String EXTRA_KEY_USER_ID = "extra_user_id";
    public static final String EXTRA_KEY_DN = "extra_dn";



}
