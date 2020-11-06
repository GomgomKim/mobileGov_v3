package com.HybridPlatformPlugin;

import android.content.Context;

import kr.go.mobile.common.v3.hybrid.CBHybridException;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridBrokerPlugin;

public class SEMP extends CBHybridBrokerPlugin {
    public SEMP(Context context) {
        super(context);
    }

    public void request(String jsonArgs) throws CBHybridException {
        //TODO callbackID 필요
        super.asyncRequest("", jsonArgs);
    }
}
