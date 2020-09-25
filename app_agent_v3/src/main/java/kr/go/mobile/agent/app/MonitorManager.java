package kr.go.mobile.agent.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import kr.go.mobile.agent.service.monitor.ILocalMonitorService;
import kr.go.mobile.agent.service.monitor.IntegrityConfirm;
import kr.go.mobile.agent.service.monitor.MonitorService;
import kr.go.mobile.agent.service.monitor.SecureNetwork;
import kr.go.mobile.agent.service.monitor.ThreatDetection;

public class MonitorManager {

    static MonitorManager create(IBinder service) {
        return new MonitorManager(service);
    }

    public static void startSecureNetwork(Context ctx, String loginId, String loginPw) {
        Intent intent = new Intent(ctx, MonitorService.class);
        intent.setAction(MonitorService.START_SECURE_NETWORK);
        intent.putExtra("id", loginId);
        intent.putExtra("pw", loginPw);
        ctx.startService(intent);
    }

    public static void stopSecureNetwork(Context ctx) {
        Intent intent = new Intent(ctx, MonitorService.class);
        intent.setAction(MonitorService.FORCE_STOP_SECURE_NETWORK);
        ctx.startService(intent);
    }

    public static void addMonitorPackage(Context ctx, Bundle info) {
        Intent intent = new Intent(ctx, MonitorService.class);
        intent.setAction(MonitorService.MONITOR_ADD_ADMIN_PACKAGE);
        intent.putExtra("admin_info", info);
        ctx.startService(intent);
    }

    public static void removeMonitorPackage(Context ctx, String requestId) {
        Intent intent = new Intent(ctx, MonitorService.class);
        intent.setAction(MonitorService.MONITOR_REMOVE_ADMIN_PACKAGE);
        intent.putExtra("req_id_base64", requestId);
        ctx.startService(intent);
    }

    private ILocalMonitorService SERVICE;
    private Bundle runningTarget;

    MonitorManager(IBinder binder) {
        this.SERVICE = (ILocalMonitorService) binder;
    }



    public void setAnotherConfirm(Bundle extra) {
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

    public String getErrorMessage(int type) {
        return SERVICE.getErrorMessage(type);
    }

    public Bundle getRunningPackageInfo() {
        return runningTarget;
    }

    public void clear() {
        SERVICE.clear();
    }
}
