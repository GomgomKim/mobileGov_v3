package kr.go.mobile.agent.service.monitor;

import android.os.Bundle;

import kr.go.mobile.agent.service.session.UserSigned;

public interface ILocalMonitorService {
    ThreatDetection.STATUS getThreatStatus();
    String getTokens();
    SecureNetwork.STATUS getSecureNetworkStatus();
    void registerPackage(Bundle extra);
}
