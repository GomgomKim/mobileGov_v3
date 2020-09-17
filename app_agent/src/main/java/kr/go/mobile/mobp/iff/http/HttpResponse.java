package kr.go.mobile.mobp.iff.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;

import org.apache.http.HttpStatus;
import org.json.JSONObject;

import com.dreamsecurity.ssl.SSLSocket;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.mobp.iff.ProgressActivity;
import kr.go.mobile.mobp.iff.http.connection.CustomUrlConnection;
import kr.go.mobile.mobp.iff.http.parser.HTTPChunkedInputStream;
import kr.go.mobile.mobp.iff.http.parser.HTTPProtocol;
import kr.go.mobile.mobp.iff.http.parser.HTTPResponseParser;
import kr.go.mobile.mobp.iff.http.parser.HttpHeader;
import kr.go.mobile.mobp.iff.util.DownLoadFile;
import kr.go.mobile.mobp.iff.util.SingleProgressDialog;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;

public class HttpResponse {
	// SSLSocket sslsoket;
	private Context context;

	private CustomUrlConnection customConn;
	private InputStream responseBodyAsStream;

	private SingleProgressDialog downProgress;

	private HttpHeader httpHeader;
	
	private HTTPResponseParser response;
	private SSLSocket sslsoket;
	private StringBuffer headers;
	
	private String contentType = "";
	private String moPageCount = "";
	private String moPageWidth = "";
	private String moPageHeight = "";
	private String moConverting = "";
	private String moHashCode = "";
	private String contentDisposition = "";
	private String strServiceId = "";

	private String moErrCode = "";
	private String moState = "";
	
	private String strResponseHeader = "";
	private String strResponseData = "";

	private int statusCode;
	private int contentLength;

	private boolean first = true;

	public HttpResponse(Context c, CustomUrlConnection conn, String serviceId) {
		context = c;
		customConn = conn;
		strServiceId = serviceId;
		first = true;
	}
	

	public HttpResponse(Context c,SSLSocket socket, String serviceId){
		context = c;
		sslsoket = socket; 
		strServiceId = serviceId;
		first = true;
	}

	public String getContentString() throws Exception {
		String responseData = "";

		exceute();

		int stateCode = getStatusCode();

		if (stateCode == HttpStatus.SC_OK) {
			InputStream is2 = getResponseBodyAsStream();
			responseData = readResponseData(is2);

			try {
				is2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		this.strResponseData = responseData;

		return responseData;
	}

	public InputStream getContentStream() throws Exception {

		exceute();

		int stateCode = getStatusCode();

		InputStream is2 = null;
		if (stateCode == HttpStatus.SC_OK) {
			is2 = getResponseBodyAsStream();
		}

		return is2;
	}

	private byte[] readStream(SingleProgressDialog downProgress) throws InterruptedIOException  {
		byte[] data = null;
		int readsum = 0;
		ByteArrayOutputStream baos = null;
		// ///////////////////
		try {

			InputStream is = customConn.getInputStream();
			if (is == null) {
				LogUtil.d(getClass(), "HttpResponse readStream customConn :: " + customConn.toString());
				return data;
			}
			
			if (downProgress != null) {

//				float a = (float)1000 / (float)1024;
//				float nMaxSize = (float)httpHeader.getContentLength() * (float)a * (float)a;
//				String contentLength = String.format("%.0f", Math.floor(nMaxSize));
////				LogUtil.d(getClass(), "HttpResponse getContentLength a :: " + contentLength);
				
//				downProgress.setMax(Integer.valueOf(contentLength));
				downProgress.setMax(httpHeader.getContentLength());
				downProgress.setProgress(0);
			}

			baos = new ByteArrayOutputStream();
			byte[] byteBuffer = new byte[1024];
			// byte[] byteData = null;
			int nLength = 0;
			while ((nLength = is.read(byteBuffer, 0, byteBuffer.length)) != -1) {
				baos.write(byteBuffer, 0, nLength);
				
				if (downProgress != null) {
					readsum = readsum + nLength;
					downProgress.setProgress(readsum);
				}
			}
			data = baos.toByteArray();
			return data;
		} catch (InterruptedIOException e) {
			throw e;
		} catch (IOException e) {
			LogUtil.e(getClass(), "", e);
		} catch (Exception e) {
			LogUtil.e(getClass(), "", e);
		}  finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException ioe) {
					LogUtil.e(getClass(), "", ioe);
				}
				baos = null;
			}

		}
		// ///////////////////
		return data;
	}


//	private void writeToFile(String data) {
//	    try {
//	        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(Environment.getExternalStorageDirectory() + "/download/test2.txt", Context.MODE_PRIVATE));
//	        outputStreamWriter.write(data);
//	        outputStreamWriter.close();
//	    }
//	    catch (IOException e) {
//	        Log.e("Exception", "File write failed: " + e.toString());
//	    } 
//	}
	
//	private boolean writeFile(File file , byte[] file_content){
//        boolean result;
//        FileOutputStream fos;
//        if(file!=null&&file.exists()&&file_content!=null){
//            try {
//                fos = new FileOutputStream(file);
//                try {
//                    fos.write(file_content);
//                    fos.flush();
//                    fos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            result = true;
//        }else{
//            result = false;
//        }
//        return result;
//    }
	
	
	/**
	* @Method Name	:	exceute
	* @작성일				:	2015. 11. 19. 
	* @작성자				:	조명수
	* @변경이력				:
	* @Method 설명 		:  내부행정용
	*/
	public void exceute() throws Exception {
		if (customConn == null) return;
		
		byte[] tempReadText = null;
		int readsum = 0;

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();// 8192

		// 1. 헤더필드를 읽어 들인다.
		httpHeader = new HttpHeader(context, customConn);
		httpHeader.getHeaderFields();

		while (true) {
			tempReadText = readStream(null);
			buffer.write(tempReadText, 0, tempReadText.length);

			if (downProgress != null) {
				readsum = readsum + tempReadText.length;
				downProgress.setProgress(readsum);
			}

			if (readContent(buffer.toByteArray())) {
				break;
			}
			
		}

		this.contentLength = httpHeader.getContentLength();
		this.contentType = httpHeader.getContentType();
		this.statusCode = customConn.getResponseCode();
		// this.headers = httpHeader.getHTTPHeader();
		this.moPageCount = httpHeader.getMOPageCount();
		this.moConverting = httpHeader.getMOConverting();
		this.moPageWidth = httpHeader.getMOPageWidth();
		this.moPageHeight = httpHeader.getMOPageHeight();
		this.contentDisposition = httpHeader.getContentDisposition();
		
		this.strResponseHeader = httpHeader.getResponseHeader();
		
		this.moHashCode = httpHeader.getMOHashCode();
		LogUtil.d(getClass(), "HttpHeader MO_HASHCODE :: " + moHashCode);

		this.moErrCode = httpHeader.getMOErrCode();
		LogUtil.d(getClass(), "HttpHeader MO_ERRCODE :: " + moErrCode);

		this.moState = httpHeader.getMOState();
		LogUtil.d(getClass(), "HttpHeader MO_STATE :: " + moState);
		
	}
	
	/**
	* @Method Name	:	exceuteOnE2ESocket
	* @작성일				:	2015. 11. 19. 
	* @작성자				:	조명수
	* @변경이력				:
	* @Method 설명 		:  현장행정용
	*/
	public void exceuteOnE2ESocket() throws Exception {
		byte[] tempReadText = null;
		boolean bReadHttpHeader = false;
		int readsum = 0;

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();//8192

		while(true){
			tempReadText = sslsoket.readDecrypt();
			buffer.write(tempReadText, 0, tempReadText.length);
			
	 		if ( bReadHttpHeader == false ){
	 			bReadHttpHeader = isHTTPHeaderDone( buffer.toByteArray() );
	 			if ( !bReadHttpHeader )	continue;
	 		}
			
	 		if(downProgress != null){
	 			readsum = readsum + tempReadText.length;
				downProgress.setProgress(readsum);
	 		}
	 		
			if (readContentOnE2ESocket(buffer.toByteArray())) {
				break;
			}
		}
		
			
		this.contentLength = response.getContentLength();
		this.contentType = response.getContentType();
		this.statusCode = response.getStatusCode();
		this.headers =  response.getHTTPHeader();
		this.moPageCount =  response.getMoPage();
		this.moConverting =  response.getMoConverting();
		this.contentDisposition =  response.getContentDisposition();
				
		this.strResponseHeader = headers.toString().replace("\n", "");
		

		LogUtil .e(getClass(), "http 통신 상태리턴값 : " + statusCode);
		LogUtil .e(getClass(), "http 통신 headers값 : " + headers);

	}
	
	public String getResponseHeader(){
		return this.strResponseHeader;
	}

	private boolean isHTTPHeaderDone(byte[] buffer) {
		String header = new String(buffer);

		if (header.indexOf(HTTPProtocol.CRLF + HTTPProtocol.CRLF) >= 0)
			return true;
		else
			return false;
	}

	private boolean readContent(byte[] readText) {
		ByteArrayInputStream bis = new ByteArrayInputStream(readText);

		int bodyLengtha = bis.available();

		int contentLength = httpHeader.getContentLength();
		bodyLengtha = bis.available();

		if (E2ESetting.FILE_DOWNLOAD_SERVICE.equals(strServiceId)) {
			if (DownLoadFile.downProgress != null && first) {
				first = false;
				downProgress = DownLoadFile.downProgress;
				downProgress.setMax(contentLength);
				downProgress.setProgress(0);
			} else if (first) {
				if (ProgressActivity.downProgress != null) {
					first = false;
					downProgress = ProgressActivity.downProgress;
					downProgress.setMax(contentLength);
					downProgress.setProgress(0);
				}
			}
		}

		// chunked
		if (httpHeader.isChunked()) {
			InputStream tempIs = new HTTPChunkedInputStream(bis);
			try {
				ByteArrayOutputStream outstream = new ByteArrayOutputStream(contentLength > 0L ? (int) contentLength : 4096);
				byte[] buffer = new byte[4096];
				int len;
				while ((len = tempIs.read(buffer)) > 0) {
					outstream.write(buffer, 0, len);
				}
				outstream.close();
				byte[] responseBody = outstream.toByteArray();
				responseBodyAsStream = new ByteArrayInputStream(responseBody);
				return true;
			} catch (Throwable e) {
				return false;
			}
			// content length
		} else {

			int bodyLength = bis.available();
			if (bodyLength == contentLength || contentLength == -1) {
				responseBodyAsStream = bis;
				return true;
			} else {
				return false;
			}
		}

	}
	
	private boolean readContentOnE2ESocket(byte[] readText) throws Exception{
		ByteArrayInputStream bis = new ByteArrayInputStream(readText);
		
		try {
			int bodyLengtha = bis.available();
			response = new HTTPResponseParser(bis);
			int contentLength = response.getContentLength();
			bodyLengtha = bis.available();
			
			if(E2ESetting.FILE_DOWNLOAD_SERVICE.equals(strServiceId)){
				if(DownLoadFile.downProgress != null && first){
					first = false;
					downProgress = DownLoadFile.downProgress;
					downProgress.setMax(contentLength);
					downProgress.setProgress(0);
				}else if(first){
					if(ProgressActivity.downProgress != null){
						first = false;
						downProgress = ProgressActivity.downProgress;
						downProgress.setMax(contentLength);
						downProgress.setProgress(0);
					}
				}
			}
			
			//chunked
			if(response.isChunked()){
				InputStream tempIs = new HTTPChunkedInputStream(bis);
				try{
					ByteArrayOutputStream outstream = new ByteArrayOutputStream(contentLength > 0L ? (int)contentLength : 4096);
					byte[] buffer = new byte[4096];
					int len;
					while ((len = tempIs.read(buffer)) > 0) {
						outstream.write(buffer, 0, len);
					}
					outstream.close();
					byte[] responseBody = outstream.toByteArray();
					responseBodyAsStream = new ByteArrayInputStream(responseBody);
					return true;
				}catch(Throwable e){
					return false;
				}
				// content length	
			}else {
				int bodyLength = bis.available();
				if(bodyLength == contentLength){
					responseBodyAsStream = bis;
					return true;
				}else{
					return false;
				}
			}
		} catch (IOException e) {
			throw e;
		}
	}

	public String getDownloadStream(String file, SingleProgressDialog downProgress, Handler handler) throws Exception {
		String result = "";
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();

		byte[] tempReadText = null;
		int readsum = 0;
		int firstRead = 0;

		FileOutputStream fos = null;
		File mFile = new File(file);
		mFile.mkdirs();
		mFile.delete();
		
		// 1. 헤더필드를 읽어 들인다.
		httpHeader = new HttpHeader(context, customConn);
		httpHeader.getHeaderFields();

		if (mFile.createNewFile()) {
			fos = new FileOutputStream(file);

			tempReadText = readStream(downProgress);
			if (tempReadText == null) {
				fos.close();
				return result;
			}
				

			ByteArrayInputStream bis = new ByteArrayInputStream(tempReadText);
			
			if (customConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				if (httpHeader.getContentType().startsWith("application/json")) {
					JSONObject resultJsonObject = new JSONObject(new JSONObject(readResponseData(bis)).getString("methodResponse"));
					String resultCode = resultJsonObject.getString("code");
					String resultMsg = resultJsonObject.getString("msg");

					bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
					msg.what = -1;
					msg.setData(bundle);
					handler.sendMessage(msg);
					
				} else if (httpHeader.getContentDisposition() == null || "".equals(httpHeader.getContentDisposition())) {
					msg.what = -1;
					bundle.putString("result", E2ESetting.PUBLIC_ERROR_MESSAGE);
					msg.setData(bundle);
					handler.sendMessage(msg);
				}

//				downProgress.setMax(httpHeader.getContentLength());
//				downProgress.setProgress(0);

				firstRead = tempReadText.length;

				ByteArrayOutputStream buffer = new ByteArrayOutputStream();// 8192				
				buffer.write(tempReadText, 0, tempReadText.length);

			} else {
				msg.what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				msg.setData(bundle);
				handler.sendMessage(msg);
			}

			fos.write(tempReadText, 0, tempReadText.length);

//			if (downProgress != null) {
//				readsum = readsum + tempReadText.length;
//				downProgress.setProgress(readsum);
//			}

			if (readsum >= httpHeader.getContentLength() - firstRead) {
				result = httpHeader.getContentDisposition();
				HttpManager.downContentLength = httpHeader.getContentLength();
			}

		}

		if (fos != null) {
			fos.close();
			fos = null;
		}

		return result;
	}

	
	//파일 다운로드일 경우
	public String getDownloadStreamOnE2ESocket(String file, SingleProgressDialog downProgress, Handler handler) throws Exception{
		String result = "";
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();
		
		byte[] tempReadText = null;
		int readsum = 0;
		int firstRead = 0;
		
		FileOutputStream fos = null;
		File mFile = new File(file);
		mFile.mkdirs();
		mFile.delete();
		boolean bReadHttpHeader = false;
		boolean start = true;
		
		Exception exceptionReport = null;
		
		if (mFile.createNewFile() ) {
			try {
			fos = new FileOutputStream(file);
			while (true) {
				tempReadText = sslsoket.readDecrypt();
				
				if(start){
					start = false;
					
					//contentType 체크 - json일 경우 에러가 리턴된 경우임
					ByteArrayInputStream bis = new ByteArrayInputStream(tempReadText);
					response = new HTTPResponseParser(bis);
					LogUtil.e(getClass(), "download =========== StatusCode :: " + response.getStatusCode());
					LogUtil.e(getClass(), "download =========== ContentType :: " + response.getContentType());
					LogUtil.e(getClass(), "download =========== ContentLength :: " + response.getContentLength());
					LogUtil.e(getClass(), "download =========== ContentDisposition :: " + response.getContentDisposition());
					
					if(response.getStatusCode() == 200){
						if(response.getContentType().startsWith("application/json") ){
							//에러동작
							JSONObject resultJsonObject = new JSONObject(new JSONObject(readResponseData(bis)).getString("methodResponse"));
							String resultCode = resultJsonObject.getString("code");
							String resultMsg = resultJsonObject.getString("msg");
							LogUtil.e(getClass(), "HTTP Manager :: resultMsg >> " + resultMsg);
//							resultMsg = E2ESetting.PUBLIC_ERROR_MESSAGE;
							bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
							msg.what = -1;
							msg.setData(bundle);
							handler.sendMessage(msg);
							
							break;
						}
						else if(response.getContentDisposition() == null || "".equals(response.getContentDisposition())){
							msg.what = -1;
							bundle.putString("result", E2ESetting.PUBLIC_ERROR_MESSAGE);
							msg.setData(bundle);
							handler.sendMessage(msg);
							break;
						}
						
						//다운로드 progress설정
						downProgress.setMax(response.getContentLength());
						downProgress.setProgress(0);
						
						//헤더값의 길이 저장
						firstRead = tempReadText.length;
						
						//최초 헤더값 체크
						ByteArrayOutputStream buffer = new ByteArrayOutputStream();//8192
						buffer.write(tempReadText, 0, tempReadText.length);
				 		if ( bReadHttpHeader == false ){
				 			bReadHttpHeader = isHTTPHeaderDone( buffer.toByteArray() );
				 			if (bReadHttpHeader){
				 				continue;
				 			}
				 		}
					}else{
						msg.what = -1;
						bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
						msg.setData(bundle);
						handler.sendMessage(msg);
						break;
					}
				}
				
				fos.write(tempReadText, 0, tempReadText.length);
				
				if(downProgress != null){
		 			readsum = readsum + tempReadText.length;
					downProgress.setProgress(readsum);
		 		}
				
				//다운받은 값 >= 전체길이 - 헤더값길이
//				if(readsum >= response.getContentLength() - firstRead){
				if(readsum >= response.getContentLength()){
					result = response.getContentDisposition();
					HttpManager.downContentLength = response.getContentLength();
					break;
					}
				}
			}catch(IOException e) {
				LogUtil.e(getClass(), "", e);
				exceptionReport = e;
			}catch(Exception e) {
				LogUtil.e(getClass(), "", e);
				exceptionReport = e;
			}
			
			if (fos != null) {
				fos.close();
			}
			
			if (exceptionReport != null) {
				throw exceptionReport;
			}
		}
		
		return result;
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public InputStream getResponseBodyAsStream() {
		return responseBodyAsStream;
	}

	public StringBuffer getHttpHeaders() {
		return headers;
	}

	public String getMoPage() {
		return moPageCount;
	}

	public String getMoConverTing() {
		return moConverting;
	}
	
	/**
	* @Method Name	:	getMoPageWidth
	* @작성일				:	2015. 10. 15. 
	* @작성자				:	조명수
	* @변경이력				:   현재 문서 변환의 경우에만 사용됨
	* @Method 설명 		:   문서변환 요청시 변환된 문서의 가로...
	* 								해당 가로사이즈로 DocumentActivity 에서 이미지 뷰의 가로사이즈를 결정
	*/	
	public String getMoPageWidth() {
		return moPageWidth;
	}
	
	/**
	* @Method Name	:	getMOPageHeight
	* @작성일				:	2015. 10. 15. 
	* @작성자				:	조명수
	* @변경이력				:   현재 문서 변환의 경우에만 사용됨
	* @Method 설명 		:   문서변환 요청시 변환된 문서의 세로...
	* 								해당 세로사이즈로 DocumentActivity 에서 이미지 뷰의 세로사이즈를 결정
	*/	
	public String getMoPageHeight() {
		return moPageHeight;
	}
	
	public String getMoHashCode() {
		return moHashCode;
	}

	public String getMoErrCode() {
		return moErrCode;
	}

	public String getMoState() {
		return moState;
	}
	
	public String getContentDisposition() {
		return contentDisposition;
	}

	public String readResponseData(InputStream is) {
		String ret = null;
		StringBuilder builder = new StringBuilder();

		try {
			// msjo@dkite.com 2015-09-15 한글
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			char[] buf = new char[10 * 1024];
			int readByte = 0;

			while ((readByte = br.read(buf)) != -1) {
				builder.append(buf, 0, readByte);
			}
			ret = builder.toString();
			
			this.strResponseData = ret;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public String getResponseData(){
		return this.strResponseData;
	}
}
