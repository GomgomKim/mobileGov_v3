package kr.go.mobile.iff;

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import kr.go.mobile.iff.service.ISessionManagerService;
import kr.go.mobile.iff.service.SessionManagerService;
import kr.go.mobile.iff.util.LogUtil;

/**
 * 모바일 전자정부에서 사용하는 보안에이전트 (Security Agent)의 Application 객체.  
 * 
 * @author yoongi
 *
 */
public class SAGTApplication extends Application {
	
	private static final String TAG = SAGTApplication.class.getSimpleName();
	
	private static final String PRIVATE_PREFERENCE = "history";
//	public static final String PREFS_KEY_SET_PROFILE= "key_set_profile";
	
	// 세션 관리 서비스 관련 객체. 
	private static ISessionManagerService SMService = null;
	private ServiceConnection mSMServiceConnection;
	
	private static Object mLock = new Object();
	private static SharedPreferences PREFS;
	
	private static SAGTApplication THIS;
	
	@Override
	public void onCreate() {
		super.onCreate();
		THIS = this;
		LogUtil.e(getClass(), "보안에이전트 ....");
		setShutdownMonitor();
		initSharedPrefernces();
		bindSessionManagerService();
		LogUtil.e(getClass(), "보안에이전트 초기화 중....");
	}
	public void onTerminate() {
		super.onTerminate();
		LogUtil.e(getClass(), "@@@@@@@@@@@@@ TERMINATE");
	}
	
	static boolean getBoolean(String key) {
		boolean ret;
		synchronized (mLock) {
			ret = PREFS.getBoolean(key, false);
		}
		return ret;
	}
	
	static void savePrefs(String key, boolean value) {
		synchronized (mLock) {
			SharedPreferences.Editor editor = PREFS.edit();
			editor.putBoolean(key, value);
			editor.commit();
		}
	}

	static ISessionManagerService getSessionManagerService() {
		synchronized (mLock) {
			return SMService;
		}
	}

	// FIXME 갑자기 종료될 경우 앱을 다시 실행하여 정보를 유지하도록 한다.
	private void setShutdownMonitor() {
		final UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				THIS.unbindSessionManagerService();
				LogUtil.e(getClass(), "uncaughtException");
				defaultHandler.uncaughtException(thread, ex);
			}
		});
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				THIS.unbindSessionManagerService();
				LogUtil.e(getClass(), "ShutdownHook");
			}
		});
	}
	
	private void initSharedPrefernces() {
		PREFS = getSharedPreferences(PRIVATE_PREFERENCE, MODE_PRIVATE);
	}
	
	private void bindSessionManagerService() {
		mSMServiceConnection = new ServiceConnection() {
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				LogUtil.d(TAG, "bindSessionManagerService - connected");
				SessionManagerService.Binder binder = (SessionManagerService.Binder) service;
				synchronized (mLock) {
					SMService = binder.getService();
				}
			}
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				LogUtil.d(TAG, "bindSessionManagerService - disconnected");
				synchronized (mLock) {
					SMService = null;
				}
			}
			
		};
		LogUtil.d(TAG, "bindSessionManagerService()");
		bindService(new Intent(this, SessionManagerService.class), mSMServiceConnection, 
				Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
	}
	
	private void unbindSessionManagerService() {
		if (mSMServiceConnection != null) {
			LogUtil.d(TAG, "unbindSessionManagerService()");
			getSessionManagerService().stopVPN();
			unbindService(mSMServiceConnection);
			mSMServiceConnection = null;
		}
	}
}
