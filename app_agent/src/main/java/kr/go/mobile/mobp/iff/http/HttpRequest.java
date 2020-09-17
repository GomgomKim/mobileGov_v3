package kr.go.mobile.mobp.iff.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;

import kr.go.mobile.iff.util.Utils;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;
import kr.go.mobile.mobp.iff.R;

abstract public class HttpRequest {
	E2ESetting e2eSetting = new E2ESetting();
	
	private String httpVersion = "1.0";
	
	private String host = "localhost";
	
	private String port = "443";
	
	private String schem = "https";
	
	URL url = null;
	
//	private String mobpUrl = e2eSetting.getHostUrl();
	
	Map<String, String> defaultHeaders = new HashMap<String,String> ();
	
	private String bodyString;
	
//	private Map<String, Object> attachBodyMap;
	
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
	
	
	public HttpRequest (final Context c, String serviceId) throws MalformedURLException {
		
		this.serviceId = serviceId;
		String host = Utils.decrypt(c.getString(R.string.MagicMRSLicense), c.getString(R.string.agenturl));
		this.url = new URL(host);
		this.host = url.getHost();
		this.port = String.valueOf(url.getPort());
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
		defaultHeaders.put(HttpHeaderConstans.HOST, getURL().getHost() + (getURL().getPort() > 0 ? ":"+getURL().getPort() : ""));
		defaultHeaders.put(HttpHeaderConstans.SERVICE_ID, this.serviceId);
		defaultHeaders.put(HttpHeaderConstans.X_AGENT_DETAIL, this.agentDetail);
		
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

//	public Map<String, Object> getAttachBodyMap() {
//		return attachBodyMap;
//	}
	
//	public Map<String, Object> getBodyMap() {
//		return bodyMap;
//	}

	/**
	* @Method Name	:	setBodyMap
	* @작성일			:	2015. 10. 14. 
	* @작성자			:	조명수
	* @변경이력		:
	* @Method 설명 	: 	업로드 경로 url 저장
	*/
//	public void setBodyMap(Map<String, Object> bodyMap) {
//		this.bodyMap = bodyMap;
//	}
	
	public ArrayList<String> getBodyList() {
		return bodyList;
	}
	
	public void setBodyList(ArrayList<String> list) {
		this.bodyList = list;
	}

//	public void setAttachBodyMap(Map<String, Object> attachBodyMap) {
//		this.attachBodyMap = attachBodyMap;
//	}
	
//	public String getMobpUrl() {
//		return mobpUrl;
//	}
//
//	public void setMobpUrl(String mobpUrl) {
//		this.mobpUrl = mobpUrl;
//	}
	
	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

}
