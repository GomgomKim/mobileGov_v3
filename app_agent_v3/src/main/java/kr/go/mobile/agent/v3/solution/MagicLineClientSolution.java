package kr.go.mobile.agent.v3.solution;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.dreamsecurity.magicline.client.MagicLine;
import com.dreamsecurity.magicline.client.MagicLineType;

import java.util.Arrays;

import kr.go.mobile.agent.service.session.UserSigned;
import kr.go.mobile.agent.solution.Solution;
import kr.go.mobile.agent.utils.Aria;
import kr.go.mobile.agent.utils.HardwareUtils;
import kr.go.mobile.mobp.iff.R;

public class MagicLineClientSolution extends Solution<Void, UserSigned> {

    private static final String TAG = MagicLineClientSolution.class.getSimpleName();

    // MagiclienClient 는 인증서 관리 Activity 를 포함하고 있으므로 화면 호출에 대한 기능만 제공한다.
    // 인증서 로그인 화면(Activity) 처리 결과는 호출한(caller) Activity 의 onResultActivity 에서 처리해야 한다.
    static final int REQUEST_CODE_CERT_MANAGER = 9010;
    static final int REQUEST_CODE_CERT_SIGN = 9011;

    private MagicLine magicLine;

    public MagicLineClientSolution(Context context) {
        super(context);
        Log.d(TAG, "인증서 로그인 모듈 초기화");
        try {
            magicLine = MagicLine.getIntance(context,
                    context.getString(R.string.MagicMRSLicense),
                    MagicLineType.VALUE_CERTDOMAIN_GPKI | MagicLineType.VALUE_CERTDOMAIN_EPKI,
                    MagicLineType.VALUE_KEYSECURITY_NFILTER );

            magicLine.setIntranetCertMove(
                    new Aria(context.getString(R.string.MagicMRSLicense)).decrypt(context.getString(R.string.MagicMRSIPIntra)), //MAGICMRS_IP_INTRA,
                    context.getResources().getInteger(R.integer.MagicMRSPort),
                    context.getString(R.string.MagicMRSAppId), //MAGICMRS_APPID,
                    context.getString(R.string.import_from_internet),
                    context.getString(R.string.import_from_intra));
        } catch (Exception e) {
            throw new SolutionRuntimeException("인증서 로그인 모듈 초기화를 실패하였습니다.", e);
        }
    }

    private byte[] getSignedData(Context context) throws Exception {
        StringBuilder signedData = new StringBuilder();

        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            // FIXME AOS 10 부터 IMEI 값을 추출할 수 없음.
            // String imei = HardwareUtils.getDeviceId(context);
            String phoneNum = HardwareUtils.getLine1Number(context);
            String secureAndroidID = HardwareUtils.getAndroidID(context);
            if (phoneNum == null || secureAndroidID == null) {
                throw new NullPointerException("단말 정보 획득에 실패하였습니다.");
            }

            signedData.append("PhoneMAC=").append("null");
            signedData.append("&PhoneIMEI=").append("null");
            signedData.append("&PhoneUDID=").append(secureAndroidID);
            signedData.append("&PhoneNo=").append(phoneNum);
            signedData.append("&PhoneOSName=").append("Android");
            signedData.append("&PhoneOSVer=").append(android.os.Build.VERSION.RELEASE);
            signedData.append("&ServiceAppId=").append(context.getPackageName());
            signedData.append("&ServiceAppName=").append(context.getString(R.string.app_description));
            signedData.append("&ServiceAppVer=").append(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Exception("보안 Agent 정보를 획득할 수 없습니다.", e);
        } catch (Exception e) {
            throw e;
        }
        return signedData.toString().getBytes();
    }

