package com.infrawaretech.relayservice.impl;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import com.infrawaretech.relayservice.HttpRequest;
import com.infrawaretech.relayservice.utils.HTTPHeader;
import com.infrawaretech.relayservice.utils.StringUtils;

public class HttpMultiPartRequest extends HttpRequest {
	
	public static String method = "POST";
	
	private String boundary = "";

	private String strFilePath = "";

	@Deprecated
	public HttpMultiPartRequest(String serviceId) throws MalformedURLException {
		super(serviceId);
	}
	
	public String getAttachFilePath() {
		return strFilePath;
	}
	
	public void setAttachFilePath(final String filePath) {
		strFilePath = filePath;
	}


	@Override
	public Map<String, String> generateHeader() {
		
		this.boundary = "--MOBPMultiPart" + System.currentTimeMillis();
		
		StringBuffer edBody = new StringBuffer();
		edBody.append("--").append(this.boundary).append("--").append(HTTPHeader.CRLF);
		
		setMultiPartEndString(edBody.toString());

		Map<String, String> headers = initDefaultHeader();
		if (getBodyList() != null && getBodyList().size() > 0) {
			StringBuffer sbBody = new StringBuffer();
			if (getBodyString() != null) {
				sbBody.append(getBodyString()).append("&");
			}

			try {
				for (int i = 0; i < getBodyList().size(); i++) {					
					String data = getBodyList().get(i);
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

//					sbBody.append(splitData[0]).append("=").append(value).append("&");
				}

				String resultData = sbBody.substring(0, sbBody.length() - 1);

				if (resultData.length() > 0) {
					setBodyString(resultData);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		if(getBodyString() != null) {
			StringBuffer sbBody = new StringBuffer();
			String [] arrParamPair = getBodyString().split("[&]");
			for(String paramPair : arrParamPair) {
				String [] arrParam = paramPair.split("[=]" , -1);
				if(arrParam != null && arrParam.length > 0) {
					sbBody.append("--").append(this.boundary).append(HTTPHeader.CRLF);
					sbBody.append("Content-Disposition: form-data; name=\"").append(arrParam[0]).append("\"").append(HTTPHeader.CRLF);
					sbBody.append(HTTPHeader.CRLF);
					if(arrParam.length > 1) {
						sbBody.append(arrParam[1]).append(HTTPHeader.CRLF);
					}
				}
			}
			
			if(getAttachUrl() != null) {
				setBodyString(sbBody.toString());
			}else{
				try {
					setBodyString(URLEncoder.encode(sbBody.toString(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
	
		File file = null;
		
		if(getAttachUrl() != null) {
			StringBuffer sbBody = new StringBuffer();
			file = new File(getAttachUrl().getFile());
			sbBody.append("--").append(this.boundary).append(HTTPHeader.CRLF);
			sbBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"").append(HTTPHeader.CRLF);
			sbBody.append(HTTPHeader.CONTENT_TYPE).append(": application/octet-stream").append(HTTPHeader.CRLF);
			sbBody.append("Content-Transfer-Encoding: binary").append(HTTPHeader.CRLF);
			
			if(getBodyString() == null) {
				setBodyString(sbBody.toString());
			}
			else {
				setBodyString(getBodyString() + sbBody.toString());
			}
		}
		
		headers.put(HTTPHeader.CONTENT_TYPE, HTTPHeader.CONTENT_MULTIPART_VALUE+this.boundary);
		headers.put(HTTPHeader.CONTENT_LENGTH, getBodyString().getBytes().length + edBody.toString().getBytes().length + (file != null ? file.length() : 0) +"");
		
		return headers;
	}
	
	

	
	@Override
	public String generateCommandLine() {
		URL url = getURL();
		String query = url.getQuery();
		
		return method + " " + getURL().getPath() + " HTTP/" + getHttpVersion();
	}

}
