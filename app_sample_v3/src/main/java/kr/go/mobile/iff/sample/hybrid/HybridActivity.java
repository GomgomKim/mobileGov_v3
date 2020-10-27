package kr.go.mobile.iff.sample.hybrid;

import android.os.Bundle;
import android.webkit.WebView;

import kr.go.mobile.common.v3.hybrid.CBHybridActivity;

public class HybridActivity extends CBHybridActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView.setWebContentsDebuggingEnabled(true);
        addPlugin("CustomService", HybridCustomAPI.class);
        String cn = getIntent().getStringExtra("cn");
        String ou = getIntent().getStringExtra("ou");
        loadUrl("file:///android_asset/www/index.html?cn="+cn+"&ou="+ou);
    }
}
