package com.sds.mobile.servicebrokerLib;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sds.mobile.servicebrokerLib.aidl.ByteArray;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteService;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceCallback;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;

class ServBrokTask extends AsyncTask<ServBrokTask.ServBrokData, Void, String> {

	static class ServBrokData {

		private String serviceID;
		private String hostUrl;
		private String header;
		private String filePath;
		private String fileName;
		private String parameter;
		private String dataType;
		private int timeout;

		private ResponseListener mListener;
		private IRemoteServiceCallback mServiceCallBack;
		private ServiceBrokerLib.ServiceBrokerCB mBrokerCallBack;

		public ServBrokData(Bundle bundle) {
			this.serviceID = bundle.getString(ServiceBrokerLib.KEY_SERVICE_ID, "");
			this.hostUrl = bundle.getString(ServiceBrokerLib.KEY_HOST, "");
			this.header = bundle.getString(ServiceBrokerLib.KEY_HEADER, "");
			this.filePath = bundle.getString(ServiceBrokerLib.KEY_FILE_PATH, "");
			this.fileName = bundle.getString(ServiceBrokerLib.KEY_FILE_NAME, "");
			this.parameter = bundle.getString(ServiceBrokerLib.KEY_PARAMETER, "");
			this.dataType = bundle.getString(ServiceBrokerLib.KEY_DATA_TYPE, "json");
			this.timeout= bundle.getInt(ServiceBrokerLib.KEY_TIMEOUT, 60 * 1000);
		}

		public void addResponseListener(ResponseListener listener) {
			this.mListener = listener;
		}

		public void setIRemoteServiceCallback(IRemoteServiceCallback serviceCallBack) {
			this.mServiceCallBack = serviceCallBack;
		}

		public void setServiceBrokerCallback(ServiceBrokerLib.ServiceBrokerCB brokerCallBack) {
			this.mBrokerCallBack = brokerCallBack;
		}
	}

	private String TAG = "ServBorkTask";
	
	private IRemoteService mService;

	public ServBrokTask(IRemoteService service) {
		this.mService = service;
	}

	@Override
	protected void onPreExecute() {
		Log.d(TAG, "ServiceBroker Prepare ... ");
	}
	
