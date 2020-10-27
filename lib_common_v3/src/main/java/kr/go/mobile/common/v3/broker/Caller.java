package kr.go.mobile.common.v3.broker;

import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import kr.go.mobile.agent.service.broker.BrokerResponse;
import kr.go.mobile.agent.service.broker.Document;
import kr.go.mobile.agent.service.broker.IBrokerService;
import kr.go.mobile.agent.service.broker.IBrokerServiceCallback;

public class Caller implements Callable<Response>, Runnable {
    private static final Object sPoolSync = new Object();
    private static final int MAX_POOL_SIZE = 30;
    private static Caller sPool;
    private static int sPoolSize = 0;

    public static Caller obtain() {
        synchronized (sPoolSync) {
            if(sPool != null) {
                Caller c = sPool;
                sPool = c.next;
                c.next = null;
                c.flags = 0; // in-use 플래그 초기화.
                sPoolSize--;

                return c;
            }
        }
        return new Caller();
    }

    final int FLAG_IN_USE = 1;
    final int FLAG_ASYNCHRONOUS = 1 << 1;

    IBrokerService service;
    Caller next;
    Request request;
    Response.Listener listener;
    int flags;

    Caller() { }

    static void removeAll() {
        if (sPool != null) {
            while (sPoolSize > 0) {
                Caller c = sPool;
                sPool = c.next;
                c.clear();
                sPoolSize--;
            }
        }
    }

    /**
     * 비동기 호출 방법을 제공한다.
     *
     * @param request ddd
     * @param listener
     * @return Response resp
     */
    public void enqueue(Request request, Response.Listener listener) {
        defaultSetting(true, request, listener);
        BrokerManager.enqueue(this);
    }

    /**
     * 동기 호출 방법을 제공한다.
     *
     * @param request
     */
    @Deprecated
    public Response execute(Request request) throws ExecutionException, InterruptedException {
        defaultSetting(false, request, null);
        return BrokerManager.submit(this);
    }

    private void defaultSetting(boolean async, Request req, Response.Listener listener) {
        markInUse();
        if (async) {
            this.flags |= FLAG_ASYNCHRONOUS;
        } else {
            this.flags &= ~FLAG_ASYNCHRONOUS;
        }
        this.request = req;
        this.listener = listener;
    }

    boolean isAsynchronous() {
        return ((this.flags & FLAG_ASYNCHRONOUS) == FLAG_ASYNCHRONOUS);
    }

    private boolean isInUse() {
        return ((flags & FLAG_IN_USE) == FLAG_IN_USE);
    }

    private void markInUse() {
        flags |= FLAG_IN_USE;
    }

    private void clearInUse() {
        flags &= ~ FLAG_IN_USE;
    }

    // 비동기 처리 요청
    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        if (this.isAsynchronous()) {
            try {
                final int callerId = hashCode();
                service.enqueue(request.task, new IBrokerServiceCallback.Stub() {
                    @Override
                    public void onResponse(BrokerResponse response) {
                        BrokerManager.handleResponse(callerId, response);
                    }

                    @Override
                    public void onFailure(int code, String msg)  {
                        BrokerManager.handleResponse(callerId, code, msg);
                    }
                });
            } catch (RemoteException e) {
                this.listener.onFailure(100, "", e);
            }
        } else {
            Log.e("Caller.run()", "ERROR ::: "+Thread.currentThread().getName());
        }
    }

    @Override
    public Response call() throws Exception {
        // TODO 미구현
        Log.e("Caller.call()", Thread.currentThread().getName());
        if (!this.isAsynchronous()) {
            BrokerResponse response = service.execute(request.task);
            return Response.convert(response);
        }
        return null;
    }

    private void clear() {
        flags = FLAG_IN_USE;
        listener = null;
        request = null;
    }

    public void recycle() {
        if (isInUse()) {
            Log.w("@@@@@", "사용중인 Caller 는 recycle 할 수 없습니다.");
            return;
        }
        clear();

        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }

    public Request getRequest() {
        return this.request;
    }
}
