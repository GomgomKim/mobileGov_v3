package kr.go.mobile.mobp.iff.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;

import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class HeaderUtil{
	Context context;
	E2ESetting e2eSetting = new E2ESetting();
	
	public HeaderUtil(Context con){
		context = con;
	}
	
	@Deprecated
	public String getUUID(){
		String uuid = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID); //디바이스 uuid 체크
		return uuid;
	}
	
	private String getBaseHeader(){
		@SuppressWarnings("static-access")
		Display display = ((WindowManager)context.getSystemService(context.WINDOW_SERVICE)).getDefaultDisplay();
		
		StringBuilder baseHeader = new StringBuilder();
		// 2018-04-10 서비스 종류 구분을 위한 KEY/VALUE 추가. 
		baseHeader.append(";FLD=I");
		// END - 서비스 종류 구분을 위한 KEY/VALUE 추가 
		baseHeader.append(";OT=").append(E2ESetting.OSTYPE);
		baseHeader.append(";OV=").append(E2ESetting.DEVICEVERSION);
		baseHeader.append(";RV=").append("0");
		baseHeader.append(";DW=").append(display.getWidth());
		baseHeader.append(";DH=").append(display.getHeight());
		baseHeader.append(";DPI=").append(getDPI());
		baseHeader.append(";TD=").append(e2eSetting.getTestYN());
		baseHeader.append(";UD=").append(getUUID());
		baseHeader.append(";UI=").append(e2eSetting.getUserId());
		baseHeader.append(";OC=").append(e2eSetting.getOfficeCode());
		baseHeader.append(";OG=").append(e2eSetting.getOfficeName());
		baseHeader.append(";MD=").append(android.os.Build.DEVICE);

    	return baseHeader.toString();
	}
	
	private String getPackageInfoHeader(){
		StringBuilder header = new StringBuilder();
		try {
	    	String packageName = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA).packageName;
	    	int appVer = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA).versionCode;
	    	header.append("PK=").append(packageName);
	    	header.append(";AV=").append(appVer);
		} catch (NameNotFoundException e) {
			LogUtil.e(getClass(), "해당 패키지 정보가 존재하지 않습니다.", e);
		} 
		return header.toString();
	}
	
	//어플 버전 형식변경
	@Deprecated
    public String versionFormat(String appVer){
    	String[] version = appVer.split("\\.");
    	String ver = "";
    	DecimalFormat format = new DecimalFormat("00");
    	
    	for(String aa : version){
    		int i = Integer.parseInt(aa);
    		if("".equals(ver)){
    			ver = ver + format.format(i);
    		}else{
    			ver = ver + "." +format.format(i);
    		}
    	}
    	return ver;
    }
    
    public String getDPI(){
    	String dpi = "";
    	DisplayMetrics metrics = new DisplayMetrics();
    	Display display = ((WindowManager)context.getSystemService(context.WINDOW_SERVICE)).getDefaultDisplay();
    	display.getMetrics(metrics);
    	int densityDpi =metrics.densityDpi;   // 단말의 density. 120 dpi 해상도의 경우 120 값
		
    	if (densityDpi > 480) {
    		dpi = "XXHDPI"; // 480 값을 초과할 경우 DPI 값이 없는 상태가됨. 
    	} else if(densityDpi == 480){
			dpi = "XXHDPI";
		}else if(densityDpi == 320){
			dpi = "XHDPI";
		}else if(densityDpi == 240){
			dpi = "HDPI";
		}else if(densityDpi == 160){
			dpi = "MDPI";
		}else if(densityDpi == 120){
			dpi = "LDPI";
		}else{
			LogUtil.w(getClass(), "densityDpi :: " + densityDpi);
		}
    	
    	return dpi;
    }
    
    @Deprecated
    public int getRate(){
    	String dpi = getDPI();
    	int rate = 100;
    	
    	if("XXHDPI".equals(dpi)){
    		rate = 300;
		}
    	else if("XHDPI".equals(dpi)){
    		rate = 200;
		}
    	else if("HDPI".equals(dpi)){
    		rate = 150;
		}
    	else if("MDPI".equals(dpi)){
    		rate = 100;
		}
    	else if("LDPI".equals(dpi)){
    		rate = 75;
		}
    	
    	return rate;
    }
    
    public String getHeader(){
    	String header = getPackageInfoHeader() + getBaseHeader();
    	LogUtil.d(getClass(), "getHeader :: " + header);
    	try {
    		header = URLEncoder.encode(header, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(getClass(), "지원하지 않는 인코딩입니다.", e);
		}
    	return header;
    }
    
    public String getServiceHeader(String packageInfo){
    	String header = packageInfo + getBaseHeader();
    	LogUtil.d(getClass(), "getServiceHeader :: " + header);
    	try {
    		header = URLEncoder.encode(header, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(getClass(), "", e);
		}
    	return header;
    }
}