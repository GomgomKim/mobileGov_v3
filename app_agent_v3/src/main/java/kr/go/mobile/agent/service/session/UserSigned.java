package kr.go.mobile.agent.service.session;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class UserSigned implements Parcelable {
    private static final String TAG = UserSigned.class.getSimpleName();

    private final long mAuthSuccessTime;
    private final long mAuthMaintainTime;
    private final String mUserDN;
    private final String mSignedBase64;
    private final String mUserID;
    private final String mOfficeCode;
    private final String mOfficeName;

    public UserSigned(String dn, String signedBase64, int sessionTimeout) {
        this.mAuthSuccessTime = System.currentTimeMillis();
        this.mAuthMaintainTime = sessionTimeout * 1000;
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
                continue;
            }
        }
        this.mUserID = ui;
        this.mOfficeCode = "0000000";
        this.mOfficeName = ou;
    }


    protected UserSigned(Parcel in) {
        mAuthSuccessTime = in.readLong();
        mAuthMaintainTime = in.readLong();
        mUserDN = in.readString();
        mSignedBase64 = in.readString();
        mUserID = in.readString();
        mOfficeCode = in.readString();
        mOfficeName = in.readString();
    }

    public static final Creator<UserSigned> CREATOR = new Creator<UserSigned>() {
        @Override
        public UserSigned createFromParcel(Parcel in) {
            return new UserSigned(in);
        }

        @Override
        public UserSigned[] newArray(int size) {
            return new UserSigned[size];
        }
    };

    public void validSession() throws ExpiredException {
        if (mAuthMaintainTime == 0) {
            // 0 값은 무한대로 유지함.
            return;
        }
        if ((System.currentTimeMillis() - mAuthSuccessTime) > mAuthMaintainTime) {
            throw new ExpiredException("서명 유효 시간을 초과했습니다.");
        } else {
            Log.d(TAG, "유지시간: " + mAuthMaintainTime + " 경과시간: " + (System.currentTimeMillis() - mAuthSuccessTime));
        }
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mAuthSuccessTime);
        dest.writeLong(mAuthMaintainTime);
        dest.writeString(mUserDN);
        dest.writeString(mSignedBase64);
        dest.writeString(mUserID);
        dest.writeString(mOfficeCode);
        dest.writeString(mOfficeName);
    }

    public static class ExpiredException extends Throwable {
        public ExpiredException(String message) {
            super(message);
        }
    }
}
