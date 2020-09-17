package kr.go.mobile.agent.service.monitor;

public interface ILocalMonitorService {

    String getErrorMessage();
    String getThreatMessage();

    void startSecureNetwork(SecureNetworkData secureNetworkData);
    boolean enabledSecureNetwork();
}
