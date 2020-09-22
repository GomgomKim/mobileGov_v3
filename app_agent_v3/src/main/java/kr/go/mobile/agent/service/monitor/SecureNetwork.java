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
        _STOP,
        _DESTROY;
    }

    public enum STATUS {
        _CONNECTED,
        _CONNECTING,
        _DISCONNECTED;
    }


    private Solution<?, ?> secureNetworkSolution;
    String loginId;
    String loginPw;
    String failMessage = "";
    CMD command;
    STATUS status;

    public SecureNetwork(Context context, String solutionName) throws SolutionManager.ModuleNotFoundException {
        secureNetworkSolution = SolutionManager.initSolutionModule(context, solutionName);
    }

    public void start(String loginId, String loginPW, Solution.EventListener listener) {
        this.command = CMD._START;
        this.loginId = loginId;
        this.loginPw = loginPW;
        String[] params = {this.loginId, this.loginPw, this.command.name()};
        if (secureNetworkSolution instanceof SecuwizVPNSolution) {
            ((SecuwizVPNSolution)secureNetworkSolution).execute(null, params, listener);
        }
    }

    public void stop(Solution.EventListener listener) {
        this.command = CMD._STOP;
        String[] params = {this.loginId, this.loginPw, this.command.name()};
        if (secureNetworkSolution instanceof SecuwizVPNSolution) {
            ((SecuwizVPNSolution)secureNetworkSolution).execute(null, params, listener);
        }
    }

    public void changeStatus(STATUS s) {
        this.status = s;
    }

    public String getFailMessage() { return this.failMessage; }

    public CMD getCommand() {
        return this.command;
    }

    public STATUS getStatus() {
        return this.status;
    }

    public boolean retryConnection() {
        if (status == STATUS._DISCONNECTED && command == CMD._START) {
            return true;
        }
        return false;
    }

    public String getLoginID() {
        return this.loginId;
    }

    public String getLoginPw() {
        return this.loginPw;
    }
}
