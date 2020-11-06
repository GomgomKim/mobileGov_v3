package kr.go.mobile.agent.service.monitor;

import android.content.Context;

import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.Solution.EventListener;
import kr.go.mobile.agent.solution.SolutionManager;

public class IntegrityConfirm {

    public enum STATUS {
        _UNKNOWN, // 무결성 검증 결과를 알 수 없음.
        _VERIFIED, // 무결성 검증 성공
        _NOT_VERIFIED; // 무결성 검증 실패
    }

    private STATUS status;
    private String agentToken;
    private String anotherToken;
    final private Solution<?, String> integritySolution;

    public IntegrityConfirm(Context context, String solutionName, EventListener<String> listener) throws SolutionManager.ModuleNotFoundException  {
        status = STATUS._UNKNOWN;
        integritySolution = SolutionManager.initSolutionModule(context, solutionName);
        integritySolution.setDefaultEventListener(listener);
    }

    public void setAnotherConfirm(String token) {
        this.anotherToken = token;
        this.agentToken = null;
    }

    public void confirm(Context context) {
        status = STATUS._UNKNOWN;
        integritySolution.execute(context);
    }

    public void setConfirm(String token) {
        status = STATUS._VERIFIED;
        agentToken = token;
    }

    public void setDeny(String denyMessage) {
        status = STATUS._NOT_VERIFIED;
        agentToken = denyMessage;
    }


    public String getErrorMessage() {
        if (status == STATUS._NOT_VERIFIED)
            return agentToken;
        return "N/A";
    }

    public STATUS status() {
        return status;
    }

    public String getTokens() {
        if (this.agentToken == null || this.anotherToken == null) {
            return null;
        }
        return String.format("%s,%s", agentToken, anotherToken);
    }

    public void clear() {
        agentToken = null;
        anotherToken = null;
    }
}
