package kr.go.mobile.common.v3.broker;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.IBrokerService;
import kr.go.mobile.agent.service.broker.UserAuthentication;
import kr.go.mobile.common.R;
import kr.go.mobile.common.v3.CBApplication;

public class BrokerManager {


    private static BrokerManager instance;

    private static final int MESSAGE_OK = Integer.MAX_VALUE;
    private static final int MESSAGE_ERROR = Integer.MIN_VALUE;

    public static void bindService(Context context, ServiceConnection agentServiceConnection) {
        Intent i = new Intent("kr.go.mobile.action.BROKER_SERVICE");
        i.setPackage(context.getString(R.string.iff_launcher_pkg));
        context.bindService(i, agentServiceConnection, Context.BIND_AUTO_CREATE
                | Context.BIND_ADJUST_WITH_ACTIVITY | Context.BIND_ABOVE_CLIENT | Context.BIND_DEBUG_UNBIND
                | Context.BIND_IMPORTANT | Context.BIND_WAIVE_PRIORITY /*| Context.BIND_EXTERNAL_SERVICE*/);
    }

    public static BrokerManager create(IBinder binder) {
        if (instance == null) {
            IBrokerService service = IBrokerService.Stub.asInterface(binder);
            if (service == null) {
                return null;
            }
            BrokerManager.instance = new BrokerManager(service);
        }
        return BrokerManager.instance;
    }

    private static BrokerManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("서비스 연결이 불안정합니다. 앱 종료 후 다시 실행해주시기 바랍니다.");
        }
        return instance;
    }

    // 앱이 종료되거나 특정 상태에서 BrokerManager 에 요청한 caller 를 모두 취소한다.
    public static void removeAll() {
        // TODO 큐 클리어, 모든 작업 취소.
        Caller.removeAll();
        instance.requestPoolExecutor = null;
        instance = null;
    }

    // 인증서버로 부터 획득한 사용자 인증 정보 제공한다.
    static UserAuthentication getUserAuth() throws RemoteException {
        return getInstance().service.getUserAuth();

    }

    // BrokerManager 의 ThreadPool 에 caller 를 실행한다.
    static void enqueue(Caller caller) {
        caller.service = getInstance().service;
        getInstance().execute(caller);
    }

    @Deprecated
    static Response submit(Caller caller) throws ExecutionException, InterruptedException {
        caller.service = getInstance().service;
        Future<Response> future = getInstance().requestPoolExecutor.submit((Callable<Response>) caller);
        if (future == null) {
            return null;
        }
        while (true) {
            if (future.isCancelled() || future.isDone()) break;
        }
        return future.get();
    }

    static void handleResponse(int callerId, BrokerResponse<?> response) {
        Message m = Message.obtain(getInstance().handler, MESSAGE_OK, callerId, 0, response);
        m.sendToTarget();
    }

    static void handleResponse(int callerId, int code, String msg) {
        Message m = Message.obtain(getInstance().handler, MESSAGE_ERROR, callerId, code, msg);
        m.sendToTarget();
    }

    private IBrokerService service;
    private ThreadPoolExecutor requestPoolExecutor;
    private Map<Integer, Response.Listener> listenerMap = Collections.synchronizedMap(new HashMap<Integer, Response.Listener>());
    private int sizeCoreThread = 10;
    private int sizeTotalThread = 20;
    private int capacityQueue = 30;
    private int keepAliveTime = 5; // thread 가 full 일 경우 5 초 대기

    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            // MainThread (UI 처리 가능)
            Response.Listener listener;
            if (listenerMap.containsKey(msg.arg1)) {
                listener = listenerMap.get(msg.arg1);
                switch (msg.what) {
                    case MESSAGE_OK:
                        BrokerResponse<?> brokerResp = (BrokerResponse) msg.obj;
                        listener.onSuccess(Response.convert(brokerResp));
                        break;
                    case MESSAGE_ERROR:
                    default:
                        listener.onFailure(msg.arg2, (String) msg.obj, null);
                        break;
                }
                return;
            }
            super.handleMessage(msg);
        }
    };

    private BrokerManager(IBrokerService service) {
        this.service = service;
        this.requestPoolExecutor = new ThreadPoolExecutor(sizeCoreThread, sizeTotalThread, keepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(capacityQueue)) {

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (t != null) {
                    Log.e("", t.getMessage(), t);
                }
            }
        };
    }

    public boolean isAlive() {
        return this.service.asBinder().isBinderAlive();
    }

    void execute(Caller caller) {
        listenerMap.put(caller.hashCode(), caller.listener);
        requestPoolExecutor.execute(caller); // --> Caller.run();
    }
}
