package kr.go.mobile.agent.v3.solution;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.infratech.ve.agent.remote.VGObserver;
import com.infratech.ve.agent.remote.VGRemote;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import kr.go.mobile.agent.service.monitor.ThreatData;
import kr.go.mobile.agent.solution.Solution;

import static kr.go.mobile.agent.service.monitor.ThreatData.STATUS_THREATS;

public class VGuardSolution extends Solution<Void, ThreatData> implements VGObserver {

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

    VGRemote mVGRemote;
    final AtomicBoolean isBound = new AtomicBoolean(false);

    public VGuardSolution(EventListener<ThreatData> listener) {
        super(listener);
    }

    @Override
    protected void prepare(Context context, Void in) {
        Log.d(TAG, "STEP 1. V-Guard 초기화 ");
        mVGRemote = new VGRemote(context, this);
    }

    @Override
    public void onBinded() {
        Log.d(TAG, "STEP 2. V-Guard 연결 성공 ");
        isBound.getAndSet(true);
    }

    @Override
    public void onUnbinded() {
        Log.d(TAG, "V-Guard 연결 해제");
        mVGRemote.closeUnRegisterReceiver();
        isBound.getAndSet(false);
    }

    @Override
    public void onRegistedReceiver(Intent intent) {
        String action = intent.getAction();
        String strThreatData = intent.getStringExtra(VGRemote.SECYRITYLIST);
        if (Objects.equals(action, VGRemote.SECYRITYACTION) && strThreatData != null) {
            Result<ThreatData> result = validSecurityThreat(strThreatData, true);
            processResult(getContext(), result);
        } else {
            Log.w(TAG, "V-Guard 엔터프라이즈로부터 수신한 이벤트 값을 확인할 수 없습니다.");
        }
    }


    @Override
    protected Result<ThreatData> execute(Context context) {

        while (true) {
            if (!isBound.compareAndSet(false, false)) break;
        }

        int resultCode;

        Log.d(TAG, "STEP 3. V-Guard 허용 퍼미션을 확인합니다.");
        String ret = mVGRemote.VGRunCMD(VGRemote.CMD_VG_PERMISSION);
        if ((resultCode = checkResultMessage(ret)) != ERROR_NONE) {
            return new Result<>(RESULT_CODE._INVALID, convertErrorMessage(resultCode));
        }
        Log.d(TAG, "STEP 4. V-Guard 정책을 설정합니다.");
        ret = mVGRemote.VGRunCMD(VGRemote.CMD_POLICY_SAVE, "default_policy");
        if  ((resultCode = checkResultMessage(ret)) != ERROR_NONE) {
            return new Result<>(RESULT_CODE._INVALID, convertErrorMessage(resultCode));
        }
/*
        ret = mVGRemote.VGRunCMD(VGRemote.CMD_POLICY_APPLY);
        if  ((resultCode = checkResultMessage(ret)) != ERROR_NONE) {
            return new Result(RESULT_CODE._INVALID, convertErrorMessage(resultCode));
        }
*/
        Log.d(TAG, "STEP 5. V-Guard 스캔을 시작합니다.");
        ret = mVGRemote.VGRunCMD(VGRemote.CMD_VG_SECURITY_THREAT);
        return validSecurityThreat(ret);
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

    private Result<ThreatData> validSecurityThreat(String strThreatData) {
        return validSecurityThreat(strThreatData, false);
    }

    private Result<ThreatData> validSecurityThreat(String strThreatData, boolean realtime) {
        Log.d(TAG, "STEP 6. V-Guard 스캔 결과 확인");
        JSONObject jsonObject;
        String VG_DATA_SPLIT = ";";
        String[] arrData = strThreatData.split(VG_DATA_SPLIT);
        ThreatData threatData = new ThreatData();
        try {
            byte realtimeFlag = realtime ? (byte) 1 : (byte) 0;
            String VG_IS_ROOTING_DEVICE = "isRooting";
            String VG_IS_CHECKED_MALWARE = "isMalwareCheck";
            String VG_ENABLED_REALTIME_SCAN = "isRealtimeScanServiceEnable";

            for (String data : arrData) {
                if (data.contains(VG_IS_CHECKED_MALWARE)) {
                    jsonObject = new JSONObject(data);
                    if (jsonObject.getBoolean(VG_IS_CHECKED_MALWARE)) {
                        Log.d(TAG, "종료 - 악성코드 존재");
                        threatData = new ThreatData(STATUS_THREATS,
                                "단말에 악성코드(앱)이 존재합니다. 치료 후 다시 실행해주시기 바랍니다.",
                                realtimeFlag);
                        break;
                    }
                } else if (data.contains(VG_IS_ROOTING_DEVICE)) {
                    jsonObject = new JSONObject(data);
                    if (jsonObject.getBoolean(VG_IS_ROOTING_DEVICE)) {
                        Log.d(TAG, "종료 - 루팅 단말");
                        threatData = new ThreatData(STATUS_THREATS,
                                "루팅된 단말입니다. 보안을 위하여 루팅 단말에서 실행할 수 없습니다.",
                                realtimeFlag);
                        break;
                    }
                } else if (data.contains(VG_ENABLED_REALTIME_SCAN)) {
                    jsonObject = new JSONObject(data);
                    if (!jsonObject.getBoolean(VG_ENABLED_REALTIME_SCAN)) {
                        Log.d(TAG, "종료 - 실시간 검사 비활성화");
                        threatData = new ThreatData(STATUS_THREATS,
                                "보안을 위하여 실시간 검사가 활성화되어 있어야 합니다. V-Guard 환경설정에서 실시간 검사를 활성화해주시기 바랍니다.",
                                realtimeFlag);
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "종료 - 결과 확인 중 에러", e);
            return new Result<>(RESULT_CODE._INVALID, e.getMessage());
        }

        Result<ThreatData> result = new Result<>(RESULT_CODE._OK);
        result.out = threatData;
        return result;
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

    @Override
    protected void processResult(Context context, Result<ThreatData> result) {
        super.processResult(context, result);
        if(mVGRemote != null) {
            mVGRemote.close();
        }
    }
}