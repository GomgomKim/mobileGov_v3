package kr.go.mobile.mobp.iff.http.impl;

import java.net.MalformedURLException;
import java.util.Map;

import android.content.Context;
import kr.go.mobile.mobp.iff.http.HttpRequest;



public class HttpPutRequest extends HttpRequest {
	
	public static String method = "PUT";
	
	public HttpPutRequest(final Context c, String serviceId) throws MalformedURLException {
		super(c, serviceId);
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
