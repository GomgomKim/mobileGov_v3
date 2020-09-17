package kr.go.mobile.mobp.mff.lib.util;

import java.util.Locale;

public class E2ESetting {
	public static final String DATA_TYPE_JSON = "json";
	public static final String DATA_TYPE_XML = "xml";
	
	public static final String CONNECTION_TYPE_HTTPS = "https";
	public static final String CONNECTION_TYPE_HTTP = "http";
	
	public static final String CONTEXT_URL = "mois/rpc";
	
	/////////////////////////////////////////////////////////////////////////////

	
	/** APK 다운 Setting **/
	private static String appDownForderPath = "/download/MFF/";				//APK 다운로드 폴더
	private static String appDownPath = appDownForderPath+"MFF.apk";		//APK 경로
	
	/** DATA 저장 Setting **/
	public static final String dataForderPath = "/.MFF/";				//데이터 임시 생성 폴더
	
	/** 변수 **/
	private static String testYN = "N";										//테스트 버전 유무
	
	private static long sessionTime = System.currentTimeMillis();				//session time
	private static String sessionActivity = "";									//session activity
	private static String userDN = "";											//user DN정보
	private static String userDNWithDeviceID = "";											//user DN정보 + DEVICE ID
	private static String userId = "";											//userId
	private static boolean guideNewYn = false;									//공지사항 신규등록 여부

	private static String officeName = "";										//부서이름
	private static String officeCode = "";										//부서코드
	private static String departmentName = "";									//상위부서명
	private static String departmentCode = "";									//상위부서코드
	private static String nickName = "";										//별명
	
	/** 상수  **/
	public static final String MFF_KEY= "MFF_MAIN_KEY";
	public static final String MFF_CATEGORY = "android.intent.category.mobp.mff";
	public static final String IFF_CATEGORY = "android.intent.category.mobp.iff";
	public static final Locale LOCALE = Locale.KOREA;							//국가코드
	public static final String OSTYPE = "A";									//OS타입 고정
	public static final String DEVICEVERSION = android.os.Build.VERSION.RELEASE;//OS버전 고정
	
	public static final int NETWORK_ERROR_CODE = -100;
	public static final int SERVER_ERROR_CODE = -107;
	public static final int DATA_ERROR_CODE = -109;
	public static final String NETWORK_ERROR_MESSAGE = "네트워크 상태 오류입니다.";
	public static final String SERVER_ERROR_MESSAGE = "서버 연결에 실패 하였습니다.";
	public static final String DATA_ERROR_MESSAGE = "데이터 처리 중 오류가 발생했습니다.";
	public static final String PUBLIC_ERROR_MESSAGE = "잠시 후 다시 시도해 주세요.";
	public static final String VPN_SSL_ERROR_MESSAGE = "(VPN 연결 실패) 네트워크 연결을 확인해 주세요.";
	public static final String VPN_CERT_ERROR_MESSAGE = "인증에 실패했습니다.유효한 사용자인지 확인 해 주시기 바랍니다.";
	
//	public static final String MENU1 = "행정용 앱스토어";
//	public static final String MENU2 = "인증센터";
//	public static final String MENU3 = "공지사항";
//	public static final String MENU4 = "화면보호기 설정";
//	public static final String MENU5 = "이용안내";

	
	/** 필수 어플 패키지 정보**/
	public static final String FILE_DOWNLOAD_SERVICE = "CMM_FILE_DOWNLOAD"; // 앱 패키지 파일 다운로드
	public static final String FILE_UPLOAD_SERVICE = "CMM_FILE_UPLOAD";
	public static final String DOCUMENT_SERVICE = "CMM_DOC_IMAGE_LOAD";
	// 2016.11.04 윤기현 - 요청에 의하여 CMM_CERT_AUTH 값을 CMM_CERT_AUTH_MAM으로 변경함.  
	public static final String CERT_AUTH_SERVICE = "CMM_CERT_AUTH_MAM";
	
	public static final String APPBOARD_GET_LIST = "CMM_APPBOARD_GET_LIST";
	public static final String APPBOARD_GET_DETAIL = "CMM_APPBOARD_GET_DETAIL";
	public static final String APPBOARD_ADD = "CMM_APPBOARD_ADD";
	public static final String APPBOARD_MODIFY = "CMM_APPBOARD_MODIFY";
	public static final String APPBOARD_REMOVE = "CMM_APPBOARD_REMOVE";
	
	public static final String REPORT_APP_INSTALL = "CMM_REPORT_APP_INSTALL";
	public static final String REPORT_APP_UPDATE = "CMM_REPORT_APP_UPDATE";
	public static final String REPORT_APP_UNINSTALL = "CMM_REPORT_APP_UNINSTALL";
	public static final String REPORT_FILE_DOWNLOAD = "CMM_REPORT_FILE_DOWNLOAD";
	public static final String REPORT_FILE_UPLOAD = "CMM_REPORT_FILE_UPLOAD";
	
	//msjo@dkitec.com 2015-10-14
	public static final String ZIP_LIST_SERVICE = "CMM_DOC_ZIP_LIST";
	
	//msjo@dkitec.com 2015-05-27
	private static String cn = "";	

	
	public void destroy(){
		setUserId("");
		setUserDN("");
		setOfficeName("");
        setOfficeCode("0000000");
		setSessionTime(System.currentTimeMillis());
	}
	
	public String getHostUrl() {
		return "__DEFAULT__";
	}


	public String getAppDownForderPath() {
		return appDownForderPath;
	}

	public void setAppDownForderPath(String appDownForderPath) {
		this.appDownForderPath = appDownForderPath;
	}
	
	public String getAppDownPath() {
		return appDownPath;
	}

	public void setAppDownPath(String appDownPath) {
		this.appDownPath = appDownPath;
	}
	
	public String getTestYN() {
		return testYN;
	}

	public void setTestYN(String testYN) {
		this.testYN = testYN;
	}
	
	public long getSessionTime() {
		return sessionTime;
	}

	public void setSessionTime(long sessionTime) {
		this.sessionTime = sessionTime;
	}
	
//	public String getSessionActivity() {
//		return sessionActivity;
//	}
//
//	public void setSessionActivity(String sessionActivity) {
//		this.sessionActivity = sessionActivity;
//	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getUserDN() {
		return userDN;
	}

	public void setUserDN(String userDN) {
		this.userDN = userDN;
	}
	
	public String getUserDNWithDeviceID() {
		return userDNWithDeviceID;
	}

	public void setUserDNWithDeviceID(String userDN) {
		this.userDNWithDeviceID = userDN;
	}
	
	
	public String getOfficeCode() {
		return officeCode;
	}

	public void setOfficeCode(String officeCode) {
		this.officeCode = officeCode;
	}
	
	public String getOfficeName() {
		return officeName;
	}

	public void setOfficeName(String officeName) {
		this.officeName = officeName;
	}
	
	public String getDepartmentName() {
		return departmentName;
	}

	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}
	
	public String getDepartmentCode() {
		return departmentCode;
	}

	public void setDepartmentCode(String departmentCode) {
		this.departmentCode = departmentCode;
	}
	
	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	
	public boolean getGuideNewYn() {
		return guideNewYn;
	}

	public void setGuideNewYn(boolean guideNewYn) {
		this.guideNewYn = guideNewYn;
	}
	
	
	public void setCN(final String cn) {
		this.cn = cn;
	}
	
	public String getCN(){
		return this.cn;
	}
}
