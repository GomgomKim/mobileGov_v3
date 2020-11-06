package kr.go.mobile.common.v3.hybrid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

/**
 * Created by ChangBum Hong on 2020-07-22.
 * cloverm@infrawareglobal.com
 */
class CBHybridWebView extends WebView {

    private static final String TAG = CBHybridWebView.class.getSimpleName();
    private final static String URL_SPLIT_CHAR = "#";
    private final static String URL_FILE_ANDROID_ASSET = "file:///android_asset/";

    public CBHybridWebView(@NonNull final Context context) {
        super(context);

        this.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d(TAG, cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId() );
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
                callback.invoke(origin, true, false);
            }



            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(message);
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        result.cancel();
                    }
                });
                builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        result.confirm();
                    }
                });
                builder.show();
                return true;
            }
        });

        this.setWebViewClient(new WebViewClient() {

            private String failingUrl = "";
            private boolean isReceivedError = false;

            /**
             * ajax 등에서 에러 발생시 상위 URL로 (#이 없는 URL) 이동하게 하는 처리 코드
             * @param view WebView
             * @param failingUrl 이동 시도 URL
             * @return
             */
            private boolean receivedError(WebView view, String failingUrl) {
                if (!this.isReceivedError && failingUrl.toLowerCase().startsWith(URL_FILE_ANDROID_ASSET) && failingUrl.contains(URL_SPLIT_CHAR)) {
                    String[] url = failingUrl.split("#");
                    view.loadUrl(url[0]);
                    view.setVisibility(View.INVISIBLE);

                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ignored) {

                    }

                    this.isReceivedError = true;
                    this.failingUrl = failingUrl;
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
//                //KitKat 버전에서 발생하는 버그 방어코드로 보여짐
//                if (!url.equals("file:///android_asset/webkit/android-weberror.png")) {
//                    super.onLoadResource(view, url);
//                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    if (!receivedError(view, failingUrl)) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                    }
                }
            }


            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!receivedError(view, request.getUrl().toString())) {
                    super.onReceivedError(view, request, error);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //ajax 등에서 에러 발생시 상위 URL로 (#이 없는 URL) 이동하게 하는 처리 코드
                if (!TextUtils.isEmpty(this.failingUrl) && this.failingUrl.split(URL_SPLIT_CHAR)[0].equals(url)) {
                    WebBackForwardList webBackForwardList = copyBackForwardList();
                    int backListSize = webBackForwardList.getSize();

                    if (backListSize >= 2 && webBackForwardList.getItemAtIndex(backListSize - 2).getUrl().equals(failingUrl)) {
                        goBack();
                    } else {
                        loadUrl(url);
                    }

                    this.failingUrl = "";
                    this.isReceivedError = false;

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }

                    setVisibility(View.VISIBLE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = view.getUrl();
                if (url.startsWith("tel:")) {
                    return true;
                } else if (url.startsWith("sms:")) {
                    return true;
                } else {
                    return super.shouldOverrideUrlLoading(view, request);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("tel:")) {
                    return true;
                } else if (url.startsWith("sms:")) {
                    return true;
                } else {
                    view.loadUrl(url);
                    return super.shouldOverrideUrlLoading(view, url);
                }
            }
        });

        this.setOnKeyListener(null);
        this.setHorizontalScrollBarEnabled(true);
        this.setVerticalScrollBarEnabled(false);
        this.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        this.setBackgroundColor(Color.BLACK);

        // WebSetting
        {
            WebSettings settings = getSettings();
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setAppCacheEnabled(true);
            settings.setGeolocationEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setNeedInitialFocus(true);
        }



        try {
            //삼성 관련 특정 런처가 있으면 ZoomControl 설정 변경(태블릿 단말 관련 인듯)
            context.getPackageManager().getPackageInfo("com.sec.android.app.twlauncher", 0);
            this.getSettings().setBuiltInZoomControls(false);
        } catch (PackageManager.NameNotFoundException e) {
            this.getSettings().setBuiltInZoomControls(true);
        }
        this.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void destroy() {
        this.getSettings().setJavaScriptEnabled(false);
        this.clearAnimation();
        this.clearCache(true);
        this.setWebChromeClient(null);
        this.setWebViewClient(null);
        this.setOnTouchListener(null);
        this.stopLoading();
        super.destroy();
    }
}
