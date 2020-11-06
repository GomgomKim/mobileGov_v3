package kr.go.mobile.common.v3.hybrid;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONTokener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPlugin;
import kr.go.mobile.common.v3.hybrid.plugin.CBHybridPluginResult;

/**
 * Created by ChangBum Hong on 2020-07-22.
 * cloverm@infrawareglobal.com
 * 플러그인 관리매니저
 */
class CBHybridPluginManager {

    private static final String TAG = CBHybridPluginManager.class.getSimpleName();
    private static final String PRE_DEFINED_ASYNC_METHOD = "async";
    private Context mCtxForHybridActivity;
    private HashMap<String, String> mPluginNameMap; //Native 객체 클래스명 정보
    private HashMap<String, CBHybridPlugin> mPluginObjMap; //Native 객체

    public CBHybridPluginManager(Context context) {
        mPluginNameMap = new HashMap<>();
        mPluginObjMap = new HashMap<>();
        mCtxForHybridActivity = context;
    }

    /**
     * 플러그인 등록
     *
     * @param cls 플러그인 클래스명
     */
    public void addPlugin(String pluginName, Class<? extends CBHybridPlugin> cls) {

        boolean isHybridPluginClass = false;

        //상속 받은 클래스 확인
        if (Objects.equals(cls.getSuperclass().getName(), CBHybridPlugin.class.getName())) {
            isHybridPluginClass = true;
        }

        //구 라이브러리 지원을 위해 추가
        if (Objects.equals(cls.getSuperclass().getSuperclass().getName(), CBHybridPlugin.class.getName())) {
            isHybridPluginClass = true;
        }

        if (isHybridPluginClass) {
            if (!mPluginNameMap.containsKey(cls.getSimpleName())) {
                mPluginNameMap.put(pluginName, cls.getName());
            }
        }
    }

    /**
     * Plugin 객체 전달
     * @param pluginName Plugin class Name
     * @return
     */
    public CBHybridPlugin getPlugin(String pluginName) {
        return mPluginObjMap.get(pluginName);
    }


    /**
     * WebView에서 호출하는 부분
     *
     * @param callbackID WebView 쪽 콜백을 위한 ID값
     * @param callMethod 클래스명.메소드명(ex UserPlugin.setName)
     */
    @JavascriptInterface
    public void exec(String callbackID, String callMethod) {
        exec(callbackID, callMethod, null);
    }

