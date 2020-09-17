package kr.go.mobile.agent.solution;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

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
        public RESULT_CODE code;
        public String errorMessage;
        public OUT out;
        public Result(RESULT_CODE code) {
            this(code, null);
        }
        public Result(RESULT_CODE code, String errorMessage) {
            this.code = code;
            this.errorMessage = errorMessage;
        }
    }

    public interface EventListener<OUT> {
        // 사용자 취소
        void onCancel(Context context);
        // 모듈 연계 실패
        void onFailure(Context context, String message, Throwable t);
        // 모듈 처리 에러
        void onError(Context context, RESULT_CODE errorCode, String errorMessage);
        // 모듈 처리 정상
        void onCompleted(Context context, OUT out);
    }

    final Object LOCK = new Object();
    final EventListener<OUT> mEventListener;

    private Context mContext;
    boolean isOperation = false;
    boolean isCancel = false;
    Thread thread;

    public Solution(EventListener<OUT> listener) {
        this.mEventListener = (listener == null) ? new EventListener<OUT>() {
            @Override
            public void onCancel(Context context) {
                Log.w("Solution.EventListener", "Solution.EventListener is null, Solution 생성자에 EventListener 를 선언하시기 바랍니다. (onCancel)");
            }

            @Override
            public void onFailure(Context context, String message, Throwable t) {
                Log.w("Solution.EventListener", "Solution.EventListener is null, Solution 생성자에 EventListener 를 선언하시기 바랍니다. (onFailure)");
            }

            @Override
            public void onError(Context context, RESULT_CODE errorCode, String errorMessage) {
                Log.e("@@@", errorMessage + ", code : " + errorCode);
                Log.w("Solution.EventListener", "Solution.EventListener is null, Solution 생성자에 EventListener 를 선언하시기 바랍니다. (onError)");
            }

            @Override
            public void onCompleted(Context context, Object o) {
                Log.w("Solution.EventListener", "Solution.EventListener is null, Solution 생성자에 EventListener 를 선언하시기 바랍니다. (onCompleted)");
            }
        } : listener;
    }

    public final void execute() {
        execute((IN) null);
    }

    public final void execute(boolean isAsyncExecute) {
        execute((IN) null, isAsyncExecute);
    }

    public final void execute(IN in) {
        execute(in, true);
    }

    public final void execute(IN in, boolean isAsyncExecute) {
        synchronized (LOCK) {
            if (isOperation) {
                Log.d(getClass().getSimpleName(), "이미 실행 중입니다.");
                return;
            }
            isCancel = false;
            isOperation = true;
        }

        this.mContext = getContext();

        prepare(mContext, in);

        final Result<OUT>[] result = new Result[1];

        if (isAsyncExecute) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    result[0] = execute(mContext);
                    if(result[0] == null) {
                        return;
                    }
                    processResult(mContext, result[0]);
                }
            });
            thread.setName(getClass().getName());
            thread.start();
        } else {
            result[0] = execute(mContext);
            if (result[0] == null) {
                return;
            }
            processResult(mContext, result[0]);
        }

    }

    protected void prepare(Context context, IN in) { }

    protected abstract Result<OUT> execute(Context context);

    protected void processResult(Context context, Result<OUT> result) {
        if (result.code == RESULT_CODE._OK) {
            onCompleted(result.out);
        } else {
            onError(result.code, result.errorMessage);
        }
    }

    protected final void onCompleted(OUT out) {
        synchronized (LOCK) {
            isOperation = false;
            if (isCancel) {
                return;
            }
        }
        mEventListener.onCompleted(mContext, out);
    }

    protected final void onCancel() {
        synchronized (LOCK) {
            this.isCancel = true;
            this.isOperation = false;
        }
        mEventListener.onCancel(mContext);
    }

    protected final void onFailure(Throwable t) {
        synchronized (LOCK) {
            isOperation = false;
        }
        mEventListener.onFailure(mContext, t.getMessage(), t);
    }

    protected void onError(RESULT_CODE code, String errorMessage) {
        synchronized (LOCK) {
            isOperation = false;
        }
        mEventListener.onError(mContext, code, errorMessage);
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
        synchronized (LOCK) {
            isOperation = false;
            isCancel = false;
            mContext = null;
        }
    }


    public void cancel() {
        synchronized (LOCK) {
            if(isCancel || !isOperation) {
                return;
            }
            isCancel = true;
        }

        if (thread != null) {
            thread.interrupt();
        } else {
            mEventListener.onCancel(mContext);
        }
    }

    protected final Context getContext() throws RuntimeException {
        // FIXME 확인 후 불필요할 경우 삭제
        SAGTApplication application = (SAGTApplication) getApplication();
        Context ctx = application.getTopActivity();
        if (ctx == null) {
            return application.getApplicationContext();
        } else {
            return ctx;
        }
    }

    protected final Application getApplication() {
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
