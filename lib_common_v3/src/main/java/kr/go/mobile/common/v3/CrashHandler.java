package kr.go.mobile.common.v3;

import android.os.Handler;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

class CrashHandler implements Thread.UncaughtExceptionHandler {

    private String TAG = CrashHandler.class.getSimpleName();
    private Handler handler;

    class MegRuntimeException extends RuntimeException {
        int pid;
        String threadName;
        MegRuntimeException(Throwable e, int pid, String threadName) {
            super(e);
            this.pid = pid;
            this.threadName = threadName;
        }
    }

    CrashHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull final Throwable e) {
        Log.w(TAG, "uncaught exception (thread : " + t + ") e : ", e);
        final int pid = Process.myPid();
        final String threadName = t.getName();
        handler.post(new Runnable(){
            public void run() {
                throw new MegRuntimeException(e, pid, threadName);
            }
        });
    }
}
