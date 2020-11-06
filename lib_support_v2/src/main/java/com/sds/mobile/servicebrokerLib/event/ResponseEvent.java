package com.sds.mobile.servicebrokerLib.event;

public class ResponseEvent {

    int resultCode;
    String resultData;
    byte resultBytes[];

    public ResponseEvent(int resultCode, String resultData){
        this.resultCode = resultCode;
        this.resultData = resultData;
    }

    public String getResultData(){
        return resultData;
    }

    public int getResultCode(){
        return resultCode;
    }
}
