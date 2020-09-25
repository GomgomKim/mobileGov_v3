package kr.go.mobile.agent.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import kr.go.mobile.agent.service.broker.UserAuthentication;

public class UserAuthenticationUtils {

    static Calendar CALENDAR = Calendar.getInstance();
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.KOREA);

    public static String generateBody(String singedBase64) throws JSONException {
        // STEP 1. 사용자 인증서 검증 요청 값 생성
        JSONObject reqValues = new JSONObject();
        reqValues.put("reqType", "1");
        reqValues.put("transactionId", DATE_FORMAT.format(CALENDAR.getTime()));
        reqValues.put("signedData", singedBase64);

        // STEP 2. 요청 파라미터 생성
        JSONObject reqParam = new JSONObject();
        // "\" -> "" 으로 변환
        reqParam.put("reqAusData", reqValues.toString().replace("\\", ""));

        // STEP 3. 요청 파라미터 목록 생성.
        JSONArray reqParamArray = new JSONArray();
        reqParamArray.put(reqParam);

        // STEP 4. 요청 파라미터 목록 값 등록
        JSONObject reqParams = new JSONObject();
        reqParams.put("params", reqParamArray);
        reqParams.put("id", DummyClient.TransactionIdGenerator.getInstance().nextKey());

        JSONObject reqMethodCall = new JSONObject();
        reqMethodCall.put("methodCall", reqParams);

        return reqMethodCall.toString();
    }

    /**
       Tom 200914
       Return data Json parsing
    */
    public static UserAuthentication parseUserAuthentication(final String resp) throws JSONException {
        UserAuthentication auth = new UserAuthentication();
        //try {
            JSONObject response = new JSONObject(new JSONObject(resp).getString("methodResponse"));
            auth.result = response.getString("result");

            JSONObject jsonData = new JSONObject(response.getString("data"));
            auth.verifyState = jsonData.get("verifyState").toString();
            auth.verifyStateCert = jsonData.get("verifyStateCert").toString();
            auth.verifyStateLDAP = jsonData.get("verifyStateLDAP").toString();
            auth.userID = jsonData.get("cn").toString();
            auth.userDN = jsonData.get("dn").toString();
            auth.ouName = jsonData.get("ou").toString();
            auth.ouCode = jsonData.get("oucode").toString();
            auth.departmentName = jsonData.get("companyName").toString();
            auth.departmentCode = jsonData.get("topOuCode").toString();
            auth.nickName = jsonData.get("displayName").toString();
            auth.code = response.getString("code");
            auth.msg = response.getString("msg");
        //} catch (JSONException e) {
            // README 여기서 예외사항을 처리하지 않고 .. 넘기면 ?? 다음에는 어떻게 될까요 ?
            // e.printStackTrace();
        //}
        return auth;
    }

    static final int AUTH_STATE_ELSE = 9;
    static final int AUTH_STATE_SUCCESS = 0;

    static final String[] ldapArray = {
            "검증성공",
            "폐지된 인증서입니다.",
            "만료된 인증서입니다.",
            "유효하지 않은 인증서입니다.",
            "","","","","",
            "인증서 확인에 실패하였습니다."};
    static final String[] certArray = {
            "검증성공",
            "조회 불가능한 LDAP정보입니다.",
            "", "", "","","","","",
            "LDAP 인증실패하였습니다."};

    public static class InvalidatedAuthException extends Throwable {
        public InvalidatedAuthException(String message) {
            super(message);
        }
    }

    public static void confirm(UserAuthentication auth) throws InvalidatedAuthException {
        String resultMsg = "인증 실패";
        if(auth.result.equals("1")) {
            /*verifyStateCert가 NULL 로 오는 경우(비정상적인 경우)가 있을 수 있으므로
             * certStateValued의 초기 상태값을 AUTH_STATE_ELSE=9 로 설정
             * 0으로 초기값 선택시  ldapArray, certArray의 0번 화면 출력 메세지가 검증 성공이기 때문에
             * 메세지가 잘못 출력될 가능성이 있음
             */
            int verifyStateValue = AUTH_STATE_ELSE;
            int certStateValue = AUTH_STATE_ELSE;
            int ldapStateValue = AUTH_STATE_ELSE;

            try{
                verifyStateValue = Integer.parseInt(auth.verifyState);
                certStateValue = Integer.parseInt(auth.verifyStateCert);
                ldapStateValue = Integer.parseInt(auth.verifyStateLDAP);
            }catch(NullPointerException | NumberFormatException e){
                //e.printStackTrace();
            }

            if (certStateValue < AUTH_STATE_SUCCESS
                    || certStateValue >= certArray.length)
                certStateValue = AUTH_STATE_ELSE;

            if (ldapStateValue < AUTH_STATE_SUCCESS
                    || ldapStateValue >= ldapArray.length)
                ldapStateValue = AUTH_STATE_ELSE;

            if (verifyStateValue == AUTH_STATE_SUCCESS) { // 통합 인증 체크
                return;
            } else if (certStateValue != AUTH_STATE_SUCCESS) { // 인증서 체크
                resultMsg = certArray[certStateValue];
            } else if (ldapStateValue != AUTH_STATE_SUCCESS) { // LDAP 체크
                resultMsg = ldapArray[ldapStateValue];
            } else {
                resultMsg = "인증 실패";
            }
        }
        throw new InvalidatedAuthException(resultMsg);
    }



}
