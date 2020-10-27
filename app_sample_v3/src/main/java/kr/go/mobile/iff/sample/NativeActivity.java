package kr.go.mobile.iff.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.broker.Response;
import kr.go.mobile.common.v3.broker.SSO;
import kr.go.mobile.iff.sample.util.TimeStamp;

public class NativeActivity extends AppCompatActivity {

    private static final String TAG = "@@@@";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native);
        Log.d("@@@", "--- Native Activity");

        String dn = getIntent().getStringExtra("dn");
        String cn = getIntent().getStringExtra("cn");
        String ou = getIntent().getStringExtra("ou");


        StringBuilder sb = new StringBuilder();
        sb.append("이름: ").append(cn);
        sb.append("\n소속기관: ").append(ou);
        sb.append("\n(").append(dn).append(")");

        TextView txtView = findViewById(R.id.txtUesrInfo);
        txtView.setText(sb.toString());

        makeServiceBrokerList();
        makeDocViewerList();
    }

    private void makeServiceBrokerList() {
        // 서비스 브로커 기능
        ServiceBrokerListAdapter adapter = new ServiceBrokerListAdapter();
        adapter.addServiceVO("NOT_EXIST_SERVICE_ID", "", new Response.Listener() {
            @Override
            public void onSuccess(Response resp) {

                int code = resp.getErrorCode();
                String message = resp.getResponseString();
                final StringBuilder sb = new StringBuilder();
                sb.append("Result :: ")
                        .append("code = ").append(code)
                        .append(", result = ").append(message);
                Log.d("Result : ", sb.toString());

                Toast.makeText(NativeActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int errorCode, String errMessage, Throwable t) {
                Log.e(TAG, errMessage + "(code : " + errorCode + ")", t);
                Toast.makeText(NativeActivity.this, errMessage + "(code : " + errorCode + ")", Toast.LENGTH_LONG).show();
            }
        });
        adapter.addServiceVO(false, "IF_MSERVICE", "", new Response.Listener() {
            @Override
            public void onSuccess(Response resp) {

                int code = resp.getErrorCode();
                String message = resp.getResponseString();
                final StringBuilder sb = new StringBuilder();
                sb.append("Result :: ")
                        .append("code = ").append(code)
                        .append(", result = ").append(message);
                Log.d("Result : ", sb.toString());

                Toast.makeText(NativeActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int errorCode, String errMessage, Throwable t) {
                Log.e(TAG, errMessage + "(code : " + errorCode + ")", t);
                Toast.makeText(NativeActivity.this, errMessage + "(code : " + errorCode + ")", Toast.LENGTH_LONG).show();
            }
        });
        adapter.addServiceVO("IF_MSERVICE", "", new Response.Listener() {
            @Override
            public void onSuccess(Response resp) {
                TimeStamp.endTime("broker");
                int code = resp.getErrorCode();
                String message = resp.getResponseString();
                final StringBuilder sb = new StringBuilder();
                sb.append("Result :: ")
                        .append("code = ").append(code)
                        .append(", result = ").append(message);
                Log.d("Result : ", sb.toString());

                Toast.makeText(NativeActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int errorCode, String errMessage, Throwable t) {
                Log.e(TAG, errMessage + "(code : " + errorCode + ")", t);
                Toast.makeText(NativeActivity.this, errMessage + "(code : " + errorCode + ")", Toast.LENGTH_LONG).show();
            }
        });
        adapter.addServiceVO("IF_MSERVICE", "req=big", new Response.Listener() {
            @Override
            public void onSuccess(Response resp) {
                int code = resp.getErrorCode();
                String message = resp.getResponseString();
                StringBuilder sb = new StringBuilder();
                sb.append("Result :: ")
                        .append("code = ").append(code)
                        .append(", result = ").append(message);
                Log.d("Result : ", sb.toString());
                Toast.makeText(NativeActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int errorCode, String errMessage, Throwable t) {
                Log.e(TAG, errMessage + "(code : " + errorCode + ")", t);
                Toast.makeText(NativeActivity.this, errMessage + "(code : " + errorCode + ")", Toast.LENGTH_LONG).show();
            }
        });

        adapter.addServiceVO("IF_MSERVICE", new Byte[1024*1024].toString(), new Response.Listener() {
            @Override
            public void onSuccess(Response resp) {
                int code = resp.getErrorCode();
                String message = resp.getResponseString();
                StringBuilder sb = new StringBuilder();
                sb.append("Result :: ")
                        .append("code = ").append(code)
                        .append(", result = ").append(message);
                Log.d("Result : ", sb.toString());
                Toast.makeText(NativeActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int errorCode, String errMessage, Throwable t) {
                Log.e(TAG, errMessage + "(code : " + errorCode + ")", t);
                Toast.makeText(NativeActivity.this, errMessage + "(code : " + errorCode + ")", Toast.LENGTH_LONG).show();
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
        final StringBuilder sb = new StringBuilder();
        try {
            SSO sso = CommonBasedAPI.getSSO();
            sb.append("====== 사용자 정보 획득 ======\n");
            sb.append("nickname : ").append(sso.getUserDN()).append("\n");
            sb.append("cn : ").append(sso.getUserID()).append("\n");
            sb.append("ou : ").append(sso.getOuName()).append("\n");
            sb.append("ou code : ").append(sso.getOuCode()).append("\n");
            sb.append("department : ").append(sso.getDepartmentName()).append("\n");
            sb.append("department number : ").append(sso.getDepartmentCode()).append("\n");
            sb.append("========================");
        } catch (CommonBasedAPI.CommonBaseAPIException e) {
            Log.e(TAG, "사용자 정보 획득에 실패하였습니다.", e);
            sb.append("** 사용자 정보 획득에 실패하였습니다. **");
        }

        Log.d("사용자 정보", sb.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(NativeActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
            }
        });
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
}
