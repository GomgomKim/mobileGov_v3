package kr.go.mobile.agent.solution;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.ArrayBlockingQueue;

import kr.go.mobile.agent.app.SAGTApplication;

import static java.lang.Class.forName;

public abstract class Solution<IN, OUT> {

    public enum RESULT_CODE {
        _OK,        // 정상 처리
        _FAIL,      // 처리 실패(에러)
        _TIMEOUT,   // 응답 없음
        _CANCEL,    // 사용자 취소
        _INVALID,   // 실행 실패(환경변수 오류)
        _WAIT
    }

    public static class Result<OUT> {
        public OUT out;
        RESULT_CODE code;
        String errorMessage;
        public Result(OUT out) {
            this(RESULT_CODE._OK, null);
            this.out = out;
        }
        public Result(RESULT_CODE code, String errorMessage) {
            this.code = code;
            this.errorMessage = errorMessage;
        }

        public RESULT_CODE getCode() {
            return this.code;
        }
        public String getErrorMessage() { return this.errorMessage; }

        public boolean waitResult() {
            return code == RESULT_CODE._WAIT;
        }
    }

    public static class SolutionRuntimeException extends RuntimeException {
        public SolutionRuntimeException(String message) {
            super(message);
        }
        public SolutionRuntimeException(String message, Exception e) {
            super(message, e);
        }
    }

    public interface EventListener<OUT> {
        // 모듈 연계 실패 (예외 발생)
        void onFailure(Context context, String message, Throwable t);
        // 모듈 처리 정상
        void onCompleted(Context context, Result<OUT> out);
    }

    boolean isOperation = false;
    boolean isCancel = false;
    ArrayBlockingQueue<Result<OUT>> waitQueue;
    EventListener<OUT> defaultEventListener;
    Thread processThread;
    boolean enabledMonitor;
    Thread monitorThread;


    public Solution(Context context) {

    }

    public void setDefaultEventListener(EventListener<OUT> listener) {
        this.defaultEventListener = listener;
    }

    public final void execute(Context context) {
        execute(context, (IN) null);
    }

    public final void execute(Context context, boolean enabledUI) {
        execute(context, (IN) null, enabledUI);
    }

    public final void execute(Context context, IN in) {
        execute(context, in, false);
    }

    public final void execute(final Context context, final IN in, boolean enabledUI) {
        if (isOperation) {
            Log.d(getClass().getSimpleName(), "이미 실행 중입니다.");
            return;
        }
        isCancel = false;
        isOperation = true;

        if (defaultEventListener == null) {
            throw new IllegalArgumentException("솔루션 처리 후 응답 받을 EventListener 가 존재하지 않습니다.");
        }

        if (enabledUI) {
            handle(context, in);
        } else {
            processThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    handle(context, in);
                }
            });
            processThread.setName(getClass().getSimpleName() + "- THREAD");
            processThread.start();
        }
    }

    final void handle(Context context, IN in) {
        Log.d(getClass().getSimpleName(), "------------- begin --------------");

        try {
            Result<OUT> result = process(context, in);
            if (result.waitResult()) {
                waitQueue = new ArrayBlockingQueue<>(1);
                // result 가 null 이면 응답을 기다려야함.
                result = waitQueue.take();
                waitQueue.clear();
                waitQueue = null;
            }
            defaultEventListener.onCompleted(context, result);
        } catch (SolutionRuntimeException | InterruptedException e) {
            defaultEventListener.onFailure(context, e.getMessage(), e);
        } finally {
            finish();
        }
    }

    public void setResult(Result<OUT> result) {
        if (waitQueue == null) {
            Log.e(getClass().getSimpleName(), "응답 데이터를 기다리지 않고 있습니다.");
            return;
        }
        waitQueue.offer(result);
    }

    public void enabledMonitor() {
        if (waitQueue == null) {
            waitQueue = new ArrayBlockingQueue<>(1);
        }
        Log.d(getClass().getSimpleName(), "모니터링 활성화");
        enabledMonitor = true;
        if (monitorThread == null) {
            monitorThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    do {
                        try {
                            Result<OUT> result = waitQueue.take();
                            defaultEventListener.onCompleted(null, result);
                        } catch (InterruptedException ignored) {
                        }
                    } while (enabledMonitor);
                }
            }, getClass().getSimpleName() + "-monitor");
            monitorThread.start();
        }
    }

    public void disabledMonitor() {
        enabledMonitor = false;
        monitorThread.interrupt();
        monitorThread = null;
        Log.d(getClass().getSimpleName(), "모니터링 비활성화");
    }

    protected abstract Result<OUT> process(Context context, IN in) throws SolutionRuntimeException;

    protected Integer[] getRequestCodes() {
        return new Integer[0];
    }

    protected boolean onActivityResult(Context context, int requestCode, int resultCode, Intent intent) {
        return false;
    }

    public void finish() {
        if (isOperation) {
            if (processThread != null) {
                processThread.interrupt();
                processThread = null;
            }
            isOperation = false;
            isCancel = false;
            Log.d(getClass().getSimpleName(), "------------- finish --------------");
        }
    }

    public void cancel() {
        if (processThread != null) {
            processThread.interrupt();
            processThread = null;
        }
    }

//    @Deprecated
//    final Context getContext() throws RuntimeException {
//        // FIXME 확인 후 불필요할 경우 삭제
//        SAGTApplication application = (SAGTApplication) getApplication();
//        Context ctx = application.getTopActivity();
//        if (ctx == null) {
//            return application.getApplicationContext();
//        } else {
//            return ctx;
//        }
//    }

    final Application getApplication() {
        try {
            @SuppressLint("PrivateApi") Class<?> appGlobalsClass = forName("android.app.AppGlobals");
            Method method = appGlobalsClass.getMethod("getInitialApplication");
            Application application = (Application) method.invoke(null);
            if(application == null) {
                throw new RuntimeException("Context 객체 획득 에러");
            }
            return application;
        } catch (Exception e) {
            throw new RuntimeException("Context 객체 획득 에러", e);
        }
    }
}

