package kr.go.mobile.iff.service;

import kr.go.mobile.iff.exception.ExpiredSignException;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;
import android.content.Intent;

class SignedInfo {
	
	private final long mAuthSuccTime;
	private final long mAuthMainTime;
	private final String mUserDN;
	private final String mUserDNWithDeviceID;
	private final String mSignedBase64;
	
	private final String mUserID;
	private final String mOfficeName;
	
	SignedInfo(Intent intent) throws ExpiredSignException, Exception {
		this.mAuthSuccTime = intent.getLongExtra(SessionManagerService.EXTRA_SIGNED_SUCCESS_TIME, System.currentTimeMillis());
		this.mAuthMainTime = intent.getIntExtra(SessionManagerService.EXTRA_SIGNED_MAINTAIN_TIME, 24*60*60) *1000;
		this.mUserDN = intent.getStringExtra(SessionManagerService.EXTRA_USER_DN);
		this.mUserDNWithDeviceID = intent.getStringExtra(SessionManagerService.EXTRA_USER_DN_WITH_DEVICE_ID);
		this.mSignedBase64 = intent.getStringExtra(SessionManagerService.EXTRA_SIGNED_BASE64);
		
		String id = "";
		String[] cnName = mUserDN.split(",");
		for(String name : cnName){
			if(name.startsWith("cn=")){
				id = name.split("cn=")[1];
				break;
			}
		}
		this.mUserID = id;
		
		String officeName = "";
		String[] ouName = mUserDN.split(",");
		for(String name : ouName){
			if(name.startsWith("ou=")){
				officeName = name.split("ou=")[1];
				break;
			}
		}
		this.mOfficeName = officeName;
		
		validAuth();
		
		////////기존에 사용하던 E2ESetting 데이터를 사용하기 위한 코드 추가. - BEGIN 
		E2ESetting e2eSetting = new E2ESetting();
		e2eSetting.setTestYN("N");
		e2eSetting.setOfficeCode("0000000"); /*초기값으로 0000000 값일 경우 office code 값을 얻어올 수 있음*/
		e2eSetting.setOfficeName(getOfficeName());
		e2eSetting.setUserId(getUserID());
		//////// 기존에 사용하던 E2ESetting 데이터를 사용하기 위한 코드 추가. - END
	}
	
	final String getUserDN() {
		return this.mUserDN;
	}
	
	final String getUserDNWithDeviceID()  {
		return this.mUserDNWithDeviceID;
	}
	
	final String getUserID()  {
		return this.mUserID;
	}
	
	final String getOfficeName() {
		return this.mOfficeName;
	}
	
	final String getSignedBase64() {
		return this.mSignedBase64;
	}
	
	void validAuth() throws ExpiredSignException {
		if (mAuthMainTime == 0) {
			// 0 값은 무한대로 유지함. 
			return;
		}
		if ((System.currentTimeMillis() - mAuthSuccTime) > mAuthMainTime) {
			throw new ExpiredSignException("서명 유효 시간을 초과했습니다.");
		} else {
			LogUtil.d(getClass(), "유지시간: " + mAuthMainTime + " 경과시간: " + (System.currentTimeMillis() - mAuthSuccTime));
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append(" [");
		sb.append("Singed Time=").append(mAuthSuccTime);
		sb.append(", Sign MaintainTime=").append(mAuthMainTime);
		boolean expired = false;
		try {
			validAuth();
		} catch (ExpiredSignException e) {
			expired = true;
		}
		sb.append(", ExpiredSign=").append(expired);
		sb.append("]");
		
		return sb.toString();
	}
}
