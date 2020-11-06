package kr.go.mobile.iff.sample.customdoc;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;

import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.document.ConvertStatus;
import kr.go.mobile.common.v3.document.ConvertedDoc;
import kr.go.mobile.common.v3.document.DocConvertManager;
import kr.go.mobile.iff.sample.R;


public class CustomDocActivity extends AppCompatActivity {

    private CustomPagerAdapter mPagerAdapter;
    private ProgressBar mProgressBar;
    private TextView mTVPageNum;
    private int mTotalPageNum;
    private AlertDialog mDialog;
    DocConvertManager manager;

    private static final String DOC_FILE_NAME = "sample.pdf";
    private static final String DOC_FILE_URL = "http://10.180.22.77:65535/MOI_API/file/sample.pdf";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_doc_activity);

        CustomViewPager viewPager = findViewById(R.id.viewPager);
        mProgressBar = findViewById(R.id.progressBar);
        mTVPageNum = findViewById(R.id.tvBottom);
        mTotalPageNum = 0;
        mPagerAdapter = new CustomPagerAdapter();
        viewPager.setAdapter(mPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //페이지 번호 갱신
                updatePageNum(position + 1);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mProgressBar.setVisibility(View.VISIBLE);

        this.setTitle(DOC_FILE_NAME);

        // 공통기반 문서 관리자 연동
        try {
            manager = CommonBasedAPI.createDocConvertManager(DOC_FILE_URL, DOC_FILE_NAME, "");
            manager.setConvertedListener(new DocConvertManager.DocConvertListener() {
                @Override
                public void updateConvertedStatus(ConvertStatus convertStatus) {
                    if (mTotalPageNum != convertStatus.getConvertedPageCount()) {
                        mTotalPageNum = convertStatus.getConvertedPageCount();
                        int currentPageNum = mPagerAdapter.getCount();
                        for (int i = currentPageNum + 1 ; i <=mTotalPageNum ; i++) {
                            Bitmap loadingDoc = BitmapFactory.decodeResource(getResources(), R.drawable.loading);
                            mPagerAdapter.addView(loadingDoc);

                            try {
                                manager.requestConvertedData(i);
                            } catch (DocConvertManager.DocConvertException e) {
                                finishDocActivity(e.getMessage());
                            }
                        }
                    }
                }

                @Override
                public void onConverted(ConvertedDoc convertedDoc) {
                    if (convertedDoc.getPageDoc() == 1) {
                        updatePageNum(convertedDoc.getPageDoc());
                    }
                    mProgressBar.setVisibility(View.INVISIBLE);
                    Bitmap docBitmap = convertedDoc.getBitmapDoc();
                    mPagerAdapter.setView(docBitmap, convertedDoc.getPageDoc());
                    mPagerAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFailed(int i, String s) {
                    showMsgPopup(s);
                }
            });
            manager.requestConvertedData(1);

        } catch (DocConvertManager.DocConvertException e) {
            finishDocActivity(e.getMessage());
        }
    }

    private void finishDocActivity(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("알림");
        builder.setMessage("문서뷰어를 연동할 수 없습니다. (" + msg + ")");
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mDialog.dismiss();
                        finish();
                    }
                });
        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_btn1:
                String tel ="tel:15886441";
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(tel)));

                break;
            case R.id.action_btn2:
                showMsgPopup("문서 결재하기");
                break;
            case R.id.action_btn3:
                TedPermission.with(CustomDocActivity.this)
                        .setPermissionListener(new PermissionListener() {

                            @Override
                            public void onPermissionGranted() {
                                Toast.makeText(CustomDocActivity.this, "문의 문자를 발송하였습니다.", Toast.LENGTH_SHORT).show();
                                SmsManager manager = SmsManager.getDefault();
                                manager.sendTextMessage("15886441", null, DOC_FILE_NAME + " 문서 관련 문의드립니다.", null, null);
                            }

                            @Override
                            public void onPermissionDenied(ArrayList<String> deniedPermissions) {

                            }
                        })
                        .setRationaleMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
                        .setDeniedMessage("문자 전송을 위한 접근 권한이 필요합니다.")
                        .setPermissions(Manifest.permission.SEND_SMS)
                        .check();

                break;
        }

        return  super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.clear();
    }

    private void updatePageNum(int currentPosition) {
        mTVPageNum.setText(currentPosition + "/" + mTotalPageNum);
    }

    private void showMsgPopup(String msg) {
        if (mDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("알림");
            builder.setMessage(msg);
            builder.setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mDialog.dismiss();
                        }
                    });

            mDialog = builder.create();
            mDialog.show();
        } else {
            if (mDialog.isShowing()) {
                return;
            } else {
                mDialog.setMessage(msg);
                mDialog.show();
            }
        }
    }
}
