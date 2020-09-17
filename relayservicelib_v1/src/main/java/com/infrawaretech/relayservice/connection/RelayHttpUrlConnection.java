package com.infrawaretech.relayservice.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class RelayHttpUrlConnection extends RelayUrlConnection {

	HttpURLConnection httpConn;

	@Override
	protected void connectionInit() {
		// TODO Auto-generated method stub

	}

//	@Override
//	protected URLConnection setToConnection(URLConnection conn, String requestMethod) {
//		httpConn = (HttpURLConnection) conn;
//
//		try {
//			httpConn.setRequestMethod(requestMethod);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return httpConn;
//	}
	
	@Override
	protected URLConnection setToConnection(URL url, String requestMethod) {
		try {
			httpConn = (HttpURLConnection) url.openConnection();
			httpConn.setRequestMethod(requestMethod);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return httpConn;
	}

	@Override
	public int getResponseCode() {
		try {
			return httpConn.getResponseCode();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return -1;
	}
	

	@Override
	public OutputStream getOutputStream()  throws IOException{
		return httpConn.getOutputStream();
	}

	@Override
	public InputStream getInputStream()  throws IOException{
		return httpConn.getInputStream();
	}

	@Override
	public Map<String, List<String>> getHeaderFields() {
		if(httpConn != null){
			return httpConn.getHeaderFields();
		}
		
		return null;
	}

	@Override
	public String getHeaderField(String key) {
		if (httpConn != null) {
			return httpConn.getHeaderField(key);
		}
		
		return "";
	}

	@Override
	public void disconnect() {
		if (httpConn != null) {
			httpConn.disconnect();
		}
	}
	

	@Override
	public void setConnectTimeout(int timeout) {
		if (httpConn != null) {
			httpConn.setConnectTimeout(timeout);
		}
	}

	@Override
	public void setReadTimeout(int timeout) {
		if (httpConn != null) {
			httpConn.setReadTimeout(timeout);
		}
	}

	@Override
	public void setDoOutput(boolean newValue) {
		if (httpConn != null) {
			httpConn.setDoOutput(newValue);
		}
	}

	@Override
	public void setDoInput(boolean newValue) {
		if (httpConn != null) {
			httpConn.setDoInput(newValue);
		}
	}

	@Override
	public void setUseCaches(boolean newValue) {
		if (httpConn != null) {
			httpConn.setUseCaches(newValue);
		}
	}

	@Override
	public void setRequestProperty(String field, String newValue) {		
		if (httpConn != null) {
			httpConn.setRequestProperty(field, newValue);
		}
	}


}
