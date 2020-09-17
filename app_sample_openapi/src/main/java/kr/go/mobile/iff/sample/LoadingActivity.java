package kr.go.mobile.iff.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.infrawaretech.docviewer.DocConvertManager;
import com.sds.BizAppLauncher.gov.aidl.GovController;

import java.util.StringTokenizer;

import kr.co.everspin.eversafe.EversafeHelper;

public class LoadingActivity extends AppCompatActivity {

    private static final int GOV_INIT_REQUEST = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_loading);

        SampleApplication sample = (SampleApplication) getApplication();

        EversafeHelper.getInstance().initialize(sample.getMSMUrl());

        EversafeHelper.GetVerificationTokenTask d = new EversafeHelper.GetVerificationTokenTask();
        d.setTimeout(-1);
        d.execute();

        new EversafeHelper.GetVerificationTokenTask() {
            @Override
            protected void onCompleted(byte[] verificationToken, String verificationTokenAsByte64, boolean isEmergency) {
                super.onCompleted(verificationToken, verificationTokenAsByte64, isEmergency);
                // 보안검증 라이브러리 초기화 성공
                // 검증토큰 취득
                Log.d("@@@", "보안토큰 취득 성공");
                Log.d("@@@", "verificationTokenAsByte64 = " + verificationTokenAsByte64);

                // 공통기반 서비스 초기화
                Log.d("@@@", "공통기반 서비스 초기화 시작");
                GovController.startGovActivityForResult(LoadingActivity.this, GOV_INIT_REQUEST, verificationTokenAsByte64);
            }

            @Override
            protected void onTimeover() {
                super.onTimeover();
                // 정해진 시간 이내에 검증토큰 취득 실패
                // 애플리케이션 종료
                finish();
            }

            @Override
            protected void onTerminated() {
                super.onTerminated();
                // 보안검증 라이브러리 초기화 실패
                // 애플리케이션 종료
                finish();
            }
        }.setTimeout(-1).execute();
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (intent != null) {
            if (requestCode == GOV_INIT_REQUEST) {
                if (resultCode == Activity.RESULT_OK) {

                    // 공통기반 서비스 초기화 성공
                    Log.d("@@@", "공통기반 서비스 초기화 성공");

                    // dn 추출 및 파싱
                    String dn = null;
                    String cn = null;
                    String ou = null;
                    dn = intent.getStringExtra("dn");
                    StringTokenizer st = new StringTokenizer(dn, ",");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        // cn 추출
                        if (token.startsWith("cn")) {
                            cn = token.substring(3);
                        }
                        // ou 추출
                        if (token.startsWith("ou")) {
                            ou = token.substring(3);
                        }
                    }

                    //문서뷰어 OPEN API 초기화
                    DocConvertManager.getInstance().init(this.getApplicationContext());

                    // 다음 화면으로 전환.
                    Intent _intent = new Intent(LoadingActivity.this, MainActivity.class);
                    _intent.putExtra("dn", dn);
                    _intent.putExtra("cn", cn);
                    _intent.putExtra("ou", ou);
                    startActivity(_intent);
                    finish();
                } else {
                    // 공통기반 API 초기화 실패
                    // 애플리케이션 종료
                    Log.i("@@@", "공통기반 API 초기화 실패");
                    Log.d("@@@", "resultCode = " + resultCode);
                    finish();
                }
            }
        }
    }
}
