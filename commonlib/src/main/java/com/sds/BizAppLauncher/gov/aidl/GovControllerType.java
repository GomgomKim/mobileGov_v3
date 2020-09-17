package com.sds.BizAppLauncher.gov.aidl;


/**
 * GovController 에서 사용하는 공통 상수를 정의한다. 
 * 
 * @since 2016.11.14
 * @author 윤기현
 *
 */
public class GovControllerType {

	public static final String ACTION_ALIVE_SERVICE = "kr.go.mobile.service.ACTION_ALIVE_SERVICE";
	
	public final static String ACTION_GOV_LAUNCHER = "kr.go.mobile.LAUNCH_GMOBILE";
	public final static String ACTION_CONTROL = "kr.go.mobile.ACTION_CONTROL";
	public final static String ACTION_STATUS = "kr.go.mobile.ACTION_ADMINAPP_STATUS";
	
	public final static String EXTRA_TOKEN = "extra_token";
	public final static String EXTRA_PACKAGE_NAME = "extra_package";
	public final static String EXTRA_TYPE = "extra_type";
	public final static String EXTRA_STATUS = "extra_status";
	
	public final static int TYPE_KILL = 100;
	
	public final static String STATUS_STARTED = "0"; 
	public final static String STATUS_FINISHED = "1"; 
	public final static String STATUS_FINISHED_MSM = "2"; 
	public final static String STATUS_FINISHED_EXCEPTION = "3"; 
}
