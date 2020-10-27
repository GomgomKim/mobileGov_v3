package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class BrokerTask implements Parcelable {

    public String serviceId;
    public String serviceParam;
    public String serviceLocalPath;
    IBrokerServiceCallback serviceCallback;

    public static BrokerTask obtain(String serviceId) {
        // TODO 재사용 로직으로 구현 필요
        return new BrokerTask(serviceId);
    }

    BrokerTask(String serviceId) {
        this.serviceId = serviceId;
    }

    protected BrokerTask(Parcel in) {
        serviceId = in.readString();
        serviceParam = in.readString();
    }

    public String getServiceId() { return serviceId; }

    public String getOriginalServiceParam() {
        return serviceParam;
    }

    // 중계 서버와 사전에 정의된 내용으로 value 값만 URL 인코딩이 필요함.
    public String getServiceParam() throws UnsupportedEncodingException {
        StringBuilder ret = new StringBuilder();
        String[] params = serviceParam.split("[&;]");
        for (String param : params) {
            if (ret.length() > 0) {
                ret.append("&");
            }
            param = param.trim();
            String[] splitData = param.split("=");
            String value = "";
            if (splitData.length == 2) {
                value = URLEncoder.encode(splitData[1], "UTF-8");
                value = Utils.replace(value, "+", "%20");
                ret.append(splitData[0]).append("=").append(value);
            } else if (splitData.length == 1) {
                ret.append(splitData[0]).append("=").append(value);
            } else if (splitData.length > 2) {
                value = URLEncoder.encode(param.substring(param.indexOf("=")+1, param.length()), "UTF-8");
                value = Utils.replace(value, "+", "%20");
                ret.append(param.substring(0, param.indexOf("="))).append("=").append(value);
            }
        }
        return ret.toString();
    }

    @Deprecated
    public int getContentsLength() {
        return serviceParam.length();
    }

    public String getServiceLocalPath() {
        return serviceLocalPath;
    }

    public static final Creator<BrokerTask> CREATOR = new Creator<BrokerTask>() {
        @Override
        public BrokerTask createFromParcel(Parcel in) {
            return new BrokerTask(in);
        }

        @Override
        public BrokerTask[] newArray(int size) {
            return new BrokerTask[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(serviceId);
        dest.writeString(serviceParam);
    }


    public byte[] getFileBytes() {
        return null;
    }

    public String getFileName() {
        return "";
    }
}
