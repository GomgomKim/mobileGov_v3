package kr.go.mobile.agent.service.broker;

import android.os.Build;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// 중계 서버로부터 응답 받은 response 의 body 중 methodResponse 에 대한 기본 표현 값.
public class MethodResponse implements Parcelable {

    final String TAG = MethodResponse.class.getSimpleName();
    final static int PARCEL_TRANSACTION_SIZE_LIMIT = 520000;
    boolean isLargeData = false;

    String id;
    String result;
    String relayServerMessage;
    String relayServerCode;
    ParcelFileDescriptor pfd;
    byte[] data = new byte[0];

    public MethodResponse() {
        this("", "1", "00000000", "OK");
    }

    public MethodResponse(String id, String result, String code, String msg) {
        this.id = id;
        this.relayServerCode = code;
        this.relayServerMessage = msg;
        this.result = result;
    }

    protected MethodResponse(Parcel in) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isLargeData = in.readBoolean();
        } else {
            isLargeData = in.readByte() == 0b1;
        }
        if (isLargeData) {
            pfd = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
        } else {
            data = new byte[in.readInt()];
            in.readByteArray(data);
        }
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
        Log.e("PARCEL", "parcel size = " + data.length);
        if (data.length > PARCEL_TRANSACTION_SIZE_LIMIT) {
            try {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                pfd = Utils.pipeFrom(byteArrayInputStream);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dest.writeBoolean(true);
                } else {
                    dest.writeByte((byte)0x1);
                }
                dest.writeParcelable(pfd, flags);
            } catch (IOException e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dest.writeBoolean(false);
                } else {
                    dest.writeByte((byte)0x0);
                }
                byte[] errorMessage = ("대용량 데이터 처리중 에러가 발생하였습니다. ( cause = " +  e.getMessage() + ")").getBytes(StandardCharsets.UTF_8);
                dest.writeInt(errorMessage.length);
                dest.writeByteArray(errorMessage);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dest.writeBoolean(false);
            } else {
                dest.writeByte((byte)0x0);
            }
            dest.writeInt(data.length);
            dest.writeByteArray(data);
        }
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

    public String getRelayServerCode() {
        return this.relayServerCode;
    }

    public String getRelayServerMessage() {
        try {
            return URLDecoder.decode(this.relayServerMessage, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return this.relayServerMessage;
        }
    }

    public byte[] getResponseBytes() {
        if (data == null) {
            try {
                return Utils.readParcelFile(pfd);
            } catch (IOException e) {
                e.printStackTrace();
                return ("대용량 데이터를 읽는 중 에러가 발생하였습니다. (cause = " + e.getMessage() + ")").getBytes(StandardCharsets.UTF_8);
            }
        } else {
            return data;
        }
    }

    public String getServiceServerResponse() {
        return new String(getResponseBytes(), StandardCharsets.UTF_8);
    }

    public void setServiceServerResponse(byte[] respData) {
        this.data = respData;
    }

    public void setServiceServerResponse(String respData) {
        setServiceServerResponse(respData.getBytes(StandardCharsets.UTF_8));
    }

    public void reset() {
        if (pfd != null) {
            try {
                pfd.close();
            } catch (IOException ignored) {
            }
        }
    }
}
