package kr.go.mobile.common.v3.document;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kr.go.mobile.common.R;
import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.CommonBasedConstants;

public class DefaultDocumentActivity extends AppCompatActivity {

    private static final String TAG = DefaultDocumentActivity.class.getSimpleName();
    // 종료키 연속입력 시간
    private static final long DURATION_TIME = 2000L;
    private int VIEWPAGER_OFF_SCREEN_PAGE_LIMIT;

    private List<Integer> loadedPosition = new ArrayList<>();
    private int orientation;

    // UI Element
    private ProgressBar progressBarLoading;
    private TextView txtCurrentPage;
    private SeekBar seekDocPaging;
    private TextView txtPageInfo;
    private Toast toastMsg;

    // finish timeout check
    private long prevPressedTime = 0L;
    boolean disableMenu = false;
    private DefaultDocPagerAdapter defaultDocPagerAdapter;
    private DefaultDocViewPager docPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_base_default_document);

        Bundle extra = getIntent().getExtras();
        String url = extra.getString(CommonBasedConstants.EXTRA_DOC_URL, "N/A");
        String fileName = extra.getString(CommonBasedConstants.EXTRA_DOC_FILE_NAME, "N/A");
        String createdDate = extra.getString(CommonBasedConstants.EXTRA_DOC_CREATED, "N/A");

        try {
            if (Objects.equals(fileName, "N/A")) {
                throw new Exception("문서 파일이름이 없습니다.");
            }
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

            final DocConvertManager docConvertManager = CommonBasedAPI.createDocConvertManager(url, fileName, createdDate);
            docConvertManager.setConvertedListener(new DocConvertManager.DocConvertListener() {

                @Override
                public void updateConvertedStatus(ConvertStatus status) {
                    int convertedPageCount = status.getConvertedPageCount();

                    // pager 의 전체 페이지 설정
                    defaultDocPagerAdapter.totalCount(convertedPageCount);

                    Log.d(TAG, "update max page : " + convertedPageCount);
                    // UI 업데이트
                    seekDocPaging.setMax(convertedPageCount - 1);
                    if (status.isConverted()) {
                        if (seekDocPaging.getVisibility() == View.INVISIBLE) {
                            seekDocPaging.setVisibility(View.VISIBLE);
                        }
                    }

                    changedPageState();
                    invalidateOptionsMenu();
                }

                @Override
                public void onConverted(ConvertedDoc convertedDoc) {
                    Log.e(TAG, "onConverted : " + convertedDoc.getPageDoc() + ", " + convertedDoc.getBitmapDoc());
                    PhotoView view = defaultDocPagerAdapter.getDocView(convertedDoc.getPageDoc() - 1);
                    if(view == null) {
                        // TODO showDialog 획득한 이미지가 없습니다. or 획득한 이미지를 표현할 객체가 없습니다.
                    } else {
                        view.setImageBitmap(convertedDoc.getBitmapDoc());
                        isLoadedPage(convertedDoc.getPageDoc());
                    }
                }

                @Override
                public void onFailed(int errorCode, String message) {
                    Log.e(TAG, message + " code : " + errorCode);
                    showFinishDialog(message);
                }
            });

            /*
             * [2017-06-13][YOONGI][ISSUE #02] 저사양한 단말에서는 동시에 많은 이미지 작업을 요청할 경우 out of memory 발생으로 정상동작을 할
             * 수 없다. 이로 인한 비정상 실행을 해결하기 위하여 하나의 작업만 요청하도록 한다.
             */
            int maxMemory = (int) (Runtime.getRuntime().maxMemory() / (1024.0f * 1024.0f));
            VIEWPAGER_OFF_SCREEN_PAGE_LIMIT = getResources().getInteger(R.integer.cb_dv_int_less_than_64_off_screen_page_limit);
            if (maxMemory > 128) {
                VIEWPAGER_OFF_SCREEN_PAGE_LIMIT = getResources().getInteger(R.integer.cb_dv_int_over_64_off_screen_page_limit);
            }

            // UI 초기화
            initialize(fileName, docConvertManager);
        } catch (Exception e) {
            Log.e(TAG, "문서뷰어를 실행할 수 없습니다. 개발사 또는 지원센터에 문의하시기 바랍니다.", e);
            showFinishDialog(R.string.cb_dv_runtime_error_msg);
        }
    }

    private void initialize(String fileName, DocConvertManager docConvertManager) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(fileName);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        defaultDocPagerAdapter = new DefaultDocPagerAdapter(docConvertManager, new DefaultDocPagerAdapter.DocPagerEventListener() {
            @Override
            public void destroyItem(int page) {
                // 로드되었던 이미지가 없어짐.
                loadedPosition.remove(Integer.valueOf(page));
            }
        });
        docPager = findViewById(R.id.cb_default_document_pager);
        docPager.setAdapter(defaultDocPagerAdapter);
        docPager.setOffscreenPageLimit(VIEWPAGER_OFF_SCREEN_PAGE_LIMIT);
        docPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            int prevState = ViewPager.SCROLL_STATE_IDLE;

            @Override
            public void onPageSelected(int position) {
                seekDocPaging.setProgress(position);

                changedPageState();

                isLoadedPage(position + 1, false);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (prevState) {
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        switch (state) {
                            case ViewPager.SCROLL_STATE_IDLE:
                                String msg = "";
                                if (seekDocPaging.getProgress() == 0) {
                                    msg = getString(R.string.cb_dv_toast_msg_first);
                                } else if (seekDocPaging.getProgress() == seekDocPaging.getMax()) {
                                    msg = getString(R.string.cb_dv_toast_msg_last);
                                } else {
                                    msg = getString(R.string.cb_dv_toast_msg_wait_retry);
                                }
                                showToastMessage(msg);
                                break;
                        }
                }
                this.prevState = state;
            }
        });
        Log.d(TAG, "DocumentViewPager created.");

        // 데이터 로딩바
        progressBarLoading = findViewById(R.id.cb_default_document_loading);
        Log.d(TAG, "ProgressBar created.");

        // 페이지 이동바
        seekDocPaging = findViewById(R.id.cb_paging_seekbar);
        // 페이지 이동바는 전체 페이지 정보를 획득하기전까지 보여주지 않음.
        seekDocPaging.setVisibility(View.INVISIBLE);
        seekDocPaging.setMax(Integer.MAX_VALUE);
        seekDocPaging.setProgress(0);
        seekDocPaging.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    txtCurrentPage.setText(String.valueOf(seekBar.getProgress() + 1));
                    txtCurrentPage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 80);
                    txtCurrentPage.setVisibility(View.VISIBLE);
                    txtCurrentPage.bringToFront();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                txtCurrentPage.setText("");
                txtCurrentPage.setVisibility(View.INVISIBLE);
                int position = seekBar.getProgress();
                docPager.setCurrentItem(position);
            }
        });
        Log.d(TAG, "PageCtr SeekBar created.");

        // 페이지 정보 표시화면
        txtPageInfo = findViewById(R.id.cb_page_txt);
        Log.d(TAG, "PageCtr TextView created.");

        txtCurrentPage = findViewById(R.id.cb_default_document_move_page);
        txtCurrentPage.setTextColor(getResources().getColor(R.color.cb_dv_color_current_page));

        enableLoadingBar(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.cb_dv_menu, menu);

        // 현재 페이지
        final int currentPage = seekDocPaging.getProgress() + 1;
        // 전체 페이지
        final int pageCount = seekDocPaging.getMax() + 1;

        MenuItem rotationMenu = menu.getItem(0);
        MenuItem prevMenu = menu.getItem(1);
        MenuItem nextMenu = menu.getItem(2);

        if(disableMenu) {
            rotationMenu.setEnabled(false);
        } else {
            rotationMenu.setEnabled(true);
        }

        if (currentPage == 1 /* first page */ || disableMenu) {
            prevMenu.setEnabled(false);
        } else {
            prevMenu.setEnabled(true);
        }

        if (currentPage >= pageCount /* last page */ || disableMenu) {
            nextMenu.setEnabled(false);
        } else {
            nextMenu.setEnabled(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        int currentPosition = docPager.getCurrentItem();
        int changedPosition = -1;
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.cb_dv_action_rotation) {
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            } else {
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
            setRequestedOrientation(orientation);
            return true;
        } else if (currentPosition != 0 && itemId == R.id.cb_dv_action_prev) {
            changedPosition = currentPosition - 1;
        } else if (currentPosition != seekDocPaging.getMax() && itemId == R.id.cb_dv_action_next) {
            changedPosition = currentPosition + 1;
        }
        if (changedPosition >= 0) {
            docPager.setCurrentItem(changedPosition, true);
            seekDocPaging.setProgress(changedPosition);

            changedPageState();
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void changedPageState() {
        String formatPageInfo = (seekDocPaging.getVisibility() == View.VISIBLE) ? "%s / %s" : "%s / %s (" + getString(R.string.cb_dv_msg_converting) + ")";
        txtPageInfo.setText(String.format(formatPageInfo, seekDocPaging.getProgress() + 1, seekDocPaging.getMax() + 1));
    }

    void isLoadedPage(int page) {
        isLoadedPage(page, true);
    }

    void isLoadedPage(int page, boolean converted) {
        if (converted) {
            // 로드 완료된 뷰를 기록.
            loadedPosition.add(page);

            if (seekDocPaging.getProgress() + 1 == page) {
                enableLoadingBar(false);
            }
        } else {
            // TODO 이미 로그된 페이지라면 로딩바를 보여줄 필요 없음.
            try {
                if (loadedPosition.contains(page)) {
                    return;
                }
            } catch (ArrayIndexOutOfBoundsException ignored) {
            }
            enableLoadingBar(true);
        }
    }

    void enableLoadingBar(boolean enable) {
        if (enable) {
            seekDocPaging.setEnabled(false);
            docPager.setSwipeable(false);
            disableMenu = true;
            invalidateOptionsMenu();
            progressBarLoading.setVisibility(View.VISIBLE);
            progressBarLoading.bringToFront();
        } else {
            if (toastMsg != null) {
                toastMsg.cancel();
                toastMsg = null;
            }
            seekDocPaging.setEnabled(true);
            docPager.setSwipeable(true);
            disableMenu = false;
            invalidateOptionsMenu();
            progressBarLoading.setVisibility(View.INVISIBLE);
        }
    }

    private void showFinishDialog(int msgId) {
        String message = getString(msgId);
        showFinishDialog(message);
    }

    private void showFinishDialog(String msg) {
        if (this.isDestroyed()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("문서변환에러");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setNeutralButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }

    void showToastMessage(String message) {
        if (toastMsg != null) {
            toastMsg.cancel();
            toastMsg = null;
        }
        toastMsg = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toastMsg.show();
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - prevPressedTime <= DURATION_TIME) {
            if (toastMsg != null) {
                toastMsg.cancel();
                toastMsg = null;
            }
            super.onBackPressed();
        } else {
            prevPressedTime = System.currentTimeMillis();
            showToastMessage(getString(R.string.cb_dv_msg_finish_toast));
        }
    }
}
