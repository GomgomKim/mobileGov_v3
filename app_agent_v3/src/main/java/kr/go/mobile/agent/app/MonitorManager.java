package kr.go.mobile.agent.app;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import kr.go.mobile.agent.service.monitor.ILocalMonitorService;
import kr.go.mobile.agent.service.monitor.IntegrityConfirm;
import kr.go.mobile.agent.service.monitor.MonitorService;
import kr.go.mobile.agent.service.monitor.SecureNetwork;
import kr.go.mobile.agent.service.monitor.ThreatDetection;
import kr.go.mobile.agent.service.session.SessionService;

public class MonitorManager {

    static MonitorManager create(IBinder service) {
        return new MonitorManager(service);
    }

    public static void bindService(Context context, ServiceConnection localServiceConnection) {
        Intent bindIntent = new Intent(context, MonitorService.class);
        bindIntent.setAction("local");
        context.bindService(bindIntent, localServiceConnection,
                Context.BIND_AUTO_CREATE |
                        Context.BIND_ADJUST_WITH_ACTIVITY |
                        Context.BIND_WAIVE_PRIORITY |
                        Context.BIND_ABOVE_CLIENT |
                        Context.BIND_IMPORTANT);
    }

    private ILocalMonitorService SERVICE;
    @Deprecated
    private Bundle runningTarget;

    MonitorManager(IBinder binder) {
        this.SERVICE = (ILocalMonitorService) binder;
    }

    public void execute(Bundle extra) {
        String another = extra.getString("extra_token", null);
        SERVICE.executeSolution(another);

        extra.remove("extra_token");
        runningTarget = extra;
    }

    public String getConfirm() {
        return SERVICE.getTokens();
    }

    public IntegrityConfirm.STATUS getIntegrityStatus() {
        return SERVICE.getIntegrityStatus();
    }

    public ThreatDetection.STATUS getThreatStatus() {
        return SERVICE.getThreatStatus();
    }

    public SecureNetwork.STATUS getSecureNetworkStatus() {
        return SERVICE.getSecureNetworkStatus();
    }

    public boolean enabledSecureNetwork() {
        if(getSecureNetworkStatus() == SecureNetwork.STATUS._CONNECTED) {
            SERVICE.reset();
            return true;
        }
        return false;
    }

    public void startSecureNetwork(Context ctx, String loginId, String loginPw) {
        SERVICE.startSecureNetwork(ctx, loginId, loginPw);
    }

    public void stopSecureNetwork() {
        SERVICE.stopSecureNetwork();
    }

    public void addMonitorPackage(Bundle info) {
        SERVICE.addPackage(info);
    }

    public void removeMonitorPackage(String uid) {
        SERVICE.removePackage(uid);
    }

    public String getErrorMessage(int type) {
        return SERVICE.getErrorMessage(type);
    }
    @Deprecated
    public Bundle getRunningPackageInfo() {
        return runningTarget;
    }

    public void clear() {
        SERVICE.clear();
    }

    private void reset() {
        SERVICE.reset();
    }

    public String getPackageName(int uid) {
        return SERVICE.getPackageName(uid);
    }

    public String getVersionCode(int uid) {
        return SERVICE.getVersionCode(uid);
    }
}
