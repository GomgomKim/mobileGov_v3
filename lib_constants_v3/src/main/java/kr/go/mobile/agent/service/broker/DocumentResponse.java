package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

public class DocumentResponse implements Parcelable {
    public int code;
    public String message;
    public Object obj;

    public DocumentResponse(int code, String message, Object obj){
        this.code = code;
        this.message = message;
        this.obj = obj;
    }

    public DocumentResponse(int code, String message){
        this.code = code;
        this.message = message;
    }

    protected DocumentResponse(Parcel in) {
        code = in.readInt();
        message = in.readString();
    }

    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
        dest.writeString(message);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DocumentResponse> CREATOR = new Creator<DocumentResponse>() {
        @Override
        public DocumentResponse createFromParcel(Parcel in) {
            return new DocumentResponse(in);
        }

        @Override
        public DocumentResponse[] newArray(int size) {
            return new DocumentResponse[size];
        }
    };
}
