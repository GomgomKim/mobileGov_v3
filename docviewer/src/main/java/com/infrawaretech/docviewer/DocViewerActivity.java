package com.infrawaretech.docviewer;

import com.github.chrisbanes.photoview.PhotoView;
import com.infrawaretech.docviewer.ui.R;
import com.infrawaretech.docviewer.ui.LocalConstants;
import com.infrawaretech.docviewer.ui.DocPager;
import com.infrawaretech.docviewer.ui.DocPagerAdapter;


import com.infrawaretech.docviewer.utils.Log;
import com.infrawaretech.docviewer.utils.PageUtils;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

@SuppressLint("NewApi")
public class DocViewerActivity extends FragmentActivity {

  private static final String TAG = DocViewerActivity.class.getSimpleName();
  // 종료키 연속입력 시간
  private static final long DURATION_TIME = 2000L;

  /**
   * 2017-12-12 지원센터의 요청으로 추가. 기존의 IP:PORT 값을 개선후에서는 ServiceID 값으로 전달하도록 함. 문서변환을 요청할 문서의 SERVICE ID
   */
  public static final String EXTRA_SERVICE_ID = "serviceId";
  /**
   * 문서변환을 요청할 문서의 URL
   */
  public static final String EXTRA_URL = "url";
  /**
   * 문서변환을 요청할 문서의 파일명
   */
  public static final String EXTRA_FILE_NAME = "fileName";
  /**
   * 문서변환을 요청할 문서의 생성날짜
   */
  public static final String EXTRA_CREATED = "createDate";

  // 웹뷰화면에서 종료될 때, 해당 엑티비티를 종료하기 위한 객체.
  public static DocViewerActivity THIS;

  private int VIEWPAGER_OFF_SCREEN_PAGE_LIMIT = 1;

  private SparseIntArray loadedPosition = new SparseIntArray();
  private int currentPosition = 0;

  private int mDocViewOrientation;

  // UI Element
  private ProgressBar mProgBarLoading;
  private TextView mTxtCurrentPage;
  private SeekBar mSeekDocPaging;
  private TextView mTxtPageInfo;
  private Toast mToastMsg;

  // Document View Pager
  public DocPager mDocPager;
  private DocPagerAdapter mDocPagerAdapter;

  // finish timeout check
  private long prevPressedTime = 0L;
  boolean disableMenu = false;

