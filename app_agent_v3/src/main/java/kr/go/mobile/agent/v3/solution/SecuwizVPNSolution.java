package kr.go.mobile.agent.v3.solution;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import net.secuwiz.SecuwaySSL.Api.MobileApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.concurrent.atomic.AtomicInteger;

import kr.go.mobile.agent.service.monitor.SecureNetwork;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.utils.Aria;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.ResourceUtils;
import kr.go.mobile.mobp.iff.R;

public class SecuwizVPNSolution extends Solution<String[], SecureNetwork.STATUS> implements ServiceConnection {

    private static final String TAG = SecuwizVPNSolution.class.getSimpleName();

    public static final int SECUWIZ_VPN_STATUS_DISCONNECTION = 0;
    public static final int SECUWIZ_VPN_STATUS_CONNECTION = 1;
    public static final int SECUWIZ_VPN_STATUS_CONNECTING= 2;

    private static final String ANDROID_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String SECUWIZ_VPN_STATUS_RECEIVER_ACTION = "com.secuwiz.SecuwaySSL.Service.STATUS";
    private static final String SECUWIZ_VPN_SERVICE_ACTION = "net.secuwiz.SecuwaySSL.moi";
    private static final String SECUWIZ_VPN_PACKAGE_NAME = "net.secuwiz.SecuwaySSL.moi";

    // LOLLIPOP (API 21) < Build.VERSION.SDK_INT < O (API 26) 일 경우만 Android Framework 의 Broadcast 를 수신한 후 VPN Connection 처리
    private static final boolean CHECK_ANDROID_FRAMEWORK_VPN = false;//Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O);

    private static final int STATUS_NOT_READY = 4 >> 3; // 000
    private static final int STATUS_READY = 4 >> 2; // 001
    private static final int STATUS_SECUWIZ_VPN_CONNECTION = 4 >> 1; // 010
    private static final int STATUS_ANDROID_VPN_CONNECTION = CHECK_ANDROID_FRAMEWORK_VPN ? (4 /*4 >> 0 // 100*/) : STATUS_SECUWIZ_VPN_CONNECTION;
    private static final int STATUS_COMMON_BASED_VPN_CONNECTION = STATUS_READY | STATUS_ANDROID_VPN_CONNECTION | STATUS_SECUWIZ_VPN_CONNECTION; // 111
    private static final int STATUS_COMMON_BASED_VPN_DISCONNECTION = ~STATUS_COMMON_BASED_VPN_CONNECTION;

    private final BroadcastReceiver mVPNReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SECUWIZ_VPN_STATUS_RECEIVER_ACTION.equals(intent.getAction())) {
                switch (intent.getIntExtra("STATUS", SECUWIZ_VPN_STATUS_DISCONNECTION)) {
                    case SECUWIZ_VPN_STATUS_CONNECTION:
                        if (serviceVPN == null) {
                            Log.i(TAG, "기존 보안 네트워크가 연결되어 있습니다.");
                            stopPrevConnection = true;
                            return;
                        }

                        Log.d(TAG, "STEP 4. Secuwiz SSL-VPN 연결 성공");
                        if(DELAY_FOR_VPN_CONFIG > 0) {
                            Log.w(TAG, "단말 특성상 Secuwiz SSL-VPN 연결이 지연됩니다.");
                            try {
                                Thread.sleep(DELAY_FOR_VPN_CONFIG);
                            } catch (InterruptedException ignored) {
                            }
                        }
                        status.addAndGet(STATUS_SECUWIZ_VPN_CONNECTION);
                        break;
                    case SECUWIZ_VPN_STATUS_CONNECTING:
                        Log.d(TAG, "Secuwiz SSL-VPN 연결 중");
                        break;
                    case SECUWIZ_VPN_STATUS_DISCONNECTION:
                        if(serviceVPN == null) return;

                        Log.d(TAG, "STEP 3. Secuwiz SSL-VPN 연결 해제됨");
                        int newValue = status.get() & ~STATUS_SECUWIZ_VPN_CONNECTION;
                        status.getAndSet(newValue);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + intent.getIntExtra("STATUS", SECUWIZ_VPN_STATUS_DISCONNECTION));
                }
            } else if (ANDROID_CONNECTIVITY_CHANGE.equals(intent.getAction())) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                boolean isVPN;
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    Network network = cm.getActiveNetwork();
                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                    isVPN = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
                } else {
                    NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO/*networkInfo*/);
                    NetworkInfo.DetailedState state = networkInfo.getDetailedState();
                    isVPN = networkInfo.getTypeName().equals(ConnectivityManager.TYPE_VPN) && state.equals(NetworkInfo.DetailedState.CONNECTED);
                }

                if(isVPN) {
                    Log.d(TAG, "STEP 4. Android VPN Profile 설정 성공");
                    status.addAndGet(STATUS_ANDROID_VPN_CONNECTION);
                } else {
                    int newValue = status.get() & ~STATUS_ANDROID_VPN_CONNECTION;
                    status.getAndSet(newValue);
                }
            }
