package kr.go.mobile.agent.service.monitor;

import android.content.Context;

import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.SolutionManager;

public class IntegrityConfirm {

    public enum STATUS {
        _VERIFIED,
        _NOT_VERIFIED,
        _ERROR;
    }

    private boolean confirm = false;
    private String agentToken;
    private String anotherToken;
    private Solution<?, ?> integritySolution;

    public IntegrityConfirm(Context context, String solutionName) throws SolutionManager.ModuleNotFoundException  {
        integritySolution = SolutionManager.initSolutionModule(context, solutionName);
    }

    public void confirm(Solution.EventListener listener) {
        integritySolution.execute(null, listener);
    }

    public boolean isConfirm() {
        return confirm;
    }

    public void setAnotherConfirm(String token) {
        this.anotherToken = token;
    }

    public void setConfirm(String token) {
        confirm = true;
        agentToken = token;
    }

    public void setDeny(String denyMessage) {
        confirm = false;
        agentToken = denyMessage;
    }

    public String getTokens() {
        if (this.agentToken == null || this.anotherToken == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(agentToken);
        sb.append(",");
        sb.append(anotherToken);
        return sb.toString();
    }


}
