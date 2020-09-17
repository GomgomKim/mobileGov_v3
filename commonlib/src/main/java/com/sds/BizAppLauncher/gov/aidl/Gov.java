package com.sds.BizAppLauncher.gov.aidl;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.webkit.WebView;

import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPlugin;
import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPluginResult;
import com.sds.mobiledesk.mdhybrid.common.MDHCommon;

public class Gov extends MDHPlugin
{
	private Context mContext = null;
	private GovController mController;
	IGovServiceCallback mGovCallback = new IGovServiceCallback.Stub() {
		public void callback(boolean result) throws RemoteException {
			if (result)
				Gov.this.asyncResult(true, "true");
			else
				Gov.this.asyncResult(true, "false");
		}
	};

	public void init(Context context, WebView webview)
	{
		this.mContext = context;
		this.mController = GovController.getInstance(this.mContext);
		this.mController.bindService();
		super.init(context, webview);
	}

	public MDHPluginResult getVersions(){
		MDHCommon.LOG("Get PluginExample's Version");

		String version = "1.0";

		return new MDHPluginResult(0, version);
	}

	public MDHPluginResult connect() {
		this.mController.vpnConnect(this.mGovCallback);
		return new MDHPluginResult(10000, "");
	}

	public MDHPluginResult disconnect() {
		this.mController.vpnDisconnect(this.mGovCallback);
		return new MDHPluginResult(10000, "");
	}

	public MDHPluginResult isConnected() {
		this.mController.vpnIsConnected(this.mGovCallback);
		return new MDHPluginResult(10000, "");
	}

	public MDHPluginResult clearData() {
		this.mController.vpnClearData(this.mGovCallback);
		return new MDHPluginResult(10000, "");
	}

	public void onPause(){
		//Override
	}

	public void onResume(){
		//Override
	}

	public void onDestroy(){
		this.mController.unBindService();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Override
	}

	private void asyncResult(boolean result, String msg) {
		if (result)
			sendAsyncResult(new MDHPluginResult(0, msg));
		else
			sendAsyncResult(new MDHPluginResult(-10099));
	}
}