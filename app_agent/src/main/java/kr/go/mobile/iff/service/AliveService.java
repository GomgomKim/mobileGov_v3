package kr.go.mobile.iff.service;

import java.util.Iterator;
import java.util.Set;

import kr.go.mobile.iff.service.IAliveService.Stub;
import kr.go.mobile.iff.util.LogUtil;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.sds.BizAppLauncher.gov.aidl.GovControllerType;

/**
 * 현재는 네이티브 앱에 의해서만 사용되는 서비스. 
 * 
 * 모든 행정앱이 구동 시 서비스브로커를 바인딩한다면 AliveService 는 필요없음. 
 * 
 * @author yoongi
 *
 */
public class AliveService extends Service {

	IAliveService.Stub mBinder = new Stub() {
		
		@Override
		public boolean isAlive(String packageName) throws RemoteException {
			LogUtil.d(AliveService.class, packageName);
			Intent i = new Intent(GovControllerType.ACTION_STATUS);
			i.putExtra(GovControllerType.EXTRA_PACKAGE_NAME, packageName /*실행중인 행정앱의 패지키명*/);
			i.putExtra(GovControllerType.EXTRA_STATUS, GovControllerType.STATUS_STARTED );
			sendBroadcast(i);
			return true;
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		// TEST CODE
		Bundle b = intent.getExtras();
		if (b != null) {
			Set<String> keys = b.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String key = it.next();
				Log.d("@@@@", key + " : " + b.get(key).toString());
			}
		}
		// END - TEST CODE
		return mBinder;
	}
	
	@Override
	 public boolean onUnbind(Intent intent) {
		String packageName = intent.getStringExtra(GovControllerType.EXTRA_PACKAGE_NAME);		
		
		Intent finishedIntent = new Intent(GovControllerType.ACTION_STATUS);
		finishedIntent.putExtra(GovControllerType.EXTRA_PACKAGE_NAME, packageName /*실행중인 행정앱의 패지키명*/);
		finishedIntent.putExtra(GovControllerType.EXTRA_STATUS, GovControllerType.STATUS_FINISHED_EXCEPTION /*정상 종료*/);
		sendBroadcast(finishedIntent);
		
		return super.onUnbind(intent);
	 }

}
