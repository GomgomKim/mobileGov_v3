package kr.go.mobile.iff.sample.nativz;

import kr.go.mobile.common.v3.broker.Response;

class ServiceVO {

	String serviceID;
	String params;
	Response.Listener listener;

	public String getServiceID() {
		return serviceID;
	}
	
	public String getParams() {
		return params;
	}

    public Response.Listener getListener() {
		return this.listener;
    }
}
