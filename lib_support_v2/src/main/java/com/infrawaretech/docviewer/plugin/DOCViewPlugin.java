package com.infrawaretech.docviewer.plugin;

import android.content.Context;

import kr.go.mobile.common.v3.hybrid.CBHybridException;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridBrokerPlugin;

public class DOCViewPlugin extends CBHybridBrokerPlugin {
    public DOCViewPlugin(Context context) {
        super(context);
    }

    public void load(String reqParams) throws CBHybridException {
        super.startDefaultDocumentView(reqParams);
    }
}
