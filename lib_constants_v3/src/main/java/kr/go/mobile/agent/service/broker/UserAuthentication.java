package kr.go.mobile.agent.service.broker;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

public class UserAuthentication extends MethodResponse {

    public String userDN; // Distinguished Name
    public String userID; // Common Name
    public String ouName; // OrganizationalUnit Name
    public String ouCode; // OrganizationalUnit Code
    public String departmentName; //
    public String departmentCode; //
    public String nickName; //

    public String verifyState;
    public String verifyStateCert;
    public String verifyStateLDAP;


    public UserAuthentication() {
        super();
    }

    protected UserAuthentication(Parcel in) {
        super(in);
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

    public void parseData(String data) throws JSONException {
        JSONObject jsonData = new JSONObject(data);
        this.verifyState = jsonData.get("verifyState").toString();
        this.verifyStateCert = jsonData.get("verifyStateCert").toString();
        this.verifyStateLDAP = jsonData.get("verifyStateLDAP").toString();
        this.userID = jsonData.get("cn").toString();
        this.userDN = jsonData.get("dn").toString();
        this.ouName = jsonData.get("ou").toString();
        this.ouCode = jsonData.get("oucode").toString();
        this.departmentName = jsonData.get("companyName").toString();
        this.departmentCode = jsonData.get("topOuCode").toString();
        this.nickName = jsonData.get("displayName").toString();
    }
}
