package kr.go.mobile.iff.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * 패키지 관련 유틸 
 *
 * @since 2016.11.14
 * @author 윤기현
 *
 */
public class PackageUtil {
	
	private PackageManager mPM;
	
	public PackageUtil(Context ctx) {
		this.mPM = ctx.getPackageManager();
	}
	
	/**
	 * 
	 * 
	 * @param packageName
	 * @return
	 */
	public boolean isInstalledApplication(String packageName) {
		try {
			mPM.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * 
	 * @param packageName
	 * @return
	 */
	public PackageInfo getPackageInfo(String packageName) {
		if (isInstalledApplication(packageName)) {
			try {
				return mPM.getPackageInfo(packageName, PackageManager.GET_META_DATA);
			} catch (NameNotFoundException e) {
				LogUtil.d(getClass(), e.getMessage());
			}
		}
		return null;
	}
}
