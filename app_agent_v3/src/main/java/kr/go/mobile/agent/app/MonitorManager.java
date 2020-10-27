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

    // 공통기반 라이브러리 2.x.x 를 사용하는 행정앱이 완전히 종료될 때 이벤트 발생.
    @Deprecated
    public static void removeMonitorPackage(Context ctx, String requestId) {
        Intent intent = new Intent(ctx, MonitorService.class);
        intent.setAction(MonitorService.MONITOR_REMOVE_ADMIN_PACKAGE);
        intent.putExtra("req_id", requestId);
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

    public Bundle getRunningPackageInfo() {
        return runningTarget;
    }

    public void clear() {
        SERVICE.clear();
    }

    private void reset() {
        SERVICE.reset();
    }
}
