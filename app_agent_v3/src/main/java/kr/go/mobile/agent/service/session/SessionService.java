package kr.go.mobile.agent.service.session;

import android.app.Service;
import android.content.Intent;

import android.os.Binder;
import android.os.IBinder;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Objects;

import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.v3.CommonBaseInitActivity;


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
                authentication.clear();
            if (userSigned != null) {
                userSigned.clear();
            }
        }
    }

    private UserSigned userSigned = null;
    private UserAuthentication authentication = null;
    private ResultReceiver resultReceiver;
    SessionServiceBinder binder;

    @Override
    public void onCreate() {
        binder = new SessionServiceBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        this.resultReceiver = intent.getParcelableExtra("result");
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        try {
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
