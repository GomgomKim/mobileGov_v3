package kr.go.mobile.common.v3.hybrid.plugin;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.common.v3.hybrid.CBHybridActivity;
import kr.go.mobile.common.v3.hybrid.CBHybridException;

/**
 * Created by ChangBum Hong on 2020-07-27.
 * cloverm@infrawareglobal.com
 * 전화/문자 관련 플러그인
 */
public class CBHybridTelephonyPlugin extends CBHybridPlugin {

    private static final String TAG = CBHybridTelephonyPlugin.class.getSimpleName();

    private static final int REQ_PERMISSION_CALL_PHONE = 1001;
    private static final int REQ_PERMISSION_SEND_SMS = 1002;
    private static final int STATE_READY = 0;
    private static final int STATE_NO_SIM = 1;
    private static final int STATE_AIR_PLANE = 2;
    private static final String JSON_PARAM_PHONE_NUMBER = "phoneNumber";
    private static final String JSON_PARAM_DIRECT = "direct";
    private static final String JSON_PARAM_MESSAGE = "message";

    private boolean isWaitReqPerCallPhone = true;
    private boolean isWaitReqPerSendSMS = true;

    public CBHybridTelephonyPlugin(Context context) {
        super(context);
        CBHybridActivity.IRequestPermissionListener mRequestPermissionListener = new CBHybridActivity.IRequestPermissionListener() {
            @Override
            public void onResult(int reqCode, boolean isGranted) {

                switch (reqCode) {
                    case REQ_PERMISSION_CALL_PHONE:
                        isWaitReqPerCallPhone = false;
                        break;
                    case REQ_PERMISSION_SEND_SMS:
                        isWaitReqPerSendSMS = false;
                        break;
                }
            }
        };
        addRequestPermissionListener(REQ_PERMISSION_CALL_PHONE, mRequestPermissionListener);
        addRequestPermissionListener(REQ_PERMISSION_SEND_SMS, mRequestPermissionListener);
    }


    @Override
    public String getVersionName() {
        return "1.0.0";
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

    }


    /**
     * 구버전 라이브러리 지원
     * 에러코드를 맞춰주기 위해서 별도 처리
     * 전화 걸기 기능
     * direct = true 전화걸기 , false 전화앱 실행
     *
     * @param jsonArgs {"phoneNumber":"xxxxxxxxxxx", "direct":true}
     * @return
     */
    @Deprecated
    public CBHybridPluginResult mdhCall(String jsonArgs) throws CBHybridException {
        Log.d(TAG, "mdhCall json=" + jsonArgs);
        boolean direct = true;

        try {
            JSONObject jsonObject = new JSONObject(jsonArgs);
            if (jsonObject.has(JSON_PARAM_DIRECT)) {
                direct = jsonObject.getBoolean(JSON_PARAM_DIRECT);
            }
        } catch (JSONException e) {
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR, "MDH_JSON_EXP_ERROR");
        }

        if (direct) {
            call(jsonArgs);
        } else {
            dial(jsonArgs);
        }

