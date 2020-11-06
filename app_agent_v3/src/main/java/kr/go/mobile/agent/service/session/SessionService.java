package kr.go.mobile.agent.service.session;

import android.app.Service;
import android.content.Intent;

import android.os.Binder;
import android.os.IBinder;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;

import kr.go.mobile.agent.app.SessionManager;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.mobp.iff.R;


/**
 * 공통 기반 서비스를 사용하기 위한 세션 정보를 관리하는 서비스이다.
 * 공통기반에서 사용되는 세션는 아래와 같이 정의된다.
 * - User Session (서명 세션): 로그인 후 획득한 서명 데이터를 유지함.
 * - UserAuthentication Session (인증 세션): 인증서버로부터 획득한 데이터를 유지함.
 */
public class SessionService extends Service {

    static final String TAG = SessionService.class.getSimpleName();

    public static final int RESULT_SIGNED_REGISTER_OK = 5000;
    public static final int RESULT_SIGNED_REGISTER_FAIL = 5001;

    public static class InvalidatedAuthException extends Exception {
        public InvalidatedAuthException(String message) {
            super(message);
        }
    }

    private class SessionServiceBinder extends Binder implements ILocalSessionService {
        @Override
        public String getUserID() {
            return userSigned.getUserID();
        }

        @Override
        public String getUserDN() {
            return userSigned.getUserDN();
        }

        @Override
        public void registerSigned(UserSigned signed) {
            userSigned.setSigned(signed);
        }

        @Override
        public void registerAuthentication(UserAuthentication newAuthentication) {
           authentication = newAuthentication;
        }

        @Override
        public UserAuthentication getUserAuthentication() {
            return authentication;
        }

        @Override
        public UserSigned getUserSigned() {
            if (userSigned == null) {
                try {
                    userSigned = new UserSigned(SessionService.this, SolutionManager.DREAM_SECURITY_GPKI_LOGIN);
                } catch (SolutionManager.ModuleNotFoundException e) {
                    throw new RuntimeException("보안 솔루션 모듈 로드를 실패하였습니다. (솔루션 : " + e.getNotFoundSolutionSimpleName()  +")", e);
                }
            }
            return userSigned;
        }

        @Override
        public void clear() {
            if (authentication != null)
                authentication.reset();
            if (userSigned != null) {
                userSigned.clear();
            }
        }

        @Override
        public void confirm(UserAuthentication auth) throws InvalidatedAuthException {
            final int AUTH_STATE_ELSE = 9;
            final int AUTH_STATE_SUCCESS = 0;
            String resultMsg = "인증 실패";
            if(auth.relayServerOK()) {
                /*verifyStateCert가 NULL 로 오는 경우(비정상적인 경우)가 있을 수 있으므로
                 * certStateValued의 초기 상태값을 AUTH_STATE_ELSE=9 로 설정
                 * 0으로 초기값 선택시  ldapArray, certArray의 0번 화면 출력 메세지가 검증 성공이기 때문에
                 * 메세지가 잘못 출력될 가능성이 있음
                 */
                int verifyStateValue = AUTH_STATE_ELSE;
                int certStateValue = AUTH_STATE_ELSE;
                int ldapStateValue = AUTH_STATE_ELSE;
                String[] ldapArray = getResources().getStringArray(R.array.ldap_message_set);
                String[] certArray = getResources().getStringArray(R.array.cert_message_set);

                try {
                    verifyStateValue = Integer.parseInt(auth.verifyState);
                    certStateValue = Integer.parseInt(auth.verifyStateCert);
                    ldapStateValue = Integer.parseInt(auth.verifyStateLDAP);
                } catch (NullPointerException | NumberFormatException ignored){
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

    private UserSigned userSigned = null;
    private UserAuthentication authentication = null;
    private ResultReceiver resultReceiver;
    SessionServiceBinder localBinder;

    @Override
    public void onCreate() {
        localBinder = new SessionServiceBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        this.resultReceiver = intent.getParcelableExtra("result");
        return localBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        try {
            if (intent.getAction().equals("auth_session_clean")) {
                localBinder.getUserAuthentication().reset();
                return START_NOT_STICKY; // 서비스가 재시작하지 않음.
            }

            if (this.resultReceiver == null) {
                Log.e(TAG, "서명 등록에 대한 응답값을 받을 수 없습니다. ");
                throw new Exception("서명 등록에 대한 응답값을 받을 수 없습니다. ");
            }
            if (intent.getExtras() == null) {
                Log.e(TAG, "사용자 서명이 존재하지 않습니다.");
                throw new Exception("사용자 서명이 존재하지 않습니다.");
            }
            userSigned = intent.getParcelableExtra("signed_data");
            if (userSigned == null) {
                throw new Exception("사용자 서명이 존재하지 않습니다.");
            }
            Log.i(TAG, "사용자 서명 등록 성공");
            if (flag == START_FLAG_REDELIVERY) {
                Log.w(TAG, "사용자 서명 재등록");
            }
            resultReceiver.send(RESULT_SIGNED_REGISTER_OK, null);
            return START_REDELIVER_INTENT; // 서비스가 재시작될 경우 현재 인텐트를 그대로 전달
        } catch (Exception e) {
            Log.e(TAG, "사용자 서명 등록이 실패하였습니다. - " + e.getMessage(), e);
            resultReceiver.send(RESULT_SIGNED_REGISTER_FAIL, null);
            return START_NOT_STICKY; // 서비스가 재시작하지 않음.
        }
    }
}
