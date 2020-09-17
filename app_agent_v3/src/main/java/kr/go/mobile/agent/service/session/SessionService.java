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
        private UserSigned userSigned = null;
        private UserAuthentication authentication = null;

        @Override
        public String getUserID() {
            return this.userSigned.getUserID();
        }

        @Override
        public String getUserDN() {
            return this.userSigned.getUserDN();
        }

        @Override
        public void validSignedSession() throws UserSigned.ExpiredException {
            if (userSigned != null) {
                userSigned.validSession();
            } else {
                throw new NullPointerException();
            }
        }

        @Override
        public void registerAuthentication(UserAuthentication authentication) {
            if (Objects.equals(authentication.userDN, userSigned.getUserDN())) {
                this.authentication = authentication;
                sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_AUTH_REGISTERED_OK);
            } else {
                sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_SIGNED_REGISTERED_ERROR,
                        "사용자가 변경되어 인증서 재로그인이 필요합니다.");
            }
        }

        @Override
        public UserAuthentication getUserAuthentication() {
            return this.authentication;
        }

        @Override
        public UserSigned getUserSigned() { return this.userSigned; }

//        @Override
        public void clearUserAuthentication() {
            if (this.authentication != null)
                this.authentication.clear();
        }
    }

    SessionServiceBinder binder = new SessionServiceBinder();
    private ResultReceiver resultReceiver;
    @Override
    public void onCreate() {
        Log.i(TAG, "세션 서비스 생성");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "세션 서비스 바인딩 요청");
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
            binder.userSigned = intent.getParcelableExtra("signed_data");
            if (binder.userSigned == null) {
                throw new Exception("사용자 서명이 존재하지 않습니다.");
            }
            Log.i(TAG, "사용자 서명 등록 성공");
            if (flag == START_FLAG_REDELIVERY) {
                Log.w(TAG, "사용자 서명 재등록");
            }
            resultReceiver.send(RESULT_SIGNED_REGISTER_OK, null);
//            sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_SIGNED_REGISTERED_OK);
            return START_REDELIVER_INTENT; // 서비스가 재시작될 경우 현재 인텐트를 그대로 전달
        } catch (Exception e) {
            Log.e(TAG, "사용자 서명 등록이 실패하였습니다. - " + e.getMessage(), e);
            resultReceiver.send(RESULT_SIGNED_REGISTER_FAIL, null);
//            sendBroadcastToActivity(CommonBaseInitActivity.EVENT_TYPE_SIGNED_REGISTERED_ERROR, e.getMessage());
            return START_NOT_STICKY; // 서비스가 재시작하지 않음.
        }
    }


    @Deprecated
    public void sendBroadcastToActivity(int event) {
        sendBroadcastToActivity(event, "");
    }

    @Deprecated
    public void sendBroadcastToActivity(int event, String message) {
        Intent i = new Intent(CommonBaseInitActivity.ACTION_EVENT);
        i.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        i.putExtra(CommonBaseInitActivity.ACTION_EXTRA_TYPE, event);
        i.putExtra(CommonBaseInitActivity.ACTION_EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
}
