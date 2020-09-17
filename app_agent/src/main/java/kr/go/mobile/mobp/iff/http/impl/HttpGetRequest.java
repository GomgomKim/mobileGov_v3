package kr.go.mobile.mobp.iff.http.impl;

import java.net.MalformedURLException;
import java.util.Map;

import android.content.Context;
import kr.go.mobile.mobp.iff.http.HttpRequest;

public class HttpGetRequest extends HttpRequest {

	public static String method = "GET";
	
	public HttpGetRequest(final Context c, String serviceId) throws MalformedURLException {
		super(c, serviceId);
	}
	
	public HttpGetRequest(String serviceId, String hostUrl) throws MalformedURLException {
		super(serviceId, hostUrl);
	}

	@Override
	public Map<String, String> generateHeader() {
		return initDefaultHeader();
	}

	@Override
	public String generateCommandLine() {
		return method + " " + getURL().getPath() + ((getURL().getQuery() != null) ? "?" + getURL().getQuery() : "") + " HTTP/" + getHttpVersion();
	}

}
