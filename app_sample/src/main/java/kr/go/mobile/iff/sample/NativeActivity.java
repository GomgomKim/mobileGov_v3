package kr.go.mobile.iff.sample;

import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sds.BizAppLauncher.gov.util.CertiticationUtil;
import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import org.json.JSONException;

import java.util.ArrayList;

public class NativeActivity extends AppCompatActivity {

    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override public void onRmsChanged(float rmsdB) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int i) {

        }

        @Override
        public void onResults(Bundle bundle) {
            String key = ""; key = SpeechRecognizer.RESULTS_RECOGNITION;
            ArrayList<String> mResult = bundle.getStringArrayList(key);
            final String[] rs = new String[mResult.size()];
            mResult.toArray(rs);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(NativeActivity.this, rs[0], Toast.LENGTH_SHORT).show();
                }
            });
        }



        @Override
        public void onPartialResults(Bundle bundle) {

        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native);

        String dn = getIntent().getStringExtra("dn");
        String cn = getIntent().getStringExtra("cn");
        String ou = getIntent().getStringExtra("ou");

        StringBuilder sb = new StringBuilder();
        sb.append("이름: ").append(cn);
        sb.append("\n소속기관: ").append(ou);
        sb.append("\n(").append(dn).append(")");

        TextView txtView = (TextView) findViewById(R.id.txtUesrInfo);
        txtView.setText(sb.toString());

        makeServiceBrokerList();
        makeDocViewerList();
    }

    private void makeServiceBrokerList() {
        // 서비스 브로커 기능
        ServiceBrokerListAdapter adapter = new ServiceBrokerListAdapter();
        adapter.addServiceVO("IF_MSERVICE", "", new ResponseListener() {

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
        adapter.addServiceVO("IF_MSERVICE", "req=big", new ResponseListener() {

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

        ListView listview = (ListView) findViewById(R.id.listServiceBroker) ;
        listview.setVisibility(View.GONE);
        listview.setAdapter(adapter) ;

    }

    private void makeDocViewerList() {
        // 문서 뷰어 기능
        // 운영 : 10.180.12.216:65535
        // TB : 10.180.22.77:65535

        DocViewerListAdapter adapter = new DocViewerListAdapter();
        adapter.addDocViewerVO("sample.pdf", "http://10.180.22.77:65535/MOI_API/file/sample.pdf", "");
        adapter.addDocViewerVO("sample1.pdf", "http://10.180.22.77:65535/MOI_API/file/sample1.pdf", "");
        adapter.addDocViewerVO("sample.xlsx", "http://10.180.22.77:65535/MOI_API/file/sample.xlsx", "");
        adapter.addDocViewerVO("sample.pptx", "http://10.180.22.77:65535/MOI_API/file/sample.pptx", "");

        adapter.addDocViewerVO("sample.pdf", "http://10.180.12.216:65535/MOI_API/file/sample.pdf", "");
        adapter.addDocViewerVO("sample.xlsx", "http://10.180.12.216:65535/MOI_API/file/sample.xlsx", "");
        adapter.addDocViewerVO("sample.pptx", "http://10.180.12.216:65535/MOI_API/file/sample.pptx", "");
        adapter.addDocViewerVO("sample.hwp", "http://10.180.12.216:65535/MOI_API/file/sample.hwp", "");

        ListView listview = (ListView) findViewById(R.id.listDocumentViewer) ;
        listview.setVisibility(View.GONE);
        listview.setAdapter(adapter) ;
    }

    public void onClickUserInfo(View view) {
        ServiceBrokerLib brokerLib = new ServiceBrokerLib(NativeActivity.this,
                new ServiceBrokerLib.ServiceBrokerCB() {

                    @Override
                    public void onServiceBrokerResponse(String retMsg) {
                        try {
                            CertiticationUtil certUtil = CertiticationUtil.parse(retMsg);

                            final StringBuilder sb = new StringBuilder();
                            sb.append("====== 사용자 정보 획득 ======\n");
                            sb.append("nickname : ").append(certUtil.getInfo(CertiticationUtil.KEY_NICKNAME)).append("\n");
                            sb.append("cn : ").append(certUtil.getInfo(CertiticationUtil.KEY_CN)).append("\n");
                            sb.append("ou : ").append(certUtil.getInfo(CertiticationUtil.KEY_OU)).append("\n");
                            sb.append("ou code : ").append(certUtil.getInfo(CertiticationUtil.KEY_OU_CODE)).append("\n");
                            sb.append("department : ").append(certUtil.getInfo(CertiticationUtil.KEY_DEPARTMENT)).append("\n");
                            sb.append("department number : ").append(certUtil.getInfo(CertiticationUtil.KEY_DEPARTMENT_NUMBER)).append("\n");
                            sb.append("========================");
                            Log.d("사용자 정보", sb.toString());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(NativeActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        Intent intent = new Intent();
        intent.putExtra(ServiceBrokerLib.KEY_SERVICE_ID, ServiceBrokerLib.SERVICE_GET_INFO);

        CertiticationUtil cert = new CertiticationUtil();
        cert.setRequestData(CertiticationUtil.KEY_NICKNAME);
        cert.setRequestData(CertiticationUtil.KEY_CN);
        cert.setRequestData(CertiticationUtil.KEY_OU);
        cert.setRequestData(CertiticationUtil.KEY_OU_CODE);
        cert.setRequestData(CertiticationUtil.KEY_DEPARTMENT);
        cert.setRequestData(CertiticationUtil.KEY_DEPARTMENT_NUMBER);

        intent.putExtra(ServiceBrokerLib.KEY_PARAMETER, cert.toRequestData());

        brokerLib.request(intent);
    }

    public void onClickServiceBroker(View view) {
        ListView listview = (ListView) findViewById(R.id.listServiceBroker) ;
        switch (listview.getVisibility()) {
            case View.VISIBLE:
                listview.setVisibility(View.GONE);
                break;
            case View.GONE:
                listview.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        };
    }

    public void onClickDocViewer(View view) {
        ListView listview = (ListView) findViewById(R.id.listDocumentViewer) ;
        switch (listview.getVisibility()) {
            case View.VISIBLE:
                listview.setVisibility(View.GONE);
                break;
            case View.GONE:
                listview.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        };
    }

    public void onClickDocViewerCustom(View view) {
        Toast.makeText(this, "미구현", Toast.LENGTH_SHORT).show();
        /*
        Intent _intent = new Intent(this, CustomDocActivity.class);
        startActivity(_intent);
        */
    }

    public void onClickTSS(View view) {
        Log.d("@@@", "음성인식 시작");
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        SpeechRecognizer mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mRecognizer.setRecognitionListener(listener);
        mRecognizer.startListening(i);
    }


}
