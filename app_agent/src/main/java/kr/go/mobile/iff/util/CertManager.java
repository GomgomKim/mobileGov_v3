package kr.go.mobile.iff.util;


import org.apache.commons.lang.StringUtils;

import com.dreamsecurity.magicline.client.MagicLine;
import com.dreamsecurity.magicline.client.MagicLineType;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.telephony.TelephonyManager;
import kr.go.mobile.iff.util.Utils.GET_TYPE;
import kr.go.mobile.mobp.iff.R;

public class CertManager {

	private final boolean LOG_ENABLE = true;
	
	public static final int REQUEST_CODE_CERTMANAGER = 910;
	public static final int REQUEST_CODE_SIGNSHOW = 911;
	public static final int REQUEST_CODE_CERTMOVE = 912;
	
	private static CertManager mInstance;
	private final MagicLine magicLine;
	
	private CertManager(Context context) {
		magicLine = initMagicLine(context);
		if (magicLine != null) {
			magicLine.setIntranetCertMove(
					context.getString(R.string.MagicMRSIPIntra), //MAGICMRS_IP_INTRA,
					context.getResources().getInteger(R.integer.MagicMRSPort), 
					context.getString(R.string.MagicMRSAppId), //MAGICMRS_APPID,
					context.getString(R.string.import_from_internet), 
					context.getString(R.string.import_from_intra));
		}
	}
	
	// final 변수를 처리하기위하여 사용하는 함수. 
	private final MagicLine initMagicLine(Context context) {
		try {
			return MagicLine.getIntance(context, 
					context.getString(R.string.MagicMRSLicense),
					MagicLineType.VALUE_CERTDOMAIN_GPKI | MagicLineType.VALUE_CERTDOMAIN_EPKI, 
					MagicLineType.VALUE_KEYSECURITY_NFILTER );
		} catch (NullPointerException e) {
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	private byte[] getSignedData(Context context){
		//서비스 아이디/서비스명/서비스버전/WIFI주소 null일떼처리
        StringBuilder signedData = new StringBuilder();
        
    	TelephonyManager tMng = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    	String num = tMng.getLine1Number();
    	
    	if(num != null && num.startsWith("+82")){
    		num = StringUtils.replace(num, "+82", "0");
    	}
    	
    	PackageInfo pInfo;
    	
		try {
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String appDescription =  context.getString(R.string.app_description); 
		    
			//signedData.append("PhoneMAC=").append("f8:a9:d0:51:c1:e1");
			signedData.append("PhoneMAC=").append("null");
		    signedData.append("&");
            
			signedData.append("PhoneIMEI=").append( tMng.getDeviceId());
			signedData.append("&PhoneUDID=").append(Utils.getDeviceID(context, GET_TYPE.ANDROID_ID));//TODO new HeaderUtil(context).getUUID());
			signedData.append("&PhoneNo=").append(num);
			signedData.append("&PhoneOSName=").append("Android");
			signedData.append("&PhoneOSVer=").append(android.os.Build.VERSION.RELEASE);
			signedData.append("&ServiceAppId=").append(context.getPackageName());
			signedData.append("&ServiceAppName=").append(appDescription);
			signedData.append("&ServiceAppVer=").append(pInfo.versionName);
			
			// LogUtil.d(getClass(), "SignedData: " + signedData.toString(), LOG_ENABLE);
		} catch (NameNotFoundException e) {
			LogUtil.e(getClass(), "getSignedData ERROR: " + signedData.toString(), e);
		} catch (Exception e) {
			LogUtil.e(getClass(), "getSignedData ERROR: " + signedData.toString(), e);
		}
        return signedData.toString().getBytes();
    }
	
	private void _showCertList(Context context) {
		try {
			// context, 서명데이터, 콜백함수, 패스워드 오류 시도횟수, 횟수 초과 시 삭제여부
			magicLine.signShow(context, 
					getSignedData(context),
					REQUEST_CODE_SIGNSHOW, 
					context.getResources().getInteger(R.integer.MagiclineSignWrongPasswordCount),
					true);
		} catch (NotFoundException e) {
			LogUtil.e(getClass(), "인증서 목록 화면을 호출하던 중 에러가 발생하였습니다.", e);
		} catch (Exception e) {
			LogUtil.e(getClass(), "인증서 목록 화면을 호출하던 중 에러가 발생하였습니다.", e);
		}		
	}
	
	private void _showCertManager(Context context) {
		try {
			magicLine.certManagerShow(context , 
					context.getString(R.string.MagicMRSIP),
					context.getResources().getInteger(R.integer.MagicMRSPort),
					context.getString(R.string.MagicMRSAppId),
					REQUEST_CODE_CERTMANAGER, 
					MagicLineType.VALUE_NETWORK_ALL);
		} catch (NotFoundException e) {
			LogUtil.e(getClass(), "인증서 관리 화면을 호출하던 중 에러가 발생하였습니다.", e);
		} catch (Exception e) {
			LogUtil.e(getClass(), "인증서 관리 화면을 호출하던 중 에러가 발생하였습니다.", e);
		}
	}
	
	private static final CertManager getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new CertManager(context);
		}
		return mInstance;
	}
	
	public static void showCertList(Context context) {
		CertManager.getInstance(context)._showCertList(context);
	}
	
	public static void showCertManager(Context context) {
		CertManager.getInstance(context)._showCertManager(context);
	}
	
	public static String getUserID(String userDN) {
		String[] cnName = userDN.split(",");
		for(String name : cnName){
			if(name.startsWith("cn=")){
				return name.split("cn=")[1];
			}
		}
		return "";
	}
}
