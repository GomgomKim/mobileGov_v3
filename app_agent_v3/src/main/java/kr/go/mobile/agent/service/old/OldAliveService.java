package kr.go.mobile.agent.service.old;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import kr.go.mobile.agent.app.MonitorManager;
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
        // 공통기반 라이브러리 2.x.x 를 사용하는 행정앱이 완전히 종료될 때 이벤트 발생.
        MonitorManager.removeMonitorPackage(this, unbindTarget);
        return super.onUnbind(intent);
    }
}
