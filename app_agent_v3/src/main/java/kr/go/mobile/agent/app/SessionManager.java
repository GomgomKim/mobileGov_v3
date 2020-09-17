package kr.go.mobile.agent.app;

import android.os.IBinder;

import java.util.Objects;

import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.service.session.ILocalSessionService;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.utils.Log;

public class SessionManager {

    private static final String TAG = SessionManager.class.getSimpleName();
    private static  SessionManager mInstance;

    static SessionManager create(IBinder service) {
        SessionManager.mInstance = new SessionManager((ILocalSessionService) service);
        return SessionManager.mInstance;
    }

    static void destroy() {
        SessionManager.mInstance = null;
    }

    private ILocalSessionService session;

    public SessionManager(ILocalSessionService service) {
        Log.concurrency(Thread.currentThread(), "new SessionManager");
        this.session = service;
    }

    public void validSignedSession() throws SessionException {
        try {
            Log.concurrency(Thread.currentThread(), "SessionManager.validSignedSession");
            session.validSignedSession();
        } catch (UserSigned.ExpiredException e) {
            throw new SessionException(SessionException.EXPIRED_SIGNED_SESSION, "서명 세션이 만료되었습니다. 다시 인증서 로그인이 필요합니다.");
        } catch (NullPointerException e) {
            throw new SessionException(SessionException.NO_SIGNED_SESSION, "서명 세션 정보가 없습니다.");
        }
    }

    public void registerAuthSession(UserAuthentication authentication) throws SessionException {
        String signedDN = session.getUserDN();
        String authDN = authentication.userDN;
        if (Objects.equals(signedDN, authDN)) {
            Log.concurrency(Thread.currentThread(), "SessionManager.registerAuthSession");
            session.registerAuthentication(authentication);
        } else {
            throw new SessionException(SessionException.AUTH_MISMATCH_USER_DN, "사용자 정보가 변경되었습니다.");
        }
    }

    public UserSigned getUserSigned() {
        Log.concurrency(Thread.currentThread(), "SessionManager.getUserSigned");
        return session.getUserSigned();
    }

    public String getUserDN() {
        Log.concurrency(Thread.currentThread(), "SessionManager.getUserDN");
        return this.session.getUserDN();
    }

    public String getUserId() throws NullPointerException {
        Log.concurrency(Thread.currentThread(), "SessionManager.getUserId");
        return this.session.getUserID();
    }

    public UserAuthentication getUserAuthentication() {
        return this.session.getUserAuthentication();
    }

    public static class SessionException extends Exception {

        public static final int AUTH_NO_SESSION = 200;
        public static final int AUTH_MISMATCH_USER_DN = 201;

        public static final int NO_SIGNED_SESSION = 300;
        public static final int EXPIRED_SIGNED_SESSION = 301;

        int expiredType;

        public SessionException(int type, String message) {
            super(message);
            expiredType = type;
        }

        public SessionException(int type, String message, Throwable t) {
            super(message, t);
            expiredType = type;

        }

        public int getExpiredType() {
            return this.expiredType;
        }
    }
}
