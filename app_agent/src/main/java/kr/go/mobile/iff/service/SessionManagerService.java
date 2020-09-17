package kr.go.mobile.iff.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import kr.go.mobile.iff.ServiceWaitHandler;
import kr.go.mobile.iff.exception.ExpiredSignException;
import kr.go.mobile.iff.service.HttpService.IResponseListener;
import kr.go.mobile.iff.util.AdminState;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.iff.util.Utils;
import kr.go.mobile.iff.util.Utils.TimeStamp;
import kr.go.mobile.iff.util.VpnCtlTask;
import kr.go.mobile.mobp.iff.http.HttpManager;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;
import kr.go.mobile.mobp.iff.R;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.sds.BizAppLauncher.gov.aidl.GovControllerType;

/**
 * 공통기반 서비스 초기화가 성공적으로 완료되면 공통기반 세션들을 관리하는 서비스 모듈이다.
 * 
 * @author 윤기현
 *
 */
public class SessionManagerService extends Service implements ISessionManagerService {

	public class Binder extends android.os.Binder {
		public ISessionManagerService getService() {
			return SessionManagerService.this;
		}
	}
	
	private final boolean LOG_ENABLE = true;
	
	public static final String SERVICE_NAME = SessionManagerService.class.getSimpleName();
	
	public static final String NOT_EXIST_SIGNED = "not.exist.signed";
	
	public static final String EXTRA_SIGNED_SUCCESS_TIME = "extra_auth_success_time";
	public static final String EXTRA_SIGNED_MAINTAIN_TIME = "extra_auth_maintain_time";
	public static final String EXTRA_SIGNED_BASE64 = "extra_signed_base64";
	public static final String EXTRA_USER_DN = "extra_user_dn";
	public static final String EXTRA_USER_DN_WITH_DEVICE_ID = "extra_user_dn_with_device_id";
	
	private static final int VPN_OVER_IDLE_TIME = 1;
	// 화면 전환시 VPN을 사용중인 패키지명이 삭제될 수 있다. 
	// 이 때, VPN이 즉시 끊어지는 것을 막기 위하여 REMAIN_TIME 을 설정한다.
	// FIXME 만약, 공통기반라이브러리에서 ALIVE_SERVICE 를 강제로 바인딩하면 이 기능은 불필요하다.
	private static final int VPN_REMAIN_TIME = 2;
	
	private static SessionManagerService mInstance;
	
	static SessionManagerService getInstence() throws IllegalStateException {
		if (mInstance == null) {
			throw new IllegalStateException("The SessionManagerService is not ready.");
		}
		return mInstance;
	}
	
	private final int AUTH_STATE_ELSE = 9;
	private final int AUTH_STATE_SUCCESS = 0;
		
	private Object mLock = new Object();
	
	private SignedInfo mSignedInfo;
	private VpnCtlTask mVpnCtl;

	private boolean mReqStartVpn = false;
	
	private String tmpPrevPkgName = null;

	private final Binder mBinder = new Binder();

	private Handler handlerVpnIdleTime = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case VPN_OVER_IDLE_TIME:
				if (mVpnCtl != null)
					LogUtil.d("VPN_IDLE", "IDLE TIME OVER :: VPN STOP", LOG_ENABLE);
					stopVPN();
				break;
			case VPN_REMAIN_TIME:
				synchronized (mLock) {
					if (mAlivePakcage.isEmpty()) {
						stopVPN();
					}
				}
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		String SECUWAY_SSL_SERVER = Utils.decrypt(getString(R.string.MagicMRSLicense), getString(R.string.SecuwaySSLServer));
		int SECUWAY_SSL_VERSION = getResources().getInteger(R.integer.SecuwaySSLVersion);
		
