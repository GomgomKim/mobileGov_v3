package kr.go.mobile.mobp.iff.util;

import java.io.File;
import java.util.ArrayList;

import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.iff.util.Utils;
import kr.go.mobile.mobp.iff.ProgressActivity;
import kr.go.mobile.mobp.iff.http.HttpManager;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceCallback;

@Deprecated
public class DownLoadFile {
	public static SingleProgressDialog downProgress = null;

	E2ESetting e2eSetting = new E2ESetting();
	String file;
	
	public void downloadFile(final Context context, final String fileUrl, final boolean finish){
		downProgress = new SingleProgressDialog(context);
		if(downProgress!= null && !downProgress.isShowing()){
			downProgress.show();
		}
		
		file = Environment.getExternalStorageDirectory().toString() + e2eSetting.getAppDownPath();
		
		final Handler handler = new Handler(Looper.getMainLooper()){
			public void handleMessage(Message msg) {
				Bundle bd = msg.getData();
				String message = bd.getString("result");
				if(message == null){
					try {
		    			//안드로이드 패키지 매니저를 통해 다운 받은 apk 파일을 처리하도록 한다.
	    	    		Intent intent = new Intent(Intent.ACTION_VIEW);
	    	    		String file = Environment.getExternalStorageDirectory().toString() + e2eSetting.getAppDownPath();
	    	    		File mFile = new File(file);
	    	    		intent.setDataAndType( Uri.fromFile(mFile), "application/vnd.android.package-archive");
	    	    		context.startActivity(intent);
	    	    		
	    	    		if(finish){
	    	    			Activity act = (Activity)context;
	        	    		act.finish();
	    	    		}
					} catch (ActivityNotFoundException e) {
						LogUtil.e(getClass(), "", e);
					} catch (NullPointerException e) {
						LogUtil.e(getClass(), "", e);
					}
				}else{
					// TODO !!!
					// Popup.alert(context, message, false, null);
					Utils.showAlertDialog(context, "", message, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
				}
			}
		};
		
		if(Utils.checkNetwork(context)){
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
//						Map<String,Object> map = new HashMap<String,Object>();
//						map.put("url", fileUrl);	
						ArrayList<String> list = new ArrayList<String>();
						list.add("url="+fileUrl);
						
						new HttpManager(context).appDownLoad(E2ESetting.FILE_DOWNLOAD_SERVICE, list, handler, file, downProgress, ""); 
					} catch (Exception e) {
						LogUtil.e(getClass(), "", e);
					}
				}
			}).start();
		}else{
			downProgress.cancel();
		}
	}
	
	public void serviceAppDownloadFile(final Context context, final String header, final String fileUrl, final String fileName, final IRemoteServiceCallback callback){
		downProgress = null;
		ProgressActivity.setCallBack(callback);
		Intent intent = new Intent(context, ProgressActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("header", header);
		intent.putExtra("fileUrl", fileUrl);
		intent.putExtra("fileName", fileName);
		context.startActivity(intent);
		/*Handler progressHandler = new Handler(Looper.getMainLooper()){
			public void handleMessage(Message msg) {
				downProgress = new ProgressDialog(context);
		    	downProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		    	downProgress.setTitle("다운로드...");
		    	downProgress.setCancelable(false);
				if(downProgress!= null && !downProgress.isShowing()){
					downProgress.show();
				}
			}
		};
		progressHandler.sendEmptyMessage(1);*/
		
		/*String fileName = "service.apk";
		file = Environment.getExternalStorageDirectory().toString() + E2ESetting.getAppDownForderPath() + fileName;
		
		final Handler handler = new Handler(Looper.getMainLooper()){
			public void handleMessage(Message msg) {
				try {
					Bundle bd = msg.getData();
					if(msg.what == -1){
						String message = bd.getString("result");
						callback.fail("Download 실패 : " + message);
					}else{
						String result = bd.getString("result");

						LogUtil.log_e("Download filePath :: " + file);
						callback.success(result);
					}
				} catch (Exception e) {
					LogUtil.errorLog(e);
				}
			}
		};
		
		if(NetworkUtils.checkNetwork(context)){
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Map map = new HashMap();
						map.put("url", fileUrl);
						new HttpManager(context).appDownLoad(E2ESetting.FILE_DOWNLOAD_SERVICE, map, handler, file, downProgress, header); 
					} catch (Exception e) {
						LogUtil.errorLog(e);
					}
				}
			}).start();
		}*/
	}
}

