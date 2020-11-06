package com.sds.mobiledesk.mdhybrid;

import android.content.Intent;
import android.os.Bundle;

import kr.go.mobile.common.v3.hybrid.CBHybridActivity;

public class NewMDHybridActivity extends CBHybridActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void addService(String var1, String var2){
//        addPlugin(var1, var2.getClass());
    }

    public void setLoadableUrl(String var1) {
        loadUrl(var1);
    }

    public Intent getIntent(){
        return getIntent();
    }
}
