package kr.go.mobile.mobp.iff.http.impl;

import java.net.MalformedURLException;
import java.util.Map;

import android.content.Context;
import kr.go.mobile.mobp.iff.http.HttpRequest;

public class HttpDeleteRequest extends HttpRequest {
	
	public static String method = "DELETE";
	
	public HttpDeleteRequest(final Context c, String serviceId) throws MalformedURLException {
		super(c, serviceId);
	}

	@Override
	public Map<String, String> generateHeader() {
		return null;
	}

	@Override
	public String generateCommandLine() {
		return null;
	}
}
