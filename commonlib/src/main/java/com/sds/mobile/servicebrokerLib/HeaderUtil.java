package com.sds.mobile.servicebrokerLib;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class HeaderUtil{
	String header = "";
	Context context;
	
	public HeaderUtil(Context con){
		context = con;
	}
	
	public String getHeader(){
		try {
	    	String packageName = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA).packageName;
	    	int appVer = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA).versionCode;
	    	header = "PK="+packageName+";AV="+appVer;
	    	LogUtil.log_i("header :: " + header);
		} catch (NameNotFoundException e) {
			LogUtil.errorLog(e);
		}
		
		return header;
	}
}