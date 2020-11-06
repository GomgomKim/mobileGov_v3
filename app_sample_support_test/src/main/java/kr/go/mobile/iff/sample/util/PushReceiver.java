package kr.go.mobile.iff.sample.util;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import com.dkitec.PushLibrary.Receiver.PushLibraryReceiver;

public class PushReceiver extends PushLibraryReceiver {

    @Override
    public void onMsgReceive(Context context, Bundle bundle) {
        Log.d("PushReceiver", "onMsgReceive");
        ((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1500);
        if(bundle!=null && !bundle.isEmpty()) {
            //Message ID
            String requestId = "";
            //Alert Title
            String title = "";
            //Alert Message
            String alert = "";
            //Noti선택 시 App으로 전달할 Message
            String message = "";
            //Image Link URL
            String imageLink = "";

            requestId = bundle.getString("requestid", "");
            title = bundle.getString("title", "");
            alert = bundle.getString("alert", "");
            message = bundle.getString("message", "");
            imageLink = bundle.getString("linkimage", "");

            Log.i("PushReceiver", "requestId : " + requestId);
            Log.i("PushReceiver", "title : " + title);
            Log.i("PushReceiver", "alert : " + alert);
            Log.i("PushReceiver", "message : " + message);
            Log.i("PushReceiver", "imageLink : " + imageLink);
        }
    }

    @Override
    public void onNotiReceive(Context context, Bundle bundle) {

    }

    @Override
    public void onRegistration(Context context, String s) {
        Log.d("PushReceiver", "onRegistration : " + s);
    }

    @Override
    public void didPushReportResult(Bundle bundle) {
        Log.d("PushReceiver", "didPushReportResult");
    }

    @Override
    public void didResult(Context context, String s, String s1) {

    }

    @Override
    public void didFail(Context context, String s, String s1) {

    }
}