    private void _showCertList(Context context) throws Exception {
        Log.d(TAG, "인증서 리스트 화면으로 이동");
        try {
            // context, 서명데이터, 콜백함수, 패스워드 오류 시도횟수, 횟수 초과 시 삭제여부
            magicLine.signShow(context,
                    getSignedData(context),
                    REQUEST_CODE_CERT_SIGN,
                    context.getResources().getInteger(R.integer.MagicSignWrongPasswordCount),
                    true);
        } catch (Exception e) {
            throw new Exception("인증서 목록 화면 호출을 실패하였습니다.", e);
        }
    }

    private void _showCertManager(Context context) throws Exception {
        Log.d(TAG, "인증서 관리 화면으로 이동");
        try {
            magicLine.certManagerShow(context,
                    new Aria(context.getString(R.string.MagicMRSLicense)).decrypt(context.getString(R.string.MagicMRSIP)),
                    context.getResources().getInteger(R.integer.MagicMRSPort),
                    context.getString(R.string.MagicMRSAppId),
                    REQUEST_CODE_CERT_MANAGER,
                    MagicLineType.VALUE_NETWORK_ALL);
        } catch (Exception e) {
            throw new Exception("인증서 관리 화면 호출을 실패하였습니다.", e);
        }
    }

    @Override
    protected Result<UserSigned> process(Context context, Void v) {
        Result<UserSigned> ret = null;
        try {
            _showCertList(context);
            ret = new Result<>(RESULT_CODE._WAIT, "");
        } catch (Exception e) {
            Log.e(TAG, "인증서 리스트 호출 에러", e);
            ret = new Result<>(RESULT_CODE._INVALID, e.getMessage());
        }
        return ret;
    }

    @Override
    protected Integer[] getRequestCodes() {
        return new Integer[]{REQUEST_CODE_CERT_MANAGER, REQUEST_CODE_CERT_SIGN};
    }

    @Override
    protected boolean onActivityResult(Context context, int requestCode, int resultCode, Intent intent) {
        try {
            switch (requestCode) {
                case REQUEST_CODE_CERT_MANAGER: {
                    _showCertList(context);
                    break;
                }
                case REQUEST_CODE_CERT_SIGN: {
                    if (resultCode == Activity.RESULT_OK) {
                        // FIXME 고유 암호값으로 사용할 수 있을까??
                        Log.v(TAG, "getSigner ::: " + Arrays.toString(MagicLine.getSignerVIDRandom(intent)));

                        String subjectDN = MagicLine.getSignerSubjectDN(intent);
                        String signedDataBase64 = MagicLine.getSignedDataBase64(intent);

                        UserSigned signed = new UserSigned(subjectDN, signedDataBase64);
                        Result<UserSigned> result = new Result<>(signed);
                        setResult(result);
                    } else {
                        int errorCode = MagicLine.getErrorCode(intent);
                        switch (errorCode) {
                            case MagicLineType.MAGICLINE_SIGN_EMPTY_CERT:
                            case MagicLineType.LAUNCHER_GO_MANAGE:
                                // 인증서가 없음 -> 인증서 관리 페이지 호출
                                try {
                                    _showCertManager(context);
                                } catch (Exception t) {
                                    setResult(new Result<UserSigned>(RESULT_CODE._INVALID, t.getMessage()));
                                }
                                break;
                            case MagicLineType.MAGICLINE_SIGN_USER_CANCEL:
                                // 사용자 취소 -> 앱 종료
                                setResult(new Result<UserSigned>(RESULT_CODE._CANCEL, "사용자가 로그인을 취소하였습니다."));
                                break;
                            default:
                                // 인증서 모듈 연동 에러 -> 앱 종료
                                setResult(new Result<UserSigned>(RESULT_CODE._INVALID, String.format("지원센터에 문의 바랍니다. (errorCode: %d)", errorCode)));
                                break;
                        }
                    }
                    break;
                }
                default:
                    return false;
            }
            return true;
        } catch (Exception e) {
            setResult(new Result<UserSigned>(RESULT_CODE._INVALID, e.getMessage()));
        }
        return super.onActivityResult(context, requestCode, resultCode, intent);
    }
}
