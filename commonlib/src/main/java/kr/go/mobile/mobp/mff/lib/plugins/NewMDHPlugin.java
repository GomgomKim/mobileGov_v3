package kr.go.mobile.mobp.mff.lib.plugins;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPlugin;
import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPluginResult;

import android.content.Context;
import android.content.Intent;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;

/**
* @Class Name	:	NewMDHPlugin
* @작성일			:	2015. 11. 3. 
* @작성자			:	조명수
* @변경이력			:
* @Class 설명 	: 	각 신규 플러그인들에서 사용하는 공통 전역변수 선언
*/
public class NewMDHPlugin extends MDHPlugin {
	
	protected Intent createServiceIntent(final Context c, final String jsonArgs, final String svcID){
		Intent intent = new Intent();
				
		StringBuffer buffer = new StringBuffer();
		
		try{

			JSONObject jsonObj = new JSONObject(jsonArgs);
			if(jsonObj.has("subService") == false){
				return null;
			}

			intent.putExtra("sCode", svcID);	
			
			// 아래 필드들은 굳이 서비스 앱들에서 넘길경우가 없을 듯 하지만
			// 혹시나 서비스앱에서 추가하여 호출할 경우를 위해...
			if(jsonObj.has("dataType") == false){
				intent.putExtra("dataType", E2ESetting.DATA_TYPE_JSON);        
			}

//			if(jsonObj.has("ipAddress") == false){   
//				intent.putExtra("ipAddress", MFFJni.getInstance(c).b(MFFJni.type));       
//			}
//			
//			if(jsonObj.has("portNumber") == false){
//				intent.putExtra("portNumber", MFFJni.getInstance(c).c());        
//			}
			
			if(jsonObj.has("connectionType") == false){
				intent.putExtra("connectionType", E2ESetting.CONNECTION_TYPE_HTTPS);        
			}
			
			if(jsonObj.has("contextUrl") == false){
				intent.putExtra("contextUrl", E2ESetting.CONTEXT_URL);        
			}
						
			Iterator<?> iterator = jsonObj.keys();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				// JSONObject issue = issueObj.getJSONObject(key);
				String value = jsonObj.getString(key);
				
				if (key.compareToIgnoreCase("dataType") == 0) {
					intent.putExtra("dataType", value);        
				} else if (key.compareToIgnoreCase("ipAddress") == 0) {
					intent.putExtra("ipAddress", value);        
				} else if (key.compareToIgnoreCase("portNumber") == 0) {
					intent.putExtra("portNumber", value);
				} else if (key.compareToIgnoreCase("connectionType") == 0) {
					intent.putExtra("connectionType", value);        
				}  else if (key.compareToIgnoreCase("contextUrl") == 0) {
					intent.putExtra("contextUrl", value);             
				}  else if (key.compareToIgnoreCase("filePath") == 0) {
					intent.putExtra("filePath", value);
				}  else {
					buffer.append("&" + key + "=" + value);
				}
				
			}

			// 맨앞의 "&" 를 자르기위해 1부터
			String result = buffer.substring(1, buffer.length());
			intent.putExtra("parameter", result);


		}catch(JSONException e){
			LogUtil.e(getClass(), e.getMessage());
		}catch(Exception e){
			LogUtil.e(getClass(), e.getMessage());
		}
	
		return intent;
	}

	@Override
	public MDHPluginResult getVersions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onActivityResult(int paramInt1, int paramInt2, Intent paramIntent) {
		// TODO Auto-generated method stub

	}

}