	@Override
	protected String doInBackground(ServBrokData ... data) {
		ResponseEvent event = null;

		Log.d(TAG, "ServiceBroker Doing ...");
		ServBrokData realData = data[0];

		try {
			if (ServiceBrokerLib.SERVICE_DOWNLOAD.compareToIgnoreCase(realData.serviceID) == 0) {
				Log.w(TAG, "** Download Service is deprecate.");
				Log.d(TAG, "FileName : " + realData.fileName);
				Log.d(TAG, "FilePath : " + realData.filePath);
				//mService.download(header, filePath, fileName, mServiceCallBack);
			} else if (ServiceBrokerLib.SERVICE_UPLOAD.compareToIgnoreCase(realData.serviceID) == 0) {
				Log.w(TAG, "** Upload Service is deprecate.");
				Log.d(TAG, "parameter : " + realData.parameter);
				Log.d(TAG, "FileName : " + realData.fileName);
				Log.d(TAG, "FilePath : " + realData.filePath);
				String result = "";
				//result = mService.uploadWithCB(header, E2ESetting.FILE_UPLOAD_SERVICE, filePath, parameter, mServiceCallBack);
				Log.d(TAG, "Result : " + result);
			} else if (ServiceBrokerLib.SERVICE_UPLOAD2.compareToIgnoreCase(realData.serviceID) == 0) {
				Log.w(TAG, "** (async) Upload Service is deprecate.");
				Log.d(TAG, "FileName : " + realData.fileName);
				Log.d(TAG, "FilePath : " + realData.filePath);
				Log.d(TAG, "ResponseListener : " + realData.mListener == null ? "false" : "true");
				String result = "";
				// result = mService.upload(header, filePath, parameter);
				if (realData.mListener == null) {
					Log.w(TAG, "Upload Service Listener is null. result = " + result);
				} else {
					try {
						JSONObject json = (JSONObject) new JSONTokener(result).nextValue();
						if (json.has("methodResponse")) {
							event = new ResponseEvent(ServiceBrokerLib.RESPONSE_OK, result);
						} else {
							event = new ResponseEvent(ServiceBrokerLib.RESPONSE_DATA_FORMAT_INVALID, result);
						}
					} catch (JSONException  e) {
						Log.e(TAG, "Upload result data can not be parsed. " + e.getMessage());
					}
				}
			} else if (ServiceBrokerLib.SERVICE_DOCUMENT.compareToIgnoreCase(realData.serviceID) == 0) {
				Log.w(TAG, "** Documnet View Service is deprecate.");
				// 문서뷰어 기능을 사용할 경우에도 SessionTime 을 연장해야 함. 
				mService.document(realData.header, "", null);
			} else if (ServiceBrokerLib.SERVICE_GET_INFO.compareToIgnoreCase(realData.serviceID) == 0) {
				Log.i(TAG, "** UserInfo Service ");
				Log.d(TAG, "Parameter : " + realData.parameter);
				String result = mService.getInfo(realData.parameter);
				Log.d(TAG, "Result : " + result);
				if (realData.mBrokerCallBack != null) {
					realData.mBrokerCallBack.onServiceBrokerResponse(result);
				}
			} else {
				if (realData.filePath.length() == 0 && realData.fileName.length() == 0) {
					// 기본 행정 서비스 요청 처리 
					Log.i(TAG, "** Administrative Service ");
					Log.d(TAG, "Host URL : " + realData.hostUrl);
					Log.d(TAG, "ServiceID : " + realData.serviceID);
					Log.d(TAG, "Parameter : " + realData.parameter);
					boolean largeParamSize = realData.parameter.length() > 20000;
					Log.d(TAG, "Parameter size is large : " + largeParamSize);
					
					ByteArrayInputStream byteIn = null;
					try {
						if (largeParamSize) {
							// 시작 통보를 위해 "" 리턴
							ByteArray beginByte = new ByteArray("".getBytes());
							mService.bigData(realData.header,
									realData.hostUrl,
									realData.serviceID,
									"bigData",
									beginByte,
									realData.timeout,
									realData.mServiceCallBack);
							
							byte[] byteData = realData.parameter.getBytes("UTF-8");
							byte[] byteBuffer = new byte[1024*10];
							byteIn = new ByteArrayInputStream(byteData);
							int readByteCnt = 0;
							ByteArray dataByte = null;
							while (( readByteCnt = byteIn.read(byteBuffer)) != -1) {
								if ( readByteCnt == byteBuffer.length) {
									dataByte = new ByteArray(byteBuffer);
									mService.bigData(realData.header, realData.hostUrl, realData.serviceID, realData.dataType, dataByte, realData.timeout, realData.mServiceCallBack);
								} else {
									byte[] lastBytes = new byte[readByteCnt];
									System.arraycopy(byteBuffer, 0, lastBytes, 0, readByteCnt);
									mService.bigData(realData.header, realData.hostUrl, realData.serviceID, realData.dataType, dataByte, realData.timeout, realData.mServiceCallBack);
								}
							}
							
							// 종료 통보를 위해 "" 리턴
							ByteArray endByte = new ByteArray("".getBytes());
							mService.bigData(realData.header, realData.hostUrl, realData.serviceID, realData.dataType, endByte, realData.timeout, realData.mServiceCallBack);
							
						} else {
							mService.data(realData.header, realData.hostUrl, realData.serviceID, realData.dataType, realData.parameter, realData.timeout, realData.mServiceCallBack);
						}
					} catch (IOException e) {
						Log.e(TAG, "Administrative Service process error : " + e.getMessage());
					} finally {
						if (byteIn != null) {
							byteIn.close();
						}
					}
					
				} else {
					Log.w(TAG, "** (Administrative) Upload Service is deprecate.");
					Log.d(TAG, "parameter : " + realData.parameter);
					Log.d(TAG, "FileName : " + realData.fileName);
					Log.d(TAG, "FilePath : " + realData.filePath);
					String result = "";
					//result = mService.uploadWithCB(header, serviceID, filePath, parameter, mServiceCallBack);
					Log.d(TAG, "Result : " + result);
					if (realData.mBrokerCallBack != null) {
						realData.mBrokerCallBack.onServiceBrokerResponse(result);
					}
				}
			}
		} catch (RemoteException e) {
			Log.e(TAG, "remote service call error : " + e.getMessage());
			event = new ResponseEvent(ServiceBrokerLib.REMOTE_SERVICE_EXCEPTION, e.getMessage());
		} catch (Exception e) {
			Log.e(TAG, "remote service call error : " + e.getMessage());
			event = new ResponseEvent(ServiceBrokerLib.UNDEFINED_EXCEPTION, e.getMessage());
		}
		
		if (realData.mListener != null && event != null) {
			realData.mListener.receive(event);
		}
		
		return null;
	}
	
	@Override
	protected void onPostExecute(String result) {
		Log.d(TAG, "ServiceBroker Done ...");
	}

}
