package com.infrawaretech.relayservice;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.infrawaretech.relayservice.utils.HTTPHeader;

abstract public class HttpRequest {
	private String httpVersion = "1.0";
	
	private String host = "localhost";
	
	private String port = "443";
	
	private String schem = "https";
	
	URL url = null;
	
	
	Map<String, String> defaultHeaders = new HashMap<String,String> ();
	
	private String bodyString;
	
	private Map<String, Object> bodyMap;
	
	private ArrayList<String> bodyList;
	
	private URL attachUrl;
	
	private String multiPartEndString;
	
	protected String serviceId = null;
	
	private String agentDetail = null;
	
	private String contentType = null;
	

	private String strAttachFilePath = "";
	
	public String getAttachFilePath() {
		return strAttachFilePath;
	}

	public void setAttachFilePath(final String path) {
		this.strAttachFilePath = path;
	}
	
	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getAgentDetail() {
		return agentDetail;
	}

	public void setAgentDetail(String agentDetail) {
		this.agentDetail = agentDetail;
	}

	public String getMultiPartEndString() {
		return multiPartEndString;
	}

	public void setMultiPartEndString(String multiPartEndString) {
		this.multiPartEndString = multiPartEndString;
	}

	public URL getAttachUrl() {
		return attachUrl;
	}

	public void setAttachUrl(URL attachUrl) {
		this.attachUrl = attachUrl;
	}
	
	@Deprecated
	public HttpRequest (String serviceId) throws MalformedURLException {
		// TB :
		 this(serviceId, "https://10.1.1.40:443/mois/rpc");
		// IFF :
//		this(serviceId, "https://10.1.1.30:443/mois/rpc");
	}
	
	public HttpRequest (String serviceId, String hostUrl) throws MalformedURLException {
		this.serviceId = serviceId;
		this.url = new URL(hostUrl);
		this.host = url.getHost();
		this.port = String.valueOf(url.getPort());
	}

	public URL getURL() {
		return this.url;
	}
	
	public Map<String, String> initDefaultHeader() {
		defaultHeaders.put(HTTPHeader.HOST, getURL().getHost() + (getURL().getPort() > 0 ? ":"+getURL().getPort() : ""));
		defaultHeaders.put(HTTPHeader.SERVICE_ID, this.serviceId);
		defaultHeaders.put(HTTPHeader.X_AGENT_DETAIL, this.agentDetail);
		
		return defaultHeaders;
	}
	
	abstract public Map<String, String> generateHeader();
	
	abstract public String generateCommandLine();
	
	public String getHttpVersion() {
		return httpVersion;
	}

	public void setHttpVersion(String httpVersion) {
		this.httpVersion = httpVersion;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getSchem() {
		return schem;
	}

	public void setSchem(String schem) {
		this.schem = schem;
	}
	
	public String getBodyString() {
		return bodyString;
	}

	public void setBodyString(String bodyString) {
		this.bodyString = bodyString;

	}
	
	public ArrayList<String> getBodyList() {
		return bodyList;
	}
	
	@Deprecated
	public void setBodyList(ArrayList<String> list) {
		this.bodyList = list;
	}
	
	public void addBodyParam(String param) {
		if (this.bodyList == null) {
			bodyList = new ArrayList<String>();
		}
		bodyList.add(param);
	}
	
	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	
	public void clear() {
		if (bodyList != null)
			bodyList.clear();
		if (bodyMap != null)
			bodyMap.clear();
		if (defaultHeaders != null)
			defaultHeaders.clear();
	}

}
