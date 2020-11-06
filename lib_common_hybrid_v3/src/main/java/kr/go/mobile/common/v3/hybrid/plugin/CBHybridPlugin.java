package kr.go.mobile.common.v3.hybrid.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import kr.go.mobile.common.v3.hybrid.CBHybridActivity;


/**
 * Created by ChangBum Hong on 2020-07-22.
 * cloverm@infrawareglobal.com
 * 플러그인 추상화 클래스
 */
public abstract class CBHybridPlugin {

    private CBHybridActivity parentActivity;

    protected CBHybridPlugin(Context context) {
        this.parentActivity = (CBHybridActivity) context;
    }

    public void sendAsyncResult(CBHybridPluginResult result) {

    }

    public void sendAsyncResult(String callbackID, CBHybridPluginResult result) {
        if (!parentActivity.isDestroyed()) {
            parentActivity.sendAsyncResult(callbackID, result);
        }
    }

    public void setWebViewOnKeyListener(View.OnKeyListener listener) {

    }

    public void loadUrl(String url) {
        ((CBHybridActivity)getActivity()).loadUrl(url);
    }

    public void startActivity(Intent intent) {
        parentActivity.startActivity(intent);
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        parentActivity.startActivityForResult(intent, requestCode);
    }

    public void startActivityForResult(CBHybridPlugin plugin, Intent intent, int requestCode) {
        parentActivity.startActivityForResult(plugin, intent, requestCode);
    }

    protected void addRequestPermissionListener(int requestCode, CBHybridActivity.IRequestPermissionListener requestListener) {
        parentActivity.addRequestPermissionListener(requestCode, requestListener);
    }

    public String getPackageName() {
        return parentActivity.getPackageName();
    }

    public Activity getActivity() {
        return parentActivity;
    }

    public Context getContext() {
        return parentActivity.getBaseContext();
    }

    public CBHybridPluginResult getVersions() {
        return new CBHybridPluginResult(getVersionName());
    }

    public abstract String getVersionName();

    public abstract void onPause();

    public abstract void onResume();

    public abstract void onDestroy();

    public abstract void onActivityResult(int requestCode, int resultCode, Intent intent);

}
