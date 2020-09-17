package com.infrawaretech.relayservice.impl;

import java.net.MalformedURLException;
import java.util.Map;

import com.infrawaretech.relayservice.HttpRequest;

import android.content.Context;



public class HttpPutRequest extends HttpRequest {
	
	public static String method = "PUT";

	@Deprecated
	public HttpPutRequest(String serviceId) throws MalformedURLException {
		super(serviceId);
	}
	
	public HttpPutRequest(String serviceId, String hostUrl) throws MalformedURLException {
		super(serviceId, hostUrl);
	}

	@Override
	public Map<String, String> generateHeader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String generateCommandLine() {
		// TODO Auto-generated method stub
		return null;
	}


}
