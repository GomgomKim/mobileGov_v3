package kr.go.mobile.iff.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import java.util.StringTokenizer;

import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.CommonBasedConstants;

public class LoadingActivity extends AppCompatActivity {

    private static final int GOV_INIT_REQUEST = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_loading);
        CommonBasedAPI.startInitActivityForResult(this, GOV_INIT_REQUEST);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent intent) {
        Log.d("@@@", "공통기반 서비스 초기화 결과");
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == GOV_INIT_REQUEST
                && resultCode == Activity.RESULT_OK
                && intent != null) {

            try {
                String userDN = CommonBasedAPI.handleInitActivityResult(this, resultCode, intent);

                String userId = intent.getStringExtra(CommonBasedConstants.EXTRA_KEY_USER_ID);
                Log.d("@@@", "공통기반 서비스 초기화 성공 - 사용자 : " + userId);

                // dn 추출 및 파싱
                String dn = intent.getStringExtra(CommonBasedConstants.EXTRA_KEY_DN);
                String cn = null;
                String ou = null;
                StringTokenizer st = new StringTokenizer(dn, ",");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();

                    if (token.startsWith("cn")) {
                        cn = token.substring(3);
                    } else if (token.startsWith("ou")) {
                        ou = token.substring(3);
                    }
                }

                // 다음 화면으로 전환.
                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.putExtra("dn", dn);
                mainIntent.putExtra("cn", cn);
                mainIntent.putExtra("ou", ou);
                startActivity(mainIntent);
            } catch (CommonBasedAPI.CommonBaseAPIException e) {
                // 공통기반 API 초기화 실패
                // 애플리케이션 종료
                Log.i("@@@", "공통기반 API 초기화 실패");
                Log.d("@@@", "resultCode = " + resultCode);
            }
            finish();
        }
    }
}
