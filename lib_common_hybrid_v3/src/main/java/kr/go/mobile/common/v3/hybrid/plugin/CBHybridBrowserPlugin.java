package kr.go.mobile.common.v3.hybrid.plugin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.common.v3.hybrid.CBHybridDialog;


/**
 * Created by ChangBum Hong on 2020-07-27.
 * cloverm@infrawareglobal.com
 * Key 처리 및 UI 처리 관련 플러그인
 */
public class CBHybridBrowserPlugin extends CBHybridPlugin {

    private static final String TAG = CBHybridBrowserPlugin.class.getSimpleName();

    private CBHybridDialog mAlertDialog;
    private Timer mDialogTimer;
    private List<String> mCallbackList;


    public CBHybridBrowserPlugin(Context context) {
        super(context);
        mCallbackList = new ArrayList<String>();
        mAlertDialog = new CBHybridDialog(getContext());
        mAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dismissAlertDialog();
            }
        });
    }

    @Override
    public String getVersionName() {
        return "1.0.0";
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {
        stopLoadingBar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

    }

    /**
     * 로딩바 시작
     */
    public void startLoadingBar() {
        startLoadingBar(null);
    }

    /**
     * 로딩바 시작
     *
     * @param jsonArgs 속성값
     */
    public void startLoadingBar(String jsonArgs) {
        Log.d(TAG, "startLoadingBar " + jsonArgs);
        String title = null, content = null, timeoutUrl = null;
        boolean isCancel = true;
        int timeout = 0;
        JSONObject jsonObject = null;

        if (jsonArgs != null) {
            try {
                jsonObject = new JSONObject(jsonArgs);
            } catch (JSONException e) {
                Log.e(TAG, "입력 데이터가 잘못되었습니다.");
                return;
            }

            try {
                if (jsonObject.has("title")) {
                    title = jsonObject.getString("title");
                } else {
                    title = null;
                }

                if (jsonObject.has("content")) {
                    content = jsonObject.getString("content");
                } else {
                    content = null;
                }

                if (jsonObject.has("cancelable")) {
                    isCancel = jsonObject.getBoolean("cancelable");
                } else {
                    isCancel = true;
                }

                if (jsonObject.has("timeout")) {
                    timeout = jsonObject.getInt("timeout");
                } else {
                    timeout = 0;
                }

                if (jsonObject.has("timeoutUrl")) {
                    timeoutUrl = jsonObject.getString("timeoutUrl");

                    if (!timeoutUrl.startsWith("http")) {
                        timeoutUrl = "file:///android_asset/www/" + timeoutUrl;
                    }
                } else {
                    timeoutUrl = null;
                }
            } catch (JSONException e) {
                Log.e(TAG, "입력 데이터가 잘못되었습니다.");
                return;
            }
        }

        showAlertDialog(title, content, isCancel, timeout, timeoutUrl);
    }


    /**
     * 로딩바 종료
     */
    public void stopLoadingBar() {
        Log.d(TAG, "stopLoadingBar");
        dismissAlertDialog();
    }

    /**
     * 앱 종료(현재는 activity 종료)
     */
    public void terminateApp() {
        Log.d(TAG, "terminateApp");
        //액티비티만 있는 경우에는 완전히 종료됨
        ActivityCompat.finishAffinity(getActivity());
        //TODO 가상화 에이전트와 바인드된 부분 중단 필요

        //실행중인 서비스가 있는 경우 아래 코드를 불려야 서비스가 종료되나 프로세스는 살아남(Application onCreate됨)
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * BackKey 처리용으로만 사용
     * Method 이름 중요 Method명에 Async 텍스트 필수
     * BackKey Event 받을때 마다 Async로 javascript쪽에 보냄
     */
    public void addAsyncBackKeyListener(final String callbackID) {
        Log.d(TAG, "addAsyncBackKeyListener callbackID=" + callbackID);
        mCallbackList.add(callbackID);

        View.OnKeyListener keyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult("");
                    cbHybridPluginResult.setKeepCallback(true);
                    sendAsyncResult(callbackID, cbHybridPluginResult);
                    return true;
                }
                return false;
            }
        };

        setWebViewOnKeyListener(keyListener);
    }

    /**
     * BackKey Event Listener 제거
     */
    public void removeBackKeyListener() {
        Log.d(TAG, "removeBackKeyListener");

        setWebViewOnKeyListener(null);
        CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult("");
        cbHybridPluginResult.setKeepCallback(false);
        cbHybridPluginResult.setStatus(CommonBasedConstants.HYBRID_ERROR_DELETE_CALLBACK);

        for (String callbackID : mCallbackList) {
            sendAsyncResult(callbackID, cbHybridPluginResult);
        }

        mCallbackList.clear();
    }

    /**
     * 로딩바 실행
     *
     * @param title      제목
     * @param content    내용
     * @param isCancel   사용자 취소 가능 여부
     * @param timeout    자동닫기 시간(초)
     * @param timeoutURL 자동닫기 후 이동 URL
     */
    public void showAlertDialog(final String title, final String content, final boolean isCancel, final int timeout, final String timeoutURL) {
        dismissAlertDialog();

        if (!getActivity().isDestroyed()) {
            if (timeout > 0) {
                mDialogTimer = new Timer();
                mDialogTimer.schedule(new DialogTimerTask(timeoutURL), TimeUnit.SECONDS.toMillis(timeout));
            }

            mAlertDialog.setTitle(title);
            mAlertDialog.setMessage(content);
            mAlertDialog.setCancelable(isCancel);
            mAlertDialog.setCanceledOnTouchOutside(false);
            mAlertDialog.show();
        }
    }


    /**
     * 로딩바 닫기
     */
    private void dismissAlertDialog() {
        if (!getActivity().isDestroyed()) {
            if (mAlertDialog.isShowing()) {
                mAlertDialog.dismiss();
            }
        }

        if (mDialogTimer != null) {
            mDialogTimer.cancel();
            mDialogTimer = null;
        }
    }

    /**
     * 로딩바 자동닫기 TimerTask
     */
    class DialogTimerTask extends TimerTask {

        private String mURL; //창 닫은 후 이동할 URL

        public DialogTimerTask(String url) {
            mURL = url;
        }

        @Override
        public void run() {
            dismissAlertDialog();
            if (mURL != null) {
                if (!getActivity().isDestroyed()) {
                    loadUrl(mURL);
                }
            }
        }
    }
}
