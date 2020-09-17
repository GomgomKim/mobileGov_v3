package com.infrawaretech.docviewer.manager;

import com.infrawaretech.docviewer.utils.Log;
import com.infrawaretech.relayservice.HttpRequest;
import com.infrawaretech.relayservice.HttpResponse;
import com.infrawaretech.relayservice.client.RelayClientException;
import com.infrawaretech.relayservice.client.RelayClientImpl;
import com.infrawaretech.relayservice.impl.HttpPostRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.Iterator;

import kr.go.mobile.mobp.mff.lib.util.E2ESetting;

class ReqConvertStateRunnable implements Runnable {

	public static final String TAG = ReqConvertStateRunnable.class.getSimpleName();
	
	interface TaskRunnableConvertStateMethod {
		String getServiceHeader();
		String getParam();
		void setTotalPage(int totalPage);
		void setConverted(boolean isConverted);
		void handleState();
		void setThread(Thread currentThread);
		int getDelay();
		void recycle();
	}
	
	TaskRunnableConvertStateMethod mTask;
	
	public ReqConvertStateRunnable(TaskRunnableConvertStateMethod task) {
		this.mTask = task;
	}
	
	@Override
	public void run() {
		Log.d(TAG, "reqConvertState ...");
		mTask.setThread(Thread.currentThread());
		String header = mTask.getServiceHeader();
		String parameter = mTask.getParam();
		int delay = mTask.getDelay();
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				return;
			}
		}
		
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		
		HttpResponse resp = null;
		try {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			resp = request(header, parameter);
			validResponse(resp);
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			boolean isConverted = Boolean.parseBoolean(resp.getMoConverTing());
			int totalPage = -1;
			try {
				totalPage= Integer.parseInt(resp.getMoPage());
			} catch (NumberFormatException e) {}
			
			mTask.setTotalPage(totalPage);
			mTask.setConverted(isConverted);
			
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			mTask.handleState();
		} catch (DocDownloadException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (NullPointerException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (InterruptedException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		} finally {
			if (resp != null) {
				resp.clear();
			}
			mTask.recycle();
		}
	}
	
	private HttpResponse request(String header, String parameter) throws DocDownloadException {
		RelayClientImpl customHttpClient = null;
		HttpRequest request = null;
		try {
			request = new HttpPostRequest(E2ESetting.DOCUMENT_SERVICE);
			request.setAgentDetail(header);
			JSONObject json = new JSONObject(parameter);
			Iterator<?> it = json.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				StringBuilder sb = new StringBuilder();
				sb.append(key).append("=").append(json.getString(key));
				request.addBodyParam(sb.toString());
			}

			customHttpClient = new RelayClientImpl();
			customHttpClient.createConnection(60000 * 2);
			return customHttpClient.execute(request);
		} catch (RelayClientException e) {
			throw new DocDownloadException(e.getMessage(), e);
		} catch (MalformedURLException e) {
			throw new DocDownloadException(DocDownloadException.ERROR_MALFORMED_URL, e);
		} catch (Exception e) {
			throw new DocDownloadException(DocDownloadException.ERROR_DATA_PROC, e);
		} finally {
			if (request != null) {
				request.clear();
			}
		}
	}

	private void validResponse(HttpResponse resp) throws DocDownloadException {
		Log.d(TAG, "validate Response :: " + resp.toString());
		
		int statusCode = resp.getStatusCode();
		String pageCount = resp.getMoPage();
		
		switch (statusCode) {
		case HttpResponse.OK:
			try {
				if (resp.isApplicationContentType()) {
					String strResponse = resp.getContent();
					JSONObject jsonResponse = new JSONObject(strResponse);
					JSONObject jsonMethodResp = new JSONObject(jsonResponse.getString("methodResponse"));
					String strRespCode = jsonMethodResp.getString("code");
					String strRespMsg = jsonMethodResp.getString("msg");
					Log.d(TAG, " Response Message :: " + strRespMsg);
					
					int sidx = strRespMsg.indexOf("(");
					int eidx = strRespMsg.indexOf("(", sidx + 1);
					String errorMessage = strRespMsg + "(errorCode = " + strRespCode +")";
					if (sidx == -1 || eidx == -1) {
						throw new DocDownloadException(errorMessage);
					} else {
						try {
							errorMessage = strRespMsg.substring(sidx + 1, eidx);
							throw new DocDownloadException(errorMessage);
						} catch (IndexOutOfBoundsException e) {
							throw new DocDownloadException(errorMessage, e);
						}
					}
				} else if (resp.isTextContentType() == false) {
					throw new DocDownloadException(DocDownloadException.ERROR_MALFORMED_CONTENT_TYPE);
				} else if (pageCount == null || pageCount.isEmpty()) {
					throw new DocDownloadException(DocDownloadException.ERROR_NOT_READY_DOC_CONVERT);
				} else if (resp.getContentBytes() == null || resp.getContentBytes().length == 0) {
					throw new DocDownloadException(DocDownloadException.ERROR_NO_DATA);
				} else if (resp.getMoErrCode() != null) {
						
				} else if (resp.getMoState() != null) {
					
				}
			} catch (JSONException e) {
				throw new DocDownloadException(DocDownloadException.ERROR_DATA_PROC, e);
			} catch (DocDownloadException e) {
				throw e;
			} catch (Exception e) {
				throw new DocDownloadException(DocDownloadException.ERROR_DATA_PROC, e);
			}
			break;
		default:
			throw new DocDownloadException(DocDownloadException.ERROR_DOC_CONVERT_SERVER_FAILED);
		}
	}

}
