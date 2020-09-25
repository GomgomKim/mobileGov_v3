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
    private STATUS status;

    public ThreatDetection(Context context, String solutionName, Solution.EventListener listener) throws SolutionManager.ModuleNotFoundException {
        threatDetectionSolution = SolutionManager.initSolutionModule(context, solutionName);
        threatDetectionSolution.setDefaultEventListener(listener);
    }

    public void detectedThreats() {
        if (status != STATUS._SAFE) {
            status = null;
            threatDetectionSolution.execute(null);
        }
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public STATUS getStatus() {
        return status;
    }

    public void monitor(boolean enabled) {
        if (enabled) {
            threatDetectionSolution.enabledMonitor();
        } else {
            threatDetectionSolution.disabledMonitor();
        }
    }
}
