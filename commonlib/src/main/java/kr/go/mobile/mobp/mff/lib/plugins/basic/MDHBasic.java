package kr.go.mobile.mobp.mff.lib.plugins.basic;

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
import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPluginResult;
import com.sds.mobiledesk.mdhybrid.common.MDHCommon;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.mobp.mff.lib.plugins.NewMDHPlugin;

/**
 * @Class Name MDHBoard
 * @작성일 : 2015. 10. 30.
 * @작성자 : 조명수
 * @변경이력 :
 * 
 * @Class 설명 : 네이티브 서비스앱 전용 게시판 기능 사용을 위한 클래스 MDHAdapter.2.6.min.js 와 같은 역활을
 *        하기위함
 * 
 * 
 */
public class MDHBasic extends NewMDHPlugin {

	public interface MDHBasicSuccessCallback {
		void onBasicSuccess(final int code, final String msg);
	}

	public interface MDHBasicErrorCallback {
		void onBasicError(final int code, final String msg);
	}

	private static MDHBasicSuccessCallback nativeSuccessCallback;
	private static MDHBasicErrorCallback nativeErrorCallback;

	@Deprecated
	public static class SSO {
		final static String TAG = "SSO";
		
		static int jarrayLength = 0;
		static JSONArray reqJsonarray = null;
		static ArrayList<String> reqArraylist = null;
		
		/**
		 * @Method Name : getBoardList
		 * @작성일 : 2015. 11. 2.
		 * @작성자 : 조명수
		 * @변경이력 :
		 * @Method 설명 : 게시판 게시글 목록 조회
		 */
		public static void getInfo(final Context c, final MDHBasicSuccessCallback successCallback,
				final MDHBasicErrorCallback errorCallback, final String reqList) {

			LogUtil.d(TAG, "getInfo args :: " + reqList);
			
			nativeSuccessCallback = successCallback;
			nativeErrorCallback = errorCallback;
			

			ResponseListener listener = new ResponseListener() {
				public void receive(ResponseEvent re) {
					// 서버로부터 받은 응답은 ResponseEvent 객체 re에 저장된다.
					int errorCode = re.getResultCode();
					String errorMsg = re.getResultData();

					Log.d("pjh", "n errorCode :: " + errorCode);
					Log.d("pjh", "n errorMsg :: " + errorMsg);
				}
			};

			ServiceBrokerCB svcBrokerCB = new ServiceBrokerCB() {

				@Override
				public void onServiceBrokerResponse(String retMsg) {
					Log.d("pjh", "getInfo 리턴값 :: " + retMsg);

					try {
						HashMap<String, String> hashmap = convertToHashMap(retMsg);

						MDHCommon.LOG((new StringBuilder()).append("ssoInfo.size() is ").append(hashmap.size()).toString());

						if (jarrayLength != hashmap.size())
							MDHCommon.LOG("key is not matched");
						JSONStringer jsonstringer = new JSONStringer();
						jsonstringer.object();

						LogUtil.e(TAG, "SSO Agent 리턴값11 :: " + reqArraylist.size());

						for (int j = 0; j < reqArraylist.size(); j++) {
							String key = (String) reqArraylist.get(j);

							if (hashmap.containsKey(key)) {
								jsonstringer.key(key).value(hashmap.get(key));
							} else {
								jsonstringer.key(key).value("");
							}
						}

						jsonstringer.endObject();
						LogUtil.e(TAG, "SSO Agent 리턴값2222 :: " + jsonstringer.toString());
						
						if(nativeSuccessCallback != null){
							nativeSuccessCallback.onBasicSuccess(0, jsonstringer.toString());
						}
					} catch (JSONException je) {
						//je.printStackTrace();
						LogUtil.e(TAG, je.getMessage());
					} catch (Exception e) {
						//e.printStackTrace();
						LogUtil.e(TAG, e.getMessage());
					}
				}
			};


			try {

				reqJsonarray = (JSONArray) (new JSONTokener(reqList)).nextValue();				
				jarrayLength = reqJsonarray.length();
				reqArraylist = new ArrayList<String>();
				for (int i = 0; i < jarrayLength; i++) {
					String value = reqJsonarray.getString(i);
					reqArraylist.add(value);
				}

				ServiceBrokerLib lib = new ServiceBrokerLib(c, listener, svcBrokerCB);
				Intent intent = new Intent();
				intent.putExtra("dataType", "json");
				intent.putExtra("sCode", "getInfo");
				intent.putExtra("parameter", reqList);

				lib.request(intent);
			} catch (JSONException e) {
				LogUtil.e(TAG, e.getMessage());
			} catch (Exception e) {
				LogUtil.e(TAG, e.getMessage());
			}

		}
		
		
		private static HashMap<String, String> convertToHashMap(String jsonString) {
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
			} catch (JSONException e) {
				LogUtil.e(TAG, e.getMessage());
				if(nativeErrorCallback != null){
					nativeErrorCallback.onBasicError(-1, "알 수 없는 에러가 발생하였습니다.");
				}
			} catch (Exception e) {
				LogUtil.e(TAG, e.getMessage());
				if(nativeErrorCallback != null){
					nativeErrorCallback.onBasicError(-1, "알 수 없는 에러가 발생하였습니다.");
				}
			}

			return myHashMap;
		}
	}

	@Override
	public MDHPluginResult getVersions() {
		return null;
	}

	@Override
	public void onActivityResult(int arg0, int arg1, Intent arg2) {

	}

	@Override
	public void onDestroy() {

	}

	@Override
	public void onPause() {

	}

	@Override
	public void onResume() {

	}

}
