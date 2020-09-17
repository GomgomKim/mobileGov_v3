package kr.go.mobile.iff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.sds.BizAppLauncher.gov.aidl.GovControllerType;

/**
 * 행정앱으로부터 행정앱의 상태값을 전달받는다. 
 * 
 * FIXME 차후에 REMOTE CALLBACK 함수로 변경할지 고민 필요 
 * 
 * @author Jason 
 *
 */
public class MobileGovReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Bundle extra = intent.getExtras();
		if (action.equals(GovControllerType.ACTION_STATUS) && extra != null) {
			String strStatus = extra.getString(GovControllerType.EXTRA_STATUS);
			try {
				Integer status = Integer.parseInt(strStatus);
				String packageName = extra.getString(GovControllerType.EXTRA_PACKAGE_NAME);
				final Handler H = ServiceWaitHandler.getInstance(); 
				Message msg = H.obtainMessage(ServiceWaitHandler.MESSAGE_CATEGORY_ADMINISTRATOR_BROADCAST, 
						status, 0, packageName);
				H.sendMessage(msg);
			} catch (NumberFormatException e) {
				return;
			}
		}
	}
}
