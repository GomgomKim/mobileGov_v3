package kr.go.mobile.iff.util;

import android.os.Bundle;

/**
 * 행정앱에 대한 정보를 저장하는 객체이다.<br>
 * 
 * @author 윤기현
 *
 */
public class AdminState {

	/////// 공통 라이브러리의 GovControllerType 에 동일하게 선언되어 있음.
	public static final String EXTRA_TOKEN = "extra_token";
	public static final String EXTRA_PACKAGE = "extra_package";
	public static final String EXTRA_THIS_TOKEN = "extra_this_token";
	////////////////////////////////////////////////////////////////////////////////
	
	final Bundle mBundle;

	/**
	 *  공통기반 라이브러리부터 전달받은 Intent에 포함되어 있는 Bundle 정보를 행정앱 상태 정보로 저장하는 객체이다.
	 *  
	 * @param bundle - 행정앱으로 부터 전달된 Bundle 객체.
	 */
	public AdminState(Bundle bundle) {
		this.mBundle = bundle;
	}
	
	public void putThisToken(String verificationTokenAsByte64) {
		this.mBundle.putString(EXTRA_THIS_TOKEN, verificationTokenAsByte64);
	}
	
	public String getTokens() {
		StringBuilder sb = new StringBuilder();
		sb.append(mBundle.get(EXTRA_THIS_TOKEN));
		sb.append(",");
		sb.append(mBundle.get(EXTRA_TOKEN));
		return sb.toString();
	}

	public String getPackageName() {
		return this.mBundle.getString(EXTRA_PACKAGE);
	}
}
