package kr.go.mobile.iff.sample;

import kr.go.mobile.common.v3.broker.Response;

class ServiceVO {

	boolean async;
	String serviceID;
	String params;
	Response.Listener listener;

	public boolean isAsync() { return async; }

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
