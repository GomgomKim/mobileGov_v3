package kr.go.mobile.iff.service;

import java.io.ByteArrayOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sds.mobile.servicebrokerLib.aidl.ByteArray;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteService;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceCallback;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteServiceExitCallback;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.iff.util.Utils;
import kr.go.mobile.mobp.iff.http.HttpManager;
import kr.go.mobile.mobp.iff.util.HeaderUtil;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;
import kr.go.mobile.mobp.iff.R;

/**
 * 행정앱으로부터 전달받은 요청 정보를 중계서버로 전달하는 HTTP Service 이다.<br> 
 *  기존 런처의 코드를 그대로 가지고 옴. 
 *  
 * @author 윤기현
 *
 */
public class HttpService extends Service {
	/*
	@Deprecated
	private final RemoteCallbackList<IRemoteServiceCallback> callbacks = new RemoteCallbackList<IRemoteServiceCallback>();
	@Deprecated
	private StringBuffer bigData = new StringBuffer();
	@Deprecated
	private String strFileExt = "";
	@Deprecated
	private ByteArrayOutputStream lByteArrayOutputStream;
	@Deprecated
	private byte[] lDataBytes;
	@Deprecated
	private String encodeUrl = "";
	@Deprecated
	public static byte[] imageData;
	@Deprecated
	public static IRemoteServiceExitCallback mRemoteSVCExitCB;
	@Deprecated
	private E2ESetting e2eSetting = new E2ESetting();
	*/
	
