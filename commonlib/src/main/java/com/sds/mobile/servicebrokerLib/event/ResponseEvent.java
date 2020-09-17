package com.sds.mobile.servicebrokerLib.event;


public class ResponseEvent{

    public ResponseEvent(int resultCode, String resultData){
        this.resultCode = resultCode;
        this.resultData = resultData;
    }

    public void setResultBytes(byte bytes[]){
        resultBytes = bytes;
    }

    public String getResultData(){
        return resultData;
    }

    public int getResultCode(){
        return resultCode;
    }

    public byte[] getResultBytes(){
        return resultBytes;
    }

    public static final int NOT_CONNECTED = -100;
    public static final int CONNECTION_TIMEOUT = -107;
    public static final int SERVER_NOT_FOUND = -108;
    public static final int USER_ID_NULL = -102;
    public static final int SERVICE_CODE_NULL = -103;
    public static final int IP_NULL = -104;
    public static final int UNKNOWN_ERROR = -109;
    
    int resultCode;
    String resultData;
    byte resultBytes[];
}
