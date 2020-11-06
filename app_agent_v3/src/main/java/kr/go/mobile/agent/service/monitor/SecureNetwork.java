package kr.go.mobile.agent.service.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcel;
import android.os.Parcelable;

import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.SolutionManager;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.v3.solution.SecuwizVPNSolution;

public class SecureNetwork {

    public enum CMD {
        _START,
        _STOP;
    }

    public enum STATUS {
        _UNKNOWN,
        _ERROR,
        _CONNECTED,
        _DISCONNECTED;
    }

    private Solution<?, SecureNetwork.STATUS> secureNetworkSolution;
    String loginId;
    String loginPw;
    CMD command;
    String errorMessage;

    public SecureNetwork(Context context, String solutionName, Solution.EventListener<SecureNetwork.STATUS> listener) throws SolutionManager.ModuleNotFoundException {
        secureNetworkSolution = SolutionManager.initSolutionModule(context, solutionName);
        secureNetworkSolution.setDefaultEventListener(listener);
    }

    public void monitor() {
        secureNetworkSolution.enabledMonitor();
    }

    public void disabledMonitor() {
        secureNetworkSolution.disabledMonitor();
    }

    public void start(Context context, String loginId, String loginPW) {
        this.errorMessage = null;
        this.command = CMD._START;
        this.loginId = loginId;
        this.loginPw = loginPW;
        String[] params = {this.loginId, this.loginPw, this.command.name()};
        if (secureNetworkSolution instanceof SecuwizVPNSolution) {
            ((SecuwizVPNSolution)secureNetworkSolution).execute(context, params);
        } else {
            throw new Solution.SolutionRuntimeException("솔루션을 찾을 수 없습니다. (" + secureNetworkSolution.getClass()+ ")");
        }
    }

    public void stop() {
        this.errorMessage = null;
        this.command = CMD._STOP;
        String[] params = {this.loginId, this.loginPw, this.command.name()};
        if (secureNetworkSolution instanceof SecuwizVPNSolution) {
            ((SecuwizVPNSolution)secureNetworkSolution).execute(null, params);
        } else {
            throw new Solution.SolutionRuntimeException("솔루션을 찾을 수 없습니다. (" + secureNetworkSolution.getClass()+ ")");
        }
    }

    public STATUS getStatus() {
        if (errorMessage != null) {
            return STATUS._ERROR;
        }
        if (secureNetworkSolution instanceof SecuwizVPNSolution) {
            return ((SecuwizVPNSolution)secureNetworkSolution).status();
        } else {
            throw new Solution.SolutionRuntimeException("솔루션을 찾을 수 없습니다. (" + secureNetworkSolution.getClass()+ ")");
        }
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

}
