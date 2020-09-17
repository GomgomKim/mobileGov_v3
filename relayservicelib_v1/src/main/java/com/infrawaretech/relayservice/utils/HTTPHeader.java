package com.infrawaretech.relayservice.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;

import com.infrawaretech.relayservice.connection.RelayUrlConnection;

public class HTTPHeader {
	static final String TAG = HTTPHeader.class.getSimpleName();

	private static final String CONTENT_DISPOSITION = "Content-Disposition";
	private static final String SERVER = "Server";
	private static final String CONNECTION = "Connection";
	private static final String CHUNKED = "Transfer-Encoding: chunked";
	private static final String RESPONSE_CODE = "resp_code";
	private static final String MO_PAGECOUNT = "MO_PAGECOUNT";
	private static final String MO_CONVERTING = "MO_CONVERTING";
	private static final String MO_HASHCODE = "MO_HASHCODE";
	private static final String MO_PAGEWIDTH = "MO_PAGEWIDTH";
	private static final String MO_PAGEHEIGHT = "MO_PAGEHEIGHT";
	private static final String MO_ERRCODE = "MO_ERRCODE";
	private static final String MO_STATE = "MO_STATE";
	
	public static final String HOST = "Host";
	public static final String ORIGIN_URL = "Origin-Url";
	public static final String SERVICE_ID = "Service-Id";
	public static final String X_AGENT_DETAIL = "X-Agent-Detail";
	public static final String CRLF = "\r\n";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String CONTENT_MULTIPART_VALUE = "multipart/form-data; charset=UTF-8; boundary=";
	public static final String CONTENT_POST_VALUE = "application/x-www-form-urlencoded; charset=UTF-8";
	
	// Deprecated 
	private static final String HTTP_VERSION = "HTTP/1.1";
	private static final String METHOD = "POST ";
	private static final String USER_AGENT = "User-Agent";
	private static final String CONNECTION_CLOSE = "Connection: close\r\n";
	private static final String CHUNKING = CHUNKED + CRLF;

	public static HTTPHeader parse(RelayUrlConnection conn) {
		Log.d(TAG, "read HttpHeader ");
		
		HTTPHeader header = new HTTPHeader();
		
		int resp_code = conn.getResponseCode();
		header.setValue(RESPONSE_CODE, Integer.toString(resp_code));
		
		Map<String, List<String>> map = conn.getHeaderFields();
		Set<String> keys = map.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			List<String> values = map.get(key);
			// list -> strings
			StringBuilder sb = new StringBuilder();
			Iterator<String> vIt = values.iterator();
			while (vIt.hasNext()) {
				sb.append((String) vIt.next()).append(",");
			}
			sb.deleteCharAt(sb.length()-1);
			Log.i(TAG, "key=" + key + ", values=" + sb.toString());
			if (key == null) 
				continue;
			header.setValue(key, sb.toString());
		}
		return header;
	}
	
	public static boolean isChunked(RelayUrlConnection conn) {
		String strHeaderFieldChunked = conn.getHeaderField(HTTPHeader.CHUNKED);
		if (strHeaderFieldChunked != null && strHeaderFieldChunked.length() > 0) {
			return true;
		}
		return false;
	}
	
	public static int getContentLength(RelayUrlConnection conn) {
		return Integer.parseInt(conn.getHeaderField(CONTENT_LENGTH));
	}
	
	private final Map<String, String> m;
	
	private HTTPHeader() {
		m = new HashMap<String, String>();
	}
	
	public void setValue(String key, String value) {
		m.put(key, value);
	}

	public String getConnection() {
		return m.get(CONNECTION);
	}

	public String getServer() {
		return m.get(SERVER);
	}

	public String getContentType() {
		return m.get(CONTENT_TYPE);
	}

	public String getHost() {
		return m.get(HOST);
	}
	
	public String getContentDisposition() {
		return m.get(CONTENT_DISPOSITION);
	}

	public boolean isChunked() {
		String t = m.get(CHUNKED);

		if (t != null && t.length() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public int getStatusCode() {
		try {
			return Integer.valueOf(m.get(RESPONSE_CODE));
		} catch (Exception e) {
			return -1;
		}
	}

	public String getMOPageCount() {
		return m.get(MO_PAGECOUNT);
	}

	public String getMOConverting() {
		return m.get(MO_CONVERTING);
	}
	
	public String getMOPageWidth() {
		return m.get(MO_PAGEWIDTH);
	}
	
	public String getMOPageHeight() {
		return m.get(MO_PAGEHEIGHT);
	}
	
	public String getMOHashCode() {
		return m.get(MO_HASHCODE);
	}

	public int getContentLength() {
		String t = m.get(CONTENT_LENGTH);
		
		if ( t == null) return -1;
		
		return Integer.parseInt(t);
	}
	
	public String getMOErrCode() {
		return m.get(MO_ERRCODE);
	}
	
	public String getMOState() {
		return m.get(MO_STATE);
	}

	public void clear() {
		m.clear();
	}
}
