package kr.go.mobile.iff.sample.nativz;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.common.v3.RestrictedAPI;
import kr.go.mobile.common.v3.broker.Response;
import kr.go.mobile.common.v3.broker.SSO;
import kr.go.mobile.iff.sample.R;
import kr.go.mobile.iff.sample.customdoc.CustomDocActivity;
import kr.go.mobile.iff.sample.util.TimeStamp;

public class NativeActivity extends AppCompatActivity {

    private static final String TAG = "@@@@";
    static Context context;
    static final int PICK_FROM_ALBUM = 1;
    static final Handler handler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message message) {
            if (message.what == PICK_FROM_ALBUM) {
                FileUploadVO vo = (FileUploadVO) message.obj;
                TedPermission.with(NativeActivity.context)
                        .setPermissionListener(new PermissionListener() {

                            @Override
                            public void onPermissionGranted() {
                                Intent intent = new Intent(Intent.ACTION_PICK);
                                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                                NativeActivity.startPickupActivity(intent, NativeActivity.PICK_FROM_ALBUM);
                            }

                            @Override
                            public void onPermissionDenied(ArrayList<String> deniedPermissions) {

                            }
                        })
                        .setRationaleMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
                        .setDeniedMessage("사진 및 파일을 저장하기 위하여 접근 권한이 필요합니다.")
                        .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .check();
            } else {
                throw new IllegalStateException("Unexpected value: " + message.what);
            }
        }
    };

    Response.Listener getDefaultListener() {
        return new Response.Listener() {
            @Override
            public void onSuccess(Response resp) {
                TimeStamp.endTime("broker");
                int code = resp.getErrorCode();
                StringBuilder sb = new StringBuilder();

                if(resp.OK()) {
                    String message = resp.getResponseString();
                    sb.append("Result :: ")
                            .append("code = ").append(code)
                            .append(", result = ").append(message);
                    Log.d("Result : ", sb.toString());
                    Log.d("Result : ", "(size = " + message.length() +")");
                } else {
                    String title;
                    String message = resp.getErrorMessage();
                    switch (resp.getErrorCode()) {
                        case CommonBasedConstants.BROKER_ERROR_RELAY_SYSTEM: // 공통기반 시스템에서 확인된 에러 (중계 서버에서 처리 중 발생한 에러)
                            title = "서비스 연계 에러";
                            break;
                        case CommonBasedConstants.BROKER_ERROR_SERVICE_SERVER: // 서비스 제공 서버에서 발생한 HTTP 에러 (행정 서비스 서버 접속시 발생함)
                            title = "서비스 제공 서버 HTTP 응답 에러";
                            break;
                        case CommonBasedConstants.BROKER_ERROR_FAILED_REQUEST: // 서비스 요청 실패 (네트워크 유실로 발생할 수 있음)
                            title = "서비스 요청 실패";
                            break;
                        case CommonBasedConstants.BROKER_ERROR_INVALID_RESPONSE: // 서비스 응답 메시지 처리 에러 (네트워크 유실로 발생할 수 있음)
                            title = "서비스 응답 처리 실패";
                            break;
                        default:
                            title = "정의되지 않음.";
                            message = "알수없음.";

                    }

                    sb.append("ERROR :: ").append("[").append(title).append("]\n").append(message);
                    Log.e("ERROR : ",  sb.toString());
                }
                Toast.makeText(NativeActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(int errorCode, String errMessage, Throwable t) {
                Log.e(TAG, errMessage + "(code : " + errorCode + ")", t);
                Toast.makeText(NativeActivity.this, errMessage + "(code : " + errorCode + ")", Toast.LENGTH_LONG).show();
            }
        };
    }

    static void startPickupActivity(Intent intent, int pickFromAlbum) {
        ((Activity)context).startActivityForResult(intent, pickFromAlbum);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native);
        Log.d("@@@", "--- Native Activity");
        context = getBaseContext();
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
        makeFileUploadList();
        makeFileDownloadList();
    }



    private void makeServiceBrokerList() {
        // 서비스 브로커 기능
        ServiceBrokerListAdapter adapter = new ServiceBrokerListAdapter();
        adapter.addServiceVO("NOT_EXIST_SERVICE_ID", "", getDefaultListener());
        adapter.addServiceVO("IF_MSERVICE", "", getDefaultListener());
        adapter.addServiceVO("IF_MSERVICE", "req=big", getDefaultListener());

        ListView listview = findViewById(R.id.listServiceBroker) ;
        listview.setVisibility(View.GONE);
        listview.setAdapter(adapter);
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
        adapter.addDocViewerVO("sample.hwp", "http://10.180.22.77:65535/MOI_API/file/sample.hwp", "");

        adapter.addDocViewerVO("sample.pdf", "http://10.180.12.216:65535/MOI_API/file/sample.pdf", "");
        adapter.addDocViewerVO("sample.xlsx", "http://10.180.12.216:65535/MOI_API/file/sample.xlsx", "");
        adapter.addDocViewerVO("sample.pptx", "http://10.180.12.216:65535/MOI_API/file/sample.pptx", "");
        adapter.addDocViewerVO("sample.hwp", "http://10.180.12.216:65535/MOI_API/file/sample.hwp", "");

        ListView listview = findViewById(R.id.listDocumentViewer) ;
        listview.setVisibility(View.GONE);
        listview.setAdapter(adapter) ;
    }

    private void makeFileUploadList() {
        //
        final String regex = "-crash-log-(\\d){12}.log";
        String[] stackTraces = getFilesDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(regex);
            }
        });
        String externalAppDataFilePath = getExternalFilesDir("TMP").getAbsoluteFile() + "/tmp.jpg";
        String contentProviderChoice = "";
        //
        String relayUrl = "http://10.180.22.77:65535/MOI_API/upload";

        FileUploadListAdapter adapter = new FileUploadListAdapter();
        if (stackTraces.length > 0) {
            String systemAppDataFilePath = new File(getFilesDir(), stackTraces[0]).getAbsolutePath();
            // system app dir
            adapter.addFileUploadVO(systemAppDataFilePath, relayUrl, "");
        }
        // content provider
        adapter.addFileUploadVO(contentProviderChoice, relayUrl, "");
        // external storage app data dir
        adapter.addFileUploadVO(externalAppDataFilePath, relayUrl, "");


        ListView listview = findViewById(R.id.listFileUpload) ;
        listview.setVisibility(View.GONE);
        listview.setAdapter(adapter) ;
    }

    private void makeFileDownloadList() {
        FileDownloadListAdapter adapter = new FileDownloadListAdapter();
        String relayUrl = "http://10.180.22.77:65535/MOI_API/file/sample.pdf";
        String downloadPath =
                new File(getExternalFilesDir("doc"), "sample.pdf").getAbsolutePath();
        adapter.addFileDownloadVO(relayUrl, downloadPath);

        ListView listview = findViewById(R.id.listFileDownload) ;
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
        ListView listview = findViewById(R.id.listServiceBroker) ;
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
        ListView listview = findViewById(R.id.listDocumentViewer) ;
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

    public void onClickRestricted(View view) {
        RestrictedAPI.setKey("aHR0cDovLzEwLjE4MC4yMi43Nzo2NTUzNQ==");
    }

    public void onClickUpload(View view) {
        ListView listview = findViewById(R.id.listFileUpload) ;
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

    public void onClickDownload(View view) {
        ListView listview = findViewById(R.id.listFileDownload) ;
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
        Intent i = new Intent(this, CustomDocActivity.class);
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FROM_ALBUM) {
            String targetName = null;
            Uri targetUri = data.getData();
            if (targetUri.getScheme().equals("content")) {
                Cursor cursor = getContentResolver().query(targetUri, null, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        targetName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    if (cursor != null)
                     cursor.close();
                }
            }
            if (targetName == null){
                targetName = targetUri.getLastPathSegment();
            }
            String relayUrl = "http://10.180.22.77:65535/MOI_API/upload";
            String extraParams = "";
            try {

                Response resp = RestrictedAPI.executeUpload(targetName, targetUri, relayUrl, extraParams);
                int code = resp.getErrorCode();
                if (code == CommonBasedConstants.BROKER_ERROR_NONE) {
                    String message = resp.getResponseString();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Result :: ")
                            .append("code = ").append(code)
                            .append(", result = ").append(message);
                    Log.d("Result : ", sb.toString());
                    Toast.makeText(NativeActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(NativeActivity.this, "실패 - " + resp.getErrorMessage(), Toast.LENGTH_LONG).show();
                }
            } catch (CommonBasedAPI.CommonBaseAPIException e) {
                Log.e("@@@", e.getMessage(), e);
                Toast.makeText(this, "요청 실패 : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    }
}
