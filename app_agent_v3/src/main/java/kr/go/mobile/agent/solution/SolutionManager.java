package kr.go.mobile.agent.solution;

import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import kr.go.mobile.agent.v3.solution.DKI_LocalPushSolution;
import kr.go.mobile.agent.v3.solution.EverSafeSolution;
import kr.go.mobile.agent.v3.solution.MagicLineClientSolution;
import kr.go.mobile.agent.v3.solution.SecuwizVPNSolution;
import kr.go.mobile.agent.v3.solution.VGuardSolution;

public class SolutionManager {

    public static String V_GUARD = VGuardSolution.class.getCanonicalName();
    public static String SSL_VPN = SecuwizVPNSolution.class.getCanonicalName();
    public static String EVER_SAFE = EverSafeSolution.class.getCanonicalName();
    public static String DREAM_SECURITY_GPKI_LOGIN = MagicLineClientSolution.class.getCanonicalName();
    public static String PUSH = DKI_LocalPushSolution.class.getCanonicalName();

    static Map<String, Solution<?, ?>> managerMap = new HashMap<>();

    public static synchronized Solution<?, ?> getSolutionModule(String name) throws ModuleNotFoundException {
        if (managerMap.containsKey(name)) {
            return managerMap.get(name);
        }
        throw new ModuleNotFoundException(name);
    }

    public static synchronized Solution<?, ?> initSolutionModule(Context context, String name) throws ModuleNotFoundException {
        try {
            return getSolutionModule(name);
        } catch (ModuleNotFoundException e1) {
            try {
                Class<?> solutionClass = Class.forName(name);
                Class<?>[] paramsTypes = {Context.class};
                Constructor<?> constructor = solutionClass.getConstructor(paramsTypes);
                Solution<?, ?> ret = (Solution<?, ?>) constructor.newInstance(context);
                managerMap.put(name, ret);
                return ret;
            } catch (Exception e) {
                throw new ModuleNotFoundException(name, e);
            }
        }
    }

    public static synchronized void onActivityResult(Context context, int requestCode, int resultCode, Intent intent) {
        for(String solutionName : managerMap.keySet()) {
            Solution<?, ?> solution = managerMap.get(solutionName);
            if (solution != null) {
                for (int definedRequestCode : solution.getRequestCodes()) {
                    if (definedRequestCode == requestCode) {
                        if (solution.onActivityResult(context, requestCode, resultCode, intent)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public static synchronized void destroySolution(String name) {
        Solution<?, ?> solution = managerMap.remove(name);
        if(solution != null) {
            solution.cancel();
        }
    }
    public static synchronized void finishSolution(String name) {
        Solution<?, ?> solution = managerMap.get(name);
        solution.finish();
    }

    public static class ModuleNotFoundException extends Exception {

        public ModuleNotFoundException(String name) {
            super(name);
        }

        public ModuleNotFoundException(String name, Exception e) {
            super(name, e);
        }

        public String getNotFoundSolutionName() {
            return getMessage();
        }

        public String getNotFoundSolutionSimpleName() {
            String name = getMessage();
            if (name.contains(".")) {
                int lastIndex = name.lastIndexOf(".");
                name = name.substring(lastIndex+1);
            }
            return name;
        }

        public void printCause() {
            getCause().printStackTrace();
        }
    }
}
