package kr.go.mobile.agent.service.session;

import android.content.Context;
import android.util.Log;

import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.mobp.iff.R;

public class UserSigned {
    private static final String TAG = UserSigned.class.getSimpleName();


    public enum STATUS {
        _OK,
        _ERROR,
        _USER_CANCEL;
    }

    Solution<?, ?> certificationLoginSolution;

    private long mAuthSuccessTime;
    private long mAuthMaintainTime;
    private String mUserDN;
    private String mSignedBase64;
    private String mUserID;
    private String mOfficeCode;
    private String mOfficeName;

    public UserSigned(Context context, String solutionName) throws SolutionManager.ModuleNotFoundException {
        certificationLoginSolution = SolutionManager.initSolutionModule(context, solutionName);
        this.mAuthMaintainTime = context.getResources().getInteger(R.integer.SIGNED_SESSION_MAINTAIN_SEC) * 1000;
    }

    public UserSigned(String dn, String signedBase64) {
        this.mAuthSuccessTime = System.currentTimeMillis();

        this.mUserDN = dn;
        this.mSignedBase64 = signedBase64;

        String ui = null, ou = null;
        String[] splitData = mUserDN.split(",");
        for(String data : splitData){
            if(data.startsWith("cn=")){
                ui = data.split("cn=")[1];
                continue;
            }
            if(data.startsWith("ou=")){
                ou = data.split("ou=")[1];
            }
        }
        this.mUserID = ui;
        this.mOfficeCode = "0000000";
        this.mOfficeName = ou;
    }

    public void startLoginActivityForResult(Context context, Solution.EventListener listener) {
        certificationLoginSolution.setDefaultEventListener(listener);
        certificationLoginSolution.execute(context, true);
    }

    public void validSession() throws ExpiredException {
        if (mAuthSuccessTime == 0) {
            throw new ExpiredException("등록된 서명값이 없습니다.");
        } else if (mAuthMaintainTime > 0) {
            if ((System.currentTimeMillis() - mAuthSuccessTime) > mAuthMaintainTime) {
                clear();
                throw new ExpiredException("서명 유효 시간을 초과했습니다.");
            } else {
                Log.d(TAG, "유지시간: " + mAuthMaintainTime + " 경과시간: " + (System.currentTimeMillis() - mAuthSuccessTime));
            }
        } // else { /* mAuthMaintainTime = 0 값은 무한대로 유지함. */ }
    }

    public String getSignedBase64() {
        return this.mSignedBase64;
    }

    public String getUserDN() {
        return this.mUserDN;
    }

    public String getUserID() {
        return this.mUserID;
    }

    public String getOfficeName() {
        return mOfficeName;
    }

    public String getOfficeCode() {
        return mOfficeCode;
    }

    public void setSigned(UserSigned signed) {
        if (signed.onlySigned()) {
            this.mSignedBase64 = signed.mSignedBase64;
            this.mAuthSuccessTime = signed.mAuthSuccessTime;
            this.mOfficeCode = signed.mOfficeCode;
            this.mOfficeName = signed.mOfficeName;
            this.mUserDN = signed.mUserDN;
            this.mUserID = signed.mUserID;
            signed.clear();
        } else {
            throw new RuntimeException("UserSigned 타입이 서명 값만 존재해야 합니다.");
        }
    }

    private boolean onlySigned() {
        return certificationLoginSolution == null;
    }

    public void clear() {
        mAuthSuccessTime = 0L;
        mUserDN = null;
        mSignedBase64 = null;
        mUserID = null;
        mOfficeCode = null;
        mOfficeName = null;
    }

    public static class ExpiredException extends Throwable {
        public ExpiredException(String message) {
            super(message);
        }
    }
}
