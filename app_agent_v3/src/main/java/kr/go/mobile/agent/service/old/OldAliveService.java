package kr.go.mobile.agent.service.old;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import kr.go.mobile.agent.app.MonitorManager;
import kr.go.mobile.agent.service.monitor.MonitorService;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.iff.service.IAliveService;

public class OldAliveService extends Service {
    private static String TAG = "OldService";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new IAliveService.Stub() {
            @Override
            @Deprecated
            public boolean isAlive(String packageName) throws RemoteException {
                return false;
            }
        };
    }

    @Override
    public boolean onUnbind(Intent intent) {
        String unbindTarget = intent.getStringExtra("extra_package");
        Log.d(TAG, "unbind target : " + unbindTarget);

        // TODO 동작 확인 필요.
        Intent removeIntent = new Intent(this, MonitorService.class);
        removeIntent.setAction(MonitorService.MONITOR_REMOVE_ADMIN_PACKAGE);
        removeIntent.putExtra("extra_package", unbindTarget);
        removeIntent.putExtra("req_id", Integer.MAX_VALUE);
        startService(removeIntent);

        return super.onUnbind(intent);
    }
}
