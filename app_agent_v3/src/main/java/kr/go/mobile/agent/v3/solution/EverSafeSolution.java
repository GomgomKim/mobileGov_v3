package kr.go.mobile.agent.v3.solution;

import android.content.Context;

import java.util.concurrent.atomic.AtomicInteger;

import kr.co.everspin.eversafe.EversafeHelper;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.mobp.iff.R;

public class EverSafeSolution extends Solution<Void, String> {

    private static final String TAG = EverSafeSolution.class.getSimpleName();
    private static final int STATUS_WAIT = 0;
    private static final int STATUS_OK = 1;
    private static final int STATUS_EMERGENCY = 10;
    private static final int STATUS_TIMEOUT = 11;
    private static final int STATUS_FAIL = 12;
    private static final int STATUS_CANCEL = 13;

    AtomicInteger status;
    String agentToken;

    public EverSafeSolution(Context context) {
        super(context);
        Log.d(TAG, "무결성 검증 솔루션 초기화");
        EversafeHelper.getInstance().initialize(context.getString(R.string.msmurl));
        EversafeHelper.getInstance().setBackgroundMaintenanceSec(600);
    }

    @Override
    protected Result<String> process(Context context, Void v) {
        // AsyncTask 는 장기적으로 @deprecated 예정이다. MSM 라이브러리에서 대응이 필요한 코드
        EversafeHelper.GetVerificationTokenTask  task = new EversafeHelper.GetVerificationTokenTask() {
            @Override
            protected void onCompleted(byte[] verificationToken, String verificationTokenAsByte64, boolean isEmergency) {
                if (isEmergency) {
                    status.getAndSet(STATUS_EMERGENCY);
                } else {
                    agentToken = verificationTokenAsByte64;
                    status.getAndSet(STATUS_OK);
                }

            }
            @Override
            protected void onTimeover() {
                status.getAndSet(STATUS_TIMEOUT);
            }
            @Override
            protected void onTerminated() {
                status.getAndSet(STATUS_FAIL);
            }
            @Override
            protected void onCancel() {
                status.getAndSet(STATUS_CANCEL);
            }
        }.setTimeout(60000);

        Log.d(TAG, "STEP 1. EverSafe 실행 합니다.");
        status = new AtomicInteger(STATUS_WAIT);
        task.execute();

        // AsyncTask 종료까지 대기 및 리턴값 획득
        Result<String> ret;
        try {
            Log.d(TAG, "STEP 2. EverSafe 실행 응답을 기다립니다.");
            do {

            } while (status.compareAndSet(STATUS_WAIT, STATUS_WAIT));

            Log.d(TAG, "STEP 3. EverSafe 실행 결과를 처리합니다." );

            switch (status.get()) {
                case STATUS_CANCEL:
                    ret = new Result<>(RESULT_CODE._CANCEL, "사용자에 의하여 취소되었습니다.");
                    break;
                case STATUS_FAIL:
                    Log.d(TAG, "종료 - 무결성 체크를 진행할 수 없습니다.");
                    ret = new Result<>(RESULT_CODE._INVALID, "무결성 체크를 진행할 수 없습니다.");
                    break;
                case STATUS_EMERGENCY:
                    Log.d(TAG, "종료 - 무결성 체크를 할 수 없는 환경입니다.");
                    ret = new Result<>(RESULT_CODE._FAIL, "무결성 체크할 수 있는 환경이 아닙니다.");
                    break;
                case STATUS_TIMEOUT:
                    Log.d(TAG, "종료 - 무결성 체크에 대한 응답값이 존재하지 않습니다.");
                    ret = new Result<>(RESULT_CODE._TIMEOUT, "무결성 체크에 대한 응값값이 존재하지 않습니다.");
                    break;
                case STATUS_OK:
                    ret = new Result<>(RESULT_CODE._OK, "");
                    ret.out = agentToken;
                    break;
                default:
                    throw new IllegalStateException("무결성 검증 결과로 예상되지 않은 값이 전달되었습니다. " + status.get());
            }
        } catch (Exception e) {
            ret = new Result<>(RESULT_CODE._INVALID, e.getMessage());
        }
        return ret; // --> onCompleted() 호출
    }
}
