package com.sds.BizAppLauncher.gov.aidl;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import com.sds.mobile.servicebrokerLib.aidl.IRemoteService;
import com.sds.mobiledesk.mdhybrid.NewMDHybridActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import kr.go.mobile.iff.R;
import kr.go.mobile.iff.service.IAliveService;
import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.iff.util.PackageUtil;

public class GovController{


	@Deprecated
	public static final String ACTION_MFF_LOGINCHECK = "kr.go.mobile.mobp.mff.logincheck";
	@Deprecated
	public static final String ACTION_IFF_LOGINCHECK = "kr.go.mobile.mobp.iff.logincheck";

	private static final String TAG = GovController.class.getSimpleName();

	private static Context mContext;
	private static GovController self;
	private static MoiApplication selfApplication;
	IGovCallback mGOVCallbackListener;

	private static final String INTENT_ACTION_HTTPDATA = "kr.go.mobile.mobp.mff.HTTPDATA";

	private ServiceConnection mAliveConnection = null;
	private ServiceConnection mSvcConnection = null;
	private BroadcastReceiver mReceiver = null;
	
	private GovController() {
	}

	@Deprecated
	private GovController(Context context)	{ /* no work */ }

	public static GovController getInstance(Context context) {
		return getInstance();
	}
	
	private static GovController getInstance() {
		if (self == null) {
			self = new GovController();
		}
		return self;
	}

	@Deprecated
	public static abstract interface IGovCallback{
		public static final int GOV_VPN_CONNECT = 0;
		public static final int GOV_VPN_DISCONNECT = 1;
		public static final int GOV_VPN_IS_CONNECTED = 2;
		public static final int GOV_VPN_CLEAR_DATA = 3;

		public abstract void callbackGov(int paramInt, boolean paramBoolean);
	}

//	static void killMyProcess(Context context) {
//		if (context instanceof Activity){
//			((Activity)context).moveTaskToBack(true);
//		} else {
//			Intent homeIntent = new Intent(Intent.ACTION_MAIN);
//			homeIntent.addCategory(Intent.CATEGORY_HOME);
//			homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//			context.startActivity(homeIntent);
//		}
//		android.os.Process.killProcess(android.os.Process.myPid());
//	}
	
	/**
	 * 공통기반 서비스의 초기화를 요청합니다.   
	 * 
	 * @since 2016.11.14
	 * @author 윤기현
	 * 
	 * @param loadingActivity - 행정앱의 로딩 액티비티 객체
	 * @param requestCode - onActivityResult 에서 요청코드를 구분하기 위하여 사용되는 코드 값. 
	 * @param verificationTokenByte64 - MSM 보안 라이브러리를 이용하여 취득한 보안토큰값. 
	 * 
	 */
	public static void startGovActivityForResult(final Activity loadingActivity, final int requestCode, final String verificationTokenByte64) {
		getInstance(loadingActivity);
		PackageUtil util = new PackageUtil(loadingActivity);
		// 모바일 정부 런처 설치 여부를 확인한다.
		String LAUNCHER_NAME = loadingActivity.getString(R.string.LAUNCHER_NAME);
		if (util.isInstalledApplication(LAUNCHER_NAME) == false) {
			// 설치되어 있지 않다면, 메시지를 보여주고 종료
			AlertDialog.Builder builder = new AlertDialog.Builder(loadingActivity);
            builder.setTitle("필수앱이 설치되어 있지 않습니다. ");
            builder.setMessage("보안 Agent 설치 후 다시 실행하시기 바랍니다.");
            builder.setCancelable(false);
            builder.setNeutralButton("닫기", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    loadingActivity.finish();
                }
            });
            builder.show();
            return;
		}
		
		if (loadingActivity.checkCallingOrSelfPermission("kr.go.mobile.permission.ACCESS_SERVICE_BROKER") //서비스브로커를 사용하기 위해서 필요한 권한으로
				== PackageManager.PERMISSION_DENIED) {
			// 서비스브로커를 사용할 수 있는 권한이 없다면, 메시지를 보여주고 종료
			AlertDialog.Builder builder = new AlertDialog.Builder(loadingActivity);
	        builder.setTitle("서비스브로커 사용 권한이 없습니다. ");
	        builder.setMessage("해당 앱을 재설치하여 실행해주시기 바랍니다. 재설치 후에도 실행되지 않을 경우 개발사에 문의바랍니다.");
	        builder.setCancelable(false);
	        builder.setNeutralButton("닫기", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
                    loadingActivity.finish();
	            }
	        });
	        builder.show();
			return;
		}

		Intent intent = new Intent(GovControllerType.ACTION_GOV_LAUNCHER);
		intent.putExtra(GovControllerType.EXTRA_TOKEN, verificationTokenByte64);
		intent.putExtra(GovControllerType.EXTRA_PACKAGE_NAME, loadingActivity.getPackageName());
        loadingActivity.startActivityForResult(intent, requestCode);
	}

	private NewMDHybridActivity mHybridActivity;
	/**
	 * 
	 * @param hybridActivity
	 */
	public void setHybridView(final NewMDHybridActivity hybridActivity) {
		mHybridActivity = hybridActivity;
		GovController.bindService(hybridActivity);
	}
	
	void finishHybridView() throws NotHybridApplicationException {
		if (mHybridActivity == null) {
			throw new NotHybridApplicationException();
		}
		GovController.unbindService();
		mHybridActivity.finish();
		mHybridActivity = null;
	}
	
	static void registerBroadcast(Context context) {
		if (self.mReceiver == null) {
			self.mReceiver = new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					if (action != null && action.equals(GovControllerType.ACTION_CONTROL)) {
						try {
							int type = intent.getIntExtra(GovControllerType.EXTRA_TYPE, 0);
							
							switch (type) {
							case GovControllerType.TYPE_KILL:
							{
								Log.v(GovController.class.getName(), "런처에 의하여 행정앱이 종료됩니다.");
								ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
								List<RunningAppProcessInfo> l = am.getRunningAppProcesses();
								for (RunningAppProcessInfo info : l) {
									if (info.processName.equals(context.getPackageName()) 
											&& info.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
										Log.v(GovController.class.getName(), "package=" + info.processName);
										Intent homeIntent = new Intent(Intent.ACTION_MAIN);
										homeIntent.addCategory(Intent.CATEGORY_HOME);
										homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
										context.startActivity(homeIntent);
										break;
									}
								}
								
								try {
									GovController.getInstance().finishHybridView();
								} catch (NotHybridApplicationException e) {
									Log.w(GovController.class.getSimpleName(), e.toString());
								}
								System.exit(0);
								break;
							}
							default:
								break;
							}

						} catch (NullPointerException e) {
							Log.w(GovController.class.getSimpleName(), "올바른 접근이 아닙니다.");
							return;
						} catch (Exception e) {
							Log.w(GovController.class.getSimpleName(), "올바른 접근이 아닙니다.");
							return;
						}
					}

				}
			};
			IntentFilter filter = new IntentFilter(GovControllerType.ACTION_CONTROL);
			context.registerReceiver(self.mReceiver, filter);
		}
	}
	
	static void unregisterBroadcast(Context context) {
		if (self.mReceiver != null) {
			context.unregisterReceiver(self.mReceiver);
			self.mReceiver = null;
		}
	}

	static void bindService(MoiApplication moiApplication) {
		GovController.selfApplication = moiApplication;
		bindService(moiApplication.getApplicationContext());
	}

	static void bindService(Context context) {
		GovController.getInstance();
		GovController.mContext = context;
		if (self.mAliveConnection == null) {
			self.mAliveConnection = new ServiceConnection() {
				
				@Override
				public void onServiceDisconnected(ComponentName name) {

				}
				
				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					Log.d(TAG, "***** BIND ALIVE_SERVICE CONNECTED *****");
					try {
						IAliveService.Stub.asInterface(service).isAlive(GovController.mContext.getPackageName());
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			};

			Intent i = new Intent(GovControllerType.ACTION_ALIVE_SERVICE);
			i.setPackage(GovController.mContext.getString(R.string.LAUNCHER_NAME));
			i.putExtra(GovControllerType.EXTRA_PACKAGE_NAME, GovController.mContext.getPackageName());
			try {
				Log.d(TAG, "***** BIND ALIVE_SERVICE *****");
				GovController.mContext.bindService(i, self.mAliveConnection, Context.BIND_AUTO_CREATE);
			} catch (Exception e) {
				Log.e(TAG, "***** ERROR BIND ALIVE_SERVICE *****", e);
			}
		}

		if (self.mSvcConnection == null) {
			self.mSvcConnection = new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
					Log.d(TAG, "***** BIND SVC_SERVICE CONNECTED *****");
					GovController.selfApplication.setSVC(IRemoteService.Stub.asInterface(iBinder));
				}

				@Override
				public void onServiceDisconnected(ComponentName componentName) {
					Log.d(TAG, "***** BIND SVC_SERVICE DISCONNECTED *****");
					GovController.selfApplication.setSVC(null);
				}
			};

			Intent bindIntent = new Intent();
			bindIntent.setPackage(GovController.mContext.getString(R.string.LAUNCHER_NAME));
			bindIntent.setAction(INTENT_ACTION_HTTPDATA);
			try {
				Log.d(TAG, "***** BIND SVC_SERVICE *****");
				GovController.mContext.bindService(bindIntent, self.mSvcConnection, Context.BIND_AUTO_CREATE);
			} catch (Exception e) {
				Log.e(TAG, "***** ERROR BIND SVC_SERVICE *****", e);
			}
		}

	}
	
	static void unbindService() {
		if (GovController.mContext == null) return;

		if (self.mAliveConnection != null) {
			GovController.mContext.unbindService(self.mAliveConnection);
			self.mAliveConnection = null;
		} 

		if (self.mSvcConnection != null) {
			GovController.mContext.unbindService(self.mSvcConnection);
			self.mSvcConnection = null;
		}

		if (self.mReceiver != null) {
			GovController.mContext.unregisterReceiver(self.mReceiver);
			self.mReceiver = null;
		}

		GovController.mContext = null;
	}
	
	/**
	 * 현재 제공되는 공통기반 서비스에서는 사용하지 않는 함수입니다.  
	 * 
   	 * @Deprecated 대신에 {@link #startGovActivityForResult(Activity, int, String)} 사용하시기 바랍니다. 
	 */
	@Deprecated
	public static void startGovActivityForResult(Context context, int requestCode) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("현재 제공되는 공통기반 서비스에서는 사용하지 않는 함수입니다. startGovActivityForResult(Activity, int, String) 함수를 호출하시기 바랍니다.");
	}

	// 나중에 운영부서와 협의하여 삭제 또는 deprecated 표시
	@Deprecated
	public void bindService() {
	}

	@Deprecated
	public void unBindService() {
	}

	@Deprecated
	public void setIGovCallbackListener(IGovCallback iGov){
		this.mGOVCallbackListener = iGov;
	}
	
	@Deprecated
	public void vpnConnect(){
		LogUtil.w(getClass(), "** GOV vpnConnect is deprecate. **");
	}

	
	@Deprecated
	public void vpnDisconnect(){
		LogUtil.w(getClass(), "** GOV vpnDisconnect is deprecate. **");
	}

	
	@Deprecated
	public void vpnIsConnected(){
		LogUtil.w(getClass(), "** GOV vpnIsConnected is deprecate. **");
	}
	
	@Deprecated
	public void vpnClearData(){
		LogUtil.w(getClass(), "** GOV vpnClearData is deprecate. **");
	}
	
	
	@Deprecated
	public void vpnConnect(IGovServiceCallback callback){
		LogUtil.w(getClass(), "** GOV vpnConnect(callBack) is deprecate. **");
		try {
			callback.callback(true);
		} catch (RemoteException e) {
			LogUtil.e(getClass(), e.getMessage());
		}
	}

	
	@Deprecated
	public void vpnDisconnect(IGovServiceCallback callback){
		LogUtil.w(getClass(), "** GOV vpnDisconnect(callBack) is deprecate. **");
		try {
			callback.callback(true);
		} catch (RemoteException e) {
		}
	}

	
	@Deprecated
	public void vpnIsConnected(IGovServiceCallback callback){
		LogUtil.w(getClass(), "** GOV vpnIsConnected(callBack) is deprecate. **");
		try {
			callback.callback(true);
		} catch (RemoteException e) {
			LogUtil.e(getClass(), e.getMessage());
		}
	}
	
	
	@Deprecated
	public void vpnClearData(IGovServiceCallback callback){
		LogUtil.w(getClass(), "** GOV vpnClearData(callBack) is deprecate. **");
		try {
			callback.callback(true);
		} catch (RemoteException e) {
			LogUtil.e(getClass(), e.getMessage());
		}
	}
}