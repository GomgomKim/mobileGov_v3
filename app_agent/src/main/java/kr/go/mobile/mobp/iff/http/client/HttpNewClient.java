package kr.go.mobile.mobp.iff.http.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.mobp.iff.http.HttpHeaderConstans;
import kr.go.mobile.mobp.iff.http.HttpRequest;
import kr.go.mobile.mobp.iff.http.HttpResponse;
import kr.go.mobile.mobp.iff.http.connection.CustomHttpUrlConnection;
import kr.go.mobile.mobp.iff.http.connection.CustomHttpsUrlConnection;
import kr.go.mobile.mobp.iff.http.connection.CustomUrlConnection;
import kr.go.mobile.mobp.iff.R;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import android.content.Context;
import android.util.Log;

import com.dreamsecurity.ssl.SSLSocket;

public class HttpNewClient extends CustomHttpClient{
	private final String boundary = "===" + System.currentTimeMillis() + "===";
	private static final String LINE_FEED = "\r\n";
	private Context context;
	
	private Map<String, String> requestHeaders;
	
	private CustomUrlConnection customConn;
	
	private PrintWriter writer;
	private OutputStream os;
	private InputStream is;

	private int nConnectTimeOut;
	private int nReadTimeOut;
	
	///// 내부행정용 /////
	@Deprecated
	private SSLSocket sslSocket;
	private String host = "";
	private String port = "";
	////////////////////
	
	@Override
	protected Object createHttpClient(Context c, int timeout) {
		context = c;		
		initConnection(timeout); 
		return this;
	}
	
	private void initConnection(final int timeOut){
		if (timeOut < 100) {
			nConnectTimeOut = context.getResources().getInteger(R.integer.HttpConnectionTimeOut);
			nReadTimeOut = context.getResources().getInteger(R.integer.HttpReadTimeOut);
		}else{
			nConnectTimeOut = timeOut;
			nReadTimeOut = timeOut;
		}
	}
	
	///// 내부행정용 /////
	public void initSocket() throws Exception {
		this.sslSocket = new SSLSocket(context);
		this.sslSocket.setConnectTimeout( nConnectTimeOut );
		this.sslSocket.setReadTimeout( nReadTimeOut );
	}
	
	///// 내부행정용 /////
	public void openSocket() throws Exception {
		this.sslSocket.open( this.host, Integer.parseInt(this.port) );
		
		this.sslSocket.startHandshake();
		if( sslSocket.isInvalidServerCert() ) {
			LogUtil.d(getClass(), "SSL invalid server cert!");
		}
	}
	
	///// 내부행정용 /////
	public void closeSocket() {
		if(sslSocket != null){
			this.sslSocket.close();
			this.sslSocket.release();
		}
	}
	
	/**
	* @Method Name	:	setMultiPartEntry
	* @작성일				:	2015. 10. 14. 
	* @작성자				:	조명수
	* @변경이력				:
	* @Method 설명 		:  해당 entry 를 생성후 excuteUpload 시 사용
	* 							     파일업로드를 위한 entry..
	* 
	* 								request.getBodyMap() 값은
	* 								HttpManager 의 getUploadData 메소드에서 저장됨
	* 								{url=http://10.47.10.136:10080/sample/upload} 와 같은 값..
	*/
	private HttpEntity setMultiPartEntry(HttpRequest request){
		MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
        multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);    
        multipartEntity.setCharset(Charset.forName("UTF-8"));

        
        FileBody  fileBody = new FileBody(new File(request.getAttachFilePath()));
        multipartEntity.addPart("file", fileBody);
        try {        	
        	
        	if(request.getBodyList() != null && request.getBodyList().size() > 0){
        		for(int i=0 ; i<request.getBodyList().size() ; i++){
        			
        			String data = request.getBodyList().get(i);
        			
        			LogUtil.d(getClass(),  "setMultiPartEntry data :: " + data);
        			String[] splitData = data.trim().split("=");
        			
        			LogUtil.d(getClass(),  "setMultiPartEntry data 0 :: " + splitData[0]);
        			if(splitData.length == 2){
        				LogUtil.d(getClass(),  "setMultiPartEntry data 1 :: " + splitData[1]);
        				multipartEntity.addTextBody(splitData[0], splitData[1], ContentType.create("text/plain", "UTF-8"));
        			}
        			
        			if(splitData.length == 1){
        				multipartEntity.addTextBody(splitData[0], "", ContentType.create("text/plain", "UTF-8"));
        			}

        		}
        	}
			
        } catch (NullPointerException e) {
        	LogUtil.e(getClass(), "", e);
		} catch (Exception e) {
			LogUtil.e(getClass(), "", e);
		}
        
