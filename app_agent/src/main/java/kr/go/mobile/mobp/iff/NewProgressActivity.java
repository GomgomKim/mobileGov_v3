package kr.go.mobile.mobp.iff;

import kr.go.mobile.mobp.iff.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

@Deprecated
public class NewProgressActivity extends Activity {
	
	public static Activity progressActivity;
	
	public static final String EXTRA_TITLE = "title";
	
	private LinearLayout layoutProgress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_progress);
		
		progressActivity = this;
		
		layoutProgress = (LinearLayout) findViewById(R.id.linlaHeaderProgress);
		
		TextView tvTitle = (TextView) findViewById(R.id.tvTitle);
		
		Intent intent = getIntent();
		String title = intent.getStringExtra(EXTRA_TITLE);
		
		tvTitle.setText(title != null ? title : "");
		
	}

	@Override
	protected void onResume() {
		if(layoutProgress.getVisibility() != View.VISIBLE){
			layoutProgress.setVisibility(View.VISIBLE);
		}
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		if(layoutProgress.getVisibility() == View.VISIBLE){
			layoutProgress.setVisibility(View.INVISIBLE);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {

		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}




//	@Override
//	public void onBackPressed() {
//		super.onBackPressed();
//		
//		Utils.IS_PROGRESS_SHOWING = false;
//	}



	
}
