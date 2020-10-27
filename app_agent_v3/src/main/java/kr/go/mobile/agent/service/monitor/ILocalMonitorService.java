package kr.go.mobile.agent.service.monitor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public interface ILocalMonitorService {
    ThreatDetection.STATUS getThreatStatus();
    SecureNetwork.STATUS getSecureNetworkStatus();
    IntegrityConfirm.STATUS getIntegrityStatus();
    String getTokens();
    void executeSolution(String another);
    void startSecureNetwork(Context ctx, String id, String pw);
    void stopSecureNetwork();
    String getErrorMessage(int type);

    void reset();
    void clear();

    void addPackage(Bundle info);
    void removePackage(String uid);
}
