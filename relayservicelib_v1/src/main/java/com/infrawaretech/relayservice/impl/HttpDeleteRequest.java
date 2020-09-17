package com.infrawaretech.relayservice.impl;

import java.net.MalformedURLException;
import java.util.Map;

import com.infrawaretech.relayservice.HttpRequest;

import android.content.Context;

public class HttpDeleteRequest extends HttpRequest {
	
	public static String method = "DELETE";

	@Deprecated
	public HttpDeleteRequest(String serviceId) throws MalformedURLException {
		super(serviceId);
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
