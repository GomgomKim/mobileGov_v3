package kr.go.mobile.mobp.iff.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.ParseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import kr.go.mobile.iff.service.HttpService.IResponseListener;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.iff.util.Utils;
import kr.go.mobile.mobp.iff.http.client.CustomHttpClient;
import kr.go.mobile.mobp.iff.http.client.HttpNewClient;
import kr.go.mobile.mobp.iff.http.impl.HttpGetRequest;
import kr.go.mobile.mobp.iff.http.impl.HttpMultiPartRequest;
import kr.go.mobile.mobp.iff.http.impl.HttpPostRequest;
import kr.go.mobile.mobp.iff.http.impl.HttpPutRequest;
import kr.go.mobile.mobp.iff.mobp.rpc.client.android.AbstractPayloadBuilder;
import kr.go.mobile.mobp.iff.mobp.rpc.client.android.JsonPayloadBuilder;
import kr.go.mobile.mobp.iff.mobp.rpc.util.RPCEnumUtils;
import kr.go.mobile.mobp.iff.util.HeaderUtil;
import kr.go.mobile.mobp.iff.util.SingleProgressDialog;

import kr.go.mobile.mobp.mff.lib.util.E2ESetting;
import kr.go.mobile.mobp.iff.R;

public class HttpManager {
	
	public static final int HTTP_POST = 1;
	public static final int HTTP_GET = 2;
	public static final int HTTP_PUT = 3;

	private static final String RESULT_SUCCESS = "1";

	private final String TAG = HttpManager.class.getSimpleName(); 
	private final boolean LOG_ENABLE = true;
	
	
	public static int downContentLength = 0;
	E2ESetting e2eSetting = new E2ESetting();
	Context context;

	private CustomHttpClient customHttpClient;

//	boolean isNull = false;

	public HttpManager(Context context) {
		// 통신 시 세션 타임 갱신
		e2eSetting.setSessionTime(System.currentTimeMillis());
//		if (e2eSetting.getUserId().equals("")) {
//			// userId 상태 체크 - 인증화면으로 이동
//			isNull = true;
//		}
		this.context = context;
	}
	
	/**
	 * 런처에서 VPN 터널링이 성공한 후 인증요청을 할 때 사용되는 함수이다. 
	 * 

	 * @param params
	 * @param listener
	 */
	public void reqCertAuth(String params, IResponseListener listener) {
		LogUtil.d(TAG, "reqCertAuth params : " + params);

		HttpRequest request = null;
		Bundle bundle = new Bundle();
		int what = IResponseListener.UNCERTIFIED;
		
		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context,  context.getResources().getInteger(R.integer.HttpConnectionTimeOut));

			request = new HttpPostRequest(context, E2ESetting.CERT_AUTH_SERVICE);
			request.setAgentDetail(new HeaderUtil(context).getHeader());
			request.setContentType("application/json;charset=utf-8");
			request.setBodyString(params);
			HttpResponse res = customHttpClient.excute(request);
			
			String data = res.getContentString();
			
			if (res.getStatusCode() == 200) {
				JSONObject resultJsonObject = new JSONObject(new JSONObject(data).getString("methodResponse"));
				String result = resultJsonObject.getString("result");

				if ("1".equals(result)) {
					what = IResponseListener.CERTIFICATION;
					bundle.putString("result", data);
				} else {
					String resultCode = resultJsonObject.getString("code");
					String resultMsg = resultJsonObject.getString("msg");
					bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
					
					LogUtil.e(TAG, "HTTP Manager :: resultMsg >> " + resultMsg);
				}
			} else {
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
			}

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.SERVER_ERROR_MESSAGE);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
		} catch (InterruptedIOException e) {
			LogUtil.e(TAG, "작업을 취소하였습니다.");
			return;
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.SERVER_ERROR_MESSAGE);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}
		}
		
		LogUtil.d(TAG, "getData onResult()  : what=" + what + ", bundle: " + bundle);
		listener.onResult(what, bundle);
	}

	public void reqData(int httpType, String hostUrl, String serviceId, ArrayList<String> list, int timeOut, IResponseListener listener, String header) {
		HttpRequest request = null;
		int what = 0;
		Bundle bundle = new Bundle();
		if (hostUrl.equals("__DEFAULT__")) {
			hostUrl = Utils.decrypt(context.getString(R.string.MagicMRSLicense), context.getString(R.string.agenturl));
		}
		LogUtil.d(TAG, String.format("getData for serviceApp httpType=%s, hostUrl=%s, serviceId=%s, list=%s, header=%s", 
				httpType, hostUrl, serviceId, list, header), LOG_ENABLE);

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, timeOut);


			if (httpType == HTTP_GET) {
				request = new HttpGetRequest(serviceId, hostUrl);
			} else if (httpType == HTTP_PUT) {
				request = new HttpPutRequest(serviceId, hostUrl);
			} else { // HTTP_POST
				request = new HttpPostRequest(serviceId, hostUrl);
			}

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}
			
			request.setAgentDetail(header);
			request.setContentType("application/x-www-form-urlencoded; charset=utf-8");
			
			if(list != null && list.size() > 0){
				request.setBodyList(list);
			}

			HttpResponse res = customHttpClient.excute(request);

			String data = res.getContentString();
			LogUtil.e(TAG, "HTTP Manager :: status >> " + res.getStatusCode());
			LogUtil.e(TAG, "HTTP Manager :: data >> " + data);


			if (res.getStatusCode() == 200) {
				JSONObject resultJsonObject = new JSONObject(new JSONObject(data).getString("methodResponse"));
				String result = resultJsonObject.getString("result");

				if ("1".equals(result)) {
					what = 0;
					bundle.putString("result", data);
				} else {
					what = -1;
					String resultCode = resultJsonObject.getString("code");
					String resultMsg = resultJsonObject.getString("msg");
					LogUtil.e(TAG, "HTTP Manager :: resultMsg >> " + resultMsg);
					bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
					
					report("getData", request, res);
				}
			} else {
				what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				
				report("getData", request, res);
			}


		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.SERVER_ERROR_MESSAGE);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.SERVER_ERROR_MESSAGE);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}
		}
		
		LogUtil.d(TAG, "getData onResult()  : what=" + what + ", bundle: " + bundle);
		listener.onResult(what, bundle);
	}
	
	public void reqDocumentData(String parameter, IResponseListener listener, String header) {
		
		LogUtil.d(TAG, String.format("reqDocumentData for serviceApp parameter=%s, header=%s", 
				parameter, header), LOG_ENABLE);
		
		int what = -1;
		Bundle bundle = new Bundle();

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, context.getResources().getInteger(R.integer.HttpConnectionTimeOut));

			HttpRequest request = new HttpPostRequest(context, E2ESetting.DOCUMENT_SERVICE);

			if (header == null || header.isEmpty()) {
				header = new HeaderUtil(context).getHeader();
			}

			ArrayList<String> list = new ArrayList<String>();
			int reqPos = -1;
			{
				JSONObject json = new JSONObject(parameter);
				Iterator<String> it = json.keys();
				while (it.hasNext()) {
					String key = (String) it.next();
					if (key.equals("page")) {
						reqPos = Integer.parseInt(json.getString(key));
					}
					StringBuilder sb = new StringBuilder();
					sb.append(key).append("=").append(json.getString(key));
					list.add(sb.toString());
				}
			}
			
			request.setAgentDetail(header);
			if(list != null && list.size() > 0){
				request.setBodyList(list);
			}

			HttpResponse res = customHttpClient.excute(request);

			InputStream data = res.getContentStream();

			// GET DOC_INFO 
			String pageCount = res.getMoPage();
			String pageConverting = res.getMoConverTing();
			String pageWidth = res.getMoPageWidth();
			String pageHeight = res.getMoPageHeight();
			String hashCode = res.getMoHashCode();
			String moErrCode = res.getMoErrCode();
			String moState = res.getMoState();
			String contentType = res.getContentType();
			
			{
				LogUtil.e(TAG, "HTTP Manager :: status >> " + res.getStatusCode());
				LogUtil.e(TAG, "HTTP Manager :: data >> " + data);
				LogUtil.e(TAG, "HTTP Manager :: getContentType >> " + contentType);
				LogUtil.e(TAG, "HTTP Manager :: pageWidth >> " + pageWidth);
				LogUtil.e(TAG, "HTTP Manager :: pageHeight >> " + pageHeight);
				LogUtil.e(TAG, "HTTP Manager :: MO_HASHCODE >> " + hashCode);
				LogUtil.e(TAG, "HTTP Manager :: MO_ERRCODE >> " + moErrCode);
				LogUtil.e(TAG, "HTTP Manager :: MO_STATE >> " + moState);
			}
			
			if (res.getStatusCode() == 200) {
				if (res.getContentType().startsWith("application/json")) {
					String respData = res.readResponseData(data);
					JSONObject resultJsonObject = new JSONObject(new JSONObject(respData).getString("methodResponse"));
					String resultCode = resultJsonObject.getString("code");
					String resultMsg = resultJsonObject.getString("msg");
					LogUtil.e(TAG, "HTTP Manager :: resultMsg >> " + resultMsg);
					int sidx = resultMsg.indexOf("(");
					int eidx = resultMsg.indexOf("(", sidx + 1);
					
					LogUtil.e(TAG, "HTTP Manager :: sidx >> " + sidx);
					LogUtil.e(TAG, "HTTP Manager :: eidx >> " + eidx);
					if (sidx == -1 || eidx == -1){
						bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
					} else {
						String realMsg;
						try {
							realMsg = resultMsg.substring(sidx + 1, eidx);
							LogUtil.e(TAG, "HTTP Manager :: realMsg >> " + realMsg);
							bundle.putString("result", realMsg);
						} catch (IndexOutOfBoundsException e) {
							bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
						}
					}
					bundle.putString("moerrcode", moErrCode);
					bundle.putString("mostate", moState);

					what = -1;

					report("getDocumentData", request, res);
										
				} else if (pageCount == null || pageCount.isEmpty()) {
					what = -1;
					bundle.putString("result", E2ESetting.PUBLIC_ERROR_MESSAGE);
					report("getDocumentData", request, res);
				} else {
					what = 0;
					JSONObject json = new JSONObject();
					List<Byte> resultData = readStream2List(data);
					
					json.put("position", reqPos);
					json.put("pageCount", pageCount);
					json.put("pageConverting", pageConverting);
					json.put("hashCode", hashCode);
					json.put("contentType", contentType);
					json.put("imageData", new JSONArray(resultData));
					
					bundle.putString("result", json.toString());
				}
			} else {
				what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				
				report("getDocumentData", request, res);
			}

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.SERVER_ERROR_MESSAGE);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.SERVER_ERROR_MESSAGE);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}
		}
		
		LogUtil.d(TAG, "getData onResult()  : what=" + what + ", bundle: ...");
		listener.onResult(what, bundle);
	}
	
	// 일반 통신 - Map - 서비스어플에서 호출 시에만 사용(service Broker)
	@Deprecated
	public void getData(int httpType, String hostUrl, String serviceId, ArrayList<String> list, int timeOut, Handler handler, String header) {
		HttpRequest request = null;
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();
		
		LogUtil.d(TAG, String.format("getData for serviceApp httpType=%s, hostUrl=%s, serviceId=%s, list=%s, header=%s", 
				httpType, hostUrl, serviceId, list, handler), LOG_ENABLE);

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, timeOut);


			if (httpType == HTTP_GET) {
				request = new HttpGetRequest(serviceId, hostUrl);
			} else if (httpType == HTTP_PUT) {
				request = new HttpPutRequest(serviceId, hostUrl);
			} else { // HTTP_POST
				request = new HttpPostRequest(serviceId, hostUrl);
			}

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}
			
			request.setAgentDetail(header);
			request.setContentType("application/x-www-form-urlencoded; charset=utf-8");
			
			if(list != null && list.size() > 0){
				request.setBodyList(list);
			}

			HttpResponse res = customHttpClient.excute(request);

			String data = res.getContentString();
			LogUtil.e(TAG, "HTTP Manager :: status >> " + res.getStatusCode());
			LogUtil.e(TAG, "HTTP Manager :: data >> " + data);


			if (res.getStatusCode() == 200) {
				JSONObject resultJsonObject = new JSONObject(new JSONObject(data).getString("methodResponse"));
				String result = resultJsonObject.getString("result");

				if ("1".equals(result)) {
					msg.what = 0;
					bundle.putString("result", data);
				} else {
					msg.what = -1;
					String resultCode = resultJsonObject.getString("code");
					String resultMsg = resultJsonObject.getString("msg");
					LogUtil.e(TAG, "HTTP Manager :: resultMsg >> " + resultMsg);
					// resultMsg = E2ESetting.PUBLIC_ERROR_MESSAGE;
					bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
					
					report("getData", request, res);
				}
			} else {
				msg.what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				
				report("getData", request, res);
			}

			msg.setData(bundle);
			handler.sendMessage(msg);

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}
		}
	}

	// 일반 통신 - String
	@Deprecated
	public void getData(int httpType, String serviceId, String params, Handler handler, String header) {
		LogUtil.e(TAG, "getData 일반 통신 serviceId : " + serviceId + ", params : " + params + ", header : " + header);

		HttpRequest request = null;
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();

		
		try {

			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context,  context.getResources().getInteger(R.integer.HttpConnectionTimeOut));

//			Utils.showProgressActivity(context);
			
			if (httpType == HTTP_GET) {
				request = new HttpGetRequest(context, serviceId);
			} else if (httpType == HTTP_PUT) {
				request = new HttpPutRequest(context, serviceId);
			} else {
				request = new HttpPostRequest(context, serviceId);
			}

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}
			request.setAgentDetail(header);
			request.setContentType("application/json;charset=utf-8");
			request.setBodyString(params);
			HttpResponse res = customHttpClient.excute(request);

			String data = res.getContentString();
			LogUtil.e(TAG, "HTTP 1 Manager :: status >> " + res.getStatusCode());
			LogUtil.e(TAG, "HTTP 1 Manager :: data >> " + data);

			if (res.getStatusCode() == 200) {
				JSONObject resultJsonObject = new JSONObject(new JSONObject(data).getString("methodResponse"));
				String result = resultJsonObject.getString("result");

				if ("1".equals(result)) {
					msg.what = 0;
					bundle.putString("result", data);
				} else {
					msg.what = -1;
					String resultCode = resultJsonObject.getString("code");
					String resultMsg = resultJsonObject.getString("msg");
					LogUtil.e(TAG, "HTTP Manager :: resultMsg >> " + resultMsg);
					// resultMsg = E2ESetting.PUBLIC_ERROR_MESSAGE;
					bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
				}
			} else {
				msg.what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
			}

			LogUtil.d(TAG, "getData sendMessage : " + msg);
			msg.setData(bundle);
			handler.sendMessage(msg);

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}

//			Utils.dismissProgressActivity(context);
		}
	}

	// 일반 통신 - Handler 없음
	@Deprecated
	public void getData(int httpType, String serviceId, String params, String header) {
		LogUtil.e(TAG, "getData 일반 통신 - Handler 없음 serviceId : " + serviceId + ", params : " + params + ", header : " + header);
		HttpRequest request = null;

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, context.getResources().getInteger(R.integer.HttpConnectionTimeOut));
			
			if (httpType == HTTP_GET) {
				request = new HttpGetRequest(context, serviceId);
			} else if (httpType == HTTP_PUT) {
				request = new HttpPutRequest(context, serviceId);
			} else {
				request = new HttpPostRequest(context, serviceId);
			}

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}
			request.setAgentDetail(header);
			request.setContentType("application/json;charset=utf-8");
			request.setBodyString(params);
			HttpResponse res = customHttpClient.excute(request);

			String data = res.getContentString();
			LogUtil.e(TAG, "HTTP Manager :: status >> " + res.getStatusCode());
			LogUtil.e(TAG, "HTTP Manager :: data >> " + data);
			

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}
		}
	}

	// 파일 업로드
	public String getUploadData(String serviceId, String filePath, ArrayList<String> list, Handler handler, String header) {
		LogUtil.d(TAG, "getUploadData serviceId on Launcher :: " + serviceId + ", header :: " + header);
		String resultMsg = "-1004";
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();

		HttpRequest request = null;

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, context.getResources().getInteger(R.integer.HttpConnectionTimeOut));
			
			long startTime = System.currentTimeMillis();
			request = new HttpMultiPartRequest(context, serviceId);

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}

			request.setAgentDetail(header);
			
			// 업로드할 로컬 파일 경로
			request.setAttachFilePath(filePath);
			
			// 업로드 경로
			if(list != null && list.size() > 0){
				request.setBodyList(list);
			}

			HttpResponse res = customHttpClient.excuteUpload(request);

			String data = res.getContentString();
			LogUtil.e(TAG, "HTTP Manager :: status >> " + res.getStatusCode());
			LogUtil.e(TAG, "HTTP Manager :: data >> " + data);

			if (res.getStatusCode() == 200) {
				resultMsg = data;

				JSONObject resultJsonObject = new JSONObject(new JSONObject(data).getString("methodResponse"));
				String result = resultJsonObject.getString("result");

				// 아래는 기존 파일업로드 서비스에서만 쓰도록 IF 문 조건 변경
				if ("1".equals(result) && serviceId.compareToIgnoreCase(E2ESetting.FILE_UPLOAD_SERVICE) == 0) {
					msg.what = 0;
					bundle.putString("result", data);
					msg.setData(bundle);
					handler.sendMessage(msg);
					// 업로드 결과 Report 전송
					long endTime = System.currentTimeMillis();
					File file = new File(filePath);
					long size = file.length();
					String fileName = FilenameUtils.getName(filePath);
					LogUtil.e(TAG, "파일명 :: " + fileName);
					String fileExt = FilenameUtils.getExtension(filePath);
					LogUtil.e(TAG, "확장자 :: " + fileExt);
					sendReport(E2ESetting.REPORT_FILE_UPLOAD, header, fileName, fileExt, size + "", startTime, endTime);
				}else if("1".equals(result) ){
					msg.what = 0;
					bundle.putString("result", data);
					msg.setData(bundle);
					handler.sendMessage(msg);
				}else{
					msg.what = -1;
					String resultCode = resultJsonObject.getString("code");
					resultMsg = resultJsonObject.getString("msg");
					LogUtil.e(TAG, "HTTP Manager :: resultMsg >> " + resultMsg);
					// resultMsg = E2ESetting.PUBLIC_ERROR_MESSAGE;
					bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
					msg.setData(bundle);
					handler.sendMessage(msg);
					
					report("getUploadData", request, res);
				}
			}else{
				msg.what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				msg.setData(bundle);
				handler.sendMessage(msg);
				
				report("getUploadData", request, res);
			}
			
			

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}

		}
		return resultMsg;
	}

	public void getZipList(String serviceId, ArrayList<String> list, Handler handler, String header) {

		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();
	

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, context.getResources().getInteger(R.integer.HttpConnectionTimeOut));
			
//			Utils.showProgressActivity(context);
			
			HttpRequest request = new HttpPostRequest(context, serviceId);

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}
			request.setAgentDetail(header);
//			request.setBodyMap(map);
			if(list != null && list.size() > 0){
				request.setBodyList(list);
			}

			HttpResponse res = customHttpClient.excute(request);
			String data = res.getContentString();

			LogUtil.e(TAG, "HTTP Manager :: status >> " + res.getStatusCode());
			LogUtil.e(TAG, "HTTP Manager :: data >> " + data);
			LogUtil.e(TAG, "HTTP Manager :: getContentType >> " + res.getContentType());

			if (res.getStatusCode() == 200) {
				JSONObject resultJsonObject = new JSONObject(new JSONObject(data).getString("methodResponse"));
				String result = resultJsonObject.getString("result");

				// 정상처리 되었을 경우.
				if (RESULT_SUCCESS.compareToIgnoreCase(result) == 0) {
					String strData = resultJsonObject.getString("data");
					msg.what = 1;
					bundle.putString("list", strData);
				} else {
					msg.what = -1;
					bundle.putString("msg", resultJsonObject.getString("msg"));
					
					report("getZipList", request, res);
				}
			} else {
				msg.what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				
				report("getZipList", request, res);
			}

			msg.setData(bundle);
			handler.sendMessage(msg);

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}

		}
	}

	// 문서 변환
	public void getDocumentData(String serviceId, ArrayList<String> list, int reqPos, Handler handler, String header) {
		LogUtil.d(TAG, "getDocumentData header :: " + header);
		LogUtil.d(TAG, "getDocumentData :: " + list.toString());

		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();


		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, context.getResources().getInteger(R.integer.HttpConnectionTimeOut));

			HttpRequest request = new HttpPostRequest(context, serviceId);

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}

			ArrayList<String> newList = new ArrayList<String>();
			
			String createDate = "";
			for(int i=0 ; i<list.size() ; i++){
				String value = list.get(i);				
				if(value.contains("createDate")){
					LogUtil.d(TAG, "getDocumentData value createDate :: " + value);
					
					String[] splitStr = value.split("=");
					if (splitStr.length > 1) {
						createDate = splitStr[1];
					}
					break;
				}
			}
			
			for(int i=0 ; i<list.size() ; i++){
				String value = list.get(i);				
				if(value.contains("fileName")){
					LogUtil.d(TAG, "getDocumentData value :: " + value);
					String fileName = value.substring(value.indexOf("fileName"), value.length());
					LogUtil.d(TAG, "getDocumentData fileName :: " + fileName);
					
					if(fileName.contains("&")){
						fileName = fileName.substring(0, fileName.indexOf("&"));
					}
					
					if(fileName.contains(".")){
						String ext = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
						if(ext.contains(";")){
							ext = ext.replaceAll(";", "");
						}
						if (newList.contains("ext=") == false) {
							if (createDate.length() > 0) {
								LogUtil.d(TAG, "getDocumentData ext and createDate :: " + ext + "::" + createDate);
								newList.add("ext=" + ext + "@" + createDate);
							} else {
								LogUtil.d(TAG, "getDocumentData ext :: " + ext);
								newList.add("ext=" + ext);
							}
						}
					}
				} else if (value.contains("createDate")) {
					continue;
				}

				newList.add(value);
			}
			
			LogUtil.d(TAG, "getDocumentData newList :: " + newList.toString());
			
			request.setAgentDetail(header);
//			request.setBodyMap(map);
			if(newList != null && newList.size() > 0){
				request.setBodyList(newList);
			}

			HttpResponse res = customHttpClient.excute(request);

			InputStream data = res.getContentStream();
			String pageCount = res.getMoPage();
			String pageConverting = res.getMoConverTing();
			String pageWidth = res.getMoPageWidth();
			String pageHeight = res.getMoPageHeight();
			
			String hashCode = res.getMoHashCode();
			
			String moErrCode = res.getMoErrCode();
			String moState = res.getMoState();
			
			String contentType = res.getContentType();

			LogUtil.e(TAG, "HTTP Manager :: status >> " + res.getStatusCode());
			LogUtil.e(TAG, "HTTP Manager :: data >> " + data);
			LogUtil.e(TAG, "HTTP Manager :: getContentType >> " + contentType);
			
			LogUtil.e(TAG, "HTTP Manager :: pageWidth >> " + pageWidth);
			LogUtil.e(TAG, "HTTP Manager :: pageHeight >> " + pageHeight);
			LogUtil.e(TAG, "HTTP Manager :: MO_HASHCODE >> " + hashCode);
			
			LogUtil.e(TAG, "HTTP Manager :: MO_ERRCODE >> " + moErrCode);
			LogUtil.e(TAG, "HTTP Manager :: MO_STATE >> " + moState);
			
			if (res.getStatusCode() == 200) {
				if (res.getContentType().startsWith("application/json")) {
					JSONObject resultJsonObject = new JSONObject(new JSONObject(res.readResponseData(data)).getString("methodResponse"));
					String resultCode = resultJsonObject.getString("code");
					String resultMsg = resultJsonObject.getString("msg");
					LogUtil.e(TAG, "HTTP Manager :: resultMsg >> " + resultMsg);
					// resultMsg = E2ESetting.PUBLIC_ERROR_MESSAGE;
					int sidx = resultMsg.indexOf("(");
					int eidx = resultMsg.indexOf("(", sidx + 1);
					
					LogUtil.e(TAG, "HTTP Manager :: sidx >> " + sidx);
					LogUtil.e(TAG, "HTTP Manager :: eidx >> " + eidx);
					if (sidx == -1 || eidx == -1){
						bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
					} else {
						String realMsg;
						try {
							realMsg = resultMsg.substring(sidx + 1, eidx);
							LogUtil.e(TAG, "HTTP Manager :: realMsg >> " + realMsg);
							bundle.putString("result", realMsg);
						} catch (IndexOutOfBoundsException e) {
							bundle.putString("result", resultMsg + " (errorCode : " + resultCode + ")");
						}
					}
					bundle.putString("moerrcode", moErrCode);
					bundle.putString("mostate", moState);

					msg.what = -1;

					report("getDocumentData", request, res);
										
				} else if (pageCount == null || "".equals(pageCount)) {
					msg.what = -1;
					bundle.putString("result", E2ESetting.PUBLIC_ERROR_MESSAGE);
					report("getDocumentData", request, res);
				} else {
					byte[] resultData = readFromStream(data);

					msg.what = 0;
					bundle.putInt("position", reqPos);
					bundle.putString("pageCount", pageCount);
					bundle.putString("pageConverting", pageConverting);
					bundle.putString("hashCode", hashCode);
					bundle.putString("contentType", contentType);
					bundle.putByteArray("imageData", resultData);

					LogUtil.e(TAG, "HTTP Manager :: hashCode >> " + hashCode);
				}
			} else {
				msg.what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				
				report("getDocumentData", request, res);
			}

			msg.setData(bundle);
			handler.sendMessage(msg);

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}

//			Utils.dismissProgressActivity(context);
		}
	}

	private void report(final String title, HttpRequest httpRequest, HttpResponse httpResponse) {
		// TODO !! 
		/*
		try {

			// 로그전송기능 사용이 아닐경우는 그냥 리턴
			if(context.getResources().getBoolean(R.bool.UseLogMail) == false)
				return;
			
			String requestServiceID = "RequestServiceID : " + httpRequest.getServiceId() + "\n\n";
			String requestServiceHeader = "RequestServiceHeader : " + httpRequest.getAgentDetail()+ "\n\n";
			String requestBodyString = "RequestBodyString : " + httpRequest.getBodyString() + "\n\n";
			String requestBodyList = "RequestBodyList : " + httpRequest.getBodyList().toString() + "\n\n\n\n";
			
			
			String responseStatusCode = "ResponseStatusCode : " + httpResponse.getStatusCode() + "\n\n";
			String responseHeader= "ResponseHeader : " + httpResponse.getResponseHeader() + "\n\n";
			String responseContentString = "ResponseContentString : " + httpResponse.getResponseData() + "\n\n";

			String emailContents = requestServiceID + requestServiceHeader + requestBodyString + requestBodyList
					+ responseStatusCode +  responseHeader + responseContentString;
			
			MOBPExceptionHandler.getInstance(context).sendEmail(title, emailContents);
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
	}
	
	// 문서변환 완료체크
	public void getDocumentChangeEndCheck(String serviceId, ArrayList<String> list, Handler handler, String header) {
		
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, context.getResources().getInteger(R.integer.HttpConnectionTimeOut));
			
			HttpRequest request = new HttpPostRequest(context, serviceId);

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}


			ArrayList<String> newList = new ArrayList<String>();
			
			for(int i=0 ; i<list.size() ; i++){
				String value = list.get(i);				
				if(value.contains("fileName")){
					String fileName = value.substring(value.indexOf("fileName"), value.length());
					
					if(fileName.contains("&")){
						fileName = fileName.substring(0, fileName.indexOf("&"));
					}
					
					if(fileName.contains(".")){
						String ext = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
						if(ext.contains(";")){
							ext = ext.replaceAll(";", "");
						}
						if (newList.contains("ext=") == false) {
							newList.add("ext=" + ext);
						}
					}
				}
				newList.add(value);
			}
			
			request.setAgentDetail(header);
			if(newList != null && newList.size() > 0){
				request.setBodyList(newList);
			}
			
			HttpResponse res = customHttpClient.excute(request);

			String data = res.getContentString();
			LogUtil.e(TAG, "HTTP Manager :: status >> " + res.getStatusCode());
			LogUtil.e(TAG, "HTTP Manager :: data >> " + data);

			
			if (res.getStatusCode() == 200) {
				// 서비스어플에서의 호출이므로 result체크 제외
				String pageCount = res.getMoPage();
				String pageConverting = res.getMoConverTing();

				LogUtil.e(TAG, "HTTP Manager :: pageCount >> " + pageCount);
				LogUtil.e(TAG, "HTTP Manager :: pageConverting >> " + pageConverting);

				if (pageCount == null || "".equals(pageCount) || pageConverting == null || "".equals(pageConverting)) {
					msg.what = -1;
					bundle.putString("result", E2ESetting.PUBLIC_ERROR_MESSAGE);
					report("getDocumentChangeEndCheck", request, res);
				} else {
					msg.what = 0;
					bundle.putString("pageCount", pageCount);
					bundle.putString("pageConverting", pageConverting);
				}

			} else {
				msg.what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				report("getDocumentChangeEndCheck", request, res);
			}

			msg.setData(bundle);
			handler.sendMessage(msg);

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} finally {
			if (customHttpClient != null) {
				customHttpClient.close();
			}

		}
	}

	// 파일 다운로드
	public void appDownLoad(String serviceId, ArrayList<String> list, Handler handler, String file, SingleProgressDialog downProgress, String header) {
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();


		InputStream data = null;
		FileOutputStream fos = null;

		long startTime = System.currentTimeMillis();

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, context.getResources().getInteger(R.integer.HttpConnectionTimeOut));
			
			HttpRequest request = new HttpPostRequest(context, serviceId);

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}
			request.setAgentDetail(header);
//			request.setBodyMap(map);

			if (list != null && list.size() > 0) {
				request.setBodyList(list);
			}

			HttpResponse res = customHttpClient.excute(request);
			String resultValue = res.getDownloadStream(file, downProgress, handler);
						

			if (resultValue != null && !"".equals(resultValue)) {
				msg.what = 0;
				msg.setData(bundle);
				handler.sendMessage(msg);

				// 다운로드 결과 Report 전송
				long endTime = System.currentTimeMillis();
				String fileName = "";
				String fileExt = "";

				String[] contentDisposition = resultValue.split("filename=");
				if (contentDisposition.length > 1) {
					fileName = contentDisposition[1].replaceAll("\"", "").replaceAll("'", "");
					LogUtil.e(TAG, "파일명 :: " + fileName);
					fileExt = FilenameUtils.getExtension(fileName);
					LogUtil.e(TAG, "확장자 :: " + fileExt);
				}
				sendReport(E2ESetting.REPORT_FILE_DOWNLOAD, header, fileName, fileExt, downContentLength + "", startTime, endTime);
			} else {
				msg.what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				msg.setData(bundle);
				handler.sendMessage(msg);
				
				report("appDownLoad", request, res);
			}

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} finally {

			try {
				if (data != null) {
					data.close();
					data = null;
				}
				if (fos != null) {
					fos.close();
					fos = null;
				}

				if (customHttpClient != null) {
					customHttpClient.close();
				}
			} catch (IOException ex) {
				LogUtil.e(TAG, "", ex);
			}

			if (downProgress != null && downProgress.isShowing()) {
				downProgress.cancel();
			}

			// Utils.dismissProgressActivity(context);
		}
	}
	
	public void appDownLoad(String serviceId, ArrayList<String> list, Handler handler, String file, final String appSize, SingleProgressDialog downProgress, String header) {
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();


		InputStream data = null;
		FileOutputStream fos = null;

		long startTime = System.currentTimeMillis();

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, context.getResources().getInteger(R.integer.HttpConnectionTimeOut));
			
			HttpRequest request = new HttpPostRequest(context, serviceId);

			if (header == null || "".equals(header)) {
				header = new HeaderUtil(context).getHeader();
			}
			request.setAgentDetail(header);
//			request.setBodyMap(map);

			if (list != null && list.size() > 0) {
				request.setBodyList(list);
			}

			HttpResponse res = customHttpClient.excute(request);
			String resultValue = res.getDownloadStream(file, downProgress, handler);

			if (resultValue != null && !"".equals(resultValue)) {
				msg.what = 0;
				msg.setData(bundle);
				handler.sendMessage(msg);

				// 다운로드 결과 Report 전송
				long endTime = System.currentTimeMillis();
				String fileName = "";
				String fileExt = "";

				String[] contentDisposition = resultValue.split("filename=");
				if (contentDisposition.length > 1) {
					fileName = contentDisposition[1].replaceAll("\"", "").replaceAll("'", "");
					LogUtil.e(TAG, "파일명 :: " + fileName);
					fileExt = FilenameUtils.getExtension(fileName);
					LogUtil.e(TAG, "확장자 :: " + fileExt);
				}
				sendReport(E2ESetting.REPORT_FILE_DOWNLOAD, header, fileName, fileExt, downContentLength + "", startTime, endTime);
			} else {
				msg.what = -1;
				bundle.putString("result", E2ESetting.DATA_ERROR_MESSAGE);
				msg.setData(bundle);
				handler.sendMessage(msg);
				
				report("appDownLoad", request, res);
			}

		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.SERVER_ERROR_MESSAGE, handler);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
			exception(E2ESetting.DATA_ERROR_MESSAGE, handler);
		} finally {

			try {
				if (data != null) {
					data.close();
					data = null;
				}
				if (fos != null) {
					fos.close();
					fos = null;
				}

				if (customHttpClient != null) {
					customHttpClient.close();
				}
			} catch (IOException ex) {
				LogUtil.e(TAG, "", ex);
			}

			if (downProgress != null && downProgress.isShowing()) {
				downProgress.cancel();
			}

			// Utils.dismissProgressActivity(context);
		}
	}

	// 이미지 다운로드
	public File appDownLoad(String serviceId, ArrayList<String> list, File file) {
//		if (isNull) {
//			return null;
//		}

		InputStream data = null;

		try {
			customHttpClient = new HttpNewClient();
			customHttpClient.createConnection(context, context.getResources().getInteger(R.integer.HttpConnectionTimeOut));
			
			HttpRequest request = new HttpPostRequest(context, serviceId);

			request.setAgentDetail(new HeaderUtil(context).getHeader());
//			request.setBodyMap(map);

			if (list != null && list.size() > 0) {
				request.setBodyList(list);
			}

			HttpResponse res = customHttpClient.excute(request);
			data = res.getContentStream();

			LogUtil.e(TAG, "HTTP Manager :: status >> " + res.getStatusCode());
			LogUtil.e(TAG, "HTTP Manager :: ContentLength >> " + res.getContentLength());

			if (res.getContentType().startsWith("application/json")) {
				JSONObject resultJsonObject = new JSONObject(new JSONObject(res.readResponseData(data)).getString("methodResponse"));
				String resultCode = resultJsonObject.getString("code");
				String resultMsg = resultJsonObject.getString("msg");
				LogUtil.e(TAG, "이미지 다운:: " + resultMsg);
			}

			if (res.getStatusCode() == 200) {
				FileOutputStream kFileOutput = new FileOutputStream(file);
				byte[] buffer = new byte[1024];
				int bufferLength = 0;

				while ((bufferLength = data.read(buffer)) > 0) {
					kFileOutput.write(buffer, 0, bufferLength);
				}
				kFileOutput.close();
			}
		} catch (SocketTimeoutException e) {
			LogUtil.e(TAG, "", e);
		} catch (UnsupportedEncodingException e) {
			LogUtil.e(TAG, "", e);
		} catch (ParseException e) {
			LogUtil.e(TAG, "", e);
		} catch (ConnectTimeoutException e) {
			LogUtil.e(TAG, "", e);
		} catch (IOException e) {
			LogUtil.e(TAG, "", e);
		} catch (Exception e) {
			LogUtil.e(TAG, "", e);
		} finally {
			try {
				if (data != null) {
					data.close();
					data = null;
				}

				if (customHttpClient != null) {
					customHttpClient.close();
				}
			} catch (IOException ex) {
				LogUtil.e(TAG, "", ex);
			}

			// Utils.dismissProgressActivity(context);
		}

		return file;
	}

	private void exception(String message, Handler handler) {

		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();

		msg.what = -1;
		bundle.putString("result", message);
		msg.setData(bundle);
		handler.sendMessage(msg);
	}

	// InputStream -> byte[]
	private static byte[] readFromStream(InputStream pInputStream) {
		if (pInputStream == null) {
			return null;
		}

		int lBufferSize = 1024;
		byte[] lByteBuffer = new byte[lBufferSize];

		int lBytesRead = 0;
		int lTotbytesRead = 0;
		int lCount = 0;

		ByteArrayOutputStream lByteArrayOutputStream = new ByteArrayOutputStream(lBufferSize * 2);

		try {
			while ((lBytesRead = pInputStream.read(lByteBuffer)) != -1) {
				lTotbytesRead += lBytesRead;
				lCount++;

				lByteArrayOutputStream.write(lByteBuffer, 0, lBytesRead);
			}
		} catch (Throwable e) {
			e.printStackTrace(System.out);
		}

		byte[] lDataBytes = lByteArrayOutputStream.toByteArray();
		return lDataBytes;
	}
	
	private static final List<Byte> readStream2List(InputStream pInputStream) {
		byte[] bytes = readFromStream(pInputStream);
		Byte[] t = new Byte[bytes.length];
		int idx = 0;
		for (byte b : bytes) {
			t[idx++] = new Byte(b);
		}
		
		return Arrays.asList(t);
	}

	// 다운로드 및 업로드 결과보고
	public void sendReport(final String serviceId, final String header, final String fileName, final String ext, final String size, final long startTime, final long endTime) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					long estimateTime = endTime - startTime;
					SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.KOREA);
					String start = timeFormat.format(new Date(startTime));
					String end = timeFormat.format(new Date(endTime));

					LogUtil.d(TAG, "fileName :: " + fileName);
					LogUtil.d(TAG, "ext :: " + ext);
					LogUtil.d(TAG, "size :: " + size);
					LogUtil.d(TAG, "startTime :: " + start);
					LogUtil.d(TAG, "endTime :: " + end);
					LogUtil.d(TAG, "estimateTime :: " + estimateTime);

					String data = "";
					AbstractPayloadBuilder requestPayload = new JsonPayloadBuilder(RPCEnumUtils.DataType.REQUEST);
					List<Object> lo = new ArrayList<Object>();
					Map map = new HashMap();
					map.put("fileName", fileName);
					map.put("ext", ext);
					map.put("size", size);
					map.put("startTime", start);
					map.put("endTime", end);
					map.put("estimateTime", estimateTime + "");
					lo.add(map);
					requestPayload.setParams(lo);
					data = requestPayload.build();

					getData(HttpManager.HTTP_POST, serviceId, data, header);
				} catch (Exception e) {
					LogUtil.e(TAG, "", e);
				}
			}
		}).start();
	}
	
	public static ArrayList<String> setParameter(final String parameter) {
		
		ArrayList<String> list = new ArrayList<String>();

		if (parameter.indexOf(";") != -1) {
			String[] paramArray = parameter.split(";");
			for (String param : paramArray) {
				String[] paramArray2 = param.split("&");
				for (String param2 : paramArray2) {
					String[] keyValue = param2.split("=");
					if (keyValue.length > 1) {
						String key = keyValue[0];
						String value = keyValue[1];
						list.add(key + "=" + value);
					} else if (keyValue.length > 0) {
						String key = keyValue[0];
						list.add(key + "=" + "");
					}
				}
			}
		} else if (parameter.indexOf("&") != -1) {
			String[] paramArray = parameter.split("&");
			for (String param : paramArray) {
				String[] keyValue = param.split("=");
				if (keyValue.length > 1) {
					String key = keyValue[0];
					String value = keyValue[1];
					list.add(key + "=" + value);
				} else if (keyValue.length > 0) {
					String key = keyValue[0];
					list.add(key + "=" + "");
				}
			}
		} else if (parameter.indexOf("=") != -1) {
			// 구분자 없이 하나의 파라메테 일 경우
			String[] keyValue = parameter.split("=");
			if (keyValue.length > 1) {
				String key = keyValue[0];
				String value = keyValue[1];
				list.add(key + "=" + value);
			} else if (keyValue.length > 0) {
				String key = keyValue[0];
				list.add(key + "=" + "");
			}
		}

		for (int i = 0; i < list.size(); i++) {
			LogUtil.d(Utils.class, "SSO DATA get ::: " + list.get(i));
		}
		
		return list;
	} 
}