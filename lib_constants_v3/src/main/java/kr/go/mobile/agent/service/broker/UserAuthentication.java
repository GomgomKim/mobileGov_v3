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

    /*
    Retrofit2 - Gson convert return 위한 POJO
    Tom 200914
     */
    public static class AuthModel implements Parcelable {
        MethodResponse methodResponse;

        protected AuthModel(Parcel in) {
        }

        public static final Creator<AuthModel> CREATOR = new Creator<AuthModel>() {
            @Override
            public AuthModel createFromParcel(Parcel in) {
                return new AuthModel(in);
            }

            @Override
            public AuthModel[] newArray(int size) {
                return new AuthModel[size];
            }
        };

        public void setMethodResponse(MethodResponse methodResponse) { this.methodResponse = methodResponse; }
        public MethodResponse getMethodResponse() { return this.methodResponse;}

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
        }
        //
    }

    public static class MethodResponse implements Parcelable{
        Data data;

        protected MethodResponse(Parcel in) {
        }

        public static final Creator<MethodResponse> CREATOR = new Creator<MethodResponse>() {
            @Override
            public MethodResponse createFromParcel(Parcel in) {
                return new MethodResponse(in);
            }

            @Override
            public MethodResponse[] newArray(int size) {
                return new MethodResponse[size];
            }
        };

        public void setData(Data data) { this.data = data; }
        public Data getData() { return this.data;}

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
        }
    }

    public static class Data implements Parcelable{
        public String dn; // Distinguished Name
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

        protected Data(Parcel in) {
            dn = in.readString();
            userID = in.readString();
            ouName = in.readString();
            ouCode = in.readString();
            departmentName = in.readString();
            departmentCode = in.readString();
            nickName = in.readString();
            result = in.readString();
            verifyState = in.readString();
            verifyStateCert = in.readString();
            verifyStateLDAP = in.readString();
            code = in.readString();
            msg = in.readString();
        }

        public static final Creator<Data> CREATOR = new Creator<Data>() {
            @Override
            public Data createFromParcel(Parcel in) {
                return new Data(in);
            }

            @Override
            public Data[] newArray(int size) {
                return new Data[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(dn);
            parcel.writeString(userID);
            parcel.writeString(ouName);
            parcel.writeString(ouCode);
            parcel.writeString(departmentName);
            parcel.writeString(departmentCode);
            parcel.writeString(nickName);
            parcel.writeString(result);
            parcel.writeString(verifyState);
            parcel.writeString(verifyStateCert);
            parcel.writeString(verifyStateLDAP);
            parcel.writeString(code);
            parcel.writeString(msg);
        }
    }

}
