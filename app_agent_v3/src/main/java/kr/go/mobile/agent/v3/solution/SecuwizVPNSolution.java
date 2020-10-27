package kr.go.mobile.agent.v3.solution;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

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

    private static final int STATUS_NOT_READY = 1 >> 1; // 0
    private static final int STATUS_READY = 1 >> 0; // 1

    private final BroadcastReceiver mVPNReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SECUWIZ_VPN_STATUS_RECEIVER_ACTION.equals(intent.getAction())) {
                switch (intent.getIntExtra("STATUS", SECUWIZ_VPN_STATUS_DISCONNECTION)) {
                    case SECUWIZ_VPN_STATUS_CONNECTION:
                        if (serviceVPN == null) {
                            stopPrevConnection = true;
                            return;
                        }

                        if(DELAY_FOR_VPN_CONFIG > 0) {
                            Log.w(TAG, "단말 특성상 Secuwiz SSL-VPN 연결이 지연됩니다.");
                            try {
                                Thread.sleep(DELAY_FOR_VPN_CONFIG);
                            } catch (InterruptedException ignored) {
                            }
                        }
                        onSecureNetworkConnection();
                        break;
                    case SECUWIZ_VPN_STATUS_CONNECTING:
                        break;
                    case SECUWIZ_VPN_STATUS_DISCONNECTION:
                        if(serviceVPN == null) return;

                        onSecureNetworkDisconnection();
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + intent.getIntExtra("STATUS", SECUWIZ_VPN_STATUS_DISCONNECTION));
                }
            }
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
        VPN_CONNECT_RETRY_COUNT = context.getResources().getInteger(R.integer.SecuwayConnectRetryCnt);
        VPN_CONNECT_RETRY_DELAY = context.getResources().getInteger(R.integer.SecuwayConnectRetryDelay);

        // Secuway SSL-VPN SERVICE 상태값을 받을 수 있는 리시버 등록.
        IntentFilter filter = new IntentFilter(SECUWIZ_VPN_STATUS_RECEIVER_ACTION);
        if (CHECK_ANDROID_FRAMEWORK_VPN) {
            filter.addAction(ANDROID_CONNECTIVITY_CHANGE);
        }
        context.registerReceiver(mVPNReceiver, filter);

        Intent bindServiceInfo = new Intent(SECUWIZ_VPN_SERVICE_ACTION);
        bindServiceInfo.setPackage(SECUWIZ_VPN_PACKAGE_NAME);
        if (context.bindService(bindServiceInfo, this, Context.BIND_AUTO_CREATE)) {
            Log.d(TAG, "SSL-VPN 초기화");
        } else {
            throw new RuntimeException("SecuwizVPN 서비스 바인딩을 시도할 수 없습니다.");
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceVPN = MobileApi.Stub.asInterface(service);
        try {
            if (serviceVPN.VpnStatus() == SECUWIZ_VPN_STATUS_CONNECTION) {
                Log.i(TAG, "기존 보안 네트워크를 종료합니다.");
                serviceVPN.StopVpn();
            }
        } catch (RemoteException e) {
            throw new RuntimeException("보안 네트워크 제어 중 에러가 발생하였습니다.", e);
        }

        Log.d(TAG, "SSL-VPN 초기화 - 성공");
        status.getAndSet(STATUS_READY);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "Secuwiz 서비스 연결 해제");
        status.getAndSet(STATUS_NOT_READY);
        serviceVPN = null;
    }

    void onSecureNetworkConnection() {
        Log.d(TAG, "STEP 2. Secuwiz SSL-VPN 연결 성공");
        Result<SecureNetwork.STATUS> ret = new Result<>(RESULT_CODE._OK, "");
        ret.out = SecureNetwork.STATUS._CONNECTED;
        setResult(ret);
    }

    void onSecureNetworkDisconnection() {
        Log.d(TAG, "Secuwiz SSL-VPN 연결 해제");
        Result<SecureNetwork.STATUS> ret = new Result<>(RESULT_CODE._OK, "");
        ret.out = SecureNetwork.STATUS._DISCONNECTED;
        setResult(ret);
    }

    @Override
    protected Result<SecureNetwork.STATUS> process(Context context, String[] loginInfo) throws SolutionRuntimeException {
        do {
            if(loginInfo.length != 3) {
                throw new RuntimeException("SecuwizVPN 을 시작하기 위한 필수 정보가 없습니다.");
            }
        } while (status.compareAndSet(STATUS_NOT_READY, STATUS_NOT_READY));

        this.retryCount = 0;
        String loginId = loginInfo[0];
        String loginPw = loginInfo[1];
        SecureNetwork.CMD command = SecureNetwork.CMD.valueOf(loginInfo[2]);
        Result<SecureNetwork.STATUS> ret;
        try {
            switch (command) {
                case _START:
                    ret = handleConnectionAction(context, loginId, loginPw);
                    break;
                case _STOP:
                    ret = handleDisconnectionAction();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + command);
            }
        } catch (RemoteException e) {
            throw new SolutionRuntimeException("SecuwizVPN 서비스 호출 중 에러가 발생하였습니다.", e);
        }
        return ret;
    }

    private Result<SecureNetwork.STATUS> handleConnectionAction(Context context, String id, String pw) throws RemoteException {
        switch (serviceVPN.VpnStatus()) {
            case SECUWIZ_VPN_STATUS_DISCONNECTION:
                if (retryCount > VPN_CONNECT_RETRY_COUNT) {
                    return new Result<>(RESULT_CODE._FAIL, "잠시 후 다시 시도해주시기 바랍니다.");
                }
                String respMessage = serviceVPN.StartVpn(SECUWAY_SSL_SERVER, id, pw, SECUWAY_SSL_VERSION);
                Result<SecureNetwork.STATUS> result = null;
                switch (respMessage) {
                    case "0":
                        retryCount = 0;
                        Log.d(TAG, "STEP 1. Secuwiz SSL-VPN 연결시도 - 응답 대기");
                        result = new Result<>(RESULT_CODE._WAIT, "");
                        break;
                    case "이미 로그인한 사용자입니다.":
                        try {
                            Thread.sleep(VPN_CONNECT_RETRY_DELAY);
                        } catch (InterruptedException ignored) {
                        }
                        retryCount++;
                        Log.d(TAG, String.format("연결 재시도 (%d/%d)", retryCount, VPN_CONNECT_RETRY_COUNT));
                        return handleConnectionAction(context, id, pw);
                    case "장시간 미사용으로 접속으로 종료합니다. 재접속 해주세요.":
                        retryCount = 0;
                        Log.e(TAG, "SSL-VPN 앱으로부터 전달받은 실패 메시지 : " + respMessage);
                        result = new Result<>(RESULT_CODE._FAIL, respMessage);
                        break;
                    default:
                        retryCount = 0;
                        Log.e(TAG, "SSL-VPN 앱으로부터 전달받은 실패 메시지 : " + respMessage);
                        // 2019-04-19 : NIA 요청으로 인하여 VPN 서버로부터 전달되는 메시지는 모두 동일하게 표현.
                        result = new Result<>(RESULT_CODE._FAIL, "보안 네트워크 연결을 실패하였습니다");
                        break;
                }
                return result;
            case SECUWIZ_VPN_STATUS_CONNECTION:
            case SECUWIZ_VPN_STATUS_CONNECTING:
                Log.w(TAG, "연결중/연결 -> 연결, 재연결이 필요한가요?");
                return new Result<>(RESULT_CODE._CANCEL, "이미 연결 중입니다.");
            default:
                throw new IllegalStateException("SecuwizVPNSolution.handleConnection() Unexpected value: " + serviceVPN.VpnStatus());
        }
    }

    private Result<SecureNetwork.STATUS> handleDisconnectionAction() throws RemoteException {
        switch (serviceVPN.VpnStatus()) {
            case SECUWIZ_VPN_STATUS_DISCONNECTION:
                Log.d(TAG, "이미 연결해제 상태입니다.");
                return new Result<>(RESULT_CODE._CANCEL, "이미 연결해제 중입니다.");
            case SECUWIZ_VPN_STATUS_CONNECTION:
            case SECUWIZ_VPN_STATUS_CONNECTING:
                Log.d(TAG, "Secuwiz SSL-VPN 연결 해제 시도");
                serviceVPN.StopVpn();
                return new Result<>(RESULT_CODE._WAIT, "");
            default:
                throw new IllegalStateException("SecuwizVPNSolution.handleDisconnection() Unexpected value: " + serviceVPN.VpnStatus());
        }
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
        } catch (Exception e) {
            Log.e(TAG, "VPN 지연 데이터를 읽을 수 없습니다. 기본 값으로 설정합니다.", e);
        }
        return 0;
    }

    public SecureNetwork.STATUS status() {
        if (serviceVPN == null) {
            return SecureNetwork.STATUS._UNKNOWN;
        }
        try {
            switch (serviceVPN.VpnStatus()) {
                case SECUWIZ_VPN_STATUS_CONNECTION:
                    return SecureNetwork.STATUS._CONNECTED;
                case SECUWIZ_VPN_STATUS_CONNECTING:
                case SECUWIZ_VPN_STATUS_DISCONNECTION:
                default:
                    return SecureNetwork.STATUS._DISCONNECTED;

            }
        } catch (RemoteException e) {
            return SecureNetwork.STATUS._UNKNOWN;
        }
    }
}
