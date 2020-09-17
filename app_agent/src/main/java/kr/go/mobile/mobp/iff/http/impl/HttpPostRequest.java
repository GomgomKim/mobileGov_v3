package kr.go.mobile.mobp.iff.http.impl;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import kr.go.mobile.mobp.iff.http.HttpHeaderConstans;
import kr.go.mobile.mobp.iff.http.HttpRequest;

import org.apache.commons.lang.StringUtils;

import android.content.Context;
import android.util.Log;


public class HttpPostRequest extends HttpRequest {
	
	public static String method = "POST";
	
	public HttpPostRequest(final Context c, String serviceId) throws MalformedURLException {
		super(c, serviceId);
	}
	
	public HttpPostRequest(String serviceId, String hostUrl) throws MalformedURLException {
		super(serviceId, hostUrl);
	}

	@Override
	public Map<String, String> generateHeader() {
		Map<String, String> headers = initDefaultHeader();
		if (getBodyList() != null && getBodyList().size() > 0) {
			StringBuffer sbBody = new StringBuffer();
			if(getBodyString() != null) {
				sbBody.append(getBodyString()).append("&");
			}
			
			try {
				for (int i = 0; i < getBodyList().size(); i++) {
					String data = getBodyList().get(i).trim();
					String value = "";

					String[] splitData = data.split("=");
					if (splitData.length == 2) {
						value = URLEncoder.encode(splitData[1], "UTF-8");
						value = StringUtils.replace(value, "+", "%20");
						sbBody.append(splitData[0]).append("=").append(value).append("&");
					} else if (splitData.length == 1) {
						sbBody.append(splitData[0]).append("=").append(value).append("&");
					} else if(splitData.length > 2){						
						value = URLEncoder.encode(data.substring(data.indexOf("=")+1, data.length()), "UTF-8");
						value = StringUtils.replace(value, "+", "%20");
						sbBody.append(data.substring(0, data.indexOf("="))).append("=").
									append(value).append("&");
					}					
				}

				String resultData = sbBody.substring(0, sbBody.length() - 1);

				if (resultData.length() > 0) {
					setBodyString(resultData);
				}else{
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}			
			////
		}else{
			try {
				if(getBodyString() != null){
					setBodyString(URLEncoder.encode(getBodyString(), "UTF-8"));
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		if(getContentType() == null) {
			headers.put(HttpHeaderConstans.CONTENT_TYPE, HttpHeaderConstans.CONTENT_POST_VALUE);
		}
		else {
			headers.put(HttpHeaderConstans.CONTENT_TYPE, getContentType());
		}
		
		if(getBodyString() != null){
			headers.put(HttpHeaderConstans.CONTENT_LENGTH, getBodyString().getBytes().length+"");
		}

		return headers;
	}

	@Override
	public String generateCommandLine() {
		URL url = getURL();
		String query = url.getQuery();
		
		return method + " " + getURL().getPath() + " HTTP/" + getHttpVersion();
	}

}
