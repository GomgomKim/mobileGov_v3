package kr.go.mobile.iff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import kr.go.mobile.iff.service.SessionManagerService;

public class CommonReceiver extends BroadcastReceiver {

	private final static String NETWORK_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
		  // TODO  
		} else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
		  // TODO 
		} else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent i = new Intent(context, SessionManagerService.class);
			context.startService(i);
		} else if (action.equals(NETWORK_CONNECTIVITY_CHANGE)) {
			Log.d("@@@", NETWORK_CONNECTIVITY_CHANGE);
		}
	}
}
