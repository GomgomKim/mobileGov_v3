package kr.go.mobile.iff.sample;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.HybridPlatformPlugin.SEMP;
import com.infrawaretech.docviewer.plugin.DOCViewPlugin;
import com.sds.mobiledesk.mdhybrid.NewMDHybridActivity;

import kr.go.mobile.iff.sample.hybrid.UserActivityPlugin;


public class HybridActivity extends NewMDHybridActivity {

	Context mContext;
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		WebView.setWebContentsDebuggingEnabled(true);
		
		this.mContext = this;  
		
		String dn = getIntent().getStringExtra("dn"); 
		String cn = getIntent().getStringExtra("cn"); 
		String ou = getIntent().getStringExtra("ou");
		
		StringBuilder sb = new StringBuilder();
		sb.append("이름: ").append(cn);
		sb.append("\n소속기관: ").append(ou);
		sb.append("\n(").append(dn).append(")");

		addService("SEMP", SEMP.class.getName());
		addService("User", UserActivityPlugin.class.getName());
		addService("DOCViewPlugin", DOCViewPlugin.class.getName());
		
//		setLoadableUrl("javascript:var cn='"+ cn +"'; var ou='" + ou + "'; dn='" + dn +"';dvhost='10.180.22.77:65535'");
		setLoadableUrl("javascript:var cn=\"+ cn +\"; var ou=\" + ou + \"; dn=\" + dn +\";dvhost='10.180.12.216:65535'");
		
		Log.i("@@@", "하이브리드 앱 메인 페이지 호출");
        setLoadableUrl("file:///android_asset/www/index.html");
	}
}
