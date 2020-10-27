package kr.go.mobile.agent.v3.solution;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import com.dkitec.PushLibrary.Listener.PushAppRegistListener;
import com.dkitec.PushLibrary.Listener.PushGetConfigListener;
import com.dkitec.PushLibrary.PushLibrary;
import com.dkitec.PushLibrary.Receiver.PushLibraryReceiver;

import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.utils.HardwareUtils;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.mobp.iff.R;

public class DKI_LocalPushSolution extends Solution<String, Bundle> {

    static class PushMessage {
        private Bundle bundle;
        static PushMessage create(Bundle bundle) {
            PushMessage message = new PushMessage();
            message.bundle = bundle;
            return message;
        }

        String getMessageID() {
            return bundle.getString("requestid", "N/A");
        }

        String getTitle() {
            return bundle.getString("title", "N/A");
        }

        String getMessage() {
            return bundle.getString("alert", "N/A");
        }

        String getNotiMessage() {
            return bundle.getString("message", "N/A");
        }

        String getImageLink() {
            return bundle.getString("imageLink", "N/A");
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
                Log.d("PushReceiver", "requestId : " + m.getMessageID());
                Log.d("PushReceiver", "title : " + m.getTitle());
                Log.d("PushReceiver", "alert : " + m.getMessage());
                Log.d("PushReceiver", "message : " + m.getNotiMessage());
                Log.d("PushReceiver", "imageLink : " + m.getImageLink());
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

    PushReceiver receiver = new PushReceiver();

    public DKI_LocalPushSolution(Context context) {
        super(context);
        String serverAddress = context.getString(R.string.pushServer); // http://125.60.52.72:9001/pis/interface/
        String appId = context.getString(R.string.pushAppId); // mc_iff_tb_agent
        int ret = PushLibrary.getInstance().setStart(context, serverAddress, appId);
        if (ret != 1400) {
            Log.e("MobileGov", "등록 실패");
        }
        PushLibrary.getInstance().GetPushConfig(context, new PushGetConfigListener() {
            @Override
            public void didGetConfigResult(Bundle bundle) {
                Log.e("MobileGov", "didGetConfigResult " + bundle.toString());
            }
        });
    }

    @Override
    protected Result<Bundle> process(Context context, String userID) throws SolutionRuntimeException {
        String packageName = context.getPackageName();
        String serverAddress = context.getString(R.string.pushServer); // http://125.60.62.72:9001/pis/interface/
        String appId = context.getString(R.string.pushAppId); // mc_iff_tb_agent
        PushLibrary.getInstance().setStart(context, serverAddress, appId);

        int retCode = PushLibrary.getInstance().AppRegist(context, new PushAppRegistListener() {
            @Override
            public void didRegistResult(Context context, Bundle bundle) {
                Log.e("MobileGov", bundle.toString());
                Result<Bundle> ret = new Result<>(bundle);
                setResult(ret);

                // 리시버 등록
                // IntentFilter filter = new IntentFilter();
                // context.registerReceiver(receiver, filter);
            }
        }, userID, null, null);
        if (retCode != 1400) {
            return new Result<>(RESULT_CODE._FAIL, "");
        }
        return new Result<>(RESULT_CODE._WAIT, ""); // 응답 대기
    }
}
