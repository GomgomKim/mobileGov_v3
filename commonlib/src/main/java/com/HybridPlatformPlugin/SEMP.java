package com.HybridPlatformPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.ServiceBrokerLib.ServiceBrokerCB;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;
import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPlugin;
import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPluginResult;
import com.sds.mobiledesk.mdhybrid.common.MDHCommon;
import com.sds.mobiledesk.mdhybrid.common.MDHCommonError;
import com.sds.mobiledesk.mdhybrid.common.MDHCommonException;

import android.content.Intent;
import android.util.Log;
import kr.go.mobile.iff.util.LogUtil;

public class SEMP extends MDHPlugin {

	// 해당 값들은 하이브리드 앱에서 호출시 들어오는 json 파라메터의 필드 네임들..
	private static final String JSON_DATATYPE = "dataType";
	private static final String JSON_SCODE = "sCode";
	private static final String JSON_PARAMETER = "parameter";
	private static final String JSON_IPADDRESS = "ipAddress";
	private static final String JSON_PORTNUMBER = "portNumber";
	private static final String JSON_CONNECTIONTYPE = "connectionType";
	private static final String JSON_CONTEXTURL = "contextUrl";
	private static final String JSON_FILEPATH = "filePath";
	private static final String JSON_FILENAME = "fileName";
	private static final String JSON_TIMEOUTINTERVAL = "timeoutInterval";
	
	private String returnDataType = "json"; // 기본은 json 타입

	public MDHPluginResult getVersions()
	{
		MDHCommon.LOG("Get SEMP's Version");

		String version = "1.08.with.VPN.20110602";

		return new MDHPluginResult(0, version);
	}

	public MDHPluginResult request(String jsonArgs)
	{
		Log.d("pjh", "SEMP request jsonArgs :: " + jsonArgs);
		MDHCommon.LOG("");
		try{
			if ((jsonArgs == "") || (jsonArgs.equals("undefined"))) {
				throw new MDHCommonException(MDHCommonError.MDH_INVALID_ARGUMENT);
			}
			

			JSONObject jsonObj = (JSONObject)new JSONTokener(jsonArgs).nextValue();
			final String cbId = getCurrentCallbackId();
			
			ResponseListener resListener = new ResponseListener(){
				public void receive(ResponseEvent re){
					//Log.d("jms", "Listener received(" + re.getResultCode() + ") : " + re.getResultData());
					MDHCommon.LOG("Listener received(" + re.getResultCode() + ") : " + re.getResultData());
					String sempMessage = "";

					int result = re.getResultCode();

					switch (result) {
					case MDHCommonError.MDH_SUCCESS:
						if (re.getResultData().length() == 0) {
							SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_SUCCESS, sempMessage));
						} 
						else if (returnDataType.equalsIgnoreCase("xml")){
							String resultString = re.getResultData();
							resultString = resultString.replace("\"", "'");
							resultString = resultString.replace("\r", " ");
							resultString = resultString.replace("\n", " ");
							resultString = resultString.replace("\t", " ");
							resultString = resultString.replace("\\", " ");

							sempMessage = "\"" + resultString + "\"";
							MDHCommon.LOG("Success SB: Listener received(request) : " + re.getResultData());
							SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_SUCCESS, sempMessage));
						} else {
							sempMessage = re.getResultData();
							try{
								//msjo@dkitec.com 2015-06-08 jsonEXCEPTION 발생하여 주석
//								sempMessage = sempMessage.replace("\\", " ");

								if (sempMessage.trim().startsWith("[")) {
									JSONArray jsonData = (JSONArray)new JSONTokener(sempMessage).nextValue();
									MDHCommon.LOG("Success SB(JSONArray): Listener received(request) : " + re.getResultData());
									SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_SUCCESS, jsonData));
								}else if(sempMessage.trim().startsWith("{")){
									JSONObject jsonData = (JSONObject)new JSONTokener(sempMessage).nextValue();
									MDHCommon.LOG("Success SB(JSONObject): Listener received(request) : " + jsonData);
									SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_SUCCESS, jsonData));	
								}else {
									MDHCommon.LOG("Success SB(JSONObject): Listener received(request) : " + re.getResultData());
									SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_SUCCESS, sempMessage));
								}
							}
							catch (JSONException e){
								LogUtil.e(getClass(), e.getMessage());
								SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_JSON_EXP_ERROR, sempMessage));
							}
						}

						break;
					default:
						sempMessage = re.getResultData();
						JSONObject ErrorData = new JSONObject();
						try {
							ErrorData.put("resultCode", result);
							ErrorData.put("resultMsg", sempMessage);
							ErrorData.put("resultData", "");
						} catch (JSONException e) {
							LogUtil.e(getClass(), e.getMessage());
							SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_JSON_EXP_ERROR, sempMessage));
						}
						MDHCommon.LOG("Error SB(JSONArray): Listener received(request) : " + ErrorData.toString());
						SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_ERROR_RETURN_JSONOBJ, ErrorData));
					}
				}
			};
			
			ServiceBrokerCB serviceBrokerCB = new ServiceBrokerCB() {
				
				@Override
				public void onServiceBrokerResponse(String retMsg) {
					try{
						Log.d("jms", "Listener received onServiceBrokerResponse : " + retMsg);

						if (retMsg.trim().startsWith("[")) {
							JSONArray jsonData = (JSONArray)new JSONTokener(retMsg).nextValue();
							SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_SUCCESS, jsonData));
						}else if(retMsg.trim().startsWith("{")){
							JSONObject jsonData = (JSONObject)new JSONTokener(retMsg).nextValue();
							SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_SUCCESS, jsonData));	
						}else {
							SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_SUCCESS, retMsg));
						}
					}
					catch (JSONException e){
						LogUtil.e(getClass(), e.getMessage());
						SEMP.this.sendAsyncResult(cbId, new MDHPluginResult(MDHCommonError.MDH_JSON_EXP_ERROR, e.toString()));
					}
					
				}
			};
			ServiceBrokerLib lib = new ServiceBrokerLib(getContext(), resListener);
			
