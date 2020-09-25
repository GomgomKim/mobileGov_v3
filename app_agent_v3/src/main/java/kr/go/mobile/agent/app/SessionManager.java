package kr.go.mobile.agent.app;

import android.os.IBinder;

import java.util.Objects;

import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.agent.service.session.ILocalSessionService;
import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.UserAuthenticationUtils;

public class SessionManager {

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
        this.session = service;
    }

    public void registerSigned(UserSigned signed) {
        session.registerSigned(signed);
    }

    public void registerAuthSession(UserAuthentication authentication) {
        session.registerAuthentication(authentication);
    }

    public UserSigned getUserSigned() {
        return session.getUserSigned();
    }

    public String getUserDN() {
        return session.getUserDN();
    }

    public String getUserId() throws NullPointerException {
        return session.getUserID();
    }

    public UserAuthentication getUserAuthentication() {
        return session.getUserAuthentication();
    }

    public void validSignedSession() throws SessionException {
        try {
            UserSigned signed = session.getUserSigned();
            signed.validSession();
        } catch (UserSigned.ExpiredException e) {
            throw new SessionException(SessionException.EXPIRED_SIGNED_SESSION, e.getMessage());
        } catch (NullPointerException e) {
            throw new SessionException(SessionException.NO_SIGNED_SESSION, e.getMessage());
        }
    }

    public void validSession() throws SessionException {
        UserAuthentication authentication = session.getUserAuthentication();
        if (authentication == null) {
            throw new SessionException(SessionException.AUTH_NO_SESSION, "사용자 인증 정보가 존재하지 않습니다.");
        }
        try {
            UserAuthenticationUtils.confirm(authentication);
        } catch (UserAuthenticationUtils.InvalidatedAuthException e) {
            throw new SessionException(SessionException.AUTH_FAILED, "사용자 인증이 실패하였습니다. - "  +e.getMessage());
        }
        if (!Objects.equals(authentication.getUserDN(), getUserDN())) {
            throw new SessionException(SessionException.AUTH_MISMATCH_USER_DN, "사용자 정보가 변경되어 다시 로그인이 필요합니다.");
        }
    }

    public void clear() {
        session.clear();
    }


    public static class SessionException extends Exception {

        public static final int AUTH_NO_SESSION = 200;
        public static final int AUTH_MISMATCH_USER_DN = 201;
        public static final int AUTH_FAILED = 202;

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
