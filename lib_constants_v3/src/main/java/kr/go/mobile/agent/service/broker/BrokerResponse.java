package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

public class BrokerResponse<T> implements Parcelable {
    int code;
    String message;
    Class<?> classType;
    T obj;

    public BrokerResponse(int code, T obj) {
        this(code, null, obj);
    }

    public BrokerResponse(int code, String message, T obj) {
        this.code = code;
        this.message = message;
        if (obj != null) {
            classType = obj.getClass();
        }
        this.obj = obj;
    }

    public int getCode() {
        return code;
    }

    public T getResult() {
        return obj;
    }

    protected BrokerResponse(Parcel in) {
        code = in.readInt();
        message = in.readString();
        classType = (Class<?>) in.readValue(Class.class.getClassLoader());
        obj = (T) in.readValue(classType.getClassLoader());
    }

    public String getErrorMessage() {
        if (code > 0)
            return message;
        else
            return null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
        dest.writeString(message);
        dest.writeValue(classType);
        dest.writeValue(obj);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BrokerResponse> CREATOR = new Creator<BrokerResponse>() {
        @Override
        public BrokerResponse<?> createFromParcel(Parcel in) {
            return new BrokerResponse(in);
        }

        @Override
        public BrokerResponse<?>[] newArray(int size) {
            return new BrokerResponse[size];
        }
    };
}
