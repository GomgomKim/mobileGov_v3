package com.infrawaretech.docviewer.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import kr.go.mobile.mobp.mff.lib.util.E2ESetting;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.infrawaretech.docviewer.DocViewerActivity;

import com.sds.mobile.servicebrokerLib.HeaderUtil;
import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.ServiceBrokerLib.ServiceBrokerCB;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

/**
 * SAT 문서뷰어를 연동하기 위하여 필요한 header를 생성하기 위한 객체
 * 
 *  @version 0.0.1
 *  @since 2017-01-25
 * 
 */
@Deprecated
public class DOCViewUtil {

	private String TAG = DOCViewUtil.class.getSimpleName();
	

	private static DOCViewUtil mInstance;
	
 	public static DOCViewUtil getInstance() {
		if (mInstance == null)
			mInstance = new DOCViewUtil();
		
		return mInstance;
	}
	
 	private DOCViewUtil() { /* private */ }

 	@Deprecated
 	public void init(Context context) {

 	}

}
