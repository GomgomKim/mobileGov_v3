package kr.go.mobile.common.v3;

import android.app.Activity;
import android.content.Context;

public class CommonBasedConstants {

    public static final String ACTION_LAUNCH_SECURITY_AGENT = "kr.go.mobile.action.LAUNCH_SECURITY_AGENT";

    public static final String BROKER_ACTION_LOAD_DOCUMENT = "document_load";
    public static final String BROKER_ACTION_CONVERT_STATUS_DOC = "convert_doc_status";
    public static final String BROKER_ACTION_FILE_UPLOAD = "upload";

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
    public static final int CMD_FORCE_KILL_DISABLED_SECURE_NETWORK = 101;

    // 공통기반 초기화에 대한 응답 값
    // 정상
    public static final int RESULT_OK = Activity.RESULT_OK;
    // 사용자 취소
    public static final int RESULT_USER_CANCELED = Activity.RESULT_CANCELED;
    // 공통기반 라이브러리에서 사용되는 실패 값 /////////////////////////
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

    /**
     * 사용자 ID (예. 000홍길동000)
     */
    public static final String EXTRA_KEY_USER_ID = "extra_user_id";
    /**
     * 사용자. 조직에 대한 식별 정보가 있는 문자열 (예, cn=000홍길동000,ou=people,ou=행정안전부,o=Government of Korea,c=KR)
     */
    public static final String EXTRA_KEY_DN = "extra_dn";

    public static final String EXTRA_DOC_URL = "extra_document_url";
    public static final String EXTRA_DOC_FILE_NAME = "extra_document_filename";
    public static final String EXTRA_DOC_CREATED = "extra_document_file_created_date";

    /*
    Broker 및 Relay에서 사용하는 오류 코드 정리
    Tom 200922
     */

    /**
     * 보안 네트워크 미연결 상태
     */
    public static final int BROKER_ERROR_SECURE_NETWORK_DISCONNECTION = 1004;
    /**
     * 브로커 서비스 처리  - 정상
     */
    public static final int BROKER_ERROR_NONE = 1000;
    /**
     * 브로커 서비스 데이터 처리 에러
     */
    public static final int BROKER_ERROR_PROC_DATA = 1001;
    /**
     * 브로커 서비스 행정 서버 에러
     * HTTP 응답 에러
     */
    public static final int BROKER_ERROR_HTTP_RESPONSE = 1002;
    /**
     * 브로커 서비스 공통기반 시스템 에러
     * result 값이 1로 오지 않을 경우
     */
    public static final int BROKER_ERROR_RESP_COMMON_DATA  = 1003;
    /**
     * Request 형식 에러
     */
    public static final int BROKER_ERROR_REQUEST_FORM  = 1004;
    /**
     * 중계 서버에서 처리 중 에러가 발생
     */
    public static final int BROKER_ERROR_RELAY_SERVER = 1005;


    public static final int HYBRID_ERROR_NONE = 9000;
    /**
     * 네이티브 호출을 위한 파라미터 에러
     */
    public static final int HYBRID_ERROR_NATIVE_CALL_PARAMETER = 9001;
    /**
     * 요청하는 플러그인을 찾을 수 없음
     */
    public static final int HYBRID_ERROR_PLUGIN_NOT_FOUND = 9002;
    /**
     * 호출 객제 생성 에러
     */
    public static final int HYBRID_ERROR_NEW_INSTANCE = 9003;
    /**
     * 함수 호출 에러
     */
    public static final int HYBRID_ERROR_METHOD_CALL = 9004;

    public static final int HYBRID_ERROR_JSON_EXPR = 9005;

    public static final int HYBRID_ERROR_PERMISSION_DENIED = 9005;

    public static final int HYBRID_ERROR_AIRPLANE_MODE = 9006;

    public static final int HYBRID_ERROR_NO_SIM = 9007;

    public static final int HYBRID_ERROR_INVALID_PARAMETER = 9008;

    public static final int HYBRID_ERROR_UNKNOWN = 9009;
    
    public static final int HYBRID_ERROR_GPS_IS_NOT_AVAILABLE = 9010;
    public static final int HYBRID_ERROR_GPS_TIMEOUT = 9011;
    public static final int HYBRID_ERROR_GENERIC_FAILURE = 9012;
    public static final int HYBRID_ERROR_RADIO_OFF = 9013;
    public static final int HYBRID_ERROR_SMS_NULL_PDU = 9014;
    public static final int HYBRID_ERROR_NO_SMS_SERVICE = 9015;
    public static final int HYBRID_ERROR_GPS_NO_DATA = 9016;
    public static final int HYBRID_ERROR_DELETE_CALLBACK = 9018;

    public static final int CONVERT_DOC_ERROR_CAN_NOT_STATUS = 10000;
    public static final int CONVERT_DOC_ERROR_CAN_NOT_CONVERT = 10001;

}
