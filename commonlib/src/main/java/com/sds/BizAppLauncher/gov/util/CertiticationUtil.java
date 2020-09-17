package com.sds.BizAppLauncher.gov.util;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CertiticationUtil {

	/**
	 * 사용자 이름을 요청한다.
	 */
	public static final String KEY_NICKNAME = "gov:nickname";
	/**
	 * 사용자 DN 값을 요청한다. 
	 */
	static final String KEY_DN = "gov:dn";
	/**
	 * 사용자 ID 정보를 요청한다. 
	 */
	public static final String KEY_CN = "gov:cn";
	/**
	 * 사용자 부서 정보를 요청한다.
	 */
	public static final String KEY_OU = "gov:ou";
	/**
	 * 사용자 부서 코드 값을 요청한다.
	 */
	public static final String KEY_OU_CODE = "gov:oucode";
	/**
	 * 사용자 부서 (?) 정보를 요청한다.
	 */
	public static final String KEY_DEPARTMENT = "gov:department";
	/**
	 * 사용자 부서 (?) 코드 값을 요청한다.
	 */
	public static final String KEY_DEPARTMENT_NUMBER = "gov:departmentnumber";

	private Map<String, String> info = new HashMap<String, String>();
	
	public void setRequestData(String key) {
		info.put(key, "");
	}
	
	public String toRequestData() {
		JSONArray json = new JSONArray();
		for (String key : info.keySet()) {
			json.put(key);
		}
		return json.toString();
	}
	
	private void setValue(String key, String value) {
		info.put(key, value);
	}
	
	public String getInfo(String key) {
		return this.info.get(key);
	}
	
	public static CertiticationUtil parse(String result) throws JSONException {
		/* 
		 * [
		 * 		{"gov:nickname":"임석일"},
		 * 		{"gov:cn":"300임석일001"},
		 * 		{"gov:ou":"행정안전부"},
		 * 		{"gov:oucode":"1741000"},
		 * 		{"gov:department":"행정안전부"},
		 * 		{"gov:departmentnumber":"1741000"}
		 * ]
		 */
		JSONArray array = new JSONArray(result);
		CertiticationUtil util = new CertiticationUtil();

		for (int idx = 0 ; idx < array.length() ; idx++) {
			JSONObject obj = (JSONObject) array.get(idx);
			String key = obj.keys().next();
			String value = (String) obj.get(key);
			util.setValue(key, value);
		}
		return util;
	}
}
