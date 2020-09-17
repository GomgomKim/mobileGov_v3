package com.sds.mobiledesk.mdhybrid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.sds.mobiledesk.mdhybrid.MDHActivity.MDHybridActivity;
import com.sds.mobiledesk.mdhybrid.MDHAgent.MDHAgent;
import com.sds.mobiledesk.mdhybrid.MDHAgent.MDHAndroidAgent;
import com.sds.mobiledesk.mdhybrid.common.MDHCommon;

public class NewMDHAndroidAgent extends MDHAndroidAgent{
	Context newContext;
	WebView newWebview;
	
	Activity mActivity;

	public NewMDHAndroidAgent(Context context, WebView webview, MDHAgent mdhagent) {
		super(context, webview, mdhagent);
		newContext = context;
		newWebview = webview;
		
		if(context instanceof Activity)
			mActivity = (Activity)newContext;
	}

	// API 15 이상부터는 web 에서 java api 호출하기 위해서는 아래 annotation 을 선언해야 함. 
	@JavascriptInterface
	@Override
	public void _exec(String paramString1, String paramString2, String paramString3) {
		String action = new String(paramString2.substring(0, paramString2.lastIndexOf(".")));
		String method = new String(paramString2.substring(paramString2.lastIndexOf(".") + 1, paramString2.length()));

		Log.e("pjh", "paramString1  :: " + paramString1);
		Log.e("pjh", "paramString2  :: " + paramString2);
		Log.e("pjh", "paramString3  :: " + paramString3);
		Log.e("pjh", "Agent name    :: " + action);
		Log.e("pjh", "Agent method  :: " + method);

		if (action.equals("SSO")){
			if (method.equals("getInfo")){
				new NewMDHSSOAgent(newContext, newWebview).getInfo(paramString1, paramString3);
			}
			else{
				((MDHybridActivity.a)newWebview).a(MDHCommon.makeErrorResult(paramString1, Integer.toString(-10098)));
			}
		}
		else if (action.equals("ScreenLock")){
			if (method.equals("isLocked")){
				new NewMDHScreenLockAgent(newContext, newWebview).isLocked(paramString1);
			}
			else if (method.equals("unlock")){
				new NewMDHScreenLockAgent(newContext, newWebview).unlock(paramString1, paramString3);
			}
			else{
				((MDHybridActivity.a)newWebview).a(MDHCommon.makeErrorResult(paramString1, Integer.toString(-10098)));
			}
		}else if (action.equals("Native")){
			Log.e("pjh", "Agent Resume action  :: " + action + ", method :: " + method);
			if (method.equals("Resume")) {
				Intent intent = new Intent(newContext, mActivity.getClass());
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				newContext.startActivity(intent);
			}
			
		}else{
			super._exec(paramString1, paramString2, paramString3);
		}
	}
}
