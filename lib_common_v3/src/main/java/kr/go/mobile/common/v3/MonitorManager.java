package kr.go.mobile.common.v3;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;

import kr.go.mobile.common.R;

class MonitorManager {

    private Messenger eventMonitorMessenger;

    public static void bindService(Context context, String launcherName, ServiceConnection connection) {
        Intent i = new Intent("kr.go.mobile.action.MONITOR_SERVICE");
        i.setPackage(launcherName);
        context.bindService(i, connection, Context.BIND_AUTO_CREATE
                | Context.BIND_ADJUST_WITH_ACTIVITY | Context.BIND_ABOVE_CLIENT | Context.BIND_DEBUG_UNBIND
                | Context.BIND_IMPORTANT | Context.BIND_WAIVE_PRIORITY /*| Context.BIND_EXTERNAL_SERVICE*/);
    }

    public static MonitorManager create(IBinder service, int req_uid, Messenger mCommandReceiver) throws RemoteException {
        return new MonitorManager(service, req_uid, mCommandReceiver);
    }

    private MonitorManager(IBinder target, int req_uid, Messenger replyTo) throws RemoteException {
        this.eventMonitorMessenger = new Messenger(target);

        Message msg = Message.obtain(null, CommonBasedConstants.EVENT_COMMAND_HANDLER_REGISTERED);
        msg.arg1 = req_uid;
        msg.replyTo = replyTo;

        this.eventMonitorMessenger.send(msg);
    }


    public void sendEvent(int event) throws RemoteException  {
        Message msg = Message.obtain(null, event);
        this.eventMonitorMessenger.send(msg);
    }

    public void sendEvent(int event, String message) throws RemoteException  {
        Message msg = Message.obtain(null, event);
        msg.obj = message;
        this.eventMonitorMessenger.send(msg);
    }
}
