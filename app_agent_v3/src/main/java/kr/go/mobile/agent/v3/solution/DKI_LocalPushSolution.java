package kr.go.mobile.agent.v3.solution;

import android.content.Context;
import android.os.Bundle;

import com.dkitec.PushLibrary.Listener.PushAppRegistListener;
import com.dkitec.PushLibrary.PushLibrary;
import com.dkitec.PushLibrary.Receiver.PushLibraryReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.PushMessageDBHelper;
import kr.go.mobile.mobp.iff.R;

public class DKI_LocalPushSolution extends Solution<String, Bundle> {
    // 임시로 여기에 구현.. 다음에 정식 사용할 때, 솔루션에 종속되지 않도록 분리해야함.
    public static class PushMessage {
        public static PushMessage create(Bundle bundle) {
            PushMessage message = new PushMessage();
            message.bundle = bundle;
            return message;
        }

        private Bundle bundle;

        public int getMessageID() {
            return Integer.valueOf(bundle.getString("requestid", "N/A"));
        }

        public String getMessageTitle() {
            return bundle.getString("alert", "N/A");
        }

        public int getMessageType()  {
            String json = getMessageOriginal();
            JSONObject o = null;
            try {
                o = new JSONObject(json);
                return o.getInt("type");
            } catch (JSONException e) {
                e.printStackTrace();
                return -1;
            }

        }

        public String getMessage() {
            String json = getMessageOriginal();
            JSONObject o = null;
            try {
                o = new JSONObject(json);
                return o.getString("msg");
            } catch (JSONException e) {
                e.printStackTrace();
                return "";
            }

        }

        public String getMessageOriginal() {
            return bundle.getString("message", "N/A");
        }


    }

    public static class PushReceiver extends PushLibraryReceiver {

        @Override
        public void onRegistration(Context context, String msg) {
            // PUSH 서비스 등록 결과 수신
            Log.d("PushReceiver", "onRegistration : " + msg);
        }

        @Override
        public void onMsgReceive(Context context, Bundle bundle) {
            Log.d("PushReceiver", "onMsgReceive");
            if(bundle!=null && !bundle.isEmpty()) {
                PushMessage m = PushMessage.create(bundle);
                // Log.d("PushReceiver", "imageLink : " + m.getImageLink());
                // Log.d("PushReceiver", "title : " + m.getTitle());
                Log.d("PushReceiver", "메시지 ID : " + m.getMessageID());
                Log.d("PushReceiver", "메시지 TYPE : " + m.getMessageTitle());
                Log.d("PushReceiver", "메시지 정보 : " + m.getMessageOriginal());
                PushMessageDBHelper.insertNotice(m);
            }
        }

        @Override
        public void onNotiReceive(Context context, Bundle var2) {

        }

        @Override
        public void didPushReportResult(Bundle bundle) {

        }

        @Override
        public void didResult(Context context, String s, String s1) {

        }

        @Override
        public void didFail(Context context, String s, String s1) {

        }
    }

    public static final String TAG = DKI_LocalPushSolution.class.getSimpleName();
    final int SUCCESS = 1400;

    int errorCode;

    public DKI_LocalPushSolution(Context context) {
        super(context);
        String serverAddress = context.getString(R.string.pushServer); // http://125.60.52.72:9001/pis/interface/
        String appId = context.getString(R.string.pushAppId); // mc_iff_tb_agent
        errorCode = PushLibrary.getInstance().setStart(context, serverAddress, appId);
    }

    @Override
    protected Result<Bundle> process(Context context, String deviceId) throws SolutionRuntimeException {
        if (errorCode == SUCCESS) {
            errorCode = PushLibrary.getInstance().AppRegist(context, new PushAppRegistListener() {
                @Override
                public void didRegistResult(Context context, Bundle bundle) {
                    Result<Bundle> ret = new Result<>(bundle);
                    setResult(ret);
                }
            }, deviceId, null, null);
            if (errorCode != 1400) {
                return new Result<>(RESULT_CODE._FAIL, "로컬 푸시 등록 실패 (code : " + errorCode +")");
            }
            Log.d(TAG, "로컬 푸시 등록 요청 ... (응답대기)");

            return new Result<>(RESULT_CODE._WAIT, ""); // 응답 대기
        }
        return new Result<>(RESULT_CODE._FAIL, "로컬 푸시 등록 실패 (code : " + errorCode +")");
    }
}
