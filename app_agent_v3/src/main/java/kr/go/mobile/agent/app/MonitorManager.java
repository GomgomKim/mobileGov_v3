package kr.go.mobile.agent.app;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import kr.go.mobile.agent.service.monitor.ILocalMonitorService;
import kr.go.mobile.agent.service.monitor.MonitorService;
import kr.go.mobile.agent.service.monitor.SecureNetworkData;
import kr.go.mobile.agent.utils.Log;

public class MonitorManager {

    private static final String TAG = MonitorManager.class.getSimpleName();
    private static MonitorManager mInstance;

    static MonitorManager create(IBinder service) {
        MonitorManager.mInstance = new MonitorManager(service);
        return MonitorManager.mInstance;
    }

    private ILocalMonitorService monitorService;

    MonitorManager(IBinder binder) {
        Log.concurrency(Thread.currentThread(), "new MonitorService");
        this.monitorService = (ILocalMonitorService) binder;
    }

    void start(Context ctx) {
        Intent intent = new Intent(ctx, MonitorService.class);
        intent.setAction("kr.go.mobile.command.START_MONITOR");
        ctx.startService(intent);
    }

    public void startSecureNetwork(SecureNetworkData secureNetworkData) {
        monitorService.startSecureNetwork(secureNetworkData);
    }

    public boolean enabledSecureNetwork() throws NotResponseServiceException {
        try {
            Log.concurrency(Thread.currentThread(), "MonitorService.enabledSecureNetwork");
            return monitorService.enabledSecureNetwork();
        } catch (NullPointerException e) {
            throw new NotResponseServiceException();
        }
    }

    public String getThreatMessage() throws NotResponseServiceException {
        try {
            Log.concurrency(Thread.currentThread(), "MonitorService.getThreatMessage");
            return monitorService.getThreatMessage();
        } catch (NullPointerException  e) {
            throw new NotResponseServiceException();
        }
    }

    public String getErrorMessage() throws NotResponseServiceException {
        try {
            return monitorService.getErrorMessage();
        } catch (NullPointerException e) {
            throw new NotResponseServiceException();
        }
    }


}
