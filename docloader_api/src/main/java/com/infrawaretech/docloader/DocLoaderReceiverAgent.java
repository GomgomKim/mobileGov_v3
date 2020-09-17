package com.infrawaretech.docloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public class DocLoaderReceiverAgent {

    private String TAG = DocLoaderReceiverAgent.class.getSimpleName();

    // broadcast receiver
    private BroadcastReceiver mVpnBroadcastRecv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            VpnStatus.setStatus(intent.getIntExtra(LocalConstants.SecureAgentVpn.EXTRA_STATUS, VpnStatus.ERROR));

            switch (VpnStatus.getStatus()) {
                case VpnStatus.ERROR: // error ?
                    Log.e(TAG, "VPN 연결 상태 확인 중 에러가 발생하였습니다.");
                    break;
                case VpnStatus.CONNECTION: // connection
                    Log.w(TAG, "VPN 연결이 정상적입니다.");
                    break;
                case VpnStatus.CONNECTTING: // connecting
                    Log.e(TAG, "VPN 연결 중 상태입니다. ");
                    break;
            }
        }
    };

    public DocLoaderReceiverAgent() {

    }

    public void register(Context context, BroadcastReceiver localBroadcastReceiver) {
        IntentFilter filter = new IntentFilter(LocalConstants.Broadcast.ACTION_DOC_CONVERT_STATUS);
        filter.addAction(LocalConstants.Broadcast.ACTION_DOC_VIEW_EVENT);
        filter.addAction(LocalConstants.Broadcast.ACTION_DOC_CONVERT_STATE);
        LocalBroadcastManager.getInstance(context).registerReceiver(localBroadcastReceiver, filter);

        // VPN 상태값 리시버 등록
        context.registerReceiver(mVpnBroadcastRecv, new IntentFilter(LocalConstants.SecureAgentVpn.ACTION_VPN_STATUS));
    }

    public void unregister(Context context, BroadcastReceiver localBroadcastReceiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(localBroadcastReceiver);

        // VPN 상태값 리시버 등록 해제
        context.unregisterReceiver(mVpnBroadcastRecv);
    }
}
