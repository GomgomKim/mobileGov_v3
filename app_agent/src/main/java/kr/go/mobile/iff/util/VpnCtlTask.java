package kr.go.mobile.iff.util;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import kr.go.mobile.mobp.iff.R;
import net.secuwiz.SecuwaySSL.Api.MobileApi;

/**
 * Secuwiz SSLVPN 와 연동하기 위한 Task 객체 
 * 
 * @author yoongi
 *
 */
public abstract class VpnCtlTask {

	public static final int VPN_STATUS_DISCONNECTION = 0;
	public static final int VPN_STATUS_CONNECTION = 1;
	public static final int VPN_STATUS_CONNECTING= 2;
	
	/*
	 * VPN 연결 상태을 공유(?)하기 위한 전역 객체 선언
	 */
	public static class VpnStatus {
		private static boolean ENABLED_SecuwaySSLVPN = false;
		private static boolean ENABLED_AndroidFrameworkVPN = Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ?
				false : /*안드로이드 버전이 LOLLIPOP 보다 높을 경우 AndroidFramework 의 Broadcast를 기다린다.*/
				true;  /* 안드로이드 버전이 LOLLIPOP 보다 낮거나 같을 경우 AndroidFramework 의  Broadcast를 기다리지 않기위해 true으로 설정한다. */

		private VpnStatus() {}
		public static final synchronized boolean enableVPN() {
			return VpnStatus.ENABLED_SecuwaySSLVPN && VpnStatus.ENABLED_AndroidFrameworkVPN;
		}
		private static final synchronized void enabledSecuwayVPN() {
			VpnStatus.ENABLED_SecuwaySSLVPN = true;
		}
		private static final synchronized void disabledSecuwayVPN() {
			VpnStatus.ENABLED_SecuwaySSLVPN = false;
		}
		private static final synchronized boolean enableSecuwayVPN() {
			return VpnStatus.ENABLED_SecuwaySSLVPN;
		}
		private static final synchronized void enabledAndroidVPN() {
			VpnStatus.ENABLED_AndroidFrameworkVPN = true;
		}
		private static final synchronized void disabledVPN() {
			VpnStatus.ENABLED_SecuwaySSLVPN = false;
			VpnStatus.ENABLED_AndroidFrameworkVPN = Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ?
					false : /*안드로이드 버전이 LOLLIPOP 보다 높을 경우 AndroidFramework 의 Broadcast를 기다린다.*/
					true;  /* 안드로이드 버전이 LOLLIPOP 보다 낮거나 같을 경우 AndroidFramework 의  Broadcast를 기다리지 않기위해 true으로 설정한다. */
		}
	}
	
	private static final String TAG = VpnCtlTask.class.getSimpleName();
	private final boolean LOG_ENABLE = true;
	
	private static final String VPN_STATUS_ACTION = "com.secuwiz.SecuwaySSL.Service.STATUS";
	private static final String VPN_SERVICE_ACTION = "net.secuwiz.SecuwaySSL.moi";
	
	enum VPN_CMD {
		_START,
		_STOP,
		_DESTROY;
	}
	
	/*
	 * VPN 연결 / 중지 / 종료에 대한 명령어를 실행하기 위한 Work 객체
	 */
	private class VpnWork {
		VPN_CMD mCmd;
		String mUserId;
		String mUserPw;
		int mRetryCnt=0;
	}
	
	// VPN 연결 재시도 횟수
	private final int VPN_CONNECT_RETRY_COUNT;
	// VPN 연결 재시도 딜레리 시간
	private final int VPN_CONNECT_RETRY_DELAY;
	// VPN 설정을 위한 대기 시간
	private final long DELAY_FOR_VPN_CONFIG;
	// 시큐위즈 VPN 서버 주소
	private final String SECUWAY_SERVER_ADDR;
	private final int SECUWAY_SSL_VERSION;
	
