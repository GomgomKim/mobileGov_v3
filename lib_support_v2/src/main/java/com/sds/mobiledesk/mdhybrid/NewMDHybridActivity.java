package com.sds.mobiledesk.mdhybrid;

import android.util.Log;

import kr.go.mobile.common.v3.hybrid.CBHybridActivity;

public class NewMDHybridActivity extends CBHybridActivity {

    public void setLoadableUrl(String s) {
        loadUrl(s);
    }

    public void addService(String semp, String name) {
        Class cls = null;
        try {
            cls = Class.forName(name);
        } catch (ClassNotFoundException e) {
            // TODO 잘못된 클래스 값 전달 시 처리
            Log.i("@@@", "잘못된 클래스 값 전달");
            e.printStackTrace();
        }
        addPlugin(semp, cls);
    }

}
