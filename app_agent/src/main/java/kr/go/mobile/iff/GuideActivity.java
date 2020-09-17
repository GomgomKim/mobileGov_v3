package kr.go.mobile.iff;

import kr.go.mobile.mobp.iff.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class GuideActivity extends Activity {

	public static final String KEY_GUIDE_TYPE = "extra_key_guide_type";
	public static final byte GUIDE_TYPE_V3_REMOVE = 0x01;
	public static final byte GUIDE_TYPE_VG_SETUP = 0x02;
	
	@JavascriptInterface
	public void webview_finish() {
		GuideActivity.this.finish();
	}
	
	@JavascriptInterface
	public int webview_scale() {
		int ret = 100;
		float BASE = 2268;
		float height = view.getHeight();
		Log.d("@@@", ""+ height);
		if (BASE > height) {
			float aaa =  (1 - (height / 2395)) * 100;
			Log.d("@@@", BASE + ", "+height + ", " + aaa );
			if (aaa > 0) {
				ret = (int) (100 - (Math.ceil(aaa))); 
			}
		}
		return ret;
	}
	
	WebView view = null;
	@Override
	  protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_guide);


	    view = (WebView) findViewById(R.id.webView);
	    
	    view.setPadding(0, 0, 0, 0);
	    view.addJavascriptInterface(this, "GuideView");
	    view.setHorizontalScrollBarEnabled(false);;
	    view.setVerticalScrollBarEnabled(false);
	    view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
	    WebSettings settings = view.getSettings();
	    settings.setLoadsImagesAutomatically(true);
	    settings.setJavaScriptEnabled(true);
	    settings.setSupportZoom(true);
	    settings.setDisplayZoomControls(true);
	    settings.setBuiltInZoomControls(true);
	    settings.setUseWideViewPort(true);
	    settings.setLoadWithOverviewMode(true);
	    settings.setDefaultTextEncodingName("UTF-8");
	    
	    byte type = getIntent().getExtras().getByte(KEY_GUIDE_TYPE);
	    switch (type) {
		case GUIDE_TYPE_V3_REMOVE:
		    setTitle(" V3 Mobile Enterprise 삭제 가이드");
			view.loadUrl("file:///android_asset/guide/v3remove.html");
			break;
		case GUIDE_TYPE_VG_SETUP:
		    setTitle(" V-Guard Enterprise 설치 가이드");
			view.loadUrl("file:///android_asset/guide/vgsetup.html");
			break;
		default:
			finish();
			break;
		}
	  }
	
	@Override
	public void onBackPressed() {
		Toast.makeText(this, "가이드 문서를 다 읽으시고 '확인'을 누르시기 바랍니다.", Toast.LENGTH_LONG).show();
	}
	

}
