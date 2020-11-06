package kr.go.mobile.common.v3.hybrid;

import android.content.Context;

import kr.go.mobile.common.v3.hybrid.plugin.CBHybridBrokerPlugin;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridBrowserPlugin;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridLocationPlugin;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPlugin;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPluginResult;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridTelephonyPlugin;

// 하이브리드 엑티비티와 하이브리드 클러그인 매니저를 연결하는 에이전트.
public class CBHybridAgent {

    CBHybridWebView cbHybridWebView;
    CBHybridPluginManager cbHybridPluginManager;

    public CBHybridAgent(Context context, CBHybridWebView webView) {
        this.cbHybridPluginManager = new CBHybridPluginManager(context);
        this.cbHybridWebView = webView;
        this.cbHybridWebView.addJavascriptInterface(cbHybridPluginManager, "CommonBasedAPI");
    }

    public void init() {
        // 기본 플러그인 등록.
        addPlugin("Browser", CBHybridBrowserPlugin.class);
        addPlugin("Location", CBHybridLocationPlugin.class);
        addPlugin("Telephony", CBHybridTelephonyPlugin.class);
        addPlugin("Broker", CBHybridBrokerPlugin.class);
    }

    public void addPlugin(String pluginName, Class<? extends CBHybridPlugin> pluginClass) {
        this.cbHybridPluginManager.addPlugin(pluginName, pluginClass);
    }

    public void sendCallback(String callbackID, CBHybridPluginResult result) {
        cbHybridPluginManager.sendCallback(callbackID, result);
    }


    public void onPause() {
        cbHybridPluginManager.pause();
    }

    public void onResume() {
        cbHybridPluginManager.resume();
    }

    public void onDestroy() {
        cbHybridWebView.addJavascriptInterface(cbHybridPluginManager, "");
        cbHybridPluginManager.destroy();
    }

}
