package kr.go.mobile.mobp.iff.http.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.mobp.iff.http.HttpHeaderConstans;
import kr.go.mobile.mobp.iff.http.HttpRequest;
import kr.go.mobile.mobp.iff.http.HttpResponse;
import android.content.Context;
import android.util.Log;

import com.dreamsecurity.ssl.SSLSocket;

public class HttpSSLClient extends CustomHttpClient{
	
	Context context;
	private SSLSocket sslSocket;
	private Map<String, String> requestHeaders;
	int connectionTimeout = 60000 * 10;
	int readTimeout = 60000 * 10;
	private String host = "";
	private String port = "";
	
//	public HttpSSLClient(Context context) {
//		this.context = context;
//	}
//	
//	public HttpSSLClient(Context context, int timeOut) {
//		this.context = context;
//		connectionTimeout = timeOut;
//		readTimeout = timeOut;
//	}
	
	@Override
	protected Object createHttpClient(Context c, int timeout) {
		this.context = c;
		connectionTimeout = timeout;
		readTimeout = timeout;
		return this;
	}
	
	public void initSocket() throws Exception {
		this.sslSocket = new SSLSocket(context);
		this.sslSocket.setConnectTimeout( connectionTimeout );
		this.sslSocket.setReadTimeout( readTimeout );
	}
	
	public void openSocket() throws Exception {
		this.sslSocket.open( this.host, Integer.parseInt(this.port) );
		
		this.sslSocket.startHandshake();
		if( sslSocket.isInvalidServerCert() ) {
			LogUtil.d(getClass() ,"SSL invalid server cert!");
		}
	}
	
	public void closeSocket() {
		if(sslSocket != null){
			this.sslSocket.close();
			this.sslSocket.release();
		}
	}
	
	public HttpResponse excute(HttpRequest request) throws Exception {
		
		this.host = request.getHost();
		this.port = request.getPort();
		
		requestHeaders = request.generateHeader();
		
		String commandLine = request.generateCommandLine();
		//LogUtil.log_d("commandLine :: " + commandLine);
		//LogUtil.log_d("requestHeaders :: " + requestHeaders.toString());
		StringBuffer sbHeader = new StringBuffer();
		sbHeader.append(commandLine).append(HttpHeaderConstans.LF);
		Set<String> headerKey = requestHeaders.keySet();
		Iterator<String> itHeader = headerKey.iterator();
		while(itHeader.hasNext()) {
			String sKey = itHeader.next();
			String sValue = requestHeaders.get(sKey);
			sbHeader.append(sKey).append(": ").append(sValue).append(HttpHeaderConstans.LF);
		}
		
		initSocket();
		openSocket();
		
		HttpResponse httpres = null;
		
		LogUtil.d(getClass(), "sbHeader :: " + sbHeader.toString());
		this.sslSocket.writeEncrypt(sbHeader.toString().getBytes());
		//LogUtil.log_d("HttpHeaderConstans :: " + HttpHeaderConstans.LF);
		this.sslSocket.writeEncrypt(HttpHeaderConstans.LF.getBytes());
		
		if(request.getBodyString() != null) {
			LogUtil.d(getClass(),"HttpSSLClient request.getBody ::" + request.getBodyString());
			
			LogUtil.d(getClass(), "HttpSSLClient request.getBody end" );
			//LogUtil.log_d("HttpHeaderConstans ::" + HttpHeaderConstans.LF);
			//this.sslSocket.writeEncrypt(request.getBodyString().getBytes());
			
			InputStream inputStream = new ByteArrayInputStream(request.getBodyString().getBytes());
	        int bytesRead, bytesAvailable, bufferSize;
	        byte[] buffer = new byte[1024];
	        byte [] tmp;
	        
	        IOException ioException = null;
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        try {
		        while ((bytesRead = inputStream.read(buffer)) != -1) {
		        	if(buffer.length == bytesRead) {
		        		sslSocket.writeEncrypt(buffer);
		        	}
		        	else{
		        		/*
	        			tmp = new byte[bytesRead];
	        			System.arraycopy(buffer, 0, tmp, 0, bytesRead);
	        			*/
		        		baos.write(buffer, 0, bytesRead);
						tmp = baos.toByteArray();
						baos.reset();
						
	        			sslSocket.writeEncrypt(tmp);
		        	}
		        }
	        } catch(IOException e) {
	        	ioException = e;
	        } finally {
	        	try {
	        		baos.close();
	        	} catch(IOException e) {
	        		LogUtil.e(getClass(), Log.getStackTraceString(e));
	        	}
	        	
	        	try {
	        		inputStream.close();
	        	} catch(IOException e) {
	        		LogUtil.e(getClass(), Log.getStackTraceString(e));
	        	}
	        }
	        
	        if (ioException != null) throw ioException;
		}
		
		if(request.getAttachUrl() == null) {
			LogUtil.d(getClass(),"info :: getAttachUrl is null");
		}
		else {
			sslSocket.writeEncrypt(HttpHeaderConstans.LF.getBytes());
			
			InputStream inputStream = new FileInputStream( new File(request.getAttachUrl().getFile()) );
	        int bytesRead, bytesAvailable, bufferSize;
	        byte[] buffer = new byte[1024];
	        byte [] tmp;
	        
	        IOException ioException = null;
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        try {
		        while ((bytesRead = inputStream.read(buffer)) != -1) {
		        	if(buffer.length == bytesRead) {
		        		sslSocket.writeEncrypt(buffer);
		        	}
		        	else{
		        		/*
		        		tmp = new byte[bytesRead];
		        		System.arraycopy(buffer, 0, tmp, 0, bytesRead);
		        		*/
		        		baos.write(buffer, 0, bytesRead);
						tmp = baos.toByteArray();
						baos.reset();
						
		        		sslSocket.writeEncrypt(tmp);
		        	}
		        }
	        } catch(IOException e) {
	        	ioException = e;
	        } finally {
	        	try {
	        		baos.close();
	        	} catch(IOException e) {
	        		LogUtil.e(getClass(), Log.getStackTraceString(e));
	        	}
	        	
	        	try {
	        		inputStream.close();
	        	} catch(IOException e) {
	        		LogUtil.e(getClass(), Log.getStackTraceString(e));
	        	}
	        }
	        
	        if (ioException != null) throw ioException;
	        
	        sslSocket.writeEncrypt(HttpHeaderConstans.LF.getBytes());
		}
		
		if(request.getMultiPartEndString() != null){
			sslSocket.writeEncrypt(request.getMultiPartEndString().getBytes());
		}
		sslSocket.writeEncrypt(HttpHeaderConstans.LF.getBytes());
        sslSocket.getSocket().getOutputStream().flush();
		
		httpres = new HttpResponse(context, sslSocket, request.getServiceId());
		
		return httpres;
	}

	@Override
	public void close() {
		closeSocket();
		
	}

	@Override
	public HttpResponse excuteUpload(HttpRequest request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}


}
