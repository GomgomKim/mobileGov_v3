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


class DocDownloadRunnable implements Runnable {

	private final static String TAG = DocDownloadRunnable.class.getSimpleName();
	
	public final static int TASK_BEGIN = 1;
	public final static int TASK_COMPLETED = 2;
	public final static int TASK_FAILED = 3;
	public final static int TASK_REPEAT_LOAD = 4;
	public final static int TASK_FINISH_LOAD = 5;
	
	private final TaskRunnableDownloadMethod mTask;
	
	interface TaskRunnableDownloadMethod {
		String getServiceHeader();
		String getParameter();
		
		// doc info - begin
		void setTotalPage(int totalPage);
		void setDocHashcode(String hashcode);
		void setByteBuffer(byte[] buffer);
		// doc info - end 
		
		public boolean cancel();
		
		void handleRequestState(int taskCompleted);
		void setDownloadThread(Thread currentThread);
		void setResultMessage(String string);
		void setConverted(boolean isConverted);
	}
	
	DocDownloadRunnable(DocRequestTask docRequestTask) {
		this.mTask = docRequestTask;
	}

	@Override
	public void run() {
		String header = mTask.getServiceHeader();
		String parameter = mTask.getParameter();
		Log.d(TAG, String.format("<< parameter=%s, header=%s", parameter, header));
		
		mTask.setDownloadThread(Thread.currentThread());
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		mTask.handleRequestState(TASK_BEGIN);
		
		HttpResponse resp = null;
		try {
			if (Thread.interrupted() || mTask.cancel()) {
				throw new InterruptedException();
			}
			resp = request(header, parameter);
			validResponse(resp);
			if (Thread.interrupted() || mTask.cancel()) {
				throw new InterruptedException();
			}
			
			// [YOONGI][ISSUE #01] 문서변환상태 정보 요청시 중계서버로부터 전달받은 데이터 오류
			// GET_INFO 를 요청하였을 때, MO_CONVERTING 값이 변환중일 때는 TRUE, 변환이 완료된 상태라면 FALSE 값이여야 한다. 
			// 하지만, 현재 중계서버로 부터 전달받는 데이터는 무조건 FALSE 값만 전달 받고 있다. 이슈를 해결하기 위하여 DocumentHandler 코드를 수정하였다.
			// !! 현재 변환 상태를 알기위해서는 문서 데이터를 요청할 때 받는 정보로 확인해야한다. 
			boolean isConverted = Boolean.parseBoolean(resp.getMoConverTing());
			int totalPage = -1;
			try {
				totalPage= Integer.parseInt(resp.getMoPage());
			} catch (NumberFormatException e) {}
			
			String docHashCode = resp.getMoHashCode();
			byte[] docData = resp.getContentBytes();

			if (Thread.interrupted() || mTask.cancel()) {
				throw new InterruptedException();
			}
			
			mTask.setTotalPage(totalPage);
			mTask.setDocHashcode(docHashCode);
			mTask.setByteBuffer(docData);
			mTask.setConverted(isConverted);
			
//			if (isConverted) {
//				mTask.handleRequestState(TASK_FINISH_LOAD);
//			} else {
//				mTask.handleRequestState(TASK_REPEAT_LOAD);
//			}
			
			mTask.handleRequestState(TASK_COMPLETED);
		} catch (DocDownloadException e) {
			setFailed(e);
		} catch (InterruptedException  e) {
			//setFailed(e);
		} catch (NullPointerException e) {
			
		} catch (Exception e) {
			setFailed(e);
		} finally {
			if (resp != null) {
				resp.clear();
			}
			mTask.setDownloadThread(null);
			Thread.interrupted();
		}
	}
	
	private void setFailed(Throwable t) {
		setFailed(t.getMessage(), t);
	}
	private void setFailed(String message, Throwable t) {
		Log.e(TAG, message, t);
		mTask.setResultMessage(message);
		mTask.handleRequestState(TASK_FAILED);
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
				} else if (resp.isImageContentType() == false) {
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
