package kr.go.mobile.agent.solution;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Method;

import kr.go.mobile.agent.app.SAGTApplication;

import static java.lang.Class.forName;

public abstract class Solution<IN, OUT> {

    public enum RESULT_CODE {
        _OK,        // 정상 처리
        _FAIL,      // 처리 실패(에러)
        _TIMEOUT,   // 응답 없음
        _CANCEL,    // 사용자 취소
        _INVALID    // 실행 실패(환경변수 오류)
    }

    public static class Result<OUT> {
        public OUT out;
        RESULT_CODE code;
        String errorMessage;
        public Result(RESULT_CODE code) {
            this(code, null);
        }
        public Result(RESULT_CODE code, String errorMessage) {
            this.code = code;
            this.errorMessage = errorMessage;
        }

        public RESULT_CODE getCode() {
            return this.code;
        }
        public String getErrorMessage() { return this.errorMessage; }
    }

    public static class SolutionRuntimeException extends RuntimeException {
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

    final Object LOCK = new Object();

    boolean isOperation = false;
    boolean isCancel = false;
    EventListener<OUT> mEventListener;
    Thread thread;

    public Solution(Context context) {
        prepare(context);
    }

    protected void prepare(Context context) { }

    public final void execute(Context context, EventListener<OUT> listener) {
        execute(context, (IN) null, listener);
    }

    public final void execute(Context context, boolean enabledUI, EventListener<OUT> listener) {
        execute(context, (IN) null, enabledUI, listener);
    }

    public final void execute(Context context, IN in, EventListener<OUT> listener) {
        execute(context, in, false, listener);
    }

    public final void execute(final Context context, final IN in, boolean enabledUI, EventListener<OUT> listener) {
        if (isOperation) {
            Log.d(getClass().getSimpleName(), "이미 실행 중입니다.");
            return;
        }
        isCancel = false;
        isOperation = true;
        mEventListener = (listener == null) ? new EventListener<OUT>() {
            @Override
            public void onFailure(Context context, String message, Throwable t) {
                Log.w("Solution.EventListener", "Solution.EventListener is null, execute() 호출 시 EventListener 를 선언하시기 바랍니다. (onFailure)");
            }

            @Override
            public void onCompleted(Context context, Result<OUT> o) {
                Log.w("Solution.EventListener", "Solution.EventListener is null, execute() 호출 시 EventListener 를 선언하시기 바랍니다. (onCompleted)");
            }
        } : listener;

        if (enabledUI) {
            process(context, in);
        } else {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    process(context, in);
                }
            });
            thread.setName(getClass().getName());
            thread.start();
        }
    }

    final void process(Context context, IN in) {
        try {
            Result<OUT> result = execute(context, in);
            if (result == null) {
                // result 가 null 이면 응답을 기다려야함.
                return;
            }
            completedProcess(context, result);
        } catch (SolutionRuntimeException e) {
            failedProcess(context, e);
        }
    }

    protected abstract Result<OUT> execute(Context context, IN in) throws SolutionRuntimeException;

    protected final void completedProcess(Context context, Result<OUT> r) {
        mEventListener.onCompleted(context, r);
    }

    protected final void failedProcess(Context context, Throwable t) {
        mEventListener.onFailure(context, t.getMessage(), t);
    }

    protected Integer[] getRequestCodes() {
        return new Integer[0];
    }

    protected boolean onActivityResult(Context context, int requestCode, int resultCode, Intent intent) {
        return false;
    }

    public void finish() {
        if (thread != null) {
            thread.interrupt();
        }
        isOperation = false;
        isCancel = false;
    }

    public void cancel() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    final Context getContext() throws RuntimeException {
        // FIXME 확인 후 불필요할 경우 삭제
        SAGTApplication application = (SAGTApplication) getApplication();
        Context ctx = application.getTopActivity();
        if (ctx == null) {
            return application.getApplicationContext();
        } else {
            return ctx;
        }
    }

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

