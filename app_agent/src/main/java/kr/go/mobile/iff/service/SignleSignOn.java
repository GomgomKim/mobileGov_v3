package kr.go.mobile.iff.service;

import java.util.Hashtable;

import kr.go.mobile.iff.util.LogUtil;

import org.apache.commons.lang.NullArgumentException;

import com.sds.BizAppLauncher.gov.util.CertiticationUtil;


class SignleSignOn  {
	
	private final String TAG = SignleSignOn.class.getSimpleName();
	
	private static SignleSignOn mInstance;
	
	static SignleSignOn getInstance() {
		if (mInstance == null)
			mInstance = new SignleSignOn();
		return mInstance;
	}
	
	private boolean isFixed = false;
	private Hashtable<String, String> mData = new Hashtable<String, String>();
	
	private SignleSignOn() { /* hide */ }
	
	synchronized void init(String dn, String userId, String nickName, 
			String ouCode, String ouName, 
			String departmentName, String departmentCode) {
		if (isFixed) {
			LogUtil.w(TAG, "이미 설정된 SSO 데이터가 존재합니다.");
			return;
		}

		if (dn == null) {
			throw new NullArgumentException("dn 값이 Null 입니다.");
		}
		 this.mData.put("dn", dn);
		
		if (userId == null) {
			throw new NullArgumentException("userId 값이 Null 입니다.");
		}
		this.mData.put(CertiticationUtil.KEY_CN, userId);
		
		if (nickName == null) {
			throw new NullArgumentException("nickName 값이 Null 입니다.");
		}
		this.mData.put(CertiticationUtil.KEY_NICKNAME, nickName);
		
		if (ouCode == null) {
			throw new NullArgumentException("ouCode 값이 Null 입니다.");
		}
		this.mData.put(CertiticationUtil.KEY_OU, ouName);
		
		if (ouName == null) {
			throw new NullArgumentException("ouName 값이 Null 입니다.");
		}
		this.mData.put(CertiticationUtil.KEY_OU_CODE, ouCode);
		
		if (departmentCode == null) {
			throw new NullArgumentException("departmentCode 값이 Null 입니다.");
		}
		this.mData.put(CertiticationUtil.KEY_DEPARTMENT_NUMBER, departmentCode);
		
		if (departmentName == null) {
			throw new NullArgumentException("departmentName 값이 Null 입니다.");
		}
		this.mData.put(CertiticationUtil.KEY_DEPARTMENT, departmentName);
		
		isFixed = true;
	}

	synchronized String getInfo(String key) {
		if (isFixed) {
			String value = mData.get(key);
			return value == null ? "" : value;
		} else {
			throw new IllegalStateException("SSO 데이터가 존재하지 않습니다.");
		}
	}
	
	synchronized void reset() {
		if (isFixed) {
			mData.clear();
			isFixed = false;
		} else {
			LogUtil.w(TAG, "SSO 데이터가 존재하지 않습니다.");
		}
	}
}
