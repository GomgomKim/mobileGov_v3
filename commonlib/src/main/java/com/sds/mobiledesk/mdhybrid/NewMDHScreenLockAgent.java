package com.sds.mobiledesk.mdhybrid;

import android.content.Context;
import android.webkit.WebView;

import com.sds.mobiledesk.mdhybrid.MDHActivity.MDHybridActivity;
import com.sds.mobiledesk.mdhybrid.common.MDHCommon;

@Deprecated
public class NewMDHScreenLockAgent{
	Context context = null;
	WebView webView = null;
	
	public NewMDHScreenLockAgent(Context paramContext, WebView paramWebView){
		context = paramContext;
		webView = paramWebView;
	}
	
	public void isLocked(String callBack){
		/*MDH_SERVICE_IS_BINDING = return -10104;
		MDH_MOBILEDESK_IS_NOT_INSTALLED = return -10100;
		MDH_SSO_SIGN_OFF = return -10101;
		MDH_SCREEN_IS_LOCKED = return -10103;
		MDH_SUCCESS = return 0;
		MDH_NO_LOCK_PASSWORD = return -10102;
		else = return -10099;*/
								
		//성공
        //((MDHybridActivity.a)webView).a(MDHCommon.makeSuccessResult(callBack, MDHCommon.ADD_QUOTATION(Boolean.toString(i == -10103))));
        //((MDHybridActivity.a)webView).a(MDHCommon.makeSuccessResult(callBack, MDHCommon.ADD_QUOTATION(Boolean.toString(true)))); //잠김상태
        ((MDHybridActivity.a)webView).a(MDHCommon.makeSuccessResult(callBack, MDHCommon.ADD_QUOTATION(Boolean.toString(false))));//풀림상태
        //실패 - 에러코드 참조
        //((MDHybridActivity.a)webView).a(MDHCommon.makeErrorResult(callBack, Integer.toString(i)));
    
	}
	
	public void unlock(String callBack, String option){
		//"" / null / undefined = return -10001;
		//getService() == null = return -10100;
		//!isSingleSignOn() = return -10101;
		//isScreenLocked() == 3 = return -10102;
		//else = return -10105
		//성공 = return 0;
		
		
		//성공
		//((MDHybridActivity.a)webView).a(MDHCommon.makeSuccessResult(callBack, MDHCommon.ADD_QUOTATION(Boolean.toString(i == 0))));
		((MDHybridActivity.a)webView).a(MDHCommon.makeSuccessResult(callBack, MDHCommon.ADD_QUOTATION(Boolean.toString(true)))); //성공
		//((MDHybridActivity.a)webView).a(MDHCommon.makeSuccessResult(callBack, MDHCommon.ADD_QUOTATION(Boolean.toString(false))));//실패
        //실패 - 에러코드 참조
        //((MDHybridActivity.a)webView).a(MDHCommon.makeErrorResult(callBack, Integer.toString(i)));
	}
}