	private Context context;
	
	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
	}

	@Override
	public IBinder onBind(Intent intent) {
		LogUtil.w(getClass(), "HttpService 도착");
		return mRSStub;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	// yoongi 서비스 브로커를 이용하여 응답을 받을 리스너를 정의함. 
	public interface IResponseListener {
		public final static int CERTIFICATION = 1;
		public final static int UNCERTIFIED = 0;
		public void onResult(int code, Bundle bundle);
	}
	
	IRemoteService.Stub mRSStub = new IRemoteService.Stub() {
		
		private ByteArrayOutputStream byteOut = null; 
		private byte[] buffer = null;

		@Override
		public boolean bigData(final String header, final String hostUrl, final String serviceId, final String dataType, final ByteArray param, final int timeOut, final IRemoteServiceCallback callback) throws RemoteException {
			LogUtil.w(getClass(), "request from \'APP\' (bigData) >>>>> ");
			
			byte[] inByteBuffer = param.getBytes();
			
			if("bigData".equals(dataType)) {
				LogUtil.d(getClass(), "** request data length is big. - init");
				if (byteOut != null) {
					try {
						byteOut.close();
					} catch (Exception e) {
					}
				}
				byteOut = new ByteArrayOutputStream();
				buffer = null;
				
			} else if (inByteBuffer == null || "".equals(new String(inByteBuffer))) {
				LogUtil.d(getClass(), "** request data length is big. - send");
				if (Utils.checkNetwork(context)) {	
					String parameter = new String(buffer).trim();
					
					LogUtil.d(getClass(), " - hostUrl :: " + hostUrl);
					LogUtil.d(getClass(), " - header :: " + header);
					LogUtil.d(getClass(), " - serviceId :: " + serviceId);
					LogUtil.d(getClass(), " - dataType :: " + dataType);
					LogUtil.d(getClass(), " - parameters :: " + parameter);
					
					HttpTask task = new HttpTask(context, 
							HttpManager.HTTP_POST, 
							new HeaderUtil(context).getServiceHeader(header), 
							hostUrl, 
							serviceId, 
							HttpManager.setParameter(parameter),
							timeOut, 
							callback);
					
					reset();
					AsyncTask.execute(task);

				} else {
					LogUtil.e(getClass(), "network state disconnection");
					callback.fail(E2ESetting.NETWORK_ERROR_CODE, E2ESetting.NETWORK_ERROR_MESSAGE);
				}
			} else {
				LogUtil.d(getClass(), "** request data length is big. - load");
				byteOut.write(inByteBuffer, 0, inByteBuffer.length);
				buffer = byteOut.toByteArray();
				LogUtil.d(getClass(), "size >>>>>> :: " + buffer.length);
			}
			return true;

		};
		
		@Override
		public boolean data(String header, final String hostUrl, final String serviceId, final String dataType, final String parameter, final int timeOut, final IRemoteServiceCallback callback) throws RemoteException {
			LogUtil.w(getClass(), "request from \'APP\' (data) >>>>> ");
			
			if(Utils.checkNetwork(context)){
				LogUtil.d(getClass(), " - hostUrl :: " + hostUrl);
				LogUtil.d(getClass(), " - header :: " + header);
				LogUtil.d(getClass(), " - serviceId :: " + serviceId);
				LogUtil.d(getClass(), " - dataType :: " + dataType);
				LogUtil.d(getClass(), " - parameters :: " + parameter);
				
				HttpTask task = new HttpTask(context, 
						HttpManager.HTTP_POST, 
						new HeaderUtil(context).getServiceHeader(header), 
						hostUrl, 
						serviceId, 
						HttpManager.setParameter(parameter),
						timeOut, 
						callback);
				
				reset();
				AsyncTask.execute(task);
			}else{
				LogUtil.e(getClass(), "network state disconnection");
				callback.fail(E2ESetting.NETWORK_ERROR_CODE, getString(R.string.try_again_later_network_instability));
			}
			return true;
		}
		

		@Override
		public String getInfo(String list) throws RemoteException {
			reset();
			LogUtil.e(getClass(), "SSO get Info >>>>  : " + list);
			String result = "";
			
			try {
				JSONArray reqJsonarray = (JSONArray)(new JSONTokener(list)).nextValue();
				JSONArray jArray = new JSONArray();
				
	        	for(int i = 0; i<reqJsonarray.length(); i++){
	        		
	        		String key = reqJsonarray.get(i).toString();
	        		String value = SignleSignOn.getInstance().getInfo(key);
	        		JSONObject jobj = new JSONObject();
	        		jobj.put(key, value);
	        		jArray.put(i, jobj);
	        	}

				result = jArray.toString();
				LogUtil.d(getClass(), "SSO load info <<<<  : " + jArray.toString());

			} catch (Exception e) {
				LogUtil.e(getClass(), "SSO load error : " + e.getMessage());
			}

			return result;
		}
		
		@Deprecated
		@Override
		public boolean document(String header, final String parameter, final IRemoteServiceCallback callback) throws RemoteException {			
			LogUtil.w(getClass(), "** document is deprecate. **");
			
			// TODO YOONGI / 2017-03-14 / 문서뷰어를 위한 서비스브로커 재구현.
			if(Utils.checkNetwork(context)){	
				/*
				final String nHeader = new HeaderUtil(context).getServiceHeader(header);
				Runnable task = new Runnable() {
					@Override
					public void run() {
						new HttpManager(context).reqDocumentData(parameter, new IResponseListener() {
								
							@Override
							public void onResult(int code, Bundle bundle) {
								try {
									switch (code) {
									case -1: // 실패
										int errorCode = E2ESetting.DATA_ERROR_CODE ;
										String errorMessage = E2ESetting.PUBLIC_ERROR_MESSAGE;
										try {
											errorCode = Integer.parseInt(bundle.getString("moerrorcode"));
										} catch (Exception e) {
										}
										try {
											errorMessage = URLDecoder.decode(bundle.getString("result"), "utf-8");
										} catch (Exception e) {
										}
										callback.fail(errorCode, errorMessage);
										break;
									case 0: // 성공
										String result = bundle.getString("result");
										if(result.length() > 20000){
											BufferedWriter out = null;
											try {
												String fileName = "" + System.currentTimeMillis();
												String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + E2ESetting.dataForderPath;
												String filePath = folderPath + File.separator + fileName;
												File folder = new File(folderPath);

												if( ! folder.exists()){
													folder.mkdirs();
												}
												folder.mkdirs();
												
												// TODO yoongi ContentProvider 를 이용하는건 어떨까 ?? 
												File file = new File(filePath);
												file.createNewFile();
												
												out = new BufferedWriter(new FileWriter(filePath));
												SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssss");
												Calendar cal = Calendar.getInstance();
										        String key = sdf.format(cal.getTime());
												result = new StringCryptoUtil().encryptAES(result, key);
//													result = AES256Cipher.getInstance().AES_Encode(result, key);
												out.write(result);
												callback.successBigData(key, filePath);
												LogUtil.d("@@@", "send big data ");
												return;
											} catch (IOException e) {
												LogUtil.e(getClass(), "", e);
												callback.fail(E2ESetting.DATA_ERROR_CODE, "");
											} finally {
												if (out != null) 
													out.close();
											}
										} else {
											callback.success(result);
										}
										break;
									default:
										break;
									}
								} catch (Exception e) {
									LogUtil.e(getClass(), "", e);
								}
							}
						}, nHeader);
					}
				};
				 */
				reset();
				// 2017-06-12 YOONGI 
				// 문서변환에 대한 요청은 문서뷰어 라이브러리가 직접 요청하도록 함. 이에 따라서 보안에이전트에서는 아래 함수를 호출하지 않는다. 
				// new Thread(task).start();
			} else {
				callback.fail(E2ESetting.NETWORK_ERROR_CODE, getString(R.string.try_again_later_network_instability));
			}
			return true;
		}
		
		@Deprecated
		@Override
		public boolean registerCallback(IRemoteServiceCallback callback) throws RemoteException {
			reset();
			LogUtil.w(getClass(), "** registerCallback is deprecate. **");
			return false;
		}

		@Override
		@Deprecated
		public boolean unregisterCallback(IRemoteServiceCallback callback) throws RemoteException {
			reset();
			LogUtil.w(getClass(), "** unregisterCallback is deprecate. **");
			return false;
		}

		@Deprecated
		@Override
		public boolean zipList(final String header, final String urlParameter, final IRemoteServiceCallback callback, final IRemoteServiceExitCallback exitCallback) throws RemoteException {
			reset();
			LogUtil.w(getClass(), "** zipList is deprecate. **");
			return true;
		}
		
		@Deprecated
		@Override
		public void download(String header, final String fileUrl, final String fileName, final IRemoteServiceCallback callback) throws RemoteException {
			reset();
			LogUtil.w(getClass(), "** download is deprecate. **");
		}

		@Deprecated
		@Override
		public boolean documentWithExitCB(String header, final String parameter, final IRemoteServiceCallback callback, final IRemoteServiceExitCallback exitcallback)
				throws RemoteException {
			reset();
			LogUtil.w(getClass(), "** documentWithExitCB is deprecate. **");
			return true;
		}

		@Deprecated
		@Override
		public String upload(String header, final String filePath, final String parameter) throws RemoteException {
			reset();
			LogUtil.w(getClass(), "** upload is deprecate. **");
			return "";
		}

		@Deprecated
		@Override
		public String uploadWithCB(String header, final String serviceID, final String filePath, final String parameter,  final IRemoteServiceCallback callback) throws RemoteException {
			reset();
			LogUtil.w(getClass(), "** uploadWithCB is deprecate. **");
			return "";
		}

		@Deprecated
		@Override
		public boolean displayMailViewer(final String header,
				final String hostUrl,final String serviceId,final String dataType,
				final String parameter, final int timeOut,
				final IRemoteServiceCallback callback, final IRemoteServiceExitCallback exitCallback) throws RemoteException {
			reset();
			LogUtil.w(getClass(), "** displayMailViewer is deprecate. **");
			return true;
		
		}
		
		private void reset() throws RemoteException {
			try {
				SessionManagerService.getInstence().reset();
			} catch (IllegalStateException e) {
				throw new RemoteException(e.getMessage());
			}
		}
	};
}