//			ServiceBrokerLib lib = new ServiceBrokerLib(getContext(), resListener, serviceBrokerCB);
			Intent intent = new Intent();
			
			// 현재 지정한 필드이외의 경우에는 인텐트에 들어가지 않으며 서비스브로커로 요청되지 않음에 주의
			putRequestParameter(intent, jsonObj, JSON_DATATYPE);
			putRequestParameter(intent, jsonObj, JSON_SCODE);
			putRequestParameter(intent, jsonObj, JSON_IPADDRESS);
			putRequestParameter(intent, jsonObj, JSON_PORTNUMBER);
			putRequestParameter(intent, jsonObj, JSON_CONNECTIONTYPE);
			putRequestParameter(intent, jsonObj, JSON_CONTEXTURL);
			
			putRequestParameter(intent, jsonObj, JSON_PARAMETER);
			putRequestParameter(intent, jsonObj, JSON_FILEPATH);
			putRequestParameter(intent, jsonObj, JSON_FILENAME);
			
			putRequestParameter(intent, jsonObj, JSON_TIMEOUTINTERVAL);


			lib.request(intent);			
		}
		catch (MDHCommonException e)
		{
			return new MDHPluginResult(MDHCommonError.MDH_UNKNOWN_ERROR, e.getErrorCode());
		} catch (JSONException e2) {
			return new MDHPluginResult(MDHCommonError.MDH_JSON_EXP_ERROR, "");
		} catch (Exception e) {
			return new MDHPluginResult(MDHCommonError.MDH_UNKNOWN_ERROR, "");
		}

		return new MDHPluginResult(MDHCommonError.MDH_START_ACTIVITY_FOR_RESULT);
	}
	
	private void putRequestParameter(Intent reqIntent, final JSONObject jsonObj, final String reqField) throws JSONException{
		if(jsonObj.has(reqField)){
			String str = "";
			Object obj = jsonObj.get(reqField);
			if(obj instanceof String){
				LogUtil.d(getClass(), "Semp instanceof String");
				str = (String)jsonObj.get(reqField);
				reqIntent.putExtra(reqField, str); 
			}else if(obj instanceof JSONObject){
				LogUtil.d(getClass(), "Semp instanceof JSONObject");
				reqIntent.putExtra(reqField, obj.toString()); 
			}else if(obj instanceof Boolean){
				LogUtil.d(getClass(), "Semp instanceof Boolean");
				reqIntent.putExtra(reqField, Boolean.valueOf(obj.toString())); 
			}else if(obj instanceof Integer){
				LogUtil.d(getClass(), "Semp instanceof Integer");
				reqIntent.putExtra(reqField, Integer.valueOf(obj.toString())); 
			}else{
				LogUtil.d(getClass(), "Semp instanceof ELES");
				reqIntent.putExtra(reqField, obj.toString()); 
			}

			
			// DATATYPE 필드의 경우 콜백합수에서 쓰므로 저장...
			if(reqField == JSON_DATATYPE && str.length() > 0){
				returnDataType = str;
			}
			LogUtil.d(getClass(), "SEMP request ["+reqField+"] : " + str);
		}
	}

	public void onPause(){
		//Override
	}

	public void onResume(){
		//Override
	}

	public void onDestroy(){
		//Override
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent){
		//Override
	}
}
