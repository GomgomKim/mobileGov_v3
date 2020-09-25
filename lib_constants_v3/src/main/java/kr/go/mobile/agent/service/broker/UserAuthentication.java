package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class UserAuthentication implements Parcelable {


    public static final Creator<UserAuthentication> CREATOR = new Creator<UserAuthentication>() {
        @Override
        public UserAuthentication createFromParcel(Parcel in) {
            return new UserAuthentication(in);
        }

        @Override
        public UserAuthentication[] newArray(int size) {
            return new UserAuthentication[size];
        }
    };

    public String userDN; // Distinguished Name
    public String userID; // Common Name
    public String ouName; // OrganizationalUnit Name
    public String ouCode; // OrganizationalUnit Code
    public String departmentName; //
    public String departmentCode; //
    public String nickName; //

    public String result;
    public String verifyState;
    public String verifyStateCert;
    public String verifyStateLDAP;

    public String code;
    public String msg;
    public UserAuthentication() { }

    protected UserAuthentication(Parcel in) {
        userDN = in.readString();
        userID = in.readString();
        ouName = in.readString();
        ouCode = in.readString();
        departmentName = in.readString();
        departmentCode = in.readString();
        nickName = in.readString();
    }

    public String getUserDN() {
        return userDN;
    }

    public String getUserID() {
        return userID;
    }

    public String getOuName() {
        return ouName;
    }

    public String getOuCode() {
        return ouCode;
    }

    public String getNickName() {
        return nickName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void clear() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userDN);
        dest.writeString(userID);
        dest.writeString(ouName);
        dest.writeString(ouCode);
        dest.writeString(departmentName);
        dest.writeString(departmentCode);
        dest.writeString(nickName);
    }
}
