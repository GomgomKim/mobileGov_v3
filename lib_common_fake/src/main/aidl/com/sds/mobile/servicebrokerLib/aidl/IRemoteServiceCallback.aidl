package com.sds.mobile.servicebrokerLib.aidl;

import com.sds.mobile.servicebrokerLib.aidl.ByteArray;

oneway interface IRemoteServiceCallback {
	void success(String data);
	void fail(int code, String data);
	void successBigData(in String code, in String data);
}