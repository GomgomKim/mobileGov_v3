package com.infrawaretech.relayservice.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import com.infrawaretech.relayservice.client.RelayClientException;


public abstract class RelayUrlConnection {


	abstract protected void connectionInit();

	abstract public int getResponseCode();
	
	abstract public Map<String,List<String>> getHeaderFields();
	
	abstract public String getHeaderField(String key);
	
	abstract public void disconnect();
	
	abstract public void setConnectTimeout(final int timeout);
	
	abstract public void setReadTimeout(final int timeout);
	
	abstract public void setDoOutput(boolean newValue);
	
	abstract public void setDoInput(boolean newValue);
	
	abstract public void setUseCaches(boolean newValue);
	
	abstract public void setRequestProperty(final String field, final String newValue);
	
	abstract public OutputStream getOutputStream() throws IOException;
	
	abstract public InputStream getInputStream() throws IOException;

	abstract protected URLConnection setToConnection(URL url, final String requestMethod) throws IOException  ;

	public final URLConnection createConnection(URL url, final String requestMethod) throws IOException{

		connectionInit();

		return setToConnection(url, requestMethod);

	}

}