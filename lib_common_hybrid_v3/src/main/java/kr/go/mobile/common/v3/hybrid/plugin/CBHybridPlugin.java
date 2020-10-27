package kr.go.mobile.common.v3.hybrid.plugin;

import android.content.Context;

import kr.go.mobile.common.v3.hybrid.CBHybridActivity;


/**
 * Created by ChangBum Hong on 2020-07-22.
 * cloverm@infrawareglobal.com
 * 플러그인 추상화 클래스
 */
public abstract class CBHybridPlugin {

    public CBHybridPlugin() {

    }

    /**
     * 초기화 - 구라이브러리와 호환성을 위해 해당 형태로 넣음
     * @param context Activity Context
     */
    abstract public void init(Context context);

    public void sendAsyncResult(Context context, String callbackID, CBHybridPluginResult result) {
        CBHybridActivity hybridActivity = (CBHybridActivity) context;
        if (!hybridActivity.isDestroyed()) {
            hybridActivity.sendAsyncResult(callbackID, result);
        }
    }

}
