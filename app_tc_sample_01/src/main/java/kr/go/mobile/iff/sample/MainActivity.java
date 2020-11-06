package kr.go.mobile.iff.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dkitec.PushLibrary.Listener.PushAppRegistListener;

import kr.go.mobile.iff.sample.hybrid.HybridActivity;
import kr.go.mobile.iff.sample.nativz.NativeActivity;
import kr.go.mobile.iff.sample.push.LocalPushUtils;

public class MainActivity extends AppCompatActivity {

    private Bundle extraData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        extraData = getIntent().getExtras();
    }

    public void onClickNative(View view) {
        Intent _intent = new Intent(this, NativeActivity.class);
        _intent.putExtras(extraData);
        startActivity(_intent);
    }

    public void onClickHybrid(View view) {
        Intent _intent = new Intent(this, HybridActivity.class);
        _intent.putExtras(extraData);
        startActivity(_intent);
    }

    public void onClickLocalPush(View view) {
        Toast.makeText(this, "등록시도...", Toast.LENGTH_SHORT).show();
        Log.d("TEST", "등록시도..");
        final String serverAddress = BuildConfig.pushurl;
        final String appID = BuildConfig.pushid;
        final String userId = getIntent().getStringExtra("cn");
        int ret = LocalPushUtils.register(this, serverAddress, appID, userId, new PushAppRegistListener() {
            @Override
            public void didRegistResult(Context context, Bundle bundle) {
                // 요청 결과 확인
                String code = bundle.getString("RT");
                String msg = bundle.getString("RT_MSG");
                if (code.equals("0000")) {
                    Toast.makeText(MainActivity.this, "등록 성공", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Code = " + code + ", Message = " + msg, Toast.LENGTH_LONG).show();
                }
                Log.d("TEST", "등록응답: Code = " + code + ", Message = " + msg);
            }
        });
        String retMessage = "";
        if (ret == 1400) {
            retMessage = "로컬 푸쉬 등록 성공";
            Log.d("TEST", retMessage);
        } else {
            retMessage = "로컬 푸쉬 등록 실패 (code : "+ ret + ")" ;
            Log.e("TEST", retMessage );
        }
        Toast.makeText(this, retMessage, Toast.LENGTH_SHORT).show();
    }
}
