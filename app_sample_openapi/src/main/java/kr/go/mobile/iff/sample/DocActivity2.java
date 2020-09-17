package kr.go.mobile.iff.sample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.infrawaretech.docviewer.DocConvertException;
import com.infrawaretech.docviewer.DocConvertManager;
import com.infrawaretech.docviewer.DocConvertStatus;
import com.infrawaretech.docviewer.DocConvertedData;

public class DocActivity2 extends AppCompatActivity {

    private CustomPagerAdapter mPagerAdapter;
    private ProgressBar mProgressBar;
    private TextView mTVPageNum;
    private int mTotalPageNum;
    private AlertDialog mDialog;

    private static final String DOC_FILE_NAME = "sample.pptx";
    private static final String DOC_FILE_URL = "http://10.180.22.77:65535/MOI_API/file/sample.pptx";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc2);

        CustomViewPager viewPager = findViewById(R.id.viewPager);
        mProgressBar = findViewById(R.id.progressBar);
        mTVPageNum = findViewById(R.id.tvBottom);

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

        try {
            DocConvertManager.getInstance().setTargetDoc(DOC_FILE_NAME, DOC_FILE_URL, new DocConvertManager.ReqCallback() {
                @Override
                public void onReqCallback(DocConvertStatus status, DocConvertedData data) {

                    mProgressBar.setVisibility(View.INVISIBLE);

                    if (status.getStatus() == DocConvertStatus.STATUS_REQ_COMPLETE) {

                        if (mTotalPageNum != status.getTotalPage()) {
                            //문서 페이지가 많은 경우 Total Page 수가 변경되어 올 수 있어 계속적으로 업데이트 처리
                            mTotalPageNum = status.getTotalPage();
                            int currentPageNum = mPagerAdapter.getCount();
                            //빈 화면 미리 추가
                            for (int i = currentPageNum + 1; i <= mTotalPageNum; i++) {
                                Bitmap loadingBitmap = BitmapFactory.decodeResource(DocActivity2.this.getResources(), R.drawable.loading);
                                mPagerAdapter.addView(loadingBitmap);
                            }

                            //페이지 다중요청
                            for (int i = currentPageNum + 1; i <= mTotalPageNum; i++) {
                                try {
                                    DocConvertManager.getInstance().requestConvertedData(i);
                                } catch (DocConvertException e) {
                                    showPopup(e.getMessage());
                                    return;
                                }
                            }
                        }

                        Bitmap bitmap = data.getBitmap();
                        //수신 데이터를 Bitmap 변환 과정에 메모리 부족 등 발생시 Bitmap이 null로 전달될 수 있다.
                        //또는 직접 Bitmap으로 변환하고 싶은 경우는 data.getBytes()의 데이터를 이용하여 직접 Bitmap으로 변경 가능하다.
                        if (bitmap == null) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inJustDecodeBounds = false;
                            bitmap = BitmapFactory.decodeByteArray(data.getBytes(), 0, data.getBytes().length, opts);
                        }

                        mPagerAdapter.setView(bitmap, status.getConvertedPage());
                        mPagerAdapter.notifyDataSetChanged();

                    } else if (status.getStatus() == DocConvertStatus.STATUS_REQ_FAILED) {
                        //요청 실패
                        showPopup(status.getErrMsg());
                    } else if (status.getStatus() == DocConvertStatus.STATUS_CONVERT) {
                        //변환 상태 요청시 응답처리
                        showPopup("문서 변환 상태:" + status.isConverted());
                    }

                    if (status.getStatus() == DocConvertStatus.STATUS_REQ_COMPLETE && status.getConvertedPage() == 1) {
                        //페이지 번호 갱신
                        updatePageNum(1);
                    }
                }
            });

            //1 페이지 요청
            DocConvertManager.getInstance().requestConvertedData(1);
            //or DocConvertManager.getInstance().beginConvertDoc();

            //변환 상태 요청시
            //DocConvertManager.getInstance().requestConvertStatus();

        } catch (DocConvertException e) {
            //문서 정보 설정 실패
            showPopup(e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DocConvertManager.getInstance().cancelAll(this);
    }


    private void updatePageNum(int currentPosition) {
        mTVPageNum.setText(currentPosition + "/" + mTotalPageNum);
    }

    private void showPopup(String msg) {
        if (mDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("알림");
            builder.setMessage(msg);
            builder.setPositiveButton("확인",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
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
