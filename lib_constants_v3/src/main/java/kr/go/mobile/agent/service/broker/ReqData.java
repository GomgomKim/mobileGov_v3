package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

public class ReqData implements Parcelable {


    public static final Creator<ReqData> CREATOR = new Creator<ReqData>() {
        @Override
        public ReqData createFromParcel(Parcel in) {
            return new ReqData(in);
        }

        @Override
        public ReqData[] newArray(int size) {
            return new ReqData[size];
        }
    };


    public String result;
    public String data;
    // TODO data Json 내 구조에 따라 보관할 데이터 POJO 구현필요

    public ReqData() { }

    protected ReqData(Parcel in) {

    }

    public void clear() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

}