    /**
     * WebView에서 호출하는 부분
     *
     * @param callbackID WebView 쪽 콜백을 위한 ID값
     * @param callMethod 클래스명.메소드명(ex UserPlugin.setName)
     * @param jsonParam  메소드 파라미터(Json 형식)
     */
    @JavascriptInterface
    public void exec(String callbackID, String callMethod, String jsonParam) {
        CBHybridPluginResult result;
        if (jsonParam != null) {
            try {
                jsonParam = new JSONTokener(jsonParam).nextValue().toString();
            } catch (JSONException e) {
                e.printStackTrace();
                result = new CBHybridPluginResult("입력된 값이 잘못되었습니다. (msg = " + e.getMessage() + ")");
                result.setStatus(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR);
                sendCallback(callbackID, result);
                return;
            }
        }

        Log.d(TAG, "nativeCall callbackID=" + callbackID + " method=" + callMethod + ", param=" + jsonParam);

        if (TextUtils.isEmpty(callbackID) || TextUtils.isEmpty(callMethod)) {
            result = new CBHybridPluginResult("호출할 native-call ID 가 정의되어 있지않습니다.");
            result.setStatus(CommonBasedConstants.HYBRID_ERROR_NATIVE_CALL_PARAMETER);
            sendCallback(callbackID, result);
            return;
        }

        //호출 Method 형식 검사 (ex User.callapi)
        String[] callMethods = callMethod.split("\\.");
        String callClassSimpleName;
        String callMethodName;

        if (callMethods.length != 2) {
            result = new CBHybridPluginResult("native-call 형식이 맞지 않습니다.");
            result.setStatus(CommonBasedConstants.HYBRID_ERROR_NATIVE_CALL_PARAMETER);
            sendCallback(callbackID, result);
            return;
        } else {
            callClassSimpleName = callMethods[0];
            callMethodName = callMethods[1];
        }

        //등록된 플러그인인지 확인
        if (!mPluginNameMap.containsKey(callClassSimpleName)) {
            result = new CBHybridPluginResult("등록된 플러그인이 없습니다. (plugin name = " + callClassSimpleName + ")");
            result.setStatus(CommonBasedConstants.HYBRID_ERROR_PLUGIN_NOT_FOUND);
            sendCallback(callbackID, result);
            return;
        }

        if (!mPluginObjMap.containsKey(callClassSimpleName)) {
            CBHybridPlugin newPluginObj = null;
            try {
                Class<? extends CBHybridPlugin> userClass = (Class<? extends CBHybridPlugin>) Class.forName(mPluginNameMap.get(callClassSimpleName));
                Class<?>[] constructorParamsType = {Context.class};
                newPluginObj = userClass.getConstructor(constructorParamsType).newInstance(mCtxForHybridActivity);
                mPluginObjMap.put(callClassSimpleName, newPluginObj);
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                result = new CBHybridPluginResult("plugin 생성을 실패하였습니다. (cause = " + e.getMessage() +")");
                result.setStatus(CommonBasedConstants.HYBRID_ERROR_NEW_INSTANCE);
                sendCallback(callbackID, result);
                return;
            }
        }

        CBHybridPlugin plugin = mPluginObjMap.get(callClassSimpleName);
        Class<?>[] methodOneParamClass = new Class[]{String.class};
        Class<?>[] methodTwoParamClass = new Class[]{String.class, String.class};
        Method method;

        //Method 호출
        //Method명에 async가 붙어있으면 비동기 호출로 인지하고 파라미터에 callbackID를 넘겨줌
        try {
            if (callMethodName.toLowerCase().contains(PRE_DEFINED_ASYNC_METHOD)) {
                if (jsonParam == null) {
                    method = plugin.getClass().getMethod(callMethodName, methodOneParamClass);
                    method.invoke(plugin, callbackID);
                } else {
                    method = plugin.getClass().getMethod(callMethodName, methodTwoParamClass);
                    method.invoke(plugin, callbackID, jsonParam);
                }
                return;
            }

            if (jsonParam == null) {
                method = plugin.getClass().getMethod(callMethodName);
                //void return Type method 지원
                if (method.getReturnType().equals(Void.TYPE)) {
                    method.invoke(plugin);
                    return;
                } else {
                    result = (CBHybridPluginResult) method.invoke(plugin);
                }
            } else {

                method = plugin.getClass().getMethod(callMethodName, methodOneParamClass);
                //void return Type method 지원
                if (method.getReturnType().equals(Void.TYPE)) {
                    method.invoke(plugin, jsonParam);
                    return;
                } else {
                    result = (CBHybridPluginResult) method.invoke(plugin, jsonParam);
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            result = new CBHybridPluginResult("요청 함수를 호출 할 수 없습니다. (method = " + callMethodName + ")");
            result.setStatus(CommonBasedConstants.HYBRID_ERROR_METHOD_CALL);
        } catch (InvocationTargetException e) {
            int errCode = CommonBasedConstants.HYBRID_ERROR_METHOD_CALL;
            String msg;
            if (e.getCause() instanceof CBHybridException) {
                CBHybridException cbHybridException = ((CBHybridException) e.getCause());
                errCode = cbHybridException.getCode();
                msg = cbHybridException.getMessage();
            } else {
                msg = "요청 함수를 호출 할 수 없습니다. (cause = " + e.getMessage() +")";
            }
            result = new CBHybridPluginResult(msg);
            result.setStatus(errCode);
        }

        //호출 결과 callback
        sendCallback(callbackID, result);
    }


    /**
     * WebView로 Callback 전달
     *
     * @param callbackID WebView 쪽 콜백을 위한 ID값
     * @param result     callback 데이터
     */
    public void sendCallback(String callbackID, CBHybridPluginResult result) {
        if (result == null || TextUtils.isEmpty(callbackID)) {
            //호출 종류에 따라 callbackID가 없거나 result 가 없는 경우 무시
            return;
        }

        CBHybridActivity hybridActivity = (CBHybridActivity) mCtxForHybridActivity;
        if (!hybridActivity.isDestroyed()) {
            hybridActivity.loadUrl("javascript:CommonBaseAPI.CallbackManager.onCallback(\"" + callbackID + "\", " + result.toJsonString() + ")");
        }
    }


    public void resume() {
        Set<String> it = mPluginObjMap.keySet();
        for (String name : it) {
            CBHybridPlugin plugin = mPluginObjMap.get(name);
            if(plugin != null)
                plugin.onResume();
        }
        it.clear();
    }

    public void pause() {
        Set<String> it = mPluginObjMap.keySet();
        for (String name : it) {
            CBHybridPlugin plugin = mPluginObjMap.get(name);
            if(plugin != null)
                plugin.onPause();
        }
        it.clear();
    }

    public void destroy() {
        Set<String> it = mPluginObjMap.keySet();
        for (String name : it) {
            CBHybridPlugin plugin = mPluginObjMap.get(name);
            if(plugin != null)
                plugin.onDestroy();
        }
        it.clear();

        mPluginObjMap.clear();
        mPluginNameMap.clear();
    }

}