//            try {
//                doComplete();
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
        }
    };

    NetworkCallback networkCallback = new NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, "STEP 4. Android VPN 설정 완료");
            status.addAndGet(STATUS_SECUWIZ_VPN_CONNECTION);
//            try {
//                doComplete();
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
            Log.d(TAG, "onAvailable: " + network.toString());
            super.onAvailable(network);

        }

        @Override
        public void onLosing(@NonNull Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
            Log.d(TAG, "onLosing: " + network.toString());
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            Log.d(TAG, "onCapabilitiesChanged: " + network.toString());
        }

        @Override
        public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {
            super.onBlockedStatusChanged(network, blocked);
            Log.d(TAG, "onBlockedStatusChanged: " + network.toString());
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            int newValue = status.get() & ~STATUS_ANDROID_VPN_CONNECTION;
            status.getAndSet(newValue);
            Log.d(TAG, "onLost: " + network.toString());
        }

    };

    // SECUWIZ SSL-VPN 서비스를 사용하기 위한 객체
    private MobileApi serviceVPN = null;
    // VPN 연결 재시도 횟수
    int VPN_CONNECT_RETRY_COUNT;
    // VPN 연결 재시도 딜레리 시간
    int VPN_CONNECT_RETRY_DELAY;
    // VPN 설정을 위한 대기 시간
    long DELAY_FOR_VPN_CONFIG;
    // 시큐위즈 VPN 서버 주소
    String SECUWAY_SSL_SERVER;
    int SECUWAY_SSL_VERSION;

    boolean stopPrevConnection = false;
    AtomicInteger status = new AtomicInteger(STATUS_NOT_READY);
    int retryCount = 0;

    public SecuwizVPNSolution(Context context) {
        super(context);

        SECUWAY_SSL_SERVER = new Aria(context.getString(R.string.MagicMRSLicense)).decrypt(context.getString(R.string.SecuwaySSLServer));
        SECUWAY_SSL_VERSION = context.getResources().getInteger(R.integer.SecuwaySSLVersion);
        DELAY_FOR_VPN_CONFIG = getDelaySecForVPNConfig(context) * 1000;
        VPN_CONNECT_RETRY_COUNT = context.getResources().getInteger(R.integer.VpnConnectRetryCnt);
        VPN_CONNECT_RETRY_DELAY = context.getResources().getInteger(R.integer.VpnConnectRetryDelay);

        // Secuway SSL-VPN SERVICE 상태값을 받을 수 있는 리시버 등록.
        IntentFilter filter = new IntentFilter(SECUWIZ_VPN_STATUS_RECEIVER_ACTION);
        if (CHECK_ANDROID_FRAMEWORK_VPN) {
            filter.addAction(ANDROID_CONNECTIVITY_CHANGE);
        }
        context.registerReceiver(mVPNReceiver, filter);

        // FIXME 현재 VPN 이벤트에 대해서는 동작하지 않는 것으로 보임.
        // Android System 에서 VPN 이 설정되었는지 확인하기 위한 callback 등록
        if (CHECK_ANDROID_FRAMEWORK_VPN || false) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    .build();
            cm.registerNetworkCallback(request, networkCallback);
        }
    }

    @Override
    protected void prepare(Context context) {
        super.prepare(context);

        Intent bindServiceInfo = new Intent(SECUWIZ_VPN_SERVICE_ACTION);
        bindServiceInfo.setPackage(SECUWIZ_VPN_PACKAGE_NAME);

        if(context.bindService(bindServiceInfo, this,  Context.BIND_AUTO_CREATE)) {
            Log.d(TAG, "STEP 1. SecuwizVPN 초기화");
        } else {
            throw new RuntimeException("SecuwizVPN 서비스 바인딩을 시도할 수 없습니다.");
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceVPN = MobileApi.Stub.asInterface(service);
        status.getAndSet(STATUS_READY);
        try {
            if (serviceVPN.VpnStatus() == SECUWIZ_VPN_STATUS_CONNECTION) {
                Log.i(TAG, "기존 보안 네트워크를 종료합니다.");
                serviceVPN.StopVpn();
            }
        } catch (RemoteException e) {
            throw new RuntimeException("보안 네트워크 제어 중 에러가 발생하였습니다.", e);
        }
        Log.d(TAG, "STEP 2. SecuwizVPN 서비스 연결 성공");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "SecuwizVPN 서비스 연결 종료");
        status.getAndSet(STATUS_NOT_READY);
        serviceVPN = null;
    }

    @Override
    protected Result<SecureNetwork.STATUS> execute(Context context, String[] loginInfo) throws SolutionRuntimeException {

        do {
            if(loginInfo.length != 3) {
                throw new RuntimeException("SecuwizVPN 을 시작하기 위한 필수 정보가 없습니다.");
            }
        } while (status.compareAndSet(STATUS_NOT_READY, STATUS_NOT_READY));

        this.retryCount = 0;
        String loginId = loginInfo[0];
        String loginPw = loginInfo[1];
        SecureNetwork.CMD command = SecureNetwork.CMD.valueOf(loginInfo[2]);
        try {
            switch (command) {
                case _START:
                    handleConnectionAction(loginId, loginPw);
                    break;
                case _STOP:
                    handleDisconnectionAction();
                    break;
                case _DESTROY:
                    handleDestroyAction();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + command);
            }
        } catch (RemoteException e) {
            throw new SolutionRuntimeException("SecuwizVPN 서비스 호출 중 에러가 발생하였습니다.", e);
        }
        return null;
    }

    /*
    protected void doComplete() throws RemoteException {
        if (serviceVPN == null || secureNetwork == null) {
            return;
        }
        SecureNetwork.STATUS s = SecureNetwork.STATUS._DISCONNECTED;
        switch (serviceVPN.VpnStatus()) {
            case SECUWIZ_VPN_STATUS_DISCONNECTION:
                s = SecureNetwork.STATUS._DISCONNECTED;
                break;
            case SECUWIZ_VPN_STATUS_CONNECTION:
                s = SecureNetwork.STATUS._CONNECTED;
                break;
            case SECUWIZ_VPN_STATUS_CONNECTING:
                s = SecureNetwork.STATUS._CONNECTING;
                break;
        }
        secureNetwork.changeStatus(s);
        if (status.compareAndSet(STATUS_COMMON_BASED_VPN_CONNECTION, STATUS_COMMON_BASED_VPN_CONNECTION)) {
            onCompleted(secureNetwork);
        } else if (status.compareAndSet(STATUS_COMMON_BASED_VPN_DISCONNECTION, STATUS_COMMON_BASED_VPN_DISCONNECTION)) {
            onCompleted(secureNetwork);
        } else if (status.compareAndSet(STATUS_READY, STATUS_READY) && secureNetwork.retryConnection()) {
            handleConnectionAction();
        }
    }
     */

    private void handleConnectionAction(String id, String pw) throws RemoteException {
        switch (serviceVPN.VpnStatus()) {
            case SECUWIZ_VPN_STATUS_DISCONNECTION:
                if (retryCount > VPN_CONNECT_RETRY_COUNT) {
                    completedProcess(null, new Result<SecureNetwork.STATUS>(RESULT_CODE._FAIL, "잠시 후 다시 시도해주시기 바랍니다."));
                }
                Log.d(TAG, "상태 변경 시도 : 연결해제 -> 연결");
                String result = serviceVPN.StartVpn(SECUWAY_SSL_SERVER, id, pw, SECUWAY_SSL_VERSION);

                if (result.equals("0")) {
                    retryCount = 0;
                    Log.d(TAG, "STEP 3. Secuwiz SSL-VPN 연결시도");
                    do {
                        if (status.compareAndSet(STATUS_COMMON_BASED_VPN_CONNECTION, STATUS_COMMON_BASED_VPN_CONNECTION)) {
                            Result<SecureNetwork.STATUS> ret = new Result<>(RESULT_CODE._OK, "");
                            ret.out = SecureNetwork.STATUS._CONNECTED;
                            completedProcess(null, ret);
                            break;
                        }
                    } while (true);
                } else if (result.equals("이미 로그인한 사용자입니다.")) {
                    try{
                        Thread.sleep(VPN_CONNECT_RETRY_DELAY);
                    } catch (InterruptedException ignored) {
                    }
                    retryCount++;
                    Log.d(TAG, "연결 재시도");
                    handleConnectionAction(id, pw);
                } else {
                    retryCount = 0;
                    Log.d(TAG, "VPN 서버로 부터 응답 받은 실패 메시지 : " + result);
                    // 2019-04-19 : NIA 요청으로 인하여 VPN 서버로부터 전달되는 메시지는 모두 동일하게 표현.
                    completedProcess(null, new Result<SecureNetwork.STATUS>(RESULT_CODE._FAIL, "보안 네트워크 연결을 실패하였습니다"));
                }
                break;
            case SECUWIZ_VPN_STATUS_CONNECTION:
            case SECUWIZ_VPN_STATUS_CONNECTING:
                Log.w(TAG, "연결중/연결 -> 연결, 재연결이 필요한가요?");
                completedProcess(null, new Result<SecureNetwork.STATUS>(RESULT_CODE._CANCEL, "이미 연결 중입니다."));
            default:
                throw new IllegalStateException("SecuwizVPNSolution.handleConnection() Unexpected value: " + serviceVPN.VpnStatus());
        }
    }

    private Result<SecureNetwork.STATUS> handleDisconnectionAction() throws RemoteException {
        switch (serviceVPN.VpnStatus()) {
            case SECUWIZ_VPN_STATUS_DISCONNECTION:
                Log.d(TAG, "이미 연결해제 상태압니다.");
                break;
            case SECUWIZ_VPN_STATUS_CONNECTION:
            case SECUWIZ_VPN_STATUS_CONNECTING:
                serviceVPN.StopVpn();
                break;
            default:
                throw new IllegalStateException("SecuwizVPNSolution.handleDisconnection() Unexpected value: " + serviceVPN.VpnStatus());
        }
        return null;
    }

    private Result<SecureNetwork.STATUS> handleDestroyAction() {
        return null;
    }

    private int getDelaySecForVPNConfig(Context c) {
        try {
            String requiredPackages = ResourceUtils.loadResourceRaw(c, R.raw.vpn_delay);
            String device = android.os.Build.MODEL;
            JSONObject jsonObj = (JSONObject) new JSONTokener(requiredPackages).nextValue();
            JSONArray jsonArr = jsonObj.getJSONArray("vpn_delay_devices");
            for (int i = 0 ; i < jsonArr.length() ; i++) {
                JSONObject object = jsonArr.getJSONObject(i);
                String deviceName = object.getString("device");
                if (device.startsWith(deviceName)) {
                    int delay_sec= 0;
                    try {
                        delay_sec = object.getInt("delay");
                    } catch (JSONException e) {
                        Log.w(TAG, "지연 시간정보를 읽을 수 없어서 기본값으로 설정합니다. (2초)", e);
                        delay_sec = 2;
                    }
                    return delay_sec;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "VPN 지연 데이터를 읽을 수 없습니다.", e);
        } catch (Exception e) {
            Log.e(TAG, "VPN 지연 데이터를 읽을 수 없습니다.", e);
        }

        return 0;
    }
}
