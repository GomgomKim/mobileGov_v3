package com.infrawaretech.relayservice.impl;

import java.net.MalformedURLException;
import java.util.Map;

import com.infrawaretech.relayservice.HttpRequest;

import android.content.Context;

public class HttpGetRequest extends HttpRequest {

	public static String method = "GET";

	@Deprecated
	public HttpGetRequest(String serviceId) throws MalformedURLException {
		super(serviceId);
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
