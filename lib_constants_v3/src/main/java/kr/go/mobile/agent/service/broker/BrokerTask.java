package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

public class BrokerTask implements Parcelable {

    static String SERVICE_ID_CERT_AUTH = "CMM_CERT_AUTH_MAM";

    public String serviceId;
    public String serviceParam;
    IBrokerServiceCallback serviceCallback;

    public static BrokerTask generateAuthTask(String authParams) {
        BrokerTask t = new BrokerTask();
        t.serviceId = SERVICE_ID_CERT_AUTH;
        t.serviceParam = authParams;
        return t;
    }

    public static BrokerTask obtain() {
        // TODO 재사용 로직으로 구현 필요
        return new BrokerTask();
    }

    private BrokerTask() {}


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

    public void setCallback(IBrokerServiceCallback callback) {
        this.serviceCallback = callback;
    }

    public String getServiceId() { return serviceId; }

}
