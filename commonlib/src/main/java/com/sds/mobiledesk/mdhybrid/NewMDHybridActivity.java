package com.sds.mobiledesk.mdhybrid;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.sds.BizAppLauncher.gov.aidl.GovController;
import com.sds.mobiledesk.mdhybrid.MDHActivity.MDHybridActivity;

public class NewMDHybridActivity extends MDHybridActivity{
	
	@Override
	public void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		
		// 20161129 윤기현 - 하이브리드 앱을 종료하기위하여 등록함. - BEGIN
		GovController.getInstance(this).setHybridView(this);
		// 하이브리드 앱을 종료하기위하여 등록함. - END
		
		WebView web = null;
        RelativeLayout webLay = getWebViewLayout();
        for(int i = 0; i < webLay.getChildCount(); i++){
        	View view = webLay.getChildAt(i);
        	
        	if(view instanceof WebView){
        		web = (WebView)view;
        		break;
        	}
        }
        
        if (web != null) {
        	NewMDHAndroidAgent androidAgent = new NewMDHAndroidAgent(this, web, mAgent);
        	web.addJavascriptInterface(androidAgent, "MDHAndroid");
        }
	}
}