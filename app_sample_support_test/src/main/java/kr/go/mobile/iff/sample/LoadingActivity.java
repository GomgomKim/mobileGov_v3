package kr.go.mobile.iff.sample;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sds.BizAppLauncher.gov.aidl.GovController;
import com.sds.BizAppLauncher.gov.util.CertiticationUtil;
import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import org.json.JSONException;

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
                if (isEmergency) {
                    Log.e("@@@", "isEmergency : true");
                    return;
                }
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
                Log.e("@@@", "onTimeover");
                finish();
            }

            @Override
            protected void onTerminated() {
                super.onTerminated();
                // 보안검증 라이브러리 초기화 실패
                // 애플리케이션 종료
                Log.e("@@@", "onTerminated");
                finish();
            }
        }.setTimeout(-1).execute();
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == GOV_INIT_REQUEST) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                // 공통기반 서비스 초기화 성공
                Log.d("@@@", "공통기반 서비스 초기화 성공 " + intent.getExtras());

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

                new ServiceBrokerLib(this, new ResponseListener() {
                    @Override
                    public void receive(ResponseEvent responseevent) {
                        int code = responseevent.getResultCode();
                        String message = responseevent.getResultData();
                        StringBuilder sb = new StringBuilder();
                        sb.append("Result :: ")
                                .append("code = ").append(code)
                                .append(", result = ").append(message);
                        Log.d("Result : ", sb.toString());
                        Toast.makeText(getBaseContext(), sb.toString(), Toast.LENGTH_LONG).show();
                    }
                });

                // 다음 화면으로 전환.
                Intent _intent = new Intent(LoadingActivity.this, MainActivity.class);
                _intent.putExtra("dn", dn);
                _intent.putExtra("cn", cn);
                _intent.putExtra("ou", ou);
                startActivity(_intent);
            } else {
                // 공통기반 API 초기화 실패
                // 애플리케이션 종료
                Log.i("@@@", "공통기반 API 초기화 실패");
                Log.d("@@@", "resultCode = " + resultCode);
            }
            finish();
        }
    }
}
