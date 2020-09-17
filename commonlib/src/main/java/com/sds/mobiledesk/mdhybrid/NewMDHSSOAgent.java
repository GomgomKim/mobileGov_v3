package com.sds.mobiledesk.mdhybrid;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.ServiceBrokerLib.ServiceBrokerCB;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;
import com.sds.mobiledesk.mdhybrid.common.MDHCommon;
import com.sds.mobiledesk.mdhybrid.common.MDHCommonError;

import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import kr.go.mobile.iff.util.LogUtil;

public class NewMDHSSOAgent {

	Context mContext;
	WebView mWebView;

	int jarrayLength = 0;
	JSONArray reqJsonarray = null;
	ArrayList<String> reqArraylist = null;

	public NewMDHSSOAgent(Context context, WebView webview) {
		mContext = context;
		mWebView = webview;
	}

	public String getInfo(final String callback, final String sso_req_list) {

		new Thread(new Runnable() {

			@Override
			public void run() {
				if (sso_req_list == "" || sso_req_list.equals("undefined")) {
					((com.sds.mobiledesk.mdhybrid.MDHActivity.MDHybridActivity.a) mWebView).a(MDHCommon.makeErrorResult(callback,
							Integer.toString(MDHCommonError.MDH_INVALID_ARGUMENT)));
					return;
				}
				try {
					MDHCommon.LOG((new StringBuilder()).append("arrayData is ").append(sso_req_list).toString());
					reqJsonarray = (JSONArray) (new JSONTokener(sso_req_list)).nextValue();
					MDHCommon.LOG((new StringBuilder()).append("arrayObj.length is ").append(reqJsonarray.length()).toString());
					jarrayLength = reqJsonarray.length();
					reqArraylist = new ArrayList<String>();
					for (int i = 0; i < jarrayLength; i++) {
						String value = reqJsonarray.getString(i);
						reqArraylist.add(value);
					}

					ServiceBrokerCB svcBrokerCB = new ServiceBrokerCB() {
						String TAG = "NewMDHSSOAgent.onServiceBrokerResponse";
						
						@Override
						public void onServiceBrokerResponse(String retMsg) {
							
							LogUtil.d(TAG, retMsg);

							try {
								HashMap<String, String> hashmap = convertToHashMap(retMsg, callback);

								MDHCommon.LOG((new StringBuilder()).append("ssoInfo.size() is ").append(hashmap.size()).toString());

								if (jarrayLength != hashmap.size())
									MDHCommon.LOG("key is not matched");
								
								JSONStringer jsonstringer = new JSONStringer();
								jsonstringer.object();

								LogUtil.d(TAG, "reqData size : " + reqArraylist.size());

								for (int j = 0; j < reqArraylist.size(); j++) {
									String key = (String) reqArraylist.get(j);

									if (hashmap.containsKey(key)) {
										jsonstringer.key(key).value(hashmap.get(key));
									} else {
										jsonstringer.key(key).value("");
									}
								}

								jsonstringer.endObject();
								
								LogUtil.d(TAG, "response Data  : " + jsonstringer.toString());
								
								((com.sds.mobiledesk.mdhybrid.MDHActivity.MDHybridActivity.a) mWebView).a(MDHCommon.makeSuccessResult(callback,
										jsonstringer.toString()));
							} catch (JSONException je) {
								LogUtil.e(TAG, je.getMessage());
							} catch (Exception e){
								LogUtil.e(TAG, e.getMessage());
							}
						}
					};

					ResponseListener listener = new ResponseListener() {
						
						String TAG = "NewMDHSSOAgent.receive";
						
						public void receive(ResponseEvent re) {
							// 서버로부터 받은 응답은 ResponseEvent 객체 re에 저장된다.
							int errorCode = re.getResultCode();
							String errorMsg = re.getResultData();

							LogUtil.d(TAG, "errorCode : " + errorCode);
							LogUtil.d(TAG, "errorMessage : " + errorMsg);
						}
					};
					
					ServiceBrokerLib lib = new ServiceBrokerLib(mContext, listener, svcBrokerCB);
					Intent intent = new Intent();
					intent.putExtra(ServiceBrokerLib.KEY_SERVICE_ID, "getInfo");
					intent.putExtra(ServiceBrokerLib.KEY_PARAMETER, sso_req_list);
					intent.putExtra("dataType", "json");

					lib.request(intent);
					
				} catch (JSONException jsonexception) {
					((com.sds.mobiledesk.mdhybrid.MDHActivity.MDHybridActivity.a) mWebView).a(MDHCommon.makeErrorResult(callback,
							Integer.toString(MDHCommonError.MDH_JSON_EXP_ERROR)));
				} catch (Exception exception) {
					((com.sds.mobiledesk.mdhybrid.MDHActivity.MDHybridActivity.a) mWebView).a(MDHCommon.makeErrorResult(callback,
							Integer.toString(MDHCommonError.MDH_UNKNOWN_ERROR)));
				}
				return;
			}
			
			private HashMap<String, String> convertToHashMap(String jsonString, String callback) {
				HashMap<String, String> myHashMap = new HashMap<String, String>();
				try {
					JSONArray jArray = new JSONArray(jsonString);
					JSONObject jObject = null;
					String keyString = null;
					for (int i = 0; i < jArray.length(); i++) {
						jObject = jArray.getJSONObject(i);
						keyString = (String) jObject.names().get(0);
						myHashMap.put(keyString, jObject.getString(keyString));
					}
				} catch (JSONException jsonexception) {
					LogUtil.e(getClass(), "" + jsonexception.getMessage());
					((com.sds.mobiledesk.mdhybrid.MDHActivity.MDHybridActivity.a) mWebView).a(MDHCommon.makeErrorResult(callback,
							Integer.toString(MDHCommonError.MDH_JSON_EXP_ERROR)));
				} catch (Exception exception) {
					((com.sds.mobiledesk.mdhybrid.MDHActivity.MDHybridActivity.a) mWebView).a(MDHCommon.makeErrorResult(callback,
							Integer.toString(MDHCommonError.MDH_UNKNOWN_ERROR)));
				}

				return myHashMap;
			}
		}).start();

		return "OK";
	}
}
