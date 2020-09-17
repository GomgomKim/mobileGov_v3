package kr.go.mobile.mobp.iff.http.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

public class CustomHttpsUrlConnection extends CustomUrlConnection {

	private HttpsURLConnection httpsConn; 

	

	@Override
	protected void connectionInit() {		
		
		setTrustAllHosts();
	}

	@Override
	protected URLConnection setToConnection(URL url, String requestMethod) {
		
		
		try {
			httpsConn = (HttpsURLConnection)url.openConnection();
			httpsConn.setHostnameVerifier(DO_NOT_VERIFY);
			httpsConn.setRequestMethod(requestMethod);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return httpsConn;
	}
	
//	@Override
//	protected URLConnection setToConnection(URL url, String requestMethod) {
//		try {
//			SSLContext sslcontext = SSLContext.getInstance("TLSv1");
//
//			sslcontext.init(null, null, null);
//			SSLSocketFactory NoSSLv3Factory = new NoSSLv3SocketFactory(sslcontext.getSocketFactory());
//
////			HttpsURLConnection.setDefaultSSLSocketFactory(NoSSLv3Factory);
//			httpsConn = (HttpsURLConnection) url.openConnection();
//			
//			httpsConn.setSSLSocketFactory(sslcontext.getSocketFactory());
//			
//			httpsConn.setHostnameVerifier(DO_NOT_VERIFY);
//			httpsConn.setRequestMethod(requestMethod);
//			
////			httpsConn.connect();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		return httpsConn;
//	}
	

	final public static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	private void setTrustAllHosts() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {

			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {

			}
			
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}
		} };

		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getResponseCode(){
		try {
			return httpsConn.getResponseCode();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return -1;
	}
	
	@Override
	public Map<String, List<String>> getHeaderFields() {
		if(httpsConn != null){
			return httpsConn.getHeaderFields();
		}else{
			return null;
		}
	}
	

	@Override
	public String getHeaderField(String key) {
		if(httpsConn != null){
			return httpsConn.getHeaderField(key);
		}
		
		return "";
		
	}



	@Override
	public void disconnect() {
		if (httpsConn != null) {
			httpsConn.disconnect();
		}
	}
	
	@Override
	public void setConnectTimeout(int timeout) {
		if (httpsConn != null) {
			httpsConn.setConnectTimeout(timeout);
		}
	}

	@Override
	public void setReadTimeout(int timeout) {
		if (httpsConn != null) {
			httpsConn.setReadTimeout(timeout);
		}
	}

	@Override
	public void setDoOutput(boolean newValue) {
		if (httpsConn != null) {
			httpsConn.setDoOutput(newValue);
		}
	}

	@Override
	public void setDoInput(boolean newValue) {
		if (httpsConn != null) {
			httpsConn.setDoInput(newValue);
		}
	}

	@Override
	public void setUseCaches(boolean newValue) {
		if (httpsConn != null) {
			httpsConn.setUseCaches(newValue);
		}
	}

	@Override
	public void setRequestProperty(String field, String newValue) {		
		if (httpsConn != null) {
			httpsConn.setRequestProperty(field, newValue);
		}
	}

	@Override
	public OutputStream getOutputStream() throws Exception{
		return httpsConn.getOutputStream();
	}

	@Override
	public InputStream getInputStream()  throws Exception{
		return httpsConn.getInputStream();
	}

	
}
