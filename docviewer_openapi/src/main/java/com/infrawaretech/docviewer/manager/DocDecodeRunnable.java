package com.infrawaretech.docviewer.manager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.infrawaretech.docviewer.utils.Log;


class DocDecodeRunnable implements Runnable {
	
	private final static String TAG = DocDecodeRunnable.class.getSimpleName();
	
    //private static final long SLEEP_TIME_MILLISECONDS = 2500;
	private static final long SLEEP_TIME_MILLISECONDS = 500;
	
	public final static int TASK_BEGIN = 1;
	public final static int TASK_COMPLETED = 2;
	public final static int TASK_FAILED = 3;
	
	private final TaskRunnableDecodeMethod mTask;
	
	interface TaskRunnableDecodeMethod {
		void setDecodeThread(Thread currentThread);
		void handleDecodeState(int taskBegin);
		byte[] getByteBuffer();
		void setDocPage(Bitmap returnBitmap);
		void trimCacheSize();
	}
	
	DocDecodeRunnable(TaskRunnableDecodeMethod docDecodeTask) {
		int maxMemory = (int) (Runtime.getRuntime().maxMemory() / (1024.0f * 1024.0f));
		if (maxMemory > 128) {
			Log.i(TAG, "MaxMemory is " + maxMemory);
		}
		this.mTask = docDecodeTask;
	}
	

	@Override
	public void run() {
		mTask.setDecodeThread(Thread.currentThread());
		
		byte[] imageBuffer = mTask.getByteBuffer();
		Bitmap returnBitmap = null;
		
		try {
			mTask.handleDecodeState(TASK_BEGIN);
			
			returnBitmap = simpleDecode(imageBuffer);

		} catch (InterruptedException e) {
			Log.e(TAG, "사용자에 의하여 문서 decode 작업이 취소되었습니다.");
		} finally {
			if (returnBitmap == null) {
				mTask.handleDecodeState(TASK_FAILED);
			} else {
				mTask.setDocPage(returnBitmap);
				mTask.handleDecodeState(TASK_COMPLETED);
			}

			mTask.setDecodeThread(null);
			Thread.interrupted();
		}
	}
	
	private Bitmap simpleDecode(byte[] imageBuffer) throws InterruptedException {
		BitmapFactory.Options opts = new BitmapFactory.Options();

		opts.inJustDecodeBounds = false;
		
		Log.e(TAG, "in decode stage. (freeMemory = " + Runtime.getRuntime().freeMemory()
				+ ", totalMemory =  " + Runtime.getRuntime().totalMemory()
				+ ", maxMemory =  " + Runtime.getRuntime().maxMemory()
				+ ")");
		try {
			return BitmapFactory.decodeByteArray(imageBuffer, 0, imageBuffer.length, opts);
		} catch (Throwable T) {
			Log.e(TAG, "Out of memory in decode stage. (freeMemory = " + Runtime.getRuntime().freeMemory()
					+ ", totalMemory =  " + Runtime.getRuntime().totalMemory()
					+ ", maxMemory =  " + Runtime.getRuntime().maxMemory()
					+ ")");

			mTask.trimCacheSize();

			try {
				Thread.sleep(SLEEP_TIME_MILLISECONDS);
			} catch (InterruptedException e) {
				throw e;
			}
		}

		return null;
	}
}
