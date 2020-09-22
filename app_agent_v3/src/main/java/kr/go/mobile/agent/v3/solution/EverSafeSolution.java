package kr.go.mobile.agent.v3.solution;

import android.content.Context;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import kr.co.everspin.eversafe.EversafeHelper;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.mobp.iff.R;

public class EverSafeSolution extends Solution<Void, String> {

    private static final String TAG = EverSafeSolution.class.getSimpleName();
    private static final int STATUS_WAIT = 0;
    private static final int STATUS_FINISH = 1;

    private static final int ERROR_NONE = 1;
    private static final int ERROR_EMERGENCY = 10;
    private static final int ERROR_TIMEOUT = 11;
    private static final int ERROR_FAIL = 12;
    private static final int ERROR_CANCEL = 13;

    AtomicInteger status;
    int errorCode;
    String agentToken;
    EversafeHelper.GetVerificationTokenTask task;

    public EverSafeSolution(Context context) {
        super(context);
    }

    @Override
    protected void prepare(Context context) {
        super.prepare(context);
        Log.d(TAG, "STEP 1. EverSafe 초기화합니다.");

        EversafeHelper.getInstance().setBackgroundMaintenanceSec(600);
        EversafeHelper.getInstance().initialize(context.getString(R.string.msmurl));

        // AsyncTask 는 장기적으로 @deprecated 예정이다. MSM 라이브러리에서 대응이 필요한 코드
        task = new EversafeHelper.GetVerificationTokenTask() {
            @Override
            protected void onCompleted(byte[] verificationToken, String verificationTokenAsByte64, boolean isEmergency) {
                if (isEmergency) {
                    errorCode = ERROR_EMERGENCY;
                } else {
                    agentToken = verificationTokenAsByte64;
                    errorCode = ERROR_NONE;
                }
                status.getAndSet(STATUS_FINISH);
            }
            @Override
            protected void onTimeover() {
                errorCode = ERROR_TIMEOUT;
                status.getAndSet(STATUS_FINISH);
            }
            @Override
            protected void onTerminated() {
                errorCode = ERROR_FAIL;
                status.getAndSet(STATUS_FINISH);
            }
            @Override
            protected void onCancel() {
                errorCode = ERROR_CANCEL;
                status.getAndSet(STATUS_FINISH);
            }
        }.setTimeout(60000);
    }

    @Override
    protected Result<String> execute(Context context, Void v) {
        Log.d(TAG, "STEP 2. EverSafe 실행 합니다.");
        status = new AtomicInteger(STATUS_WAIT);
        task.execute();

        // AsyncTask 종료까지 대기 및 리턴값 획득
        Result<String> ret;
        try {
            Log.d(TAG, "STEP 3. EverSafe 실행 응답을 기다립니다.");
            do {

            } while (status.compareAndSet(STATUS_WAIT, STATUS_WAIT));

            Log.d(TAG, "STEP 4. EverSafe 실행 결과를 처리합니다. ");
            switch (errorCode) {
                case ERROR_CANCEL:
                    ret = new Result<>(RESULT_CODE._CANCEL, "사용자에 의하여 취소되었습니다.");
                    break;
                case ERROR_FAIL:
                    Log.d(TAG, "종료 - 무결성 체크를 진행할 수 없습니다.");
                    ret = new Result<>(RESULT_CODE._INVALID, "무결성 체크를 진행할 수 없습니다.");
                    break;
                case ERROR_EMERGENCY:
                    Log.d(TAG, "종료 - 무결성 체크를 할 수 없는 환경입니다.");
                    ret = new Result<>(RESULT_CODE._FAIL, "무결성 체크할 수 있는 환경이 아닙니다.");
                    break;
                case ERROR_TIMEOUT:
                    Log.d(TAG, "종료 - 무결성 체크에 대한 응답값이 존재하지 않습니다.");
                    ret = new Result<>(RESULT_CODE._TIMEOUT, "무결성 체크에 대한 응값값이 존재하지 않습니다.");
                    break;
                case ERROR_NONE:
                    ret = new Result<>(RESULT_CODE._OK, "");
                    ret.out = agentToken;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + errorCode);
            }
        } catch (Exception e) {
            ret = new Result<>(RESULT_CODE._INVALID, e.getMessage());
        }
        return ret; // --> onCompleted() 호출
    }
}