	private final BroadcastReceiver mVpnReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive (Context context, Intent intent) {
			String action = intent.getAction();
			LogUtil.d(getClass(), "onReceive :: action " + intent.getAction());
			if (action.equals(VPN_STATUS_ACTION)) {
				handleSecuwizVPN(intent);
			} else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
				handleAndroidVPN(intent);
			}
		}
		
		private void handleSecuwizVPN(Intent intent) {
			int vpnStatus = intent.getIntExtra("STATUS", VPN_STATUS_DISCONNECTION);
			LogUtil.d(TAG, String.format("Secuwiz VPN status onReceive : %d", vpnStatus), LOG_ENABLE);
			switch (vpnStatus) {
			case VPN_STATUS_CONNECTION:
				LogUtil.i(TAG, "Secuwiz VPN CONNECTED!! ");
				VpnStatus.enabledSecuwayVPN();
				
				if (VpnStatus.enableVPN()) {
					LogUtil.d(TAG, "VPN REAL CONNECTED!! ", LOG_ENABLE);
					
					// 단말에 따라서 android framework에서 VPN 설정을 위한 지연 시간이 필요할 수 있다. 
					if (DELAY_FOR_VPN_CONFIG > 0) {
						try {
							Thread.sleep(DELAY_FOR_VPN_CONFIG);
						} catch (InterruptedException e) {
							LogUtil.e(TAG, "VPN 연결 후 설정값 적용을 위하여 대기 중 에러가 발생하였습니다", e);
						}
					}
					onVpnConnected();
					mWork = null;
				} else {
					LogUtil.i(TAG, "wait Android Broadcast");
				}
				break;
			case VPN_STATUS_DISCONNECTION:
				if (mWork == null) {
					VpnStatus.disabledVPN();
					onVpnDisconnected();
				} else {
					switch (mWork.mCmd) {
					case _START:
					case _DESTROY:
						LogUtil.d(TAG, "re-exectue : " + mWork.mCmd);
						execute(mWork);
						break;
					case _STOP:
					default:
						mWork = null;
						break;
					}
				}
				break;
			case VPN_STATUS_CONNECTING:
				VpnStatus.disabledSecuwayVPN();
				onVpnConnecting();
				break;
			}
		}
		
		@SuppressWarnings("deprecation")
		private void handleAndroidVPN(Intent intent) {
			NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO/*networkInfo*/);
			boolean isVPN = false;

			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M 
					&& networkInfo == null) {
				
				ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
				Network[] networks = connectivityManager.getAllNetworks();
				for (Network n : networks) {
					NetworkInfo info = connectivityManager.getNetworkInfo(n);
					NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(n);
					LogUtil.d("TODO", "Network : " + n.toString(), LOG_ENABLE);
					LogUtil.d("TODO", "NetworkInfo: " + info.toString(), LOG_ENABLE);
					LogUtil.d("TODO", "VPN: " + capabilities.hasCapability(NetworkCapabilities.TRANSPORT_VPN), LOG_ENABLE);
					LogUtil.d("TODO", "NOT_VPN: " + capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN), LOG_ENABLE);
				}
				networkInfo = connectivityManager.getActiveNetworkInfo();
				Network network = connectivityManager.getActiveNetwork();
				NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
				LogUtil.d("TODO", "ActiveNetworkInfo :: "+capabilities, LOG_ENABLE);
				isVPN = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
			} else {
				networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO/*networkInfo*/);
				isVPN = networkInfo.getTypeName().equals("VPN");
			}
				
			if (isVPN && networkInfo != null) {
				DetailedState state = networkInfo.getDetailedState();
				LogUtil.d(TAG, "VPN DetailedState:: " + state.toString(), LOG_ENABLE);
				
				if (state.equals(DetailedState.CONNECTED) && VpnStatus.enableSecuwayVPN()) {
					VpnStatus.enabledAndroidVPN();
					LogUtil.d(TAG, "VPN REAL CONNECTED!! ", LOG_ENABLE);
					// VPN 설정을 위한 지연 시간이 필요하면 해당 시간만큼 대기한다.
					if (DELAY_FOR_VPN_CONFIG > 0) {
						try {
							Thread.sleep(DELAY_FOR_VPN_CONFIG);
						} catch (InterruptedException e) {
							LogUtil.e(TAG, "VPN 연결 후 설정값 적용을 위하여 대기 중 에러가 발생하였습니다", e);
						}
					}
					onVpnConnected();
					mWork = null;
				}
			} else {
				// TODO 에러 처리 필요
			}
		}
	};

	// 서비스 연결 상태를 동기화하기 위한 LOCK 객체
	private final Object mLock = new Object();
	// SECUWIZ SSLVPN 서비스를 사용하기 위한 객체
	private MobileApi mVpnService = null;
	
	private final ServiceConnection mVpnServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			LogUtil.d(TAG, "VPNService.onServiceDisconnected", LOG_ENABLE);
			try {
				synchronized (mLock) {
					if (mVpnService != null) {
						mContext.unregisterReceiver(mVpnReceiver);
						mVpnService = null;
					}
				}
			} catch (NullPointerException e) {
				// TODO 에러 처리 ! 
				LogUtil.w(TAG, "??? ", e);
			} catch (Exception e) {
				// TODO 에러 처리 ! 
				LogUtil.w(TAG, "??? ", e);
			}
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LogUtil.d(TAG, "VPNService.onServiceConnected: Thread - " + Thread.currentThread().getName(), LOG_ENABLE);
			
			try {
				synchronized (mLock) {
					// 서비스 바인딩.
					mVpnService = MobileApi.Stub.asInterface(service);
					
					// VPN SERVICE 상태값을 받을 수 있는 리시버 등록.
					IntentFilter filter = new IntentFilter(VPN_STATUS_ACTION);
					filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
					mContext.registerReceiver(mVpnReceiver, filter);
					LogUtil.d(TAG, "VPN Service Ready!", LOG_ENABLE);
					onVpnServiceReady();
				}
			} catch (IllegalArgumentException e) {
				LogUtil.e(TAG, "onServiceConnected : ERROR", e);
			} catch (Exception e) {
				LogUtil.e(TAG, "onServiceConnected : ERROR", e);
			}
		}
	};
	
	private final Context mContext;
	
	private VpnWork mWork = null;
	
	public VpnCtlTask(Context context, String sslServerAddr, int sslVersion) {
		this.mContext = context;
		this.VPN_CONNECT_RETRY_COUNT = context.getResources().getInteger(R.integer.VpnConnectRetryCnt);
		this.VPN_CONNECT_RETRY_DELAY = context.getResources().getInteger(R.integer.VpnConnectRetryDelay);
		this.SECUWAY_SERVER_ADDR = sslServerAddr;
		this.SECUWAY_SSL_VERSION = sslVersion;
		this.DELAY_FOR_VPN_CONFIG = Utils.getDelaySecForVPNConfig(context) * 1000;
		Intent bindServiceInfo = new Intent(VPN_SERVICE_ACTION);
		bindServiceInfo.setPackage(VPN_SERVICE_ACTION);
		if ( !mContext.bindService(bindServiceInfo, mVpnServiceConnection,  Context.BIND_AUTO_CREATE)) {
			LogUtil.e(TAG, "VPN SERVICE BIND : ERROR");
		}
	}
	
	protected abstract void onVpnServiceReady();
	
	protected abstract void onVpnConnected();
	
	protected abstract void onVpnDisconnected();
	
	protected abstract void onVpnConnecting();
	
	protected abstract void onResult(String msg);
	
	public void start(final String userID, final String userPW) {
		final VpnWork work = new VpnWork();
		work.mCmd = VPN_CMD._START;
		work.mUserId = userID;
		work.mUserPw = userPW;
		execute(work);
	}
	
	private void start(final VpnWork work) {
		execute(work);
	}
	
	public void stop() {
		if (mVpnService == null) {
			// TODO 예외사항 발생
			return;
		}
		VpnWork work = new VpnWork();
		work.mCmd = VPN_CMD._STOP;
		execute(work);
	}
	
	public boolean status() {
		if (mVpnService == null) {
			// TODO 예외사항 발생
			return false;
		}
		try {
			if (mVpnService.VpnStatus() == VPN_STATUS_CONNECTION) {
				// 이미 연결된 상태이므로 VpnStatus 값을 enabled 로 변경
				VpnStatus.enabledSecuwayVPN();
				VpnStatus.enabledAndroidVPN();
				return true;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void close() {
		synchronized (mLock) {
			if (mVpnService == null) {
				LogUtil.e(TAG, "VpnService is null");
				return;
			}
		}
		VpnWork work = new VpnWork();
		work.mCmd = VPN_CMD._DESTROY;
		execute(work);
	}
	
	private boolean retryStart(String ret) {
		if ("이미 로그인한 사용자입니다.".equals(ret)) {
			return true;
		}
		return false;
	}
	
	private final void execute(VpnWork ... works) {
		new AsyncTask<VpnWork,  Void,  Void>() {

			@Override
			protected Void doInBackground(VpnWork ... works) {
				int MAX_WAIT_TIME = 6 * 1000;
				int CURRENT_WAIT_TIME = 0;
				try {
					while (true) {
						synchronized (mLock) {
							if (mVpnService != null) break;
						}
						if (CURRENT_WAIT_TIME >= MAX_WAIT_TIME) {
							LogUtil.d(TAG, "VPN Service connttion timeout!.", LOG_ENABLE);
							onResult("SSLVPN 서비스 연결에 실패하였습니다. 다시 시도해주시기 바랍니다.");
							break;
						}
						LogUtil.d(TAG, "VPN Service connection waitting...", LOG_ENABLE);
						Thread.sleep(2000);
						CURRENT_WAIT_TIME += 2000;
					}
				} catch (InterruptedException e) {
					LogUtil.e(TAG, "VPN Service Conntection waitting error.. .", e);
					return null;
				}
				
				try {
					int curStatus = mVpnService.VpnStatus(); 
					switch (works[0].mCmd) {
					case _START:
					{
						if (curStatus == VPN_STATUS_DISCONNECTION) {
							if (works[0].mRetryCnt < VPN_CONNECT_RETRY_COUNT) {
								LogUtil.d(TAG, "DISCONNECTION -> CONNECTION", LOG_ENABLE);
								LogUtil.d(TAG, "INFO] " + works[0].mUserId, LOG_ENABLE);
								String ret = mVpnService.StartVpn(SECUWAY_SERVER_ADDR, 
										works[0].mUserId, works[0].mUserPw, SECUWAY_SSL_VERSION);
								
								LogUtil.d(TAG, "Connection result: " + ret, LOG_ENABLE);
								if (ret.equals("0")) {
									// 정상실행. 
									mWork = works[0];
								} else if (retryStart(ret)) {
									try{
										Thread.sleep(VPN_CONNECT_RETRY_DELAY);
									} catch (InterruptedException e) {
										LogUtil.e(TAG, "", e);
									}
									works[0].mRetryCnt++;
									start(works[0]);
								} else {
									// 2019-04-19 : VPN 에서 전달되는 메시지는 모두 동일하게 표현. 
									LogUtil.w(getClass(), "VPN 연결 실패 : " + ret);
									onResult("VPN 연결이 실패하였습니다");
								}
							} else {
								onResult("잠시 후 다시 시도해 주세요");
							}
						} else if (curStatus ==  VPN_STATUS_CONNECTION){
							// 이미 연결되어 있는 상태. 연결을 해제함.
							LogUtil.d(TAG, "(R) CONNECTION -> DISCONNECTION", LOG_ENABLE);
							mWork = works[0];
							mVpnService.StopVpn();
						} else { // curStatus ==  VPN_STATUS_CONNECTING
							// 연결중인 상태.... 
							LogUtil.d(TAG, "(R) CONNECTING -> DISCONNECTION", LOG_ENABLE);
							mWork = works[0];
							mVpnService.StopVpn();
						}
						break;
					}
					case _STOP: 
					{
						if (curStatus == VPN_STATUS_CONNECTION) {
							LogUtil.d(TAG, "CONNECTION -> DISCONNECTION", LOG_ENABLE);
							mVpnService.StopVpn();
						} else if (curStatus == VPN_STATUS_CONNECTING) {
							// TODO 접속중이라면 연결이 성공하면 연결해제 ?? 
						}
						break;
					}
					case _DESTROY: 
					{
						if (curStatus == VPN_STATUS_DISCONNECTION) {
							mContext.unbindService(mVpnServiceConnection);
						} else { // Connection or Connecting
							mWork = works[0];
							mVpnService.StopVpn();
						}
						break;
					}
					}
				} catch (RemoteException e) {
					LogUtil.e(TAG, "", e);
				}
				return null;
			}
			
			///////// UI 
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}
			
			@Override
			protected void onProgressUpdate(Void ... values) {
				super.onProgressUpdate(values);
			}
			
			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
			}
			
		}.execute(works);
	}
}
