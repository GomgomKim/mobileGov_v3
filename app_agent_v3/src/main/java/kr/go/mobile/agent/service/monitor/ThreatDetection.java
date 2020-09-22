package kr.go.mobile.agent.service.monitor;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.solution.SolutionManager;

public class ThreatDetection {

    public enum STATUS {
        _SAFE,
        _ROOTING_DEVICE,
        _EXIST_MALWARE,
        _DISABLED_REAL_TIME_SCAN,
        _ERROR;
    }

    private Solution<?, ?> threatDetectionSolution;
    private STATUS status = null;

    public ThreatDetection(Context context, String solutionName) throws SolutionManager.ModuleNotFoundException {
        threatDetectionSolution = SolutionManager.initSolutionModule(context, solutionName);
    }

    public void detectedThreats(Solution.EventListener listener) {
        threatDetectionSolution.execute(null, listener);
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public STATUS getStatus() {
        return this.status;
    }

    public boolean isSafe() {
        return status.equals(STATUS._SAFE);
    }
}
