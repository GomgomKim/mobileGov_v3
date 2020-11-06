package com.sds.mobile.servicebrokerLib.aidl;

import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceCallback;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceExitCallback;
import com.sds.mobile.servicebrokerLib.aidl.ByteArray;

interface IRemoteService {
	boolean data(in String header, in String hostUrl, in String serviceId, in String dataType, in String parameter, in int timeOut, in IRemoteServiceCallback callback);	
	boolean bigData(in String header, in String hostUrl, in String serviceId, in String dataType, in ByteArray parameter, in int timeOut, in IRemoteServiceCallback callback);
	String uploadWithCB(in String header, in String serviceID, in String filePath, in String parameter, in IRemoteServiceCallback callback);
	String upload(in String header, in String filePath, in String parameter);
	void download(in String header, in String filePath, in String fileName, in IRemoteServiceCallback callback);
	boolean document(in String header, in String url, in IRemoteServiceCallback callback);
	boolean documentWithExitCB(in String header, in String url, in IRemoteServiceCallback callback, in IRemoteServiceExitCallback exitcallback);
	boolean displayMailViewer(in String header, in String hostUrl, in String serviceId, in String dataType, in String parameter, in int timeOut, in IRemoteServiceCallback callback, in IRemoteServiceExitCallback exitcallback);
	String getInfo(in String list);
	boolean registerCallback(IRemoteServiceCallback callback);
	boolean unregisterCallback(IRemoteServiceCallback callback);
	
	boolean zipList(in String header, in String url, in IRemoteServiceCallback callback, in IRemoteServiceExitCallback exitcallback);
}