  private BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action == null) {
        Log.w(TAG, "Broadcast Action is null.");
      } else if (action.equals(LocalConstants.Broadcast.ACTION_DOC_VIEW_EVENT)) {
        int event = intent.getIntExtra(LocalConstants.Broadcast.EXTRA_EVENT, 1);
        int page = intent.getIntExtra(LocalConstants.Broadcast.EXTRA_PAGE, -1);
        int position = PageUtils.pageToPosition(page);
        if (page < 0)
          return;
        switch (event) {
        case LocalConstants.DocViewEvent.BEGIN:
        case LocalConstants.DocViewEvent.SHOW:
        case LocalConstants.DocViewEvent.DECODE_FAILED:
          loadedPosition.put(Integer.valueOf(position), Integer.valueOf(event));
          break;
        case LocalConstants.DocViewEvent.REMOVE:
          loadedPosition.delete(Integer.valueOf(position));
          break;
        }
        isLoadedPage();
      } else if (action.equals(LocalConstants.Broadcast.ACTION_DOC_CONVERT_STATUS)) {
        int status = intent.getIntExtra(LocalConstants.Broadcast.EXTRA_STATUS, LocalConstants.DocViewStatus.VPN_DISCONNECTION);
        switch (status) {
        case LocalConstants.DocViewStatus.VPN_DISCONNECTION:
          showFinishDialog(R.string.it_dv_vpn_status_error_msg);
          break;
        case LocalConstants.DocViewStatus.REQUEST_FAILED:
          String message = intent.getStringExtra(LocalConstants.Broadcast.EXTRA_ERROR_MESSAGE);
          showFinishDialog(message);
        default:
          break;
        }
      } else if (action.equals(LocalConstants.Broadcast.ACTION_DOC_CONVERT_STATE)) {
        Bundle bundle = intent.getExtras();
        boolean isConverted = bundle.getBoolean(LocalConstants.Broadcast.EXTRA_STATE);
        String hashCode = bundle.getString(LocalConstants.Broadcast.EXTRA_HASHCODE);
        int totalPage = bundle.getInt(LocalConstants.Broadcast.EXTRA_TOTAL_PAGE);

        // 변환된 전체 페이지를 설정하고 PagerAdapter 한데 알림
        notifyProgress(totalPage);
        
        // UI 갱신
        mSeekDocPaging.setMax(PageUtils.pageToPosition(totalPage));

        mTxtCurrentPage.setText("");
        mTxtCurrentPage.setVisibility(View.INVISIBLE);

        if (isConverted) {
          if (mSeekDocPaging.getVisibility() == View.INVISIBLE) {
            mSeekDocPaging.setVisibility(View.VISIBLE);
          }
        } else {
          //DocDownloadManager.reqDocConvertState(mDocPagerAdapter.getOption());
        }

        // 변경된 페이지 위치를 알려줌
        changedPageState();
        // 상단 메뉴 버튼 업데이트.
        invalidateOptionsMenu();
      }
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.it_dv_activity);
    THIS = this;

    // 문서뷰어 라이브러리 초기화
    DocConvertManager.getInstance().init(this);

    try {
      mDocViewOrientation =  ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
      Bundle extra = getIntent().getExtras();
      Log.ENABLE = extra.getBoolean("LOG", false);
      Log.d(TAG, "Document Viewer Log :: " + Log.ENABLE);
      /*
       * [2017-06-13][YOONGI][ISSUE #02] 저사양한 단말에서는 동시에 많은 이미지 작업을 요청할 경우 out of memory 발생으로 정상동작을 할
       * 수 없다. 이로 인한 비정상 실행을 해결하기 위하여 하나의 작업만 요청하도록 한다.
       */
      int maxMemory = (int) (Runtime.getRuntime().maxMemory() / (1024.0f * 1024.0f));
      int offScreenPageLimitId = R.integer.it_dv_int_less_than_64_off_screen_page_limit;
      if (maxMemory > 128) {
        offScreenPageLimitId = R.integer.it_dv_int_over_64_off_screen_page_limit;
      }
      VIEWPAGER_OFF_SCREEN_PAGE_LIMIT = getResources().getInteger(offScreenPageLimitId);

      // 문서뷰어 라이브러리 설정
      String targetUrl = extra.getString(DocViewerActivity.EXTRA_URL, "");
      String fileName = extra.getString(DocViewerActivity.EXTRA_FILE_NAME, "");
      DocConvertManager.getInstance().setTargetDoc(fileName, targetUrl, new DocConvertManager.ReqCallback() {

        int MAX_SIZE = 5;
        int CURRNET_POSITION = 0;

        @Override
        public void onReqCallback(DocConvertStatus docConvertStatus, DocConvertedData docConvertedData) {

          Log.d("DocConvertManager.ReqCallback", "getStatus : " + docConvertStatus.getStatus());
          Log.d("DocConvertManager.ReqCallback", "getConvertedPage : " + docConvertStatus.getConvertedPage());
          Log.d("DocConvertManager.ReqCallback", "getTotalPage : " + docConvertStatus.getTotalPage());

          switch (docConvertStatus.getStatus()) {
            case DocConvertStatus.STATUS_REQ_COMPLETE:
                int totalPage = docConvertStatus.getTotalPage();
                int currentPage = docConvertStatus.getConvertedPage();

                notifyProgress(totalPage);

                // UI 갱신
                mSeekDocPaging.setMax(PageUtils.pageToPosition(totalPage));

                mTxtCurrentPage.setText("");
                mTxtCurrentPage.setVisibility(View.INVISIBLE);

                if (docConvertStatus.isConverted()) {
                    if (mSeekDocPaging.getVisibility() == View.INVISIBLE) {
                        mSeekDocPaging.setVisibility(View.VISIBLE);
                    }
                } else {
                  try {
                    DocConvertManager.getInstance().requestConvertStatus();
                  } catch (DocConvertException e) {
                    //TODO
                    e.printStackTrace();
                  }
                }

                PhotoView p = mDocPagerAdapter.getDocView(PageUtils.pageToPosition(currentPage));
                if (p == null) {
                  //TODO
                  Log.d("@@@@@@@@", "photoView is null");
                } else {
                  p.setImageBitmap(docConvertedData.getBitmap());
                }

                // 변경된 페이지 위치를 알려줌
                changedPageState();
                // 상단 메뉴 버튼 업데이트.
                invalidateOptionsMenu();
              break;
            case DocConvertStatus.STATUS_CONVERT:
              break;
            case DocConvertStatus.STATUS_DECODE_FAILED:
            case DocConvertStatus.STATUS_REQ_FAILED:
            default:
              showFinishDialog("문서변환 요청 중 에러가 발생하였습니다. (" + docConvertStatus.getErrMsg() + ")");
              break;
          }
        }
      });

      // UI 초기화
      initialize(fileName);
      // 리스너 설정
      setListener();
    } catch (DocConvertException e) {
      showFinishDialog(e.getMessage() + "( code : " + e.getErrorCode() + " )");
    } catch (Exception e) {
      Log.e(TAG, "문서뷰어를 실행할 수 없습니다. 개발사 또는 지원센터에 문의하시기 바랍니다.", e);
      showFinishDialog(R.string.it_dv_runtime_error_msg);
    }
  }


  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  private void initialize(String fileName) {
    mDocPagerAdapter = new DocPagerAdapter();
    mDocPager = findViewById(R.id.it_extpager_doc_page);
    mDocPager.setAdapter(mDocPagerAdapter);
    mDocPager.setOffscreenPageLimit(VIEWPAGER_OFF_SCREEN_PAGE_LIMIT);

    Log.d(TAG, "DocumentViewPager created.");

    // 데이터 로딩바
    mProgBarLoading = findViewById(R.id.it_pb_doc_loading);
    Log.d(TAG, "ProgressBar created.");

    // 액션바 설정 (아이콘 숨김, 문서이름 설정, 상위 액티비티로 전환)
    ActionBar actionBar = getActionBar();
    actionBar.setIcon(new ColorDrawable(Color.TRANSPARENT)); // 아이콘 숨김.
    actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
    actionBar.setSubtitle(fileName);
    int colorValue = getResources().getColor(R.color.it_dv_color_bottombar_bg);
    ColorDrawable colorDrawable = new ColorDrawable(colorValue);
    actionBar.setBackgroundDrawable(colorDrawable);
    Log.d(TAG, "PageCtr MenuItem created.");

    // 페이지 이동바
    mSeekDocPaging = findViewById(R.id.it_seekbar_paging);
    // 페이지 이동바는 전체 페이지 정보를 획득하기전까지 보여주지 않음.
    mSeekDocPaging.setVisibility(View.INVISIBLE);
    mSeekDocPaging.setMax(Integer.MAX_VALUE);
    mSeekDocPaging.setProgress(0);

    Log.d(TAG, "PageCtr SeekBar created.");

    // 페이지 정보 표시화면
    mTxtPageInfo = findViewById(R.id.it_txt_page);
    Log.d(TAG, "PageCtr TextView created.");

    mTxtCurrentPage = findViewById(R.id.it_req_move_page);
    mTxtCurrentPage.setTextColor(getResources().getColor(R.color.it_dv_color_current_page));
  }

  private void isLoadedPage() {

    if (loadedPosition.indexOfKey(currentPosition) > 0) {
      int event = loadedPosition.get(currentPosition);
      if (event == LocalConstants.DocViewEvent.DECODE_FAILED) {
        mDocPagerAdapter.instantiateItem(mDocPager, currentPosition);
      } else if (event == LocalConstants.DocViewEvent.SHOW) {
        enableLoadingBar(false);
      } else {
        enableLoadingBar(true);
      }
    }
  };

  private void setListener() {
    mDocPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      int prevState = ViewPager.SCROLL_STATE_IDLE;

      @Override
      public void onPageSelected(int position) {
        mSeekDocPaging.setProgress(position);
        currentPosition = position;

        changedPageState();
        isLoadedPage();
      }

      @Override
      public void onPageScrollStateChanged(int state) {
        switch (prevState) {
        case ViewPager.SCROLL_STATE_DRAGGING:
          switch (state) {
          case ViewPager.SCROLL_STATE_IDLE:
            String msg = "";
            if (mSeekDocPaging.getProgress() == 0) {
              msg = getString(R.string.it_dv_msg_first_toast);
            } else if (mSeekDocPaging.getProgress() == mSeekDocPaging.getMax()) {
              msg = getString(R.string.it_dv_msg_last_toast);
            } else {
              msg = getString(R.string.it_dv_msg_wait_retry_toast);
            }
            showToastMessage(msg);
            break;
          }
        }
        this.prevState = state;
      }
    });
    mSeekDocPaging.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        mTxtCurrentPage.setText("");
        mTxtCurrentPage.setVisibility(View.INVISIBLE);

        int position = seekBar.getProgress();
        mDocPager.setCurrentItem(position);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          mTxtCurrentPage.setText(String.valueOf(PageUtils.positionToPage(mSeekDocPaging.getProgress())));
          mTxtCurrentPage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 80);
          mTxtCurrentPage.setVisibility(View.VISIBLE);
          mTxtCurrentPage.bringToFront();
        }
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.it_dv_activity_menu, menu);

    // 현재 페이지
    final int currentPage = PageUtils.positionToPage(mSeekDocPaging.getProgress());
    // 전체 페이지 
    final int pageCount = PageUtils.positionToPage(mSeekDocPaging.getMax());

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
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      onBackPressed();
      return true;
    }

    if (itemId == R.id.it_dv_action_rotation) {
      if (mDocViewOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
        mDocViewOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
      } else {
        mDocViewOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
      }
      setRequestedOrientation(mDocViewOrientation);
      return true;
    }

    final int position = mDocPager.getCurrentItem();
    final int prevId = R.id.it_dv_action_prev;
    final int nextId = R.id.it_dv_action_next;

    int changedPosition;

    if (position != 0 && itemId == prevId) {
      changedPosition = position - 1;
    } else if (position != mSeekDocPaging.getMax() && itemId == nextId) {
      changedPosition = position + 1;
    } else {
      return false;
    }

    mDocPager.setCurrentItem(changedPosition, true);
    mSeekDocPaging.setProgress(changedPosition);

    changedPageState();

    invalidateOptionsMenu();
    return true;
  }

  @Override
  public void onBackPressed() {
    if (mToastMsg != null)
      mToastMsg.cancel();

    if (System.currentTimeMillis() - prevPressedTime <= DURATION_TIME) {
      super.onBackPressed();
    } else {
      prevPressedTime = System.currentTimeMillis();
      mToastMsg = Toast.makeText(this, R.string.it_dv_msg_finish_toast, Toast.LENGTH_SHORT);
      mToastMsg.show();
    }
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "onDestroy()");
    super.onDestroy();
    DocConvertManager.getInstance().cancelAll(this);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver);
  }

  void changedPageState() {
    String formatPageInfo = (mSeekDocPaging.getVisibility() == View.VISIBLE) ? "%s / %s" : "%s / %s (" + getString(R.string.it_dv_msg_converting) + ")";

    mTxtPageInfo.setText(
        String.format(formatPageInfo, PageUtils.positionToPage(mSeekDocPaging.getProgress()), PageUtils.positionToPage(mSeekDocPaging.getMax())));
  }

  void enableLoadingBar(boolean enable) {
    if (enable) {
      mSeekDocPaging.setEnabled(false);
      mDocPager.setSwipeable(false);
      disableMenu = true;
      invalidateOptionsMenu();
      mProgBarLoading.setVisibility(View.VISIBLE);
      mProgBarLoading.bringToFront();
    } else {
      if (mToastMsg != null) {
        mToastMsg.cancel();
        mToastMsg = null;
      }
      mSeekDocPaging.setEnabled(true);
      mDocPager.setSwipeable(true);
      disableMenu = false;
      invalidateOptionsMenu();
      mProgBarLoading.setVisibility(View.INVISIBLE);
    }
  }

  void showToastMessage(String message) {
    if (mToastMsg != null) {
      mToastMsg.cancel();
      mToastMsg = null;
    }
    mToastMsg = Toast.makeText(this, message, Toast.LENGTH_LONG);
    mToastMsg.show();
  }

  void showFinishDialog(int resID) {
    showFinishDialog(getString(resID));
  }

  void showFinishDialog(String msg) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("문서변환에러");
    builder.setMessage(msg);
    builder.setCancelable(false);
    builder.setNeutralButton(getString(android.R.string.ok), new OnClickListener() {

      @Override
      public void onClick(DialogInterface dialog, int which) {
        finish();
      }
    });
    builder.show();
  }

  public void notifyProgress(int totalPage) {
    mDocPagerAdapter.totalCount(totalPage);
  }
}
