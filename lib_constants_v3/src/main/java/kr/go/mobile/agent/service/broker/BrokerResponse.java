package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

import kr.go.mobile.common.v3.CommonBasedConstants;

public class BrokerResponse<T extends MethodResponse> implements Parcelable {
    int code;
    String message;
    T obj;

    public BrokerResponse(int errorCode, String errorMessage) {
        this(errorCode, errorMessage, null);
    }

    public BrokerResponse(T obj) {
        this(CommonBasedConstants.BROKER_ERROR_NONE, null, obj);
    }

    BrokerResponse(int code, String message, T obj) {
        this.code = code;
        this.message = message;
        this.obj = obj;
    }

    public boolean ok() {
        return this.getCode() == CommonBasedConstants.BROKER_ERROR_NONE;
    }

    public int getCode() {
        return code;
    }

    public String getErrorMessage() {
        if (code > 0)
            return message;
        else
            return null;
    }

    public T getResult() {
        return obj;
    }

    protected BrokerResponse(Parcel in) {
        code = in.readInt();
        message = in.readString();
        obj =  in.readParcelable(getClass().getClassLoader());
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
        dest.writeString(message);
        dest.writeParcelable(obj, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BrokerResponse> CREATOR = new Creator<BrokerResponse>() {
        @Override
        public BrokerResponse<?> createFromParcel(Parcel in) {
            return new BrokerResponse<>(in);
        }

        @Override
        public BrokerResponse<?>[] newArray(int size) {
            return new BrokerResponse[size];
        }
    };
}