        return multipartEntity.build();
	}
	
	/**
	* @Method Name	:	excuteUpload
	* @작성일				:	2015. 10. 14. 
	* @작성자				:	조명수
	* @변경이력				:	파일 업로드만을 위해 기존 excute 에서 별개로 생성..
	* @Method 설명 		:   MultiPartUpload 
	*/
	public HttpResponse excuteUpload(HttpRequest request) throws Exception {
		HttpResponse httpResponse = null;

		URL url = request.getURL();
//		URL url = new URL("http://10.47.10.138:8910/mois/rpc");
		String strUrl =  url.toString();

		if (strUrl.startsWith("https")) {
			customConn = new CustomHttpsUrlConnection();
			customConn.createConnection(url, "POST");
		} else if (strUrl.startsWith("http")) {
			customConn = new CustomHttpUrlConnection();
			customConn.createConnection(url, "POST");
		}

		
		// TimeOut 시간 (서버 접속시 연결 시간)
		customConn.setConnectTimeout(nConnectTimeOut);		 
		// TimeOut 시간 (Read시 연결 시간)
		customConn.setReadTimeout(nReadTimeOut);
		
		requestHeaders = request.generateHeader();
		
		String commandLine = request.generateCommandLine();
		Set<String> headerKey = requestHeaders.keySet();
		Iterator<String> itHeader = headerKey.iterator();
		while(itHeader.hasNext()) {
			String sKey = itHeader.next();
			String sValue = requestHeaders.get(sKey);
			customConn.setRequestProperty(sKey, sValue);
		}
		
		
		HttpEntity entity = setMultiPartEntry(request);
		customConn.setRequestProperty(entity.getContentType().getName(),entity.getContentType().getValue());
		customConn.setRequestProperty(HttpHeaderConstans.CONTENT_LENGTH, String.valueOf(entity.getContentLength()));
		
		customConn.setDoInput(true);
		customConn.setDoOutput(true);
		
		if (customConn != null && os == null) {
			os = customConn.getOutputStream();
		}

		
		entity.writeTo(os);

		
		httpResponse = new HttpResponse(context, customConn, request.getServiceId());

		
		return httpResponse;
	}

	public HttpResponse excute(HttpRequest request) throws Exception {
		HttpResponse httpResponse = null;

		URL url = request.getURL();
		String strUrl =  url.toString();

		if (strUrl.startsWith("https")) {
			customConn = new CustomHttpsUrlConnection();
			customConn.createConnection(url, "POST");
		} else if (strUrl.startsWith("http")) {
			customConn = new CustomHttpUrlConnection();
			customConn.createConnection(url, "POST");
		}

		requestHeaders = request.generateHeader();
		String commandLine = request.generateCommandLine();

		StringBuffer sbHeader = new StringBuffer();
		sbHeader.append(commandLine).append(HttpHeaderConstans.LF);
		Set<String> headerKey = requestHeaders.keySet();
		Iterator<String> itHeader = headerKey.iterator();
		while(itHeader.hasNext()) {
			String sKey = itHeader.next();
			String sValue = requestHeaders.get(sKey);
			
			customConn.setRequestProperty(sKey, sValue);
			
		}
		
		
		// TimeOut 시간 (서버 접속시 연결 시간)
		customConn.setConnectTimeout(nConnectTimeOut);

		// TimeOut 시간 (Read시 연결 시간)
		customConn.setReadTimeout(nReadTimeOut);

		customConn.setDoInput(true);
		customConn.setDoOutput(true);


		if (customConn != null && os == null) {
			os = customConn.getOutputStream();
		}
		
		LogUtil.d(getClass(), "HttpResponse excute getBodyString :: " + request.getBodyString());
		
		if (request.getBodyString() != null) {
			writeStream(request.getBodyString().getBytes("UTF-8"));
		}
		
		if(request.getAttachUrl() == null) {
			// XXX 파일 업로드 기능이 필요한가 ???? 
			LogUtil.d(getClass(),  "info :: getAttachUrl is null");
		}
		else {
			InputStream inputStream = new FileInputStream( new File(request.getAttachUrl().getFile()) );
	        int bytesRead, bytesAvailable, bufferSize;
	        byte[] buffer = new byte[1024];
	        byte [] tmp;
	        while ((bytesRead = inputStream.read(buffer)) != -1) {
	        	if(buffer.length == bytesRead) {
					writeStream(buffer);
	        	} else {
	        		tmp = new byte[bytesRead];
	        		System.arraycopy(buffer, 0, tmp, 0, bytesRead);
        			writeStream(tmp);
	        	}
	        }
	        inputStream.close();
		}
		
		if(request.getMultiPartEndString() != null){
			writeStream(request.getMultiPartEndString().getBytes());
		}


		httpResponse = new HttpResponse(context, customConn, request.getServiceId());
		
		return httpResponse;
	}
	
	private void writeStream(final byte[] data){
		try {			

			if (os != null) {
				os.write(data);
				os.flush();
			}
		} catch (IOException e) {
			LogUtil.e(getClass(), "", e);
		} catch (NullPointerException e){
			LogUtil.e(getClass(), "", e);
		}
	}
	
	private void closeConnection(){
		/*try {
			if (customConn != null) {
				customConn.disconnect();
				customConn = null;
			}
		} catch (NullPointerException e) {
			LogUtil.e(getClass(), "",  e);
		} catch (Exception e) {
			LogUtil.e(getClass(), "", e);
		}*/
	}

	@Override
	public void close() {
		// yoongi 왜 close 를 구현하지 않았을까 ?? 
	}

}
