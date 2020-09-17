package com.infrawaretech.relayservice.client;

import com.infrawaretech.relayservice.HttpRequest;
import com.infrawaretech.relayservice.HttpResponse;

import android.content.Context;


public abstract class AbstRelayClientImpl {

	abstract protected Object createHttpClient(final int timeout) throws RelayClientException;
	
	abstract public void close();
	
	abstract public HttpResponse execute(HttpRequest request) throws RelayClientException;
	
	abstract public HttpResponse executeUpload(HttpRequest request) throws RelayClientException;

	public final Object createConnection(final int timeout) throws RelayClientException{
		return createHttpClient(timeout);

	}

}
