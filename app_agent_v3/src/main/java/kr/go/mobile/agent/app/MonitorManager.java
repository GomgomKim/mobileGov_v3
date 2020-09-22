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
import kr.go.mobile.agent.utils.HardwareUtils;

public class MonitorManager {

    static MonitorManager create(IBinder service) {
        return new MonitorManager(service);
    }

    public static void start(Context ctx) {
        Intent intent = new Intent(ctx, MonitorService.class);
        intent.setAction("kr.go.mobile.command.START_MONITOR");
        ctx.startService(intent);
    }

    public static void startSecureNetwork(Context ctx, String loginId, String loginPw) {
        Intent intent = new Intent(ctx, MonitorService.class);
        intent.setAction("kr.go.mobile.command.START_SECURE_NETWORK");
        intent.putExtra("id", loginId);
        intent.putExtra("pw", loginPw);
        ctx.startService(intent);
    }

    public static void monitorNetwork(Context ctx) {
        Intent intent = new Intent(ctx, MonitorService.class);
        intent.setAction("kr.go.mobile.command.MONITOR_START_SECURE_NETWORK");
        ctx.startService(intent);
    }

    private ILocalMonitorService monitorService;

    MonitorManager(IBinder binder) {
        this.monitorService = (ILocalMonitorService) binder;
    }

    public void monitorPackage(Bundle extra) {
        this.monitorService.registerPackage(extra);
    }

    public SecureNetwork.STATUS getSecureNetworkStatus() {
        return monitorService.getSecureNetworkStatus();
    }

    public String getConfirm() {
        return monitorService.getTokens();
    }

    public ThreatDetection.STATUS getThreatStatus() {
        return monitorService.getThreatStatus();
    }

    public boolean enabledSecureNetwork() {
        return getSecureNetworkStatus() == SecureNetwork.STATUS._CONNECTED;
    }
}