        //"OK" 는 구 라이브러리에서 정상 실행시 javascript로 전달해주는 문구임 (중요할 수 있음)
        return new CBHybridPluginResult("OK");
    }

    /**
     * 구라이브러리 지원
     * 문자 보내기 기능
     * 에러코드를 맞춰주기 위해서 별도 처리
     * direct = true 직접 보내기 , false 문자앱 실행
     *
     * @param callbackID
     * @param jsonArgs   {"phoneNumber":["xxxxxxxxx","xxxxxxx",...],"direct":false,"message":"xxx"}
     */
    @Deprecated
    public void mdhSendSMSAsync(final String callbackID, String jsonArgs) throws CBHybridException {
        Log.d(TAG, "mdhSendSMSAsync callbackID=" + callbackID + "/json=" + jsonArgs);
        SmsInfo smsInfo = convertSmsInfo(jsonArgs);
        if (smsInfo.isDirect()) {
            sendSMS(jsonArgs, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if (getActivity().isDestroyed()) {
                        return;
                    }

                    switch (this.getResultCode()) {
                        case Activity.RESULT_OK: {
                            CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult("OK");
                            sendAsyncResult(callbackID, cbHybridPluginResult);
                        }
                        break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE: {
                            CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_GENERIC_FAILURE);
                            sendAsyncResult(callbackID, cbHybridPluginResult);
                        }
                        break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF: {
                            CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_RADIO_OFF);
                            sendAsyncResult(callbackID, cbHybridPluginResult);
                        }
                        break;
                        case SmsManager.RESULT_ERROR_NULL_PDU: {
                            CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_SMS_NULL_PDU);
                            sendAsyncResult(callbackID, cbHybridPluginResult);
                        }
                        break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE: {
                            CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_NO_SMS_SERVICE);
                            sendAsyncResult(callbackID, cbHybridPluginResult);
                        }
                        break;
                        default:
                            CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_UNKNOWN);
                            sendAsyncResult(callbackID, cbHybridPluginResult);
                    }
                    //등록 리시버 해제(중요)
                    getActivity().unregisterReceiver(this);
                }
            });
        } else {
            launchSMS(jsonArgs);
        }
    }

    /**
     * 전화 바로 걸기
     * CALL_PHONE 퍼미션 필요
     *
     * @param jsonArgs {"phoneNumber":"xxxxxxxxxxx"}
     * @return
     */
    public CBHybridPluginResult call(String jsonArgs) throws CBHybridException {
        Log.d(TAG, "call json=" + jsonArgs);

        String phoneNumber = getPhoneNumber(jsonArgs);

        validPhoneState();

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.CALL_PHONE)) {
                throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_PERMISSION_DENIED, "전화걸기 권한이 허용되지 않았습니다.");
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.CALL_PHONE},
                        REQ_PERMISSION_CALL_PHONE);

                //퍼미션 허용/거부 대기
                while (isWaitReqPerCallPhone) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_PERMISSION_DENIED, "전화걸기 권한이 허용되지 않았습니다.");
                    }
                }

                isWaitReqPerCallPhone = true;
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_PERMISSION_DENIED, "전화걸기 권한이 허용되지 않았습니다.");
                }
            }
        }

        Intent dialIntent = new Intent(Intent.ACTION_CALL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);

        return new CBHybridPluginResult("");
    }

    /**
     * 전화 앱 실행
     *
     * @param jsonArgs {"phoneNumber":"xxxxxxx"}
     * @return
     */
    public CBHybridPluginResult dial(String jsonArgs) throws CBHybridException {
        Log.d(TAG, "dial json=" + jsonArgs);
        String phoneNumber = getPhoneNumber(jsonArgs);

        validPhoneState();

        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);

        return new CBHybridPluginResult("");
    }


    /**
     * 문자 보내기 기능(직접발송) / javascript에서 호출부분
     * SEND_SMS 퍼미션 필요
     *
     * @param callbackID
     * @param jsonArgs   {"phoneNumber":["xxxxxxxxx","xxxxxxx",...],"message":"xxx"}
     */
    public void sendSMSAsync(final String callbackID, String jsonArgs) throws CBHybridException {
        Log.d(TAG, "sendSMSAsync callbackID=" + callbackID + "/json=" + jsonArgs);
        sendSMS(jsonArgs, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (getActivity().isDestroyed()) {
                    return;
                }

                switch (this.getResultCode()) {
                    case Activity.RESULT_OK: {
                        CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult("OK");
                        sendAsyncResult(callbackID, cbHybridPluginResult);
                    }
                    break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE: {
                        CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_GENERIC_FAILURE);
                        sendAsyncResult(callbackID, cbHybridPluginResult);
                    }
                    break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF: {
                        CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_RADIO_OFF);
                        sendAsyncResult(callbackID, cbHybridPluginResult);
                    }
                    break;
                    case SmsManager.RESULT_ERROR_NULL_PDU: {
                        CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_SMS_NULL_PDU);
                        sendAsyncResult(callbackID, cbHybridPluginResult);
                    }
                    break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE: {
                        CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_NO_SMS_SERVICE);
                        sendAsyncResult(callbackID, cbHybridPluginResult);
                    }
                    break;
                    default:
                        CBHybridPluginResult cbHybridPluginResult = new CBHybridPluginResult(CommonBasedConstants.HYBRID_ERROR_UNKNOWN);
                        sendAsyncResult(callbackID, cbHybridPluginResult);
                }
                //등록 리시버 해제(중요)
                getActivity().unregisterReceiver(this);
            }
        });
    }


    /**
     * 문자 보내기 기능(직접발송)
     * SEND_SMS 퍼미션 필요
     *
     * @param jsonArgs {"phoneNumber":["xxxxxxxxx","xxxxxxx",...],"message":"xxx"}
     * @param receiver SMS Send 상태 수신 리시버
     */
    public void sendSMS(String jsonArgs, BroadcastReceiver receiver) throws CBHybridException {
        Log.d(TAG, "sendSMS json=" + jsonArgs);
        SmsInfo smsInfo = convertSmsInfo(jsonArgs);

        //phone 상태 확인
        validPhoneState();

        //퍼미션 확인
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.SEND_SMS)) {

                throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_PERMISSION_DENIED, "문자보내기 권한이 허용되지 않았습니다.");

            } else {

                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.SEND_SMS},
                        REQ_PERMISSION_SEND_SMS);

                //퍼미션 허용/거부 대기
                while (isWaitReqPerSendSMS) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_PERMISSION_DENIED, "문자보내기 권한이 허용되지 않았습니다.");
                    }
                }

                isWaitReqPerSendSMS = true;

                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_PERMISSION_DENIED, "문자보내기 권한이 허용되지 않았습니다.");
                }
            }
        }


        Intent smsSendIntent = new Intent("ACTION_SMS_SEND");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, smsSendIntent, PendingIntent.FLAG_ONE_SHOT);
        getActivity().registerReceiver(receiver, new IntentFilter(smsSendIntent.getAction()));
        SmsManager smsManager = SmsManager.getDefault();

        for (int i = 0; i < smsInfo.getPhoneNumberList().size(); i++) {
            smsManager.sendTextMessage(smsInfo.getPhoneNumberList().get(i), null, smsInfo.getMessage(), pendingIntent, null);
        }
    }


    /**
     * 문자 보내기 기능(문자앱 실행)
     * 성공시 별도 리턴 없음
     *
     * @param jsonArgs {"phoneNumber":["xxxxxxxxx","xxxxxxx",...],"message":"xxx"}
     * @throws CBHybridException
     */
    public void launchSMS(String jsonArgs) throws CBHybridException {
        Log.d(TAG, "launchSMS json=" + jsonArgs);
        SmsInfo smsInfo = convertSmsInfo(jsonArgs);

        validPhoneState();

        List<String> phoneNumberList = smsInfo.getPhoneNumberList();
        StringBuilder address = new StringBuilder();
        for (int i = 0; i < phoneNumberList.size(); i++) {
            address.append(phoneNumberList.get(i)).append(";");
        }

        Intent smsSendIntent = new Intent(Intent.ACTION_SENDTO);
        smsSendIntent.setData(Uri.parse("smsto:" + address));
        smsSendIntent.putExtra("sms_body", smsInfo.getMessage());
        startActivity(smsSendIntent);
    }


    /**
     * JSON으로부터 전화번호 추출
     *
     * @param jsonArgs {"phoneNumber":"xxxxxx"}
     * @return
     * @throws CBHybridException
     */
    private String getPhoneNumber(String jsonArgs) throws CBHybridException {
        String phoneNumber = null;
        try {
            JSONObject jsonObject = new JSONObject(jsonArgs);
            if (jsonObject.has(JSON_PARAM_PHONE_NUMBER)) {
                phoneNumber = jsonObject.getString(JSON_PARAM_PHONE_NUMBER);
            }
        } catch (JSONException e) {
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR, "입력 데이터 표현이 잘못되었습니다.");
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_INVALID_PARAMETER, "전화번호 정보가 존재하지 않습니다.");
        }
        return phoneNumber;
    }

    /**
     * JSON으로부터 SMS 정보 생성
     *
     * @param jsonArgs JSON 파라미터
     * @return SmsInfo
     * @throws CBHybridException
     */
    private SmsInfo convertSmsInfo(String jsonArgs) throws CBHybridException {

        List<String> phoneNumberList = new ArrayList<String>();
        String sendMsg = "";
        boolean isDirect = false;

        try {
            JSONObject jsonObject = new JSONObject(jsonArgs);
            if (jsonObject.has(JSON_PARAM_PHONE_NUMBER)) {
                JSONArray jsonArray = jsonObject.getJSONArray(JSON_PARAM_PHONE_NUMBER);

                for (int i = 0; i < jsonArray.length(); i++) {
                    Integer.parseInt(jsonArray.getString(i));
                    phoneNumberList.add(jsonArray.getString(i));
                }
            }

            if (jsonObject.has(JSON_PARAM_DIRECT)) {
                isDirect = jsonObject.getBoolean(JSON_PARAM_DIRECT);
            }

            if (jsonObject.has(JSON_PARAM_MESSAGE)) {
                sendMsg = jsonObject.getString(JSON_PARAM_MESSAGE);
            }
        } catch (JSONException e) {
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR, "문자 전송에 대한 입력값이 잘못되었습니다.");

        } catch (NumberFormatException e) {
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_INVALID_PARAMETER, "문자를 전송할 대상은 번호로만 지정가능합니다.");
        }

        if (TextUtils.isEmpty(sendMsg) || phoneNumberList.size() == 0) {
            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_INVALID_PARAMETER, "전송할 메시지 또는 대상 번호가 없습니다.");
        }

        SmsInfo smsInfo = new SmsInfo(phoneNumberList, sendMsg, isDirect);

        return smsInfo;
    }

    /**
     * Phone 상태 확인
     *
     * @throws CBHybridException
     */
    private void validPhoneState() throws CBHybridException {

        switch (getPhoneState()) {
            case STATE_AIR_PLANE:
                throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_AIRPLANE_MODE, "비행기 모드에서는 전화걸기를 할 수 없습니다.");
            case STATE_NO_SIM:
                throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_NO_SIM, "SIM 카드가 존재하지 않습니다.");
            default:
        }
    }


    /**
     * 전화 기능 사용을 위한 상태 리턴
     *
     * @return
     */
    private int getPhoneState() {
        if (isAirplaneModeOn(getContext())) {
            return STATE_AIR_PLANE;
        }

        if (!isSimStateReady(getContext())) {
            return STATE_NO_SIM;
        }

        return STATE_READY;
    }

    /**
     * 비행기 모드 여부 확인
     *
     * @param context
     * @return
     */
    private boolean isAirplaneModeOn(Context context) {
        return android.provider.Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /**
     * SIM 사용 여부 확인
     *
     * @param context
     * @return
     */
    private boolean isSimStateReady(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
    }


    /**
     * SMS 발송 정보
     */
    private class SmsInfo {
        private List<String> phoneNumberList;
        private String message;
        private boolean direct; //구라이브러리 관련

        public SmsInfo(List<String> phoneNumberList, String message) {
            this.phoneNumberList = phoneNumberList;
            this.message = message;
        }

        //구라이브러리 지원
        @Deprecated
        public SmsInfo(List<String> phoneNumberList, String message, boolean direct) {
            this.phoneNumberList = phoneNumberList;
            this.message = message;
            this.direct = direct;
        }

        public List<String> getPhoneNumberList() {
            return this.phoneNumberList;
        }


        public String getMessage() {
            return this.message;
        }


        @Deprecated
        public boolean isDirect() {
            return this.direct;
        }
    }
}
