package com.infrawaretech.relayservice.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.infrawaretech.relayservice.HttpRequest;
import com.infrawaretech.relayservice.HttpResponse;
import com.infrawaretech.relayservice.connection.RelayHttpUrlConnection;
import com.infrawaretech.relayservice.connection.RelayHttpsUrlConnection;
import com.infrawaretech.relayservice.connection.RelayUrlConnection;
import com.infrawaretech.relayservice.utils.HTTPChunkedInputStream;
import com.infrawaretech.relayservice.utils.HTTPHeader;

public class RelayClientImpl extends AbstRelayClientImpl {
	private static final String TAG = RelayClientImpl.class.getSimpleName();

	private int nConnectTimeOut;
	private int nReadTimeOut;
	
	@Override
	protected Object createHttpClient(int timeout) {
		if (timeout < 100) {
			nConnectTimeOut = 60000;
			nReadTimeOut = 60000;
		}else{
			nConnectTimeOut = timeout;
			nReadTimeOut = timeout;
		}
		return this;
	}

	@Deprecated
	public HttpResponse executeUpload(HttpRequest request) throws RelayClientException {
		return null;
	}

	public HttpResponse execute(HttpRequest request) throws RelayClientException {
		RelayUrlConnection conn = null;
		try {
			conn = connection(request);
			writeRequest(conn, request);

			InputStream content = readContentAsStream(conn);
			HTTPHeader header = HTTPHeader.parse(conn);
			return HttpResponse.newInstance(header, content);
		} catch (RelayClientException e) {
			throw e;
		} catch (InterruptedException e) {
			throw new RelayClientException(RelayClientException.ERRNO_INTERRUPTED, e);
		} catch (Exception e) {
			throw new RelayClientException(RelayClientException.ERRNO_UNKNOWN, e);
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}
	
	private RelayUrlConnection connection(HttpRequest request) throws RelayClientException {
		RelayUrlConnection conn = null;
		URL url = request.getURL();
		String strUrl = url.toString();
		Map<String, String> requestHeaders = request.generateHeader();
		try {
			if (strUrl.startsWith("https")) {
				conn = new RelayHttpsUrlConnection();
				conn.createConnection(url, "POST");
			} else if (strUrl.startsWith("http")) {
				conn = new RelayHttpUrlConnection();
				conn.createConnection(url, "POST");
			}
		} catch (IOException e) {
			throw new RelayClientException(RelayClientException.ERRNO_NOT_CONNECTION, e);
		} catch (Exception e) {
			throw new RelayClientException(RelayClientException.ERRNO_UNKNOWN, e);
		}

		Set<String> headerKey = requestHeaders.keySet();
		Iterator<String> itHeader = headerKey.iterator();
		while(itHeader.hasNext()) {
			String sKey = itHeader.next();
			String sValue = requestHeaders.get(sKey);
			conn.setRequestProperty(sKey, sValue);
		}
		requestHeaders.clear();
		
		conn.setConnectTimeout(nConnectTimeOut);		// TimeOut 시간 (서버 접속시 연결 시간)
		conn.setReadTimeout(nReadTimeOut); 		// TimeOut 시간 (Read시 연결 시간)
		conn.setDoInput(true);
		conn.setDoOutput(true);
		
		return conn;
	}
	
	private void writeRequest(RelayUrlConnection conn, HttpRequest request) throws RelayClientException {
		OutputStream out = null;
		try {
			out = conn.getOutputStream();
			Log.d(TAG, "HttpRequest execute :: getBodyString = " + request.getBodyString());
			if (request.getBodyString() != null) {
				out.write(request.getBodyString().getBytes("UTF-8"));
				out.flush();
			}
			
			if(request.getMultiPartEndString() != null){
				out.write(request.getMultiPartEndString().getBytes());
				out.flush();
			}
		} catch (IOException e) {
			throw new RelayClientException(RelayClientException.ERRNO_CAN_NOT_WRTIE, e);
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {}
		}
	}
	
	private InputStream readContentAsStream(RelayUrlConnection conn) throws RelayClientException, InterruptedException { 
		Log.d(TAG, "Contents read ... ");
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		InputStream in = null;
		
		try {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			in  = conn.getInputStream();
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			boolean isChunked = HTTPHeader.isChunked(conn);
			int contentLength = HTTPHeader.getContentLength(conn);

			byte[] readBytes = bytesToStream(in, contentLength);
			
			while (true) {
				buffer.write(readBytes, 0, readBytes.length);
				buffer.flush();
				if (isReadAllContent(buffer.toByteArray(), contentLength, isChunked)) {
					return new ByteArrayInputStream(buffer.toByteArray());
				}
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
			}
		} catch (IOException e) {
			throw new RelayClientException(RelayClientException.ERRNO_CAN_NOT_READ, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			if (buffer != null) {
				try {
					buffer.close();
				} catch (IOException e) {}
			}
		}
	}
	
	private byte[] bytesToStream(InputStream in, int contentLength) throws RelayClientException {
		byte[] byteBuffer = new byte[1024 * 4];
		ByteArrayOutputStream byteArray = null;
		try {
			int nLength = 0;
			byteArray = new ByteArrayOutputStream();
			while ((nLength = in.read(byteBuffer, 0, byteBuffer.length)) != -1) {
				byteArray.write(byteBuffer, 0, nLength);
			}
			return byteArray.toByteArray();
		} catch (InterruptedIOException e) {
			throw new RelayClientException(RelayClientException.ERRNO_INTERRUPTED, e);
		} catch (IOException e) {
			throw new RelayClientException(RelayClientException.ERRNO_CAN_NOT_READ, e);
		} catch (Exception e) {
			throw new RelayClientException(RelayClientException.ERRNO_UNKNOWN, e);
		} finally {
			try {
				if (byteArray != null) {
					byteArray.close();
					byteArray = null;
				}
			} catch (IOException e) { }
		}
	}
	
	private boolean isReadAllContent(byte[] bytes, int contentLength, boolean isChunked) {
		int bodyLength = bytes.length;

		if (isChunked) {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			InputStream tempIs = null;
			ByteArrayOutputStream out = null;
			try {
				tempIs = new HTTPChunkedInputStream(bis);
				out = new ByteArrayOutputStream(contentLength > 0L ? (int) contentLength : 4096);
				byte[] buffer = new byte[4096];
				int len;
				while ((len = tempIs.read(buffer)) > 0) {
					out.write(buffer, 0, len);
					out.flush();
				}
				return true;
			} catch (Throwable e) {	
				return false;
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) { }
				}
				if (tempIs != null) {
					try {
						tempIs.close();
					} catch (IOException e) {}
				}
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {}
				}
			}
		} else {
			if (bodyLength == contentLength || contentLength == -1) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	@Override
	public void close() {
		// not work
	}

}
