package kr.go.mobile.mobp.iff;

import java.util.ArrayList;

import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.iff.util.Utils;
import kr.go.mobile.mobp.iff.http.HttpManager;
import kr.go.mobile.mobp.iff.util.SingleProgressDialog;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceCallback;

@Deprecated
public class ProgressActivity extends Activity{
	public static SingleProgressDialog downProgress;
	static IRemoteServiceCallback callback;
	E2ESetting e2eSetting = new E2ESetting();
	String file, fileUrl, fileName, header;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	
		downProgress = new SingleProgressDialog(ProgressActivity.this);

		if(downProgress!= null && !downProgress.isShowing()){
			downProgress.show();
		}

		
		fileUrl = getIntent().getStringExtra("fileUrl");
		fileName = getIntent().getStringExtra("fileName");
		header = getIntent().getStringExtra("header");
		
		/*if(fileName == null || fileUrl == null){
			try {
				callback.fail("Download 실패 : 필수값 부족");//callback null 발생
			} catch (RemoteException e) {
				LogUtil.errorLog(e);
			}
		}*/
		
		file = Environment.getExternalStorageDirectory().toString() + e2eSetting.getAppDownForderPath() + fileName;
		
		final Handler handler = new Handler(Looper.getMainLooper()){
			public void handleMessage(Message msg) {
				try {
					Bundle bd = msg.getData();
					if(msg.what == -1){
						String message = bd.getString("result");
						int errorCode = E2ESetting.DATA_ERROR_CODE;
						if(E2ESetting.SERVER_ERROR_MESSAGE.equals(message)){
							errorCode = E2ESetting.SERVER_ERROR_CODE;
						}
						callback.fail(errorCode, message);
					}else{
						String result = bd.getString("result");

						LogUtil.d(getClass(), "Download filePath :: " + file);
						callback.success(file);
					}
				} catch (Exception e) {
					LogUtil.e(getClass(), "", e);
				} finally {
					finish();
				}
			}
		};
		
		if(Utils.checkNetwork(ProgressActivity.this)){
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
//						Map map = new HashMap();
//						map.put("url", fileUrl);
						
						ArrayList<String> list = new ArrayList<String>();
						list.add("url="+fileUrl);
						
						new HttpManager(ProgressActivity.this).appDownLoad(E2ESetting.FILE_DOWNLOAD_SERVICE, 
								list, handler, file, downProgress, header); 
					} catch (Exception e) {
						LogUtil.e(getClass(), "", e);
					}
				}
			}).start();
		}else{
			downProgress.dismiss();
		}
	}
	
	public static void setCallBack(IRemoteServiceCallback callBack){
		callback = callBack;
	}
	
	@Override
	protected void onDestroy() {
		downProgress = null;
		super.onDestroy();
	}
}