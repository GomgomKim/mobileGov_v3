package com.sds.mobile.servicebrokerLib;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sds.BizAppLauncher.gov.aidl.GovControllerType;
import com.sds.BizAppLauncher.gov.aidl.MoiApplication;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteService;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceCallback;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceExitCallback;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseExitListener;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;


import kr.go.mobile.iff.R;

public class ServiceBrokerLib {

	public interface ServiceBrokerCB  {
		void onServiceBrokerResponse(String retMsg);
	}

	private IRemoteServiceCallback mRemoteSVCCallBack = new IRemoteServiceCallback.Stub() {
		@Override
		public void success(String data) throws RemoteException {
			Log.d(TAG, "response (sucess) <<<<<< ");
			Log.d(TAG, " - data : " + data);
			int resultCode = 0;
			final ResponseEvent re = new ResponseEvent(resultCode, data);
			if (mResponseListener != null) {
				if (mContext instanceof Activity) {
					((Activity) mContext).runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Log.d(TAG, " - CallBack (UI enabled)");
							mResponseListener.receive(re);
						}
					});

				}else{
					Log.d(TAG, " - CallBack (UI disabled)");
					mResponseListener.receive(re);
				}
			}else{
				Log.w(TAG, " - CallBack (LIstener is null)");
			}
		}

		@Override
		public void successBigData(String code, String data) throws RemoteException {
			Log.d(TAG, "response (sucess - bigData) <<<<<< ");
			Log.d(TAG, " - code : " + code);
			Log.d(TAG, " - data : " + data);

			int resultCode = 0;
			String result = "";
			FileInputStream fis = null;
			Reader reader = null;
			BufferedReader br = null;
			try {
				fis = new FileInputStream(data);
				reader = new InputStreamReader(fis,"UTF-8");
				br = new BufferedReader(reader);

				StringBuffer builder = new StringBuffer();

				String readLine = null;
				while ((readLine = br.readLine()) != null) {
					builder.append(readLine);
				}

				resultCode = RESPONSE_OK;
				result = new StringCryptoUtil().decryptAES(builder.toString(), code);

				File file = new File(data);
				if (file.exists()) {
					file.delete();
					Log.d(TAG, "file deleted. (" + file.exists() + ")");
				}

			} catch (NullPointerException e) {
				Log.e(TAG, "data is null. :: " + e.getMessage());
				resultCode = RESPONSE_DATA_FORMAT_INVALID;
				result = "데이터 처리 중 오류가 발생했습니다.";
			} catch (Exception e) {
				Log.e(TAG, "unknow error  :: " + e.getMessage());
				resultCode = RESPONSE_DATA_FORMAT_INVALID;
				result = "데이터 처리 중 오류가 발생했습니다.";
			} finally {
				try {
					if (br != null) {
						br.close();
					}
					if (reader != null) {
						reader.close();
					}
					if (fis != null) {
						fis.close();
					}
				} catch (Exception e) {
				}
			}

			final ResponseEvent re = new ResponseEvent(resultCode, result);

			if (mResponseListener != null) {
				if (mContext instanceof Activity) {
					((Activity) mContext).runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Log.d(TAG, " - CallBack (UI enabled)");
							mResponseListener.receive(re);
						}
					});

				}else{
					Log.d(TAG, " - CallBack (UI disabled)");
					mResponseListener.receive(re);
				}
			} else {
				Log.w(TAG, " - CallBack (LIstener is null)");
			}
		}

		@Override
		public void fail(int errorCode, String data) throws RemoteException {
			Log.e(TAG, "response (error) <<<<<< ");
			Log.e(TAG, " - errorCode : " + errorCode);
			Log.e(TAG, " - data : " + data);
			final ResponseEvent re = new ResponseEvent(errorCode, data);
			if (mResponseListener != null) {
				if (mContext instanceof Activity) {
					((Activity) mContext).runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Log.d(TAG, " - CallBack (UI enabled)");
							mResponseListener.receive(re);
						}
					});

				}else{
					Log.d(TAG, " - CallBack (UI disabled)");
					mResponseListener.receive(re);
				}
			} else {
				Log.w(TAG, " - CallBack (LIstener is null)");
			}
		}
	};

	Runnable ServiceBrokerRequestPolling = new Runnable() {

		@Override
		public void run() {
			// 서비스 바인드 된 상태에서만 처리
			while (MoiApplication.getSVC() != null) {
				try {
					ServBrokTask.ServBrokData data = null;
					synchronized (ServiceBrokerLib.LOCK) {
						data = ServiceBrokerLib.mQueue.take();
					}

					if (data == null || MoiApplication.getSVC() == null) {
						continue;
					}

					ServBrokTask task = new ServBrokTask(MoiApplication.getSVC());
					task.execute(data);
				} catch (InterruptedException e) {
				}
			}
		}

	};

	private static final String TAG = ServiceBrokerLib.class.getSimpleName();
	
	private static final int SERVICE_BROKER_ERROR_CODE = -1000;
	public static final int INVALID_ARGUMENT = SERVICE_BROKER_ERROR_CODE - 1;
	public static final int STRANGE_STATE_CONTEXT = SERVICE_BROKER_ERROR_CODE - 2;
	public static final int REMOTE_SERVICE_NOT_CONNTETION = SERVICE_BROKER_ERROR_CODE - 3;
	public static final int REMOTE_SERVICE_EXCEPTION = SERVICE_BROKER_ERROR_CODE - 4;
	public static final int UNDEFINED_EXCEPTION = SERVICE_BROKER_ERROR_CODE - 5;
	public static final int RESPONSE_OK = 0;
	public static final int RESPONSE_DATA_FORMAT_INVALID = SERVICE_BROKER_ERROR_CODE - 9;
	
	@Deprecated
	public static final String SERVICE_DOCUMENT = "document";
	@Deprecated
	public static final String ZIP_LIST = "ziplist";
	@Deprecated
	public static final String SERVICE_UPLOAD = "upload";
	// 기존 upload 방식용으로 처리할때 upload2사용
	@Deprecated
	public static final String SERVICE_UPLOAD2 = "upload2";
	@Deprecated
	public static final String SERVICE_DOWNLOAD = "download";
	@Deprecated
	private IRemoteServiceCallback mCallback = null;
	@Deprecated
	private long mSvcTaskStartTime = 0;
	@Deprecated
	private String mResultMsg = "-1004";
	@Deprecated
	private ByteArrayOutputStream lByteArrayOutputStream;
	@Deprecated
	private byte[] lDataBytes;
	@Deprecated
	private boolean mNeeedExitCB = false;


	static final String KEY_HOST = "host";
	static final String KEY_HEADER = "header";
	static final String KEY_TIMEOUT = "timeoutInterval";
	static final String KEY_DATA_TYPE = "dataType";

	/**
	 *
	 */
	public static final String KEY_SERVICE_ID = "sCode";
	/**
	 *
	 */
	public static final String KEY_PARAMETER = "parameter";
	/**
	 *
	 */
	public static final String KEY_FILE_PATH = "filePath";
	/**
	 *
	 */
	public static final String KEY_FILE_NAME = "fileName";

	/**
	 *
	 */
	public static final String SERVICE_GET_INFO = "getInfo";

	// 서비스브로커 요청에 대한 큐
	private static LinkedBlockingQueue<ServBrokTask.ServBrokData> mQueue = new LinkedBlockingQueue<ServBrokTask.ServBrokData>(20);
	private static final Object LOCK = new Object();
	private static boolean isInitialize = false;

	private Context mContext;
	private ResponseListener mResponseListener;
	private ResponseExitListener mResponseExitListener;
	private ServiceBrokerCB mSvcBrokerCB;

	@Deprecated
	public ServiceBrokerLib(Context context) {
		this(context, null, null);
	}
	
	public ServiceBrokerLib(Context context, ResponseListener listenerObj) {
		this(context, listenerObj, null);
	}
	
	public ServiceBrokerLib(Context context, ServiceBrokerCB cb) {
		this(context, null, cb);
	}
	
	/**
	* @Method Name	:	ServiceBrokerLib
	* @작성일				:	2015. 12. 1. 
	* @작성자				:	조명수
	* @변경이력				:
	* @파라메터				:	cb(httpService 에서 리턴을 주는경우 해당 콜백으로 값이 전달됨)
	* @Method 설명 		: 
	*/
	@Deprecated
	public ServiceBrokerLib(Context context, ResponseListener listenerObj, ServiceBrokerCB cb) {
		this.mContext = context;
		this.mResponseListener = listenerObj;		
		this.mSvcBrokerCB = cb;

		this.lByteArrayOutputStream = new ByteArrayOutputStream();
		this.lDataBytes = null;

		init();
	}
	
	/**
	 * @param context
	 * @param needExitCB
	 * @param listenerObj
	 * 
	 * Document 서비스 이용시에만 현재 사용..(AttachmentActivity 의 닫기 실행시 콜백을 받음)
	 */
	@Deprecated
	public ServiceBrokerLib(Context context, boolean needExitCB, ResponseListener listenerObj,
			ResponseExitListener exitListener) {
		this.mContext = context;
		this.mResponseListener = listenerObj;
		this.mResponseExitListener = exitListener;

		this.mNeeedExitCB = needExitCB;
		this.lByteArrayOutputStream = new ByteArrayOutputStream();
		this.lDataBytes = null;

		init();
	}

	private synchronized void init() {
		if (isInitialize) {
			return;
		}

		isInitialize = true;

		MoiApplication.setRunnable(new Runnable() {
			@Override
			public void run() {
				// 서비스 바인드 된 상태에서만 처리
				Log.d("TOM@@@", "complete binding");
				while (MoiApplication.getSVC() != null) {
					try {
						ServBrokTask.ServBrokData data = null;
						synchronized (ServiceBrokerLib.LOCK) {
							data = ServiceBrokerLib.mQueue.take();
							Log.d("TOM@@@", "take Q");
						}

						if (data == null || MoiApplication.getSVC() == null) {
							continue;
						}

						ServBrokTask task = new ServBrokTask(MoiApplication.getSVC());
						task.execute(data);
					} catch (InterruptedException e) {
					}
				}
			}
		});
	}

	private String getHostURL(Bundle data) {
		String ret = "__DEFAULT__";
		// 별도의 서비스 IP를 사용할 경우로 현재는 가이드 되지 않고 있음. 
		String connectionType = data.getString("connectionType", "");
		String ipAddress = data.getString("ipAddress", "");
		String portNumber = data.getString("portNumber", "");
		String contextUrl = data.getString("contextUrl", "");
		
		if (ipAddress.length() > 0 
				&& portNumber.length() > 0 
				&& connectionType.length() > 0 
				&& contextUrl.length() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(connectionType).append("://")
				.append(ipAddress).append(":")
				.append(portNumber)	.append("/")
				.append(contextUrl);
			ret = sb.toString();
		}
		data.putString(KEY_HOST, ret);
		return ret;
	}
	
	public void  request(Intent intent) {
		Bundle extraData = intent.getExtras();
		extraData.putString(KEY_HEADER, new HeaderUtil(mContext).getHeader());

		// 기본 정보`
		String hostURL = getHostURL(extraData); 
		String serviceID = extraData.getString(KEY_SERVICE_ID, "");
		String parameter = extraData.getString(KEY_PARAMETER, "");
		// 파일 정보
		String filePath = extraData.getString(KEY_FILE_PATH, "");
		String fileName = extraData.getString(KEY_FILE_NAME, "");

		Log.d(TAG, "request >>>>> ");
		Log.d(TAG, " - HOST_URL :  " + hostURL);
		Log.d(TAG, " - SERVICE_ID :  " + serviceID);
		Log.d(TAG, " - PARAMETER :  " + parameter);
		if (filePath.length() > 0 && fileName.length() > 0) {
			Log.d(TAG, "* FILE SERVICE INFO *");
			Log.d(TAG, " - FILE_NAME :  " + fileName);
			Log.d(TAG, " - FILE_PATH :  " + filePath);
			Log.d(TAG, "* ********************* *");
		}
		
		try {
			if (serviceID.length() == 0) {
				ResponseEvent re = new ResponseEvent(INVALID_ARGUMENT, "ServiceID가 존재하지 않습니다.");
				mResponseListener.receive(re);
				return;
			}
			
			if ((serviceID.compareToIgnoreCase("download") != 0)
				&& (filePath != null && filePath.length() > 0)) {
				File file = new File(filePath);
				if (file.exists() == false) {
					Log.e(TAG, "첨부파일(" + filePath + ")이 존재하지 않습니다.");
					ResponseEvent re = new ResponseEvent(STRANGE_STATE_CONTEXT, "첨부파일(" + filePath + ")이 존재하지 않습니다.");
					mResponseListener.receive(re);
					return;
				}
			}

			ServBrokTask.ServBrokData data = new ServBrokTask.ServBrokData(extraData);
			data.setIRemoteServiceCallback(this.mRemoteSVCCallBack);
			data.setServiceBrokerCallback(this.mSvcBrokerCB);
			data.addResponseListener(this.mResponseListener);
			Log.d("TOM@@@", "insert Q");
			ServiceBrokerLib.mQueue.put(data);
		} catch (Exception e) {
			Log.e(TAG, "request error : " + e.getMessage());
		}
	}



	/**	mRemoteSVCCallBack 를 사용해도 되나 기존 사용하고있는 서비스앱들에게 영향을 주지않기 위해서 추가
	 * 		(특정서비스앱 개발파트에서 요청이 왔기때문에 해당 이슈에만 대응하기 위해)
	 * 
	 *     아래의 생성자를 통해 생성시에 적용됨..
	 *     
	 * 	    public ServiceBrokerLib(final Context context, final boolean needExitCB, final ResponseListener listenerObj,
			final ResponseExitListener exitListener) {
	 * 
	 */
	@Deprecated
	IRemoteServiceExitCallback mRemoteSVCExitCallBack = new IRemoteServiceExitCallback.Stub() {
		
		@Override
		public void onExit() throws RemoteException {
			LogUtil.log_w("onExit Call Back ");
			if (mResponseExitListener != null) {
				if (mContext instanceof Activity) {
					((Activity) mContext).runOnUiThread(new Runnable() {
						@Override
						public void run() {
							LogUtil.log_w("onExit Call Back 1");
							mResponseExitListener.receiveExit();
						}
					});

				}else{
					LogUtil.log_w("onExit Call Back 2");
					mResponseExitListener.receiveExit();
				}
			}
		}
	};
	
	@Deprecated
	public String waitToResponse() {
		while(true){
			long currentTime = System.currentTimeMillis();
			long currenttimeSeconds = currentTime / 1000;
			long starttimeSeconds = mSvcTaskStartTime / 1000;
					
			if(!"-1004".equals(mResultMsg))
				break;
			
			// 60초이상 경과시
			if( starttimeSeconds > 0 && currenttimeSeconds - starttimeSeconds > 60){
				LogUtil.log_d("waitToResponse timeout!!!");		
				break;
			}
		}
		
		return mResultMsg;
	}
	
/*
	@Deprecated
	private class serviceBrokerTask extends AsyncTask<Bundle, Void, String> {

		@Override
		protected synchronized void onPreExecute() {
        	LogUtil.log_d("service broker onPreExecute");
        	mSvcTaskStartTime = System.currentTimeMillis();
		}

		@Override
		protected synchronized String doInBackground(Bundle... params) {
			String resultMsg = "";
			if (mRS != null) {
				Bundle extraData = params[0];
				String serviceID = extraData.getString(KEY_SERVICE_ID);
				String filePath = extraData.getString(KEY_FILE_PATH);
				
				LogUtil.log_d("service broker doInBackground : " + serviceID + ", filePath :: " + filePath);

				try {
					if (PREFIX_SERVICE_DOWNLOAD.compareToIgnoreCase(serviceID) == 0) {
						mRS.download(new HeaderUtil(mContext).getHeader(), 
								filePath, 
								fileName, 
								mRemoteSVCCallBack);
						
					} else if (PREFIX_SERVICE_UPLOAD.compareToIgnoreCase(serviceID) == 0) {
						sendUploadWithCB(E2ESetting.FILE_UPLOAD_SERVICE);
					} else if (PREFIX_SERVICE_UPLOAD2.compareToIgnoreCase(serviceID) == 0) {
						resultMsg = sendUpload(E2ESetting.FILE_UPLOAD_SERVICE);
					} else if (PREFIX_SERVICE_GET_INFO.compareToIgnoreCase(serviceID) == 0) {
		    			resultMsg = mRS.getInfo(parameter);
						if (mSvcBrokerCB != null) {
							mSvcBrokerCB.onServiceBrokerResponse(resultMsg);
						}
//					} else if (SERVICE_DOCUMENT.compareToIgnoreCase(serviceID) == 0) {
//						sendDocument();
//					} else if (SERVICE_ZIP_LIST.compareToIgnoreCase(serviceID) == 0) {
//						requestZipList();
//					} else if(SERVICE_MAIL_READ.compareToIgnoreCase(serviceID) == 0){
//						// 메일 뷰어
//						requestMailViewer();
					} else {
						if(filePath != null && filePath.length() > 0){
							resultMsg = sendUploadWithCB(mServiceID);
							if (mSvcBrokerCB != null)
								mSvcBrokerCB.onServiceBrokerResponse(resultMsg);
						}else{
							sendData();
						}
					}
				} catch (RemoteException e) {
	    			LogUtil.errorLog(e);
	    		} catch (Exception e) {
	    			LogUtil.errorLog(e);
				}
			}
			return resultMsg;
		}


        @Override
		protected synchronized void onPostExecute(String result) {
        	super.onPostExecute(result);
        	LogUtil.log_d("service broker onPostExecute result :: " + result);
        	mResultMsg = result;
		}
        
        private void sendData() {
    		if (mRS == null)
    			return;
    		
    		ByteArrayInputStream byteArrayInputStream = null;
    		
    		try {
				LogUtil.log_d("size sizexx >>> :: " + (parameter == null ? 0 : parameter.length()));
    			
    			if (parameter != null && parameter.length() > 20000) {
    				// 최초 통보를 위해 ""리턴
    				ByteArray sByteArray = new ByteArray("".getBytes());
    				mRS.bigData(new HeaderUtil(mContext).getHeader(), 
    						hostUrl, 
    						mServiceID, 
    						"bigData", 
    						sByteArray, 
    						timeoutInterval, 
    						mRemoteSVCCallBack);

    				byte[] byteData = parameter.getBytes("UTF-8");
    				byte[] lByteBuffer = new byte[2048 * 10];
    				byteArrayInputStream = new ByteArrayInputStream(byteData);
    				int i = 0;
    				while ((i = byteArrayInputStream.read(lByteBuffer)) != -1) {
    					try {
    						if (i == lByteBuffer.length) {
    							ByteArray byteArray = new ByteArray(lByteBuffer);
    							mRS.bigData(new HeaderUtil(mContext).getHeader(), 
    									hostUrl, 
    									mServiceID, 
    									dataType, 
    									byteArray, 
    									timeoutInterval,
    									mRemoteSVCCallBack);
    						} else {
    							byte[] lByteBuffer2 = new byte[i];
    							System.arraycopy(lByteBuffer, 0, lByteBuffer2, 0, i);
    							ByteArray byteArray = new ByteArray(lByteBuffer2);
    							mRS.bigData(new HeaderUtil(mContext).getHeader(), 
    									hostUrl, 
    									mServiceID, 
    									dataType, 
    									byteArray, 
    									timeoutInterval,
    									mRemoteSVCCallBack);
    						}

    					} catch (NullPointerException e) {
    						LogUtil.errorLog(e);
    					} catch (Exception e) {
    						LogUtil.errorLog(e);
    					}
    				}

    				// 마지막 통보를 위해 ""리턴
    				ByteArray eByteArray = new ByteArray("".getBytes());
    				mRS.bigData(new HeaderUtil(mContext).getHeader(), hostUrl, mServiceID, dataType, eByteArray, timeoutInterval, mRemoteSVCCallBack);
    			} 
    			else {
    				mRS.data(new HeaderUtil(mContext).getHeader(), hostUrl, mServiceID, dataType, parameter, timeoutInterval, mRemoteSVCCallBack);
    			}

    		} catch (RemoteException e) {
    			LogUtil.errorLog(e);
    		} catch (Exception e) {
    			LogUtil.errorLog(e);
    		} finally {
    			try {
					if (byteArrayInputStream != null) {
						byteArrayInputStream.close();
					}
    			} catch (Exception e) {
				}
			}
    	}

    	private String sendUpload(final String serviceID) {
    		String resultMsg = "-1004";
    		if (mRS == null)
    			return resultMsg;

    		try {
    			resultMsg = mRS.upload(new HeaderUtil(mContext).getHeader(), filePath, parameter);
    			
    			if (mResponseListener != null) {
    				LogUtil.log_e("serviceBrker sendUpload filePath okok : " + resultMsg);
    				
    				JSONObject jsonObj = (JSONObject)new JSONTokener(resultMsg).nextValue();
    				if(jsonObj.has("methodResponse")){
    					ResponseEvent re = new ResponseEvent(0, resultMsg);
    					mResponseListener.receive(re);
    				}else{
    					ResponseEvent re = new ResponseEvent(-109, resultMsg);
    					mResponseListener.receive(re);
    				}
    				
    			}else{
    				LogUtil.log_e("serviceBrker sendUpload mResponseListener null : " + resultMsg);
    			}

    		} catch (RemoteException e) {
    			LogUtil.errorLog(e);
    			ResponseEvent re = new ResponseEvent(-109, resultMsg);
    			mResponseListener.receive(re);
    		} catch (Exception e) {
    			LogUtil.errorLog(e);
    			ResponseEvent re = new ResponseEvent(-109, resultMsg);
    			mResponseListener.receive(re);
    		}

    		return resultMsg;
    	}
    	
    	private String sendUploadWithCB(final String serviceID){
    		String resultMsg = "-1004";
    		if (mRS == null)
    			return resultMsg;

    		try {
    			LogUtil.log_e("serviceBrker 파일업로드 filePath :: " + filePath);
    			resultMsg = mRS.uploadWithCB(new HeaderUtil(mContext).getHeader(), serviceID, filePath, parameter, mRemoteSVCCallBack);

    		} catch (RemoteException e) {
    			LogUtil.errorLog(e);
    		} catch (Exception e) {
    			LogUtil.errorLog(e);
    		}

    		return resultMsg;
    	}

    	private String sendGetInfo() {
    		String resultMsg = "";
    		if (mRS == null)
    			return resultMsg;

    		try {
    			Log.d("jms", "sendGetInfo !!!");
    			resultMsg = mRS.getInfo(parameter);

    		} catch (RemoteException e) {
    			LogUtil.errorLog(e);
    		} catch (Exception e) {
    			LogUtil.errorLog(e);
    		}

    		return resultMsg;
    	}

    	@Deprecated
    	private void sendDocument() {
    		if (mRS == null)
    			return;

    		try {
    			if(mNeeedExitCB)
    				mRS.documentWithExitCB(new HeaderUtil(mContext).getHeader(), parameter, mRemoteSVCCallBack, mRemoteSVCExitCallBack);
    			else
    				mRS.document(new HeaderUtil(mContext).getHeader(), parameter, mRemoteSVCCallBack);

    		} catch (RemoteException e) {
    			LogUtil.errorLog(e);
    		} catch (Exception e) {
    			LogUtil.errorLog(e);
    		}
    	}

    	@Deprecated
    	private void requestMailViewer() {
    		if (mRS == null)
    			return;

    		try {
    			mRS.displayMailViewer(new HeaderUtil(mContext).getHeader() ,
    					hostUrl, mServiceID, dataType , parameter, timeoutInterval, mRemoteSVCCallBack, mRemoteSVCExitCallBack);
    		} catch (RemoteException e) {
    			LogUtil.errorLog(e);
    		} catch (Exception e) {
    			LogUtil.errorLog(e);
    		}
    	}
    	
    	@Deprecated
    	private void requestZipList() {
    		String resultMsg = "-1004";
    		if (mRS == null)
    			return;

    		try {
    			LogUtil.log_e("serviceBrker requestZipList !!");
    			mRS.zipList(new HeaderUtil(mContext).getHeader(), parameter, mRemoteSVCCallBack, mRemoteSVCExitCallBack);

    		} catch (RemoteException e) {
    			LogUtil.errorLog(e);
    		} catch (Exception e) {
    			LogUtil.errorLog(e);
    		}

    	}

    }
    */
}