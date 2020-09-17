package kr.go.mobile.iff.sample.hybrid;

import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPlugin;
import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPluginResult;
import com.sds.mobiledesk.mdhybrid.common.MDHCommonError;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class UserActivityPlugin extends MDHPlugin {

	public UserActivityPlugin() {
		
	}
	
	@Override
	public MDHPluginResult getVersions() {
		return new MDHPluginResult(0, "1.0.0");
	}

	public MDHPluginResult request(String args) {
		Intent intent = new Intent("kr.go.mobile.TEST");
		startActivityForResult(this, intent, 0);
		return new MDHPluginResult(MDHCommonError.MDH_START_ACTIVITY_FOR_RESULT);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		Toast.makeText(getContext(), "@@@@@" + resultData.getExtras().getString("type"), Toast.LENGTH_LONG).show();
		Log.d("@@@@", resultData.getExtras().getString("type"));
		sendAsyncResult( new MDHPluginResult(MDHCommonError.MDH_SUCCESS, "OKKKK"));
	}

	@Override
	public void onDestroy() {
		Toast.makeText(getContext(), "@@@@@ onDestroy" , Toast.LENGTH_LONG).show();
	}

	@Override
	public void onPause() {
		Toast.makeText(getContext(), "@@@@@ onPause" , Toast.LENGTH_LONG).show();

	}

	@Override
	public void onResume() {
		Toast.makeText(getContext(), "@@@@@ onResume", Toast.LENGTH_LONG).show();

	}

}
