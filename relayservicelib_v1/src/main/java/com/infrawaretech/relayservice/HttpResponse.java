package com.infrawaretech.relayservice;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.infrawaretech.relayservice.client.RelayClientException;
import com.infrawaretech.relayservice.utils.HTTPHeader;

public class HttpResponse {
	String TAG = HttpResponse.class.getSimpleName();
	
	public static final int OK = 200;
	
	private HTTPHeader mHeader;
	private byte[] mContentBytes;

	public static HttpResponse newInstance(HTTPHeader header, InputStream content) throws RelayClientException {
		HttpResponse ret = new HttpResponse();
		ret.mHeader = header;
		if (ret.isApplicationContentType()) {
			ret.mContentBytes = read2String(content).getBytes();
		} else {
			byte[] bytes = read2Bytes(content);
			ret.mContentBytes = bytes;
		}
		return ret;
	}
	// InputStream -> String
	private static String read2String(InputStream is) {
		String ret = null;
		StringBuilder builder = new StringBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			char[] buf = new char[10 * 1024];
			int readByte = 0;

			while ((readByte = br.read(buf)) != -1) {
				builder.append(buf, 0, readByte);
			}
			ret = URLDecoder.decode(builder.toString(), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (Exception e) { }
		}
		return ret;
	}
	
	// InputStream -> byte[]
	private static byte[] read2Bytes(InputStream pInputStream) {
		if (pInputStream == null) {
			return null;
		}

		int lBufferSize = 4 * 1024;
		byte[] lByteBuffer = new byte[lBufferSize];

		int lBytesRead = 0;
		int lTotbytesRead = 0;
		int lCount = 0;

		ByteArrayOutputStream lByteArrayOutputStream = new ByteArrayOutputStream(
				lBufferSize);

		try {
			while ((lBytesRead = pInputStream.read(lByteBuffer)) != -1) {
				lTotbytesRead += lBytesRead;
				lCount++;

				lByteArrayOutputStream.write(lByteBuffer, 0, lBytesRead);
			}
		} catch (Throwable e) {
			e.printStackTrace(System.out);
		}

		byte[] lDataBytes = lByteArrayOutputStream.toByteArray();
		return lDataBytes;
	}
	
//	public static HttpResponse newInstance(HTTPHeader header, byte[] content) throws RelayClientException {
//		HttpResponse ret = new HttpResponse();
//		
//		InputStream in = null; 
//		InputStreamReader reader = null;
//		BufferedReader bufferedReader = null;
//		try {
//			in = new ByteArrayInputStream(content);
//			reader = new InputStreamReader(in, "UTF-8");
//			bufferedReader = new BufferedReader(reader);
//			StringBuilder sb = new StringBuilder();
//			char[] buffer = new char[1024 * 8];
//			int cnt = 0;
//			while ((cnt = bufferedReader.read(buffer)) != -1) {
//				sb.append(buffer, 0, cnt);
//			}
//			ret.mContent = sb.toString();
//			ret.mHeader = header;
//		} catch (UnsupportedEncodingException e) {
//			throw new RelayClientException(RelayClientException.ERRNO_UNSUPPORTED_ENCODING, e);
//		} catch (IOException e) {
//			throw new RelayClientException(RelayClientException.ERRNO_CAN_NOT_READ, e);
//		} finally {
//			if (bufferedReader != null)
//				try {
//					bufferedReader.close();
//				} catch (Exception _) { }
//			if (reader != null)
//				try {
//					reader.close();
//				} catch (Exception _) { }
//			if  (in != null)
//				try {
//					in.close();
//				} catch (Exception _) { }
//		}
//		return ret;
//	}

	/* hide */
	private HttpResponse() { }
	
	public int getStatusCode() {
		return mHeader.getStatusCode();
	}
	
	public String getContentType() {
		return mHeader.getContentType();
	}
	
	public boolean isApplicationContentType() {
		if (getContentType().startsWith("application/json")) {
			return true;
		}
		return false;
	}
	
	public boolean isImageContentType() {
		if (getContentType().contains("image/")) {
			return true;
		}
		return false;
	}
	
	public boolean isTextContentType() {
		if (getContentType().contains("text/html")) {
			return true;
		}
		return false;
	}
	
	public String getContent() {
		String ret = "";
		try {
			ret = new String(this.mContentBytes, "UTF-8"); 
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
/*	public List<Byte> getContentBytes() {
		return this.mContentBytes;
	}*/
	public byte[] getContentBytes() {
		return this.mContentBytes;
	}
	
	public String getMoPage() {
		return mHeader.getMOPageCount();
	}
	
	public String getMoConverTing() {
		return mHeader.getMOConverting();
	}
	
	public String getMoPageWidth() {
		return mHeader.getMOPageWidth();
	}
	
	public String getMoPageHeight() {
		return mHeader.getMOPageHeight();
	}
	
	public String getMoHashCode() {
		return mHeader.getMOHashCode();
	}
	
	public String getMoErrCode() {
		return mHeader.getMOErrCode();
	}
	
	public String getMoState() {
		return mHeader.getMOState();
	}
	
	public void clear() {
		mHeader.clear();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(TAG).append("]");
		sb.append(" StateCode = ").append(getStatusCode());
		sb.append(", ContentType = ").append(getContentType());
		sb.append(", MoErrCode = ").append(getMoErrCode());
		sb.append(", MoState = ").append(getMoState());
		sb.append(", MoConvert = ").append(getMoConverTing());
		sb.append(", MoHashCode = ").append(getMoHashCode());
		sb.append(", MoPageCount = ").append(getMoPage());
		sb.append(", MoPageWidth = ").append(getMoPageWidth());
		sb.append(", MoPageHeight = ").append(getMoPageHeight());
		return sb.toString();
	}
}