		mInstance = this;
		mVpnCtl = new VpnCtlTask(this, SECUWAY_SSL_SERVER, SECUWAY_SSL_VERSION) {
			String TAG = "SessionMAnagerService.VpnCtlTask";
			@Override
			protected void onVpnConnected() {
				TimeStamp.endTime("startVPN");
				LogUtil.d(TAG, "VpnConnected", LOG_ENABLE);
				
				sendVpnStatus(VPN_STATUS_CONNECTION);
				
				Utils.setEnableVpn(true);
				
				// 2016.11.11 VPN 터널링이 완료된 후 인증데이터를 요청해야함. 
				reqCertification(mSignedInfo.getSignedBase64(), mReqStartVpn);
				mReqStartVpn = false;
			}

			@Override
			protected void onVpnDisconnected() {
				LogUtil.d(TAG, "VpnDisconnected", LOG_ENABLE);
				synchronized (mLock) {
					if (!mAlivePakcage.isEmpty()) {
						mAlivePakcage.clear();
						LogUtil.d(TAG, "Kill all admin package... ");
						Intent intent = new Intent();
						intent.setAction(GovControllerType.ACTION_CONTROL);
						intent.putExtra(GovControllerType.EXTRA_TYPE, GovControllerType.TYPE_KILL);
						sendBroadcast(intent);
					}
				}
				
				Utils.setEnableVpn(false);
			}

			@Override
			protected void onVpnConnecting() {
				LogUtil.d(TAG, "VpnConnecting", LOG_ENABLE);
				changedVPNStatus(null);
				sendVpnStatus(VPN_STATUS_CONNECTING);
				Utils.setEnableVpn(false);
			}
			
			@Override
			protected void onResult(String msg) {
				LogUtil.d(getClass(), "VPN RESULT: " + msg);
				// VPN 연결 실패 시에도 서명값 제거 ? 
				// 제거를 안한 이유.. 토큰 타임아웃에 인하여 거부된 경우를 고려함. 
				mSignedInfo = null;  
				changedVPNStatus(msg);
			}

			@Override
			protected void onVpnServiceReady() {
				if (status() && mSignedInfo != null) {
					// 서비스가 종료되었다가 다시 살아났을 때는 VPN 연결이 정상적이고 mSignedInfo 도 존재하면
					// 인증 데이터를 재요청한다. 
					reqCertification(mSignedInfo.getSignedBase64(), mReqStartVpn);
					reset();
				}
			}

			
			// !! FIXME 2017.05.11 문서뷰어 연동 (행정앱이 VPN 연결시에만 동작하게 하기위함)
			private void sendVpnStatus(int status) {
				String ACTION_VPN_STATUS = "kr.go.mobile.docView.vpn.STATUS";
				String EXTRA_STATUS = "STATUS";
				Intent intent = new Intent(ACTION_VPN_STATUS);
				intent.putExtra(EXTRA_STATUS, status);
				
				sendBroadcast(intent);
			}
			
		};
	}
	
	@Override
	public void onDestroy() {
		LogUtil.d(getClass(), "onDestroy", LOG_ENABLE);
		if (mVpnCtl != null)
			mVpnCtl.close();
		super.onDestroy();
	}
	
	public int onStartCommand(Intent intent, int flag, int startId) {
		try {
			if (intent.getExtras() == null) {
				LogUtil.w(getClass(), "not extra data.", LOG_ENABLE);
			} else {
				if (mSignedInfo != null) {
					mSignedInfo = null;
				}
				LogUtil.d("TOM@@@", "signed info");
				mSignedInfo = new SignedInfo(intent);
				
				if (flag == START_FLAG_REDELIVERY ) {
					// 서비스가 재시작된 경우 아무 동작도 하지 않음. 
					LogUtil.d(getClass(), "restart SessionManagerService", LOG_ENABLE);
				} else {
					onChangedStatus(ISessionManagerEventListener.SIGN_EVENT, null);
				}
				LogUtil.d(getClass(), mSignedInfo.toString(), LOG_ENABLE);
			}
			// 서비스가 중단되면, 서비스를 재생성하고 서비스에 전달된 마지막 인텐트로 onStartCommand() 를 호출합니다.
			return START_REDELIVER_INTENT ;
		} catch (ExpiredSignException e) {
			handleExpiredSign();
		} catch (Exception e) {
			LogUtil.e(getClass(), "SessionManager 서비스를 실행할 수 없습니다." + e.getMessage());
		}
		return START_NOT_STICKY;
	}
	
	private void handleExpiredSign() {
		// 서비스 종료 및 GPKI 서명 재요청
		LogUtil.w(getClass(), "서명정보가 만료되었습니다");
		changedCertStatus(true, NOT_EXIST_SIGNED);
	}
	
	private void changedVPNStatus(String result) {
		Bundle data = new Bundle();
		data.putString(ISessionManagerEventListener.EXTRA_RESULT, result);
		onChangedStatus(ISessionManagerEventListener.VPN_EVENT, data);
	}
	
	
	public boolean existSigned() {
		try {
			if (mSignedInfo != null) {
				mSignedInfo.validAuth();
				return true;
			} 
		} catch (ExpiredSignException e) {
			mSignedInfo = null;
		}
		if (mVpnCtl.status()) {
			stopVPN();
		}
		return false;
	}
	
	@Override
	public void startVPN(final AdminState adminState) {
		try {
			mSignedInfo.validAuth();
			final String userDNwithDeviceID = mSignedInfo.getUserDNWithDeviceID();
			final String identity = userDNwithDeviceID + "|" + adminState.getTokens();
			mVpnCtl.start(identity, "");
			
			mReqStartVpn = true;
		} catch (ExpiredSignException e) {
			handleExpiredSign();
		}
	}
	
	@Override
	public boolean enableVpn() {
		// VPN 연결된 상태이고 서명값이 존재하면
		if (mVpnCtl.status() && mSignedInfo != null) {
			changedCertStatus(true, mSignedInfo.getUserDN());
			return true;
		}
		return false;
	}
	
	public void stopVPN() {
		tmpPrevPkgName = null;
		mVpnCtl.stop();
	}
	
	private Set<String> mAlivePakcage = new TreeSet<String>();
	
	public void addMonitorPackage(String pkgName) {
		LogUtil.d(getClass(), "addPackage: " + pkgName);
		synchronized (mLock) {
			TimeStamp.endTime("reconnect-wait");
			handlerVpnIdleTime.removeMessages(VPN_REMAIN_TIME);
			mAlivePakcage.add(pkgName);
		}
	}
	
	public void removeMonitorPackage(String pkgName) {
		removeMonitorPackage(pkgName, true);
	}
	
	@Override
	public void removeMonitorPackage(String pkgName, boolean immediately) {
		synchronized (mLock) {
			if (mAlivePakcage.isEmpty()) {
				return;
			}
			LogUtil.d(getClass(), "removePackage: " + pkgName);
			mAlivePakcage.remove(pkgName);
			
			if (mAlivePakcage.isEmpty()) {
				LogUtil.d(getClass(), "no alive package. (immediately =" + immediately  + ")");
				TimeStamp.startTime("reconnect-wait");
				if (immediately) {  
					tmpPrevPkgName = null;
				} else { // 정상종료되었기 때문에 실행중이던 pkgName을 남긴다. 
					tmpPrevPkgName = pkgName;
				}
				LogUtil.d(getClass(), "tmpPrevPkgName = " + tmpPrevPkgName);
				handlerVpnIdleTime.sendEmptyMessageDelayed(VPN_REMAIN_TIME, getResources().getInteger(R.integer.VpnRemainTimeSec) * 1000);
			} else {
				if (LOG_ENABLE) {
					Iterator<String> pk = mAlivePakcage.iterator();
					StringBuffer sb = new StringBuffer();
					while (pk.hasNext()) {
						sb.append(pk.next()).append("\n");
					}
					LogUtil.d(getClass(), "remainPackage: " + sb.toString());
				}
			}
			
			LogUtil.d(getClass(), "alive packages count = " + mAlivePakcage.size(), LOG_ENABLE);
		}
	}
	
	
	private void reqCertification(String signed, final boolean reqSendBR) {
		try {
			final JSONObject json = new JSONObject();
			json.put("reqType", "1");
			json.put("transactionId", Utils.genTransactionId());
			json.put("signedData", signed);

			Runnable task = new Runnable() {
				@Override
				public void run() {
					TimeStamp.startTime("reqCert");
					Utils.TimeStamp.startTime("req_ret_time_test");
					// 2016.11.14 윤기현 - VPN 터널링 후 사용자 인증을 요청하고 리턴받는 구조를 Listener 구조로 변경. 런처에서만 사용한다. 
					new HttpManager(SessionManagerService.this).reqCertAuth(
							Utils.buildRequestPayload(json), 
							getListener(reqSendBR)
						); 
				}
			};
			AsyncTask.execute(task);
		} catch (JSONException e) {
			String msg = "인증 요청 중 에러가 발생하였습니다";
			LogUtil.e(getClass(), msg, e);
			changedCertStatus(false, msg);
		} catch (Exception e) {
			String msg = "인증 요청 중 에러가 발생하였습니다";
			LogUtil.e(getClass(), msg, e);
			changedCertStatus(false, msg);
		}
	}
	
	private final IResponseListener getListener(final boolean reqSendBR) {
		return new IResponseListener() {
			
			@Override
			public void onResult(int what, Bundle bd) {
				String result = bd.getString("result") == null ? "NULL" : bd.getString("result");
				
				boolean isCert = false;
				String resultMsg;
				
				LogUtil.d(getClass(), "RESULT: " + result );
				switch (what) {
				case CERTIFICATION:
					if (result.equals("NULL")) {
						changedCertStatus(false, "result is null");
						return;
					} else {
						try {
							JSONObject resultJsonObject = new JSONObject(new JSONObject(result).getString("methodResponse"));

							String jsonData = resultJsonObject.getString("data");
							JSONObject jsonObj = new JSONObject(jsonData);
							LogUtil.d(getClass(), "CertHandler jsonData :: " + jsonData, LOG_ENABLE);
							
							String verifyState = jsonObj.get("verifyState").toString();

							// 가상화 전환시 해당 리소스가 존재하면 에러가 발생하여 하드코딩으로 변환
							// String[] ldapArray = getResources().getStringArray(R.array.ldap_state);
							// String[] certArray = getResources().getStringArray(R.array.cert_state);

							String[] ldapArray = {
							        "검증성공",
                                    "폐지된 인증서입니다.",
                                    "만료된 인증서입니다.",
                                    "유효하지 않은 인증서입니다.",
                                    "","","","","",
                                    "인증서 확인에 실패하였습니다."};
							String[] certArray = {
							        "검증성공",
                                    "조회 불가능한 LDAP정보입니다.",
                                    "", "", "","","","","",
                                    "LDAP 인증실패하였습니다."};
							
							String verifyStateCert = jsonObj.get("verifyStateCert").toString();
							String verifyStateLDAP = jsonObj.get("verifyStateLDAP").toString();
							
							/*verifyStateCert가 NULL 로 오는 경우(비정상적인 경우)가 있을 수 있으므로
							 * certStateValued의 초기 상태값을 AUTH_STATE_ELSE=9 로 설정
							 * 0으로 초기값 선택시  ldapArray, certArray의 0번 화면 출력 메세지가 검증 성공이기 때문에
							 * 메세지가 잘못 출력될 가능성이 있음
							 */
							int verifyStateValue = AUTH_STATE_ELSE;
							int certStateValue = AUTH_STATE_ELSE;
							int ldapStateValue = AUTH_STATE_ELSE;
							
							try{
								verifyStateValue = Integer.valueOf(verifyState);
								certStateValue = Integer.valueOf(verifyStateCert);
								ldapStateValue = Integer.valueOf(verifyStateLDAP);
							}catch(NullPointerException e){
								LogUtil.e(getClass(), "", e );
							}catch(NumberFormatException e){
								LogUtil.e(getClass(), "", e );
							}
							
							if (certStateValue < AUTH_STATE_SUCCESS 
									|| certStateValue >= certArray.length)
								certStateValue = AUTH_STATE_ELSE;
							
							if (ldapStateValue < AUTH_STATE_SUCCESS 
									|| ldapStateValue >= ldapArray.length)
								ldapStateValue = AUTH_STATE_ELSE;
							
							/*
							String verifyPolicy = jsonObj.get("verifyPolicy").toString();
							String verifyStateMDM = jsonObj.get("verifyStateMDM").toString();
							if(verifyStateLDAP == null || verifyStateLDAP.length() <= 0){
								verifyStateLDAP = "0";
							}
							*/
							
							if (verifyStateValue == AUTH_STATE_SUCCESS) {
								String userId = jsonObj.get("cn").toString();
								if(userId == null || userId.length() <= 0){
									changedCertStatus(false, "인증이 실패하였습니다.");
									return;
								}
								TimeStamp.endTime("reqCert");
								
								String ouName = jsonObj.get("ou").toString();
								String ouCode = jsonObj.get("oucode").toString();
								String departmentName = jsonObj.get("companyName").toString();
								String departmentCode = jsonObj.get("topOuCode").toString();
								String nickName = jsonObj.get("displayName").toString();
								
								//////// 기존 코드를 유지하기 위하여 E2ESetting 객체에 설정 - BEGIN
								E2ESetting e2eSetting = new E2ESetting();
								e2eSetting.setUserId(userId);
								e2eSetting.setOfficeCode(ouCode);
								e2eSetting.setOfficeName(ouName);
								e2eSetting.setDepartmentName(departmentName);
								e2eSetting.setDepartmentCode(departmentCode);
								e2eSetting.setNickName(nickName);
								//////// 기존 코드를 유지하기 위하여 E2ESetting 객체에 설정 - END
								
								SignleSignOn.getInstance().init(
										mSignedInfo.getUserDN(), 
										userId, nickName, 
										ouCode, ouName, 
										departmentName, departmentCode);
								
								isCert = true;
								resultMsg = mSignedInfo.getUserDN();

								Utils.TimeStamp.endTime("req_ret_time_test");

								SimpleDateFormat mFormat = new SimpleDateFormat("hh:mm:ss.SSS");
								String curTime = mFormat.format(new Date(System.currentTimeMillis()));
								Log.d("tom_test", "Time : end - "+curTime);


							} else if (certStateValue != AUTH_STATE_SUCCESS) { // 인증서 체크
								resultMsg = certArray[certStateValue];
							} else if (ldapStateValue != AUTH_STATE_SUCCESS) { // LDAP 체크
								resultMsg = ldapArray[ldapStateValue];
							} else {
								resultMsg = "인증 실패";
							}
						} catch (JSONException e) {
							resultMsg = "인증 데이터 파싱 에러";
							LogUtil.e(getClass(), "인증 데이터 파싱 에러 " + e.getMessage(), e );
						}
					}
					break;
				case UNCERTIFIED:
				default:
					LogUtil.e(getClass(), "인증에 실패하였습니다. (result=" + result +")");
					resultMsg = result;
				}
				
				if (reqSendBR)
					changedCertStatus(isCert, resultMsg);
			}
		};
	}

	private void changedCertStatus(boolean isCert, String result) { 
		
		Bundle data = new Bundle();
		data.putBoolean(ISessionManagerEventListener.EXTRA_IS_CERT, isCert);
		
		if (isCert) {
			data.putString(ISessionManagerEventListener.EXTRA_SIGNED_DN, result);
		} else {
			// 인증 실패시 
			stopVPN(); // VPN 연결 종료.
			mSignedInfo = null;  
			data.putString(ISessionManagerEventListener.EXTRA_RESULT, result);
		}
		
		onChangedStatus(ISessionManagerEventListener.CERT_EVENT, data);
	}
	
	private void onChangedStatus(int event, Bundle data) {
		if (ServiceWaitHandler.getInstance().existListener()) {
			Handler H = ServiceWaitHandler.getInstance();
			Message m = H.obtainMessage(ServiceWaitHandler.MESSAGE_CATEGORY_LOADING_ACTIVITY, 
					ServiceWaitHandler.CHANGE_SERVICE_EVENT, event, data);
			
			H.sendMessage(m);
		}
	}
	
	void reset() throws IllegalStateException {
		handlerVpnIdleTime.removeMessages(VPN_OVER_IDLE_TIME);
		// VPN을 사용하고 있는 중이라면 REMAIN_TIME에 대한 메시지를 삭제하여 사용중에 VPN 연결이 끊어지는 것을 방지한다. 
		handlerVpnIdleTime.removeMessages(VPN_REMAIN_TIME);
		LogUtil.d(getClass(), "[REMAIN_TIME] Message Remove..... ");
		
		if (mVpnCtl == null) {
			throw new IllegalStateException("Vpn제어 모듈이 존재하지 않습니다.");
		} 
		if ( ! mVpnCtl.status()) {
			throw new IllegalStateException("Vpn이 연결되어 있지 않습니다.");
		}
		
		synchronized (mLock) {
			// 누군가 VPN을 사용하려고 하고 패키지 목록이 비어있다면, 
			// tmpPrevPkgName 이 사용하는 것으로 사용하고 모니터링 데이터에 추가한다. 
			if(tmpPrevPkgName != null && mAlivePakcage.isEmpty()) {
				addMonitorPackage(tmpPrevPkgName);
			}
		}
		
		handlerVpnIdleTime.sendEmptyMessageDelayed(VPN_OVER_IDLE_TIME, getResources().getInteger(R.integer.VpnIdleTimeSec) * 1000);
		LogUtil.d("VPN_IDLE", "VPN IDLE TIME RESET", LOG_ENABLE);
	}
}
