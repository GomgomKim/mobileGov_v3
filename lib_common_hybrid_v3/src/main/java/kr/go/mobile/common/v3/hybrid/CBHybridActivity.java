package kr.go.mobile.common.v3.hybrid;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPlugin;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPluginResult;


/**
 * Created by ChangBum Hong on 2020-07-22.
 * cloverm@infrawareglobal.com
 * 행정앱이 상속받아 사용하는 Activity
 */

public class CBHybridActivity extends Activity {
    private CBHybridWebView cbWebView;
    private CBHybridAgent cbHybridAgent;
    private Map<Integer, IRequestPermissionListener> mReqListenerMap;

    /**
     * 퍼미션 허용 여부 확인용 Listener
     */
    public interface IRequestPermissionListener {
        void onResult(int requestCode, boolean isGranted);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //WebView 초기화
        cbWebView = new CBHybridWebView(this);
        cbWebView.setOnKeyListener(null);

        cbHybridAgent = new CBHybridAgent(this, cbWebView);
        cbHybridAgent.init();

        //Listener MAP 초기화
        mReqListenerMap = new HashMap<>();

        setContentView(cbWebView);
    }

    @Override
    protected void onPause() {
        cbHybridAgent.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        cbHybridAgent.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mReqListenerMap.clear();
        mReqListenerMap = null;

        cbHybridAgent.onDestroy();
        cbHybridAgent = null;

        cbWebView.destroy();
        cbWebView = null;
        super.onDestroy();
    }

    /**
     * 퍼미션 허용 여부 확인용 Listener 등록
     *
     * @param reqCode  퍼미션 요청 코드값
     * @param listener 등록 Listener
     */
    public void addRequestPermissionListener(int reqCode, IRequestPermissionListener listener) {
        mReqListenerMap.put(reqCode, listener);
    }

    /**
     * 플러그인 등록
     *
     * @param pluginName 플러그인 이름
     * @param cls 플러그인 클래스 객체
     */
    public void addPlugin(String pluginName, Class<? extends CBHybridPlugin> cls) {
        cbHybridAgent.addPlugin(pluginName, cls);
    }


    /**
     * WebView 페이지 로드
     *
     * @param url 로드할 페이지 URL or Javascript
     */
    public void loadUrl(final String url) {
        cbWebView.post(new Runnable() {
            @Override
            public void run() {
                cbWebView.loadUrl(url);
            }
        });
    }

    /**
     * Back키 누를 때 처리
     * 페이지 이동이 가능하면 이전 페이지로 이동
     * 해당 처리는 상속받은 사용자가 구현 처리에 따라 하는게 나을 수도 있음
     */
    @Override
    public void onBackPressed() {
        if (cbWebView.canGoBack()) {
            cbWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * WebView로 비동기 콜백관련 추가
     *
     * @param callbackID 콜백ID
     * @param result     리턴 결과
     */
    public void sendAsyncResult(String callbackID, CBHybridPluginResult result) {
        cbHybridAgent.sendCallback(callbackID, result);
    }

    /**
     * Back 이벤트 처리 관련 WebView에 리스너 등록
     *
     * @param listener KeyListener
     */
    public void setWebViewOnKeyListener(View.OnKeyListener listener) {
        cbWebView.setOnKeyListener(listener);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }

    public void startActivityForResult(CBHybridPlugin plugin, Intent intent, int requestCode) {
        // TODO 호출한 플러그인 기록.
        super.startActivityForResult(intent, requestCode);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // TODO 호출한 플러그 인으로 리턴
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        IRequestPermissionListener listener = mReqListenerMap.get(requestCode);

        if (listener != null) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                listener.onResult(requestCode, true);
            } else {
                listener.onResult(requestCode, false);
            }
        }
    }



}
