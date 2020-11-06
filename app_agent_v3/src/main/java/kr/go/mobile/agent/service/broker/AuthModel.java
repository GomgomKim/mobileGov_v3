package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

/*
    Retrofit2 - Gson convert return 위한 POJO
    Tom 200914
     */
@Deprecated
public class AuthModel implements Parcelable {
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

@Deprecated
class _MethodResponse implements Parcelable{
    Data data;

    protected _MethodResponse(Parcel in) {
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

class Data implements Parcelable{
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

    public void setDn(String dn){ this.dn = dn; }
    public void setUserID(String userID){ this.userID = userID; }
    public void setOuName(String ouName){ this.ouName = ouName; }
    public void setOuCode(String ouCode){ this.ouCode = ouCode; }
    public void setDepartmentName(String departmentName){ this.departmentName = departmentName; }
    public void setDepartmentCode(String departmentCode){ this.departmentCode = departmentCode; }
    public void setNickName(String nickName){ this.nickName = nickName; }
    public void setResult(String result){ this.result = result; }
    public void setVerifyState(String verifyState){ this.verifyState = verifyState; }
    public void setVerifyStateCert(String verifyStateCert){ this.verifyStateCert = verifyStateCert; }
    public void setCode(String code){ this.code = code; }
    public void setMsg(String msg){ this.msg = msg; }

    public String getDn(){ return this.dn; }
    public String getUserID(){ return this.userID; }
    public String getOuName(){ return this.ouName; }
    public String getOuCode(){ return this.ouCode; }
    public String getDepartmentName(){ return this.departmentName; }
    public String getDepartmentCode(){ return this.departmentCode; }
    public String getNickName(){ return this.nickName; }
    public String getResult(){ return this.result; }
    public String getVerifyState(){ return this.verifyState; }
    public String getVerifyStateCert(){ return this.verifyStateCert; }
    public String getCode(){ return this.code; }
    public String getMsg(){ return this.msg; }

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