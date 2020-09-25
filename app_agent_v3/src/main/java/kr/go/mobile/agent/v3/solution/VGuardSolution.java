package kr.go.mobile.agent.v3.solution;

import android.content.Context;
import android.content.Intent;

import com.infratech.ve.agent.remote.VGObserver;
import com.infratech.ve.agent.remote.VGRemote;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import kr.go.mobile.agent.service.monitor.ThreatDetection;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.utils.Log;

public class VGuardSolution extends Solution<Void, ThreatDetection.STATUS> implements VGObserver {

    private static final String TAG = VGuardSolution.class.getSimpleName();

    static final int ERROR_NONE = 1;
    static final int ERROR_NO_BIND = 100;
    static final int ERROR_SETTING_FAILED = 101;
    static final int ERROR_CMD_PERMISSION_NOT_GRANTED = 102;
    static final int ERROR_READ_THREAT = 103;
    static final int ERROR_SAVE_POLICY_FILE = 104;
    static final int ERROR_APPLY_POLICY_FILE = 105;
    static final int ERROR_DISABLED_REALTIME_SCAN = 10001;
    static final int ERROR_INFECTED_PACKAGE = 10002;
    static final int ERROR_ROOTING_DEVICE = 10003;

    final AtomicBoolean isBound = new AtomicBoolean(false);
    VGRemote mVGRemote;
    boolean applyPolicy = false;


    public VGuardSolution(Context context) {
        super(context);
        Log.d(TAG, "V-Guard 초기화");
        mVGRemote = new VGRemote(context, this);
    }


    @Override
    public void onBinded() {
        Log.d(TAG, "V-Guard 초기화 성공 ");
        isBound.getAndSet(true);
    }

    @Override
    public void onUnbinded() {
        Log.w(TAG, "V-Guard 연결 실패");
        mVGRemote.closeUnRegisterReceiver();
        isBound.getAndSet(false);
    }

    @Override
    public void onRegistedReceiver(Intent intent) {
        Log.i(TAG, "실시간 이벤트가 발생하였습니다.");
        // 실시간으로 VG 로부터 전달받는 이벤트.
        String action = intent.getAction();
        String strThreatData = intent.getStringExtra(VGRemote.SECYRITYLIST);
        if (Objects.equals(action, VGRemote.SECYRITYACTION) && strThreatData != null) {
            try {
                ThreatDetection.STATUS status = parse(strThreatData, true);
                Result<ThreatDetection.STATUS> result = new Result<>(RESULT_CODE._OK);
                result.out = status;
                // TODO 실시간으로 전달받은 메시지는 어떻게 처리할까 ???
                Log.e(TAG, "실시간으로 전달받은 메시지는 아직 처리하지 않고 있음. (status = " + status + ")");
                setResult(result);
            } catch (JSONException e) {
                Log.w(TAG, "V-Guard 엔터프라이즈로부터 수신한 이벤트 값을 확인할 수 없습니다.");
            }
        } else {
            Log.w(TAG, "V-Guard 엔터프라이즈로부터 수신한 이벤트 값을 확인할 수 없습니다.");
        }
    }


    @Override
    protected Result<ThreatDetection.STATUS> process(Context context, Void v) throws SolutionRuntimeException {

        do {
            if (!isBound.compareAndSet(false, false)) break;
        } while (true);

        int resultCode;

        Log.d(TAG, "STEP 1. V-Guard 허용 퍼미션을 확인합니다.");
        String ret = mVGRemote.VGRunCMD(VGRemote.CMD_VG_PERMISSION);
        if ((resultCode = checkResultMessage(ret)) != ERROR_NONE) {
            applyPolicy = false;
            return new Result<>(RESULT_CODE._INVALID, convertErrorMessage(resultCode));
        }
        if (applyPolicy) {
            Log.d(TAG, "STEP 2. V-Guard 정책이 이미 설정되어 있습니다.");
        } else {
            Log.d(TAG, "STEP 2. V-Guard 정책을 설정합니다.");
            ret = mVGRemote.VGRunCMD(VGRemote.CMD_POLICY_SAVE, "default_policy");
            if  ((resultCode = checkResultMessage(ret)) != ERROR_NONE) {
                return new Result<>(RESULT_CODE._INVALID, convertErrorMessage(resultCode));
            }
            ret = mVGRemote.VGRunCMD(VGRemote.CMD_POLICY_APPLY);
            if  ((resultCode = checkResultMessage(ret)) != ERROR_NONE) {
                return new Result<>(RESULT_CODE._INVALID, convertErrorMessage(resultCode));
            }
            applyPolicy = true;
        }

        Log.d(TAG, "STEP 3. V-Guard 스캔을 시작합니다.");
        ret = mVGRemote.VGRunCMD(VGRemote.CMD_VG_SECURITY_THREAT);
        try {
            Result<ThreatDetection.STATUS> result = new Result<>(RESULT_CODE._OK);
            Log.d(TAG, "STEP 4. V-Guard 스캔 결과 확인");
            result.out = parse(ret);
            return result;
        } catch (JSONException e) {
            throw new SolutionRuntimeException("응답 데이터 처리 중 에러가 발생하였습니다.", e);
        }
    }

    private int checkResultMessage(@NotNull String result) {
        String[] tmp = result.split(",");
        String resultData = tmp[0];
        if (resultData.equals(VGRemote.ERROR_SUCC)) {
            return ERROR_NONE;
        }

        int errorCode = -1;
        String errorMsg = "";
        if (tmp.length > 1) {
            String[] error = tmp[1].split("\\|");
            errorCode = Integer.parseInt(error[0]);
            if (error.length > 1) {
                errorMsg = error[1];
            }
        }

        if (errorCode > 300 && errorCode < 400) {
            // ERROR_SAVE_PERMISSION_NOT_GRANTED = 301;
            // ERROR_PHONE_PERMISSION_NOT_GRANTED = 302;
            // ERROR_VG_NOT_RUNNING = 303;
            mVGRemote.VGRunCMD(VGRemote.CMD_VG_RUN);
            return ERROR_CMD_PERMISSION_NOT_GRANTED;
        } else if (errorCode > 400 && errorCode < 500) {
            // ERROR_JSONPARSING_FAIL
            // ERROR_DATA_TYPE_FAIL
            // ERROR_PARAMETER_UNAVAILABLE
            // ERROR_POLICY_FILE_NOT_FOUND
            // ERROR_POLICY_FILE_IO
            return ERROR_SAVE_POLICY_FILE;
        } else if (errorCode > 500 && errorCode < 600) {
//            ERROR_SCAN_REALTIME_POLICY_SETTING_FAIL = 504;
//            ERROR_SCAN_RESERVATION_POLICY_SETTING_FAIL = 505;
//            ERROR_SCAN_ROOTING_POLICY_SETTING_FAIL = 506;
//            ERROR_SCAN_RESERVATION_TIME_POLICY_SETTING_FAIL = 507;
//            ERROR_UPDATE_AUTO_POLICY_SETTING_FAIL = 508;
//            ERROR_UPDATE_RESERVATION_POLICY_SETTING_FAIL = 509;
//            ERROR_UPDATE_RESERVATION_TIME_POLICY_SETTING_FAIL = 510;
//            ERROR_USER_SCAN_REALTIME_POLICY_SETTING_FAIL = 511;
//            ERROR_USER_SCAN_RESERVATION_POLICY_SETTING_FAIL = 512;
//            ERROR_USER_SCAN_ROOTING_POLICY_SETTING_FAIL = 513;
//            ERROR_USER_UPDATE_AUTO_POLICY_SETTING_FAIL = 514;
//            ERROR_USER_UPDATE_RESERVATION_POLICY_SETTING_FAIL = 515;
            return ERROR_APPLY_POLICY_FILE;
        }
        Log.e(TAG, "ERROR : " + errorCode + ", " + errorMsg);
        return ERROR_NO_BIND;
    }

    private ThreatDetection.STATUS parse(String strThreatData) throws JSONException {
        return parse(strThreatData,false);
    }

    private ThreatDetection.STATUS parse(String strThreatData, boolean realtime) throws JSONException {

        JSONObject jsonObject;
        String VG_DATA_SPLIT = ";";
        String[] arrData = strThreatData.split(VG_DATA_SPLIT);

        byte realtimeFlag = realtime ? (byte) 1 : (byte) 0;
        String VG_IS_ROOTING_DEVICE = "isRooting";
        String VG_IS_CHECKED_MALWARE = "isMalwareCheck";
        String VG_ENABLED_REALTIME_SCAN = "isRealtimeScanServiceEnable";

        for (String data : arrData) {
            if (data.contains(VG_IS_CHECKED_MALWARE)) {
                jsonObject = new JSONObject(data);
                if (jsonObject.getBoolean(VG_IS_CHECKED_MALWARE)) {
                    Log.d(TAG, "종료 - 악성코드 존재");
                    return ThreatDetection.STATUS._EXIST_MALWARE;

                }
            } else if (data.contains(VG_IS_ROOTING_DEVICE)) {
                jsonObject = new JSONObject(data);
                if (jsonObject.getBoolean(VG_IS_ROOTING_DEVICE)) {
                    Log.d(TAG, "종료 - 루팅 단말");
                    return ThreatDetection.STATUS._ROOTING_DEVICE;
                }
            } else if (data.contains(VG_ENABLED_REALTIME_SCAN)) {
                jsonObject = new JSONObject(data);
                if (!jsonObject.getBoolean(VG_ENABLED_REALTIME_SCAN)) {
                    Log.d(TAG, "종료 - 실시간 검사 비활성화");
                    return ThreatDetection.STATUS._DISABLED_REAL_TIME_SCAN;

                }
            }
        }

        return ThreatDetection.STATUS._SAFE;
    }

    private String convertErrorMessage(int errorCode) {
        switch (errorCode) {
            case ERROR_NO_BIND:
                return "V-Guard 서비스와 연결되지 않았습니다..";
            case ERROR_SAVE_POLICY_FILE:
                return "정책 파일을 저장할 수 없습니다";
            case ERROR_APPLY_POLICY_FILE:
                return "정책 파일을 설정할 수 없습니다.";
            case ERROR_SETTING_FAILED:
                return "V-Guard 정책을 설정할 수 없습니다.";
            case ERROR_CMD_PERMISSION_NOT_GRANTED:
                return "권한 허용을 적용하기 위하여 앱을 다시 실행하시기 바랍니다.";
            case ERROR_READ_THREAT:
                return "V-Guard 위협 사항 데이터를 확인할 수 없습니다.";
            case ERROR_DISABLED_REALTIME_SCAN:
                return "실시간검사가 비활성화 되어 있습니다.";
            case ERROR_INFECTED_PACKAGE:
                return "악성코드가 발견되었습니다.";
            case ERROR_ROOTING_DEVICE:
                return "루팅 단말입니다.";
            default:
                return "정의되지 않은 오류가 발생하였습니다.";
        }
    }

}
