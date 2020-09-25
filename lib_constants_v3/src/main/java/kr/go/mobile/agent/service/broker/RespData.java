package kr.go.mobile.agent.service.broker;

import android.os.Parcel;
import android.os.Parcelable;

public class RespData implements Parcelable {


    public static final Creator<RespData> CREATOR = new Creator<RespData>() {
        @Override
        public RespData createFromParcel(Parcel in) {
            return new RespData(in);
        }

        @Override
        public RespData[] newArray(int size) {
            return new RespData[size];
        }
    };

    public String id;
    public String result;
    public String data_result;
    public String data_data;
    public String data_data2;
    public String code;
    public String msg;
    // TODO data Json 내 구조에 따라 보관할 데이터 POJO 구현필요

    public RespData() { }

    protected RespData(Parcel in) {

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
