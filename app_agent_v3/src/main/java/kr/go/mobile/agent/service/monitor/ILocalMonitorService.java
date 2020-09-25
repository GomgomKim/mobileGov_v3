package kr.go.mobile.agent.service.monitor;

public interface ILocalMonitorService {
    ThreatDetection.STATUS getThreatStatus();
    SecureNetwork.STATUS getSecureNetworkStatus();
    IntegrityConfirm.STATUS getIntegrityStatus();
    String getTokens();
    void executeSolution(String another);
    String getErrorMessage(int type);

    void reset();
    void clear();
}
