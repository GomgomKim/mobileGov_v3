package kr.go.mobile.iff.sample.util;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class TimeStamp {
    private static boolean ENABLE = true;
    private static final String TAG = TimeStamp.class.getSimpleName();
    private static final Map<String, Long> sTEMP= new HashMap<String, Long>(5);

    public static void startTime(String tag) {
        if (ENABLE && !sTEMP.containsKey(tag)) {
            sTEMP.put(tag, System.currentTimeMillis());
        }
    }

    public static void endTime(String tag) {
        if (ENABLE) {
            if (sTEMP.containsKey(tag)) {
                Long currentTime = System.currentTimeMillis();
                Long prevTime = sTEMP.get(tag);
                Log.d(TAG, Thread.currentThread().getName() + " : " + String.format("%s: %s(ms)", tag, currentTime - prevTime));
                sTEMP.remove(tag);
            } else {
                Log.w(TAG, String.format("undefined %s (after calling startTime(String tag))", tag));
            }
        }
    }

    public void clear() {
        sTEMP.clear();
    }
}
