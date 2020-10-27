package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Objects;

// 중계 서버로부터 응답 받은 response 의 body 중 methodResponse 에 대한 기본 표현 값.
public class MethodResponse implements Parcelable {

    final String TAG = MethodResponse.class.getSimpleName();

    public String id;
    public String result;
    public String relayServerMessage;
    public String relayServerCode;
    public String data;

    public MethodResponse() {

    }

    protected MethodResponse(Parcel in) {
        data = in.readString();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(data);
    }

    public boolean relayServerOK() {
        boolean ret = false;
        if (Objects.equals(relayServerMessage, "OK")) {
            ret = true;
        }
        if (Objects.equals(relayServerCode, "00000000") != ret) {
            Log.w(TAG, "msg = OK 이지만 code = " + relayServerCode);
        }
        if (Objects.equals(result, "1") != ret) {
            Log.w(TAG, "msg = OK 이지만 result = " + result);
        }

        return ret;
    }
}
