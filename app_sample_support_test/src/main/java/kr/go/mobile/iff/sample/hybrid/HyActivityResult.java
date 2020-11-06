package kr.go.mobile.iff.sample.hybrid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import kr.go.mobile.iff.sample.R;


public class HyActivityResult extends Activity {

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_loading);
	}
	
	public void doProcess(String type) {
		Intent data = new Intent();
		data.putExtra("type", type);
		setResult(RESULT_OK, data);
		finish();

	}
	
	public void onClickSampleUesrInfo(View view) {
		doProcess("button");
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if( event.getAction() == KeyEvent.ACTION_DOWN ){           
            if( keyCode == KeyEvent.KEYCODE_BACK ){ 
            	doProcess("back");
                return false; 
            }
        }
        return super.onKeyDown( keyCode, event );
    }
}
