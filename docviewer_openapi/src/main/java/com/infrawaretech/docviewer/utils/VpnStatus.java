package com.infrawaretech.docviewer.utils;

public class VpnStatus {
    public static final int ERROR = 0;
    public static final int CONNECTION = 1;
    public static final int CONNECTING = 2;

    private static int STATUS = CONNECTING;
    public static synchronized void setStatus(int status) {
        VpnStatus.STATUS = status;
    }

    public static synchronized int getStatus() {
        return VpnStatus.STATUS;
    }
}
