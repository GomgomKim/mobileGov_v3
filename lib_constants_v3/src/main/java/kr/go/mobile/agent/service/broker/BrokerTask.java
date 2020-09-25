package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class BrokerTask implements Parcelable {

    public String serviceId;
    public String serviceParam;
    IBrokerServiceCallback serviceCallback;

    public static BrokerTask obtain() {
        return obtain("");
    }

    public static BrokerTask obtain(String serviceId) {
        // TODO 재사용 로직으로 구현 필요
        return new BrokerTask(serviceId);
    }

    private BrokerTask() {}

    private BrokerTask(String serviceId) {
        this.serviceId = serviceId;
    }


    protected BrokerTask(Parcel in) {
        serviceId = in.readString();
        serviceParam = in.readString();
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

//    public void setCallback(IBrokerServiceCallback callback) {
//        this.serviceCallback = callback;
//    }

    public String getServiceId() { return serviceId; }

    public String getServiceParam() {
        return serviceParam;
    }
}
