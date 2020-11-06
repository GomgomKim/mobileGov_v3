package kr.go.mobile.mobp.mff.lib.plugins;

import android.content.Context;
import android.content.Intent;

import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPluginResult;

import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPlugin;

public abstract class NewMDHPlugin extends CBHybridPlugin {

    protected NewMDHPlugin(Context context) {
        super(context);
    }

    public String getVersionName() {
        return "";
    }

    public abstract void onPause();

    public abstract void onResume();

    public abstract void onDestroy();
}
