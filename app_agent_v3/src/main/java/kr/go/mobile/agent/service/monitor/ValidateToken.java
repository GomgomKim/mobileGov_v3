package kr.go.mobile.agent.service.monitor;

import kr.go.mobile.agent.utils.Log;

public class ValidateToken {
    private String anotherToken;
    private String agentToken;

    public boolean existAgentToken() {
        Log.concurrency(Thread.currentThread(), "agent token get");
        return (this.agentToken != null);
    }

    public void clearAgentToken() {
        Log.concurrency(Thread.currentThread(), "agent token set");
        this.agentToken = null;
    }

    public void setAgent(String token) {
        Log.concurrency(Thread.currentThread(), "agent token set");
        this.agentToken = token;
    }

    public void setExtra(String token) {
        this.anotherToken = token;
    }

    public String getTokens() throws NullPointerException {
        if (this.agentToken == null || this.anotherToken == null) {
            throw new NullPointerException("필수 토큰값이 존재하지 않습니다.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(agentToken);
        sb.append(",");
        sb.append(anotherToken);
        return sb.toString();
    }
}
