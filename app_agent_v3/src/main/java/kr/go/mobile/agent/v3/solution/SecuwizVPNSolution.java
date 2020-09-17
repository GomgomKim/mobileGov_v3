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

import kr.go.mobile.agent.service.monitor.SecureNetworkData;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.utils.Aria;
import kr.go.mobile.agent.utils.Log;
import kr.go.mobile.agent.utils.ResourceUtils;
import kr.go.mobile.mobp.iff.R;

public class SecuwizVPNSolution extends Solution<SecureNetworkData, SecureNetworkData> implements ServiceConnection {

    private static final String TAG = SecuwizVPNSolution.class.getSimpleName();

    public static final int SECUWIZ_VPN_STATUS_DISCONNECTION = 0;
    public static final int SECUWIZ_VPN_STATUS_CONNECTION = 1;
    public static final int SECUWIZ_VPN_STATUS_CONNECTING= 2;

    private static final String ANDROID_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String SECUWIZ_VPN_STATUS_ACTION = "com.secuwiz.SecuwaySSL.Service.STATUS";
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
            if (SECUWIZ_VPN_STATUS_ACTION.equals(intent.getAction())) {
                switch (intent.getIntExtra("STATUS", SECUWIZ_VPN_STATUS_DISCONNECTION)) {
                    case SECUWIZ_VPN_STATUS_CONNECTION:
                        if (secureNetworkData == null) {
                            Log.i(TAG, "기존 보안 네트워크가 연결되어 있습니다.");
                            if (mVpnService == null) {
                                endPrevConnection = true;
                            } else {
                                try {
                                    mVpnService.StopVpn();
                                } catch (RemoteException e) {
                                    throw new RuntimeException("보안 네트워크 제어 중 에러가 발생하였습니다.", e);
                                }
                            }
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
                        Log.d(TAG, "STEP 3. Secuwiz SSL-VPN 연결 해제");
                        if(secureNetworkData == null) return;
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
            try {
                doComplete();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    NetworkCallback networkCallback = new NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, "STEP 4. Android VPN 설정 완료");
            status.addAndGet(STATUS_SECUWIZ_VPN_CONNECTION);
            try {
                doComplete();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
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
    private MobileApi mVpnService = null;
    // VPN 연결 재시도 횟수
    int VPN_CONNECT_RETRY_COUNT;
    // VPN 연결 재시도 딜레리 시간
    int VPN_CONNECT_RETRY_DELAY;
    // VPN 설정을 위한 대기 시간
    long DELAY_FOR_VPN_CONFIG;
    // 시큐위즈 VPN 서버 주소
    String SECUWAY_SSL_SERVER;
    int SECUWAY_SSL_VERSION;

    boolean endPrevConnection = false;
    AtomicInteger status = new AtomicInteger(STATUS_NOT_READY);
    SecureNetworkData secureNetworkData;
    int retryCount = 0;

    public SecuwizVPNSolution(EventListener<SecureNetworkData> listener) {
        super(listener);
    }

    @Override
    protected void prepare(Context context, SecureNetworkData secureNetworkData) {
        super.prepare(context, secureNetworkData);

        this.secureNetworkData = secureNetworkData;
        this.retryCount = 0;
        SECUWAY_SSL_SERVER = new Aria(context.getString(R.string.MagicMRSLicense)).decrypt(context.getString(R.string.SecuwaySSLServer));
        SECUWAY_SSL_VERSION = context.getResources().getInteger(R.integer.SecuwaySSLVersion);
        DELAY_FOR_VPN_CONFIG = getDelaySecForVPNConfig(context) * 1000;
        VPN_CONNECT_RETRY_COUNT = context.getResources().getInteger(R.integer.VpnConnectRetryCnt);
        VPN_CONNECT_RETRY_DELAY = context.getResources().getInteger(R.integer.VpnConnectRetryDelay);

        if (mVpnService == null) {
            // Secuway SSL-VPN SERVICE 상태값을 받을 수 있는 리시버 등록.
            IntentFilter filter = new IntentFilter(SECUWIZ_VPN_STATUS_ACTION);
            if (CHECK_ANDROID_FRAMEWORK_VPN) {
                filter.addAction(ANDROID_CONNECTIVITY_CHANGE);
            }
            context.registerReceiver(mVPNReceiver, filter);

            // FIXME 현재 VPN 이벤트에 대해서는 동작하지 않는 것으로 보임.
            // Android System 에서 VPN 이 설정되었는지 확인하기 위한 callback 등록
            if (false) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                        .build();
                cm.registerNetworkCallback(request, networkCallback);
            }

            Intent bindServiceInfo = new Intent(SECUWIZ_VPN_SERVICE_ACTION);
            bindServiceInfo.setPackage(SECUWIZ_VPN_PACKAGE_NAME);

            if(context.bindService(bindServiceInfo, this,  Context.BIND_AUTO_CREATE)) {
                Log.d(TAG, "STEP 1. SecuwizVPN 초기화");
                // 서비스가 바인딩되면 receiver 로 현재 VPN 상태가 바로 온다?
            } else {
                Log.e(TAG, "STEP 1. SecuwizVPN 초기화 - 실패");
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mVpnService = MobileApi.Stub.asInterface(service);
        status.getAndSet(STATUS_READY);
        if (endPrevConnection) {
            try {
                mVpnService.StopVpn();
                endPrevConnection = false;
                Log.i(TAG, "기존 네트워크를 종료합니다.");
            } catch (RemoteException e) {
                throw new RuntimeException("보안 네트워크 제어 중 에러가 발생하였습니다.", e);
            }
        }
        Log.d(TAG, "STEP 2. SecuwizVPN 서비스 연결 성공");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "SecuwizVPN 서비스 연결 종료");
        status.getAndSet(STATUS_NOT_READY);
        mVpnService = null;
    }

    @Override
    protected Result<SecureNetworkData> execute(Context context) {

        do {
            if (secureNetworkData == null) {
                finish();
                return null;
            }
        } while (status.compareAndSet(STATUS_NOT_READY, STATUS_NOT_READY));

        try {
            switch (secureNetworkData.getCommand()) {
                case _START:
                    handleConnectionAction();
                    break;
                case _STOP:
                    handleDisconnectionAction();
                    break;
                case _DESTROY:
                    handleDestroyAction();
                    break;
                default:
                    break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null; // 리턴값이 null 이면, 아무것도 처리하지 않음.
    }

    protected void doComplete() throws RemoteException {
        if (mVpnService == null || secureNetworkData == null) {
            return;
        }
        SecureNetworkData.STATUS s = SecureNetworkData.STATUS._DISCONNECTED;
        switch (mVpnService.VpnStatus()) {
            case SECUWIZ_VPN_STATUS_DISCONNECTION:
                s = SecureNetworkData.STATUS._DISCONNECTED;
                break;
            case SECUWIZ_VPN_STATUS_CONNECTION:
                s = SecureNetworkData.STATUS._CONNECTED;
                break;
            case SECUWIZ_VPN_STATUS_CONNECTING:
                s = SecureNetworkData.STATUS._CONNECTING;
                break;
        }
        secureNetworkData.changeStatus(s);
        if (status.compareAndSet(STATUS_COMMON_BASED_VPN_CONNECTION, STATUS_COMMON_BASED_VPN_CONNECTION)) {
            onCompleted(secureNetworkData);
        } else if (status.compareAndSet(STATUS_COMMON_BASED_VPN_DISCONNECTION, STATUS_COMMON_BASED_VPN_DISCONNECTION)) {
            onCompleted(secureNetworkData);
        } else if (status.compareAndSet(STATUS_READY, STATUS_READY) && secureNetworkData.retryConnection()) {
            handleConnectionAction();
        }
    }

    private void handleConnectionAction() throws RemoteException {
        switch (mVpnService.VpnStatus()) {
            case SECUWIZ_VPN_STATUS_DISCONNECTION:
                if (retryCount > VPN_CONNECT_RETRY_COUNT) {
                    onError(RESULT_CODE._FAIL, "잠시 후 다시 시도해주시기 바랍니다.");
                    return;
                }
                Log.d(TAG, "상태 변경 시도 : 연결해제 -> 연결");
                String result = mVpnService.StartVpn(SECUWAY_SSL_SERVER, secureNetworkData.getLoginID(), secureNetworkData.getLoginPw(), SECUWAY_SSL_VERSION);
                if (result.equals("0")) {
                    retryCount = 0;
                    Log.d(TAG, "STEP 3. Secuwiz SSL-VPN 연결시도");
                } else if (result.equals("이미 로그인한 사용자입니다.")) {
                    try{
                        Thread.sleep(VPN_CONNECT_RETRY_DELAY);
                    } catch (InterruptedException ignored) {
                    }
                    retryCount++;
                    handleConnectionAction();
                    Log.d(TAG, "연결 재시도");
                } else {
                    retryCount = 0;
                    Log.d(TAG, "연결 실패 : " + result);
                    // 2019-04-19 : NIA 요청으로 인하여 VPN 서버로부터 전달되는 메시지는 모두 동일하게 표현.
                    onError(RESULT_CODE._FAIL, "보안 네트워크 연결을 실패하였습니다");
                }
                break;
            case SECUWIZ_VPN_STATUS_CONNECTION:
            case SECUWIZ_VPN_STATUS_CONNECTING:
                Log.d(TAG, "연결중/연결 -> 연결, 연결해제 후 다시 연결 요청을 합니다.");
                mVpnService.StopVpn();
                break;
            default:
                throw new IllegalStateException("SecuwizVPNSolution.handleConnection() Unexpected value: " + mVpnService.VpnStatus());
        }
    }

    private void handleDisconnectionAction() throws RemoteException {
        switch (mVpnService.VpnStatus()) {
            case SECUWIZ_VPN_STATUS_DISCONNECTION:
                Log.d(TAG, "이미 연결해제 상태압니다.");
                break;
            case SECUWIZ_VPN_STATUS_CONNECTION:
            case SECUWIZ_VPN_STATUS_CONNECTING:
                mVpnService.StopVpn();
            default:
                throw new IllegalStateException("SecuwizVPNSolution.handleDisconnection() Unexpected value: " + mVpnService.VpnStatus());
        }
    }

    private void handleDestroyAction() {

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
