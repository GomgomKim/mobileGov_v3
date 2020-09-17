package kr.go.mobile.agent.service.monitor;

import android.os.Parcel;
import android.os.Parcelable;

public class ThreatData implements Parcelable {
    public static final int STATUS_SAFE = 0;
    public static final int STATUS_THREATS = 1;

    int dataStatus;
    String threatMessage;
    byte isRealtime;

    public static final Creator<ThreatData> CREATOR = new Creator<ThreatData>() {
        @Override
        public ThreatData createFromParcel(Parcel in) {
            return new ThreatData(in);
        }

        @Override
        public ThreatData[] newArray(int size) {
            return new ThreatData[size];
        }
    };

    public ThreatData() {
        this(STATUS_SAFE, "", (byte) 0);
    }

    public ThreatData(int newStatus, String message, byte realtime) {
        this.dataStatus = newStatus;
        this.threatMessage = message;
        this.isRealtime = realtime;
    }

    protected ThreatData(Parcel in) {
        dataStatus = in.readInt();
        threatMessage = in.readString();
        isRealtime = in.readByte();
    }

    public boolean isSafe() {
        return dataStatus == STATUS_SAFE;
    }

    public String getMessage() {
        return this.threatMessage;
    }

    public boolean isRealtimeEvent() {
        return isRealtime == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(dataStatus);
        dest.writeString(threatMessage);
        dest.writeByte(isRealtime);
    }
}
