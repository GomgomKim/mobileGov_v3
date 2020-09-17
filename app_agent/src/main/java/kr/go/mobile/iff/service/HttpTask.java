package kr.go.mobile.iff.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONObject;

import com.sds.mobile.servicebrokerLib.StringCryptoUtil;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceCallback;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import kr.go.mobile.iff.service.HttpService.IResponseListener;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.mobp.iff.http.HttpManager;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;

public class HttpTask implements Runnable {

	private Context mContext;
	private int httpType;
	private String header;
	private int timeout;
	private String hostURL;
	private String serviceID;
	private ArrayList<String> params;
	private IRemoteServiceCallback callback;
	
	private final IResponseListener listener = new IResponseListener() {
		String TAG = "HttpTask.IResponseListener";
		@Override
		public void onResult(int what, Bundle bundle) {
			LogUtil.w(getClass(), "response from \'NET\' (data) <<<<<  ");
			int errorCode = E2ESetting.DATA_ERROR_CODE;
			String result = bundle.getString("result", "");
			try {
				switch (what) {
				case -1: // 실패
					if(E2ESetting.SERVER_ERROR_MESSAGE.equals(result)){
						errorCode = E2ESetting.SERVER_ERROR_CODE;
					}
					break;
				case 0: //  정상
					if (result.length() == 0) {
						errorCode = E2ESetting.DATA_ERROR_CODE;
					} else {
						Log.d(TAG, result);
						JSONObject resultJsonObject = new JSONObject(new JSONObject(result).getString("methodResponse"));
						result = resultJsonObject.getString("data");
						
						boolean largeResultSize = result.length() > 20000;
						Log.d(TAG, "Result size is large : " + largeResultSize);
						
						if(largeResultSize){
							BufferedWriter out = null;
							try {
								String fileName = System.currentTimeMillis() + ".txt";
								String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + E2ESetting.dataForderPath;
								String filePath = folderPath + fileName;
								File folder = new File(folderPath);
								if( ! folder.exists()){
									folder.mkdirs();
								}
								folder.mkdirs();
								File file = new File(filePath);
								file.createNewFile();
								out = new BufferedWriter(new FileWriter(filePath));

								SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssss");
								Calendar cal = Calendar.getInstance();
						        String key = sdf.format(cal.getTime());
								result = new StringCryptoUtil().encryptAES(result, key);
								out.write(result); 

								LogUtil.e(getClass(), "data successBigdata :: " + filePath);
								callback.successBigData(key, filePath);
								return;
							} catch (IOException e) {
								LogUtil.e(getClass(), "", e);
								errorCode = E2ESetting.DATA_ERROR_CODE;
							} finally {
								if (out != null)
									out.close();
							}
						}else{
							callback.success(result);
							return;
						}
					}
					break;
				}
				callback.fail(errorCode, result);
			} catch (Exception e) {
				LogUtil.e(getClass(), "", e);
			}
		}
	};
	
	public HttpTask(Context ctx, int httpType, String header, String hostUrl, String serviceID, ArrayList<String> param, int timeout, IRemoteServiceCallback callback) {
		this.mContext = ctx;
		this.httpType = httpType;
		this.header = header;
		this.timeout = timeout;
		this.hostURL = hostUrl;
		this.serviceID = serviceID;
		this.params = param;
		this.callback = callback;
	}
	
	@Override
	public void run() {
		new HttpManager(this.mContext).reqData(this.httpType, this.hostURL, this.serviceID, this.params, this.timeout, this.listener, this.header);
	}
}
