package kr.go.mobile.iff.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dkitec.PushLibrary.Listener.PushAppRegistListener;
import com.dkitec.PushLibrary.PushLibrary;

import kr.go.mobile.iff.sample.hybrid.HybridActivity;

public class MainActivity extends AppCompatActivity {

    private Bundle extraData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("@@@", "--- Main Activity");
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
        final String serverAddress = getString(R.string.pushurl);
        final String appID = getString(R.string.pushid);
        int ret = PushLibrary.getInstance().setStart(this, serverAddress, appID);
        Log.d("TEST", "setStart : " + ret );

        String cn = getIntent().getStringExtra("cn");
        // PUSH 서비스 등록 요청
        ret = PushLibrary.getInstance().AppRegist(this, new PushAppRegistListener() {

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

        }, cn, null, null);
        Log.d("TEST", "AppRegist : " + ret );
        Toast.makeText(this, "등록 시도 - 정상", Toast.LENGTH_SHORT).show();
        Log.d("TEST", "등록 시도 - 정상");

    }
}