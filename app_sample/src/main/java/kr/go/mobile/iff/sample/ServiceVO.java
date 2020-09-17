package kr.go.mobile.iff.sample;

import com.sds.mobile.servicebrokerLib.event.ResponseListener;

class ServiceVO {

	private String serviceID;
	private String params;
	private ResponseListener responseListener; 
	
	
	public String getServiceID() {
		return serviceID;
	}
	
	public void setServiceID(String serviceID) {
		this.serviceID = serviceID;
	}
	
	public String getParams() {
		return params;
	}
	
	public void setParams(String params) {
		this.params = params;
	}
	
	public ResponseListener getResponseListener() {
		return this.responseListener;
	}
	
	public void setResponseListener(ResponseListener listener) {
		this.responseListener = listener;
	}
}
