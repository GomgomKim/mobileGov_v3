package com.sds.BizAppLauncher.gov.aidl;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingQueue;

import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.aidl.IRemoteService;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.util.Log;

public class MoiApplication extends Application {

	// 서비스 브로커 바인딩 객체
	private static IRemoteService mSVC;
	private static Thread threadServiceBroker;

	public void onCreate() {
		super.onCreate();
		// 저장공간 접근권한을 확인해야함.

		Log.d("TOM@@@", "Moi onCreate");
		this.setMyProcessShutdownMonitor(this);
		GovController.bindService(this);
	}
	
	public void onTerminate() {
		super.onTerminate();
		sendFinishApplication(this);
	}

	protected void setSVC(IRemoteService iRemoteService) {
		MoiApplication.mSVC = iRemoteService;
	}

	public static void setRunnable(Runnable runnable) {
		MoiApplication.threadServiceBroker = new Thread(runnable);
		MoiApplication.threadServiceBroker.start();
	}

	public static IRemoteService getSVC() {
		return MoiApplication.mSVC;
	}

	private void setMyProcessShutdownMonitor(final Context context) {
		final UncaughtExceptionHandler DEFAULT_HANDLER = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				Log.e("", "---- uncaughtException ----", ex);
				sendFinishApplication(context);
				DEFAULT_HANDLER.uncaughtException(thread, ex);
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				sendFinishApplication(context);
			}
		});
	}

	private void sendFinishApplication(Context context) {
		MoiApplication.mSVC = null;
		MoiApplication.threadServiceBroker.interrupt();

		GovController.unbindService();

		Intent finishedIntent = new Intent(GovControllerType.ACTION_STATUS);
		finishedIntent.putExtra(GovControllerType.EXTRA_PACKAGE_NAME, context.getPackageName() /*실행중인 행정앱의 패지키명*/);
		finishedIntent.putExtra(GovControllerType.EXTRA_STATUS, GovControllerType.STATUS_FINISHED);
		context.sendBroadcast(finishedIntent);
	}
}
