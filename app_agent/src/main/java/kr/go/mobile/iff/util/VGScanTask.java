package kr.go.mobile.iff.util;

import org.json.JSONException;
import org.json.JSONObject;

import com.infratech.ve.agent.remote.VGObserver;
import com.infratech.ve.agent.remote.VGRemote;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import kr.go.mobile.mobp.iff.R;

/**
 * VG 서비스를 별도의 스레드로 실행하기 위한 객체이다.
 * 
 * @author 윤기현
 *
 */
public abstract class VGScanTask extends AsyncTask<Void, Integer, Integer> implements VGObserver {

  public final static int ERROR_NONE = 1;
  
  /**
   * 백선 서비스 바인딩 에러
   */
  public final static int ERROR_NO_BIND = 100;
  /**
   * 정책 설정 에러
   */
  public final static int ERROR_SETTING_FAILED = 101;
  
  public final static int ERROR_CMD_PERMISSION_NOT_GRANTED = 102;
  /**
   * 검사 결과 확인 에러
   */
  public final static int ERROR_READ_THREAT = 103;
  /**
   * 
   */
  public final static int ERROR_DISABLED_REALTIME_SCAN = 10001;
  /**
   * 악성 코드가 발견됨
   */
  public final static int ERROR_INFECTED_PACKAGE = 10002;
  /**
   * 루팅단말. 
   */
  public final static int ERROR_ROOTING_DEVICE = 10003;

  private Object mLock = new Object();
  private static boolean IS_OPERATOR = false;

  private final boolean LOG_ENABLE = true;

  private final String VG_DATA_SPLIT = ";";
  private final String VG_IS_ROOTING_DEVICE = "isRooting";
  private final String VG_IS_CHECKED_MALWARE = "isMalwareCheck";
  private final String VG_ENABLED_REALTIME_SCAN= "isRealtimeScanServiceEnable";
  
  
  private final int ERROR_UNKNOWN_RESPONSE = 1;

  private VGRemote mVGRemote;
  
  
  private Context mContext = null;

  private boolean isShow = false;
  private ProgressDialog mProgressDialog;


  protected abstract void onCompletedScan(int infectedCount, boolean enableRealtime);

  protected abstract void onError(int errorCode);

  // V-Guard Enterprise Agent와 서비스 연결이 성공한 경우 호출됨.
  @Override
  public void onBinded() {
    System.out.println("Bind SUCC");
    
    execute();
    //   --> 해당 task 를 실행한다. 
  }

  // V-Guard Enterprise Agent와 서비스 연결이 해제된 경우 호출됨.
  @Override
  public void onUnbinded() {
    System.out.println("unBind SUCC");
    mVGRemote.closeUnRegisterReceiver();
  }
  
  @Override
  public void onRegistedReceiver(Intent intent) {
    // 2017-09-13 / Jason G. Yoon / 아직 별도의 처리를 하지 않음. 
  }
  
  /**
   * V-Guard 서비스의 바인딩을 시도하고, 바인딩이 완료되면 검사를 실행한다.
   * 
   * @param cxt
   *          - Context
   */
  public void start(Context cxt) {

    synchronized (mLock) {
      if (IS_OPERATOR)
        return;
      IS_OPERATOR = true;
    }

    if (cxt instanceof Activity) {
      isShow = true;
    }
    mContext = cxt;
    mVGRemote = new VGRemote(mContext, this);
    // --> 안티바이러스 서비스와 바인딩이 완료되면 onBinded() 메소드 호출.
  }

  @Override
  protected void onPreExecute() {
    LogUtil.d(getClass(), "onPreExecute", LOG_ENABLE);
    // 검사 결과 및 업데이트 결과에 대한 응답을 받기위한 Receiver 등록

    // 보안 검사 중입니다.
    if (isShow) {
      Activity activity = (Activity) mContext;
      if (mProgressDialog == null && !(activity.isFinishing() || activity.isDestroyed())) {
        mProgressDialog = ProgressDialog.show(activity, "", activity.getString(R.string.security_msg), true);
        mProgressDialog.show();
      }
    }

    super.onPreExecute();
  }

  @Override
  protected Integer doInBackground(Void... params) {
    int code = ERROR_NO_BIND;
    
    if(mVGRemote == null) { 
      LogUtil.d(getClass(), "VG Remote 서비스가 바인딩되어 있지 않습니다..", LOG_ENABLE);
      return code;
    }
    
    LogUtil.d(getClass(), "VG 퍼미션을 확인합니다..", LOG_ENABLE);
    String ret = mVGRemote.VGRunCMD(VGRemote.CMD_VG_PERMISSION);
    if  ((code = checkResultMessage(ret)) != ERROR_NONE) {
      return code;
    }
    
    LogUtil.d(getClass(), "VG 정책을 설정합니다.", LOG_ENABLE);
    ret = mVGRemote.VGRunCMD(VGRemote.CMD_POLICY_SAVE, "default_policy");
    if  ((code = checkResultMessage(ret)) != ERROR_NONE) {
      return code;
    }
    
    ret = mVGRemote.VGRunCMD(VGRemote.CMD_POLICY_APPLY);
    if  ((code = checkResultMessage(ret)) != ERROR_NONE) {
      return code;
    }
    
    ret = mVGRemote.VGRunCMD(VGRemote.CMD_VG_SECURITY_THREAT);
    if  ((code = checkResultMessage(ret)) != ERROR_NONE) {
      return code;
    }
    
    // 위협 상태를 확인
    return validSecurityThreat(ret);
  }
  
  private int checkResultMessage(String result) {
    if (result.equals(VGRemote.ERROR_SUCC)) {
      return ERROR_NONE;
    } else if (result.contains(String.valueOf(VGRemote.ERROR_SAVE_PERMISSION_NOT_GRANTED)) ||
        result.contains(String.valueOf(VGRemote.ERROR_PHONE_PERMISSION_NOT_GRANTED)) || 
        result.contains(String.valueOf(VGRemote.ERROR_VG_NOT_RUNNING))) {
        mVGRemote.VGRunCMD(VGRemote.CMD_VG_RUN);
      return ERROR_CMD_PERMISSION_NOT_GRANTED;
    } 
    return ERROR_UNKNOWN_RESPONSE;
  }
  
  private int validSecurityThreat(String threatData) {
    
    JSONObject jsonObject = null;
    String[] arrData= threatData.split(VG_DATA_SPLIT);
    
    try {
      for (String data : arrData) {
          
          if (data.contains(VG_IS_CHECKED_MALWARE)) {
            jsonObject = new JSONObject(data);
            if (jsonObject.getBoolean(VG_IS_CHECKED_MALWARE)) {
              return ERROR_INFECTED_PACKAGE;
            }
          } else if (data.contains(VG_IS_ROOTING_DEVICE)) {
            jsonObject = new JSONObject(data);
            if (jsonObject.getBoolean(VG_IS_ROOTING_DEVICE)) {
              return ERROR_ROOTING_DEVICE;
            }
          } else if (data.contains(VG_ENABLED_REALTIME_SCAN)) {
            jsonObject = new JSONObject(data);
            if ( ! jsonObject.getBoolean(VG_ENABLED_REALTIME_SCAN)) {
              return ERROR_DISABLED_REALTIME_SCAN;
            }
          }
      }
    } catch (JSONException e) {
      return ERROR_READ_THREAT;
    }
    
    return ERROR_NONE;
  }
  
  @Override
  protected void onProgressUpdate(Integer... values) {
    super.onProgressUpdate(values);
  }

  @Override
  protected void onPostExecute(Integer result) {
    super.onPostExecute(result);
    
    LogUtil.d(getClass(), "onPostExecute", LOG_ENABLE);
    
    if (isShow) {
      // 2016.11.22 윤기현 - 보안 환경 설정 중입니다. 다이얼로그 상자 보이기
      if (mProgressDialog != null) {
        mProgressDialog.dismiss();
        mProgressDialog = null;
      }
    }
    
    if (result == ERROR_NONE) {
      onCompletedScan(0, true);
    } else {
      onError(result);
    }


  }

  @Override
  protected void onCancelled() {
    destroy();
  }

  /**
   * V3 서비스와의 연결을 해제한다.
   */
  public void destroy() {
    synchronized (mLock) {
      IS_OPERATOR = false;
    }
    mVGRemote.closeUnRegisterReceiver();
    // AIDL unbindService
    mVGRemote.close();
    mVGRemote.close();
  }
  
  public static String getErrorMessage(int errorCode) {
    switch (errorCode) {
    case ERROR_NO_BIND:
      return "V-Guard 서비스와 연결되지 않았습니다..";
    case ERROR_SETTING_FAILED:
      return "V-Guard 정책을 설정할 수 없습니다.";
    case ERROR_CMD_PERMISSION_NOT_GRANTED:
      return "권한 허용을 적용하기 위하여 앱을 다시 실행하시기 바랍니다.";
    case ERROR_READ_THREAT:
      return "V-Guard 위협 사항 데이터를 확인할 수 없습니다.";
    case ERROR_DISABLED_REALTIME_SCAN:
      return "실시간검사가 비활성화 되어 있습니다.";
    case ERROR_INFECTED_PACKAGE:
      return "악성코드가 발견되었습니다.";
    case ERROR_ROOTING_DEVICE:
      return " 루팅 단말입니다.";
    default:
      return "정의되지 않은 오류가 발생하였습니다.";
    }
  }

}
