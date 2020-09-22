package kr.go.mobile.iff.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;


import androidx.core.app.ActivityCompat;

import kr.go.mobile.mobp.iff.mobp.rpc.client.android.AbstractPayloadBuilder;
import kr.go.mobile.mobp.iff.mobp.rpc.client.android.JsonPayloadBuilder;
import kr.go.mobile.mobp.iff.mobp.rpc.util.RPCEnumUtils;
import kr.go.mobile.mobp.iff.util.NotInstalledRequiredPackagesException;
import kr.go.mobile.mobp.iff.R;

import static android.Manifest.permission.READ_PHONE_STATE;

/**
 * 모바일 런처에서 안드로이드 시스템을 연동하기 위한 유틸 도구
 * 
 * @author 윤기현
 *
 */
public class Utils {
	
	private static final String TAG = Utils.class.getSimpleName();
	
	public static final int RESULT_OK = 0;
	
	public static class TimeStamp {
		private static boolean ENABLE = true;
		private static final String TAG = TimeStamp.class.getSimpleName();
		private static final Map<String, Long> sTEMP= new HashMap<String, Long>(0);

		public static void startTime(String tag) {
			if (ENABLE && !sTEMP.containsKey(tag)) {
				sTEMP.put(tag, System.currentTimeMillis());
			}
		}
		
		public static void endTime(String tag) {
			if (ENABLE) {
				if (sTEMP.containsKey(tag)) {
					Long currentTime = System.currentTimeMillis();
					Long prevTime = sTEMP.remove(tag);
					LogUtil.d(TAG, String.format("%s: %s(ms)", tag, currentTime - prevTime));
//					sTEMP.remove(tag);
				} else {
					LogUtil.d(TAG, String.format("undefined %s (after calling startTime(String tag))", tag));
				}
			}
		}
		
		public static void clear() {
			sTEMP.clear();
		}
	}
	
	public static enum GET_TYPE {
		NONE,
		ANDROID_ID,
		TELE_DEVICE_ID,
	}
	
	/**
	 * 
	 * 
	 * @param c
	 * @return 단말기의 고유 ID 값을 획득하는데 실패하면 null 값을 리턴한다.
	 */
	public static String getDeviceID(final Context c) {
		return getDeviceID(c, GET_TYPE.NONE);
	}
	
	/**
	 * 
	 * 
	 * @param c
	 * @param type
	 * @return 단말기의 고유 ID 값을 획득하는데 실패하면 null 값을 리턴한다.
	 */
	public static String getDeviceID(final Context c, GET_TYPE type) {
		switch (type) {
		case NONE:
			return _getDeviceID(c);
		case ANDROID_ID:
			return getAndroidID(c);
		case TELE_DEVICE_ID:
			return getTelephonyDeviceID(c);
		default:
			return null;
		}
	}
	
	private static final String _getDeviceID(Context c) {
		String deviceID = getAndroidID(c);
		if (deviceID.length() > 0) {
			return deviceID;
		}
		return getTelephonyDeviceID(c);
	}
	
	private static final String getAndroidID(Context c) {
		try {
			return Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
		} catch (NullPointerException e) {
			return "";
		} catch (Exception e) {
			return "";
		}
	}
	
	private static final String getTelephonyDeviceID(Context c) {
		try {
			TelephonyManager telManager = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
			return telManager.getSubscriberId();
		} catch (NullPointerException e) {
			return "";
		} catch (Exception e) {
			return "";
		}
	}
	
	public static final String loadResourceRaw(Context c, int id) {
		StringBuffer sb = new StringBuffer();
		InputStream in = null;
		BufferedInputStream bis = null;
		BufferedReader br = null;
		try {
			in = c.getResources().openRawResource(id);
			bis = new BufferedInputStream(in);
			br = new BufferedReader(new InputStreamReader(bis));
			String strLine = null;
			while ((strLine = br.readLine()) != null) {
				sb.append(strLine);
			}
		} catch (NullPointerException e) {
			LogUtil.e(Utils.class.getClass(), "", e);
		} catch (Exception e) {
			LogUtil.e(Utils.class.getClass(), "", e);
		} finally {
			try {
				if (in != null)
					in.close();
				if (br != null)
					br.close();
				if (bis != null)
					bis.close();
			} catch (IOException e) {
				LogUtil.e(TAG, "", e);
			}
			
		}
		return sb.toString();
	}
	
	/**
	 * 모바일 전자정부 런처를 실행하기 위한 필수앱이 설치되어 있는지 확인한다. 
	 * 
	 * @param c - Context
	 * @return 설치되지 않은 앱이 있다면, 앱의 package name 을 리턴한다. 만약, null 값을 리턴한다면 필수앱이 모두 설치되어 있는것이다. 
	 */
	public static final void checkRequiredApp(Context c) throws NotInstalledRequiredPackagesException {
		PackageManager pm = c.getPackageManager();
		String requiredPackages = Utils.loadResourceRaw(c, R.raw.required_package);
		String appLabel = null;
		String packageName = null;
		try {
			JSONObject jsonObj = (JSONObject) new JSONTokener(requiredPackages).nextValue();
			JSONArray jsonArr = jsonObj.getJSONArray("required_packages");
			for (int i = 0 ; i < jsonArr.length() ; i++) {
				JSONObject object = jsonArr.getJSONObject(i);
				packageName = object.getString("package");
				appLabel = object.getString("label");
				int thisApi = Build.VERSION.SDK_INT;
				int minApi = Build.VERSION.SDK_INT;
				int maxApi = Build.VERSION.SDK_INT;
				try {
					minApi = object.getInt("min.api");
				} catch (JSONException e) {
				}
				try {
					maxApi = object.getInt("max.api");
				} catch (JSONException e) {
				}

				if (minApi <= thisApi && thisApi <= maxApi) {
					pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
				}
			}
		} catch (NameNotFoundException e) {
			//  지정된 필수앱이 없을때 Exception 발생.
			throw new NotInstalledRequiredPackagesException(appLabel, packageName);
		} catch (Exception e) {
			LogUtil.e(TAG, "필수앱 목록을 읽을 수 없습니다.", e);
		}
	}
	
	public static final String getVersionNameOfPackage(Context c, String pkgName) {
		String versionName = null;
		PackageManager pm = c.getPackageManager();
		try {
			PackageInfo pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_META_DATA);
			versionName = pkgInfo.versionName;
		} catch (NameNotFoundException e) {
			versionName = "";
		}
		return versionName;
	}
	
	/**
	 * VPN 연결시 설정 정보가 안드로이드 시스템에 적용하기 위하여 소요시간이 필요할 수 있다. 
	 * 이를 위한 소요시간을 알려준다. 
	 * 
	 * @return 안드로이드 시스템에서 VPN 설정을 위한 대기 시간(초)
	 */
	public static final int getDelaySecForVPNConfig(Context c) {
		String requiredPackages = Utils.loadResourceRaw(c, R.raw.vpn_delay);
		String device = android.os.Build.MODEL;
		try {
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
						LogUtil.w(Utils.class.getClass(), "지연 시간정보를 읽을 수 없어서 기본값으로 설정합니다. (2초)", e);
						delay_sec = 2;
					}
					return delay_sec;
				}
			}
		} catch (JSONException e) {
			LogUtil.e(TAG, "VPN 지연 데이터를 읽을 수 없습니다.", e);
		} catch (Exception e) {
			LogUtil.e(TAG, "VPN 지연 데이터를 읽을 수 없습니다.", e);
		}
		
		return 0;
	}
	
	/**
	 * 입력된 서비스가 구동 중인지 확인합니다. 
	 * 
	 * @param context 
	 * @param serviceName - 서비스 존재 여부를 확인하기 위한 서비스 클래스 명 
	 * @return 입력한 서비스가 구동 중이면 true, 존재하지 않는다면 false 를 리턴한다.
	 */
	public static final boolean isStartedService(Context context, String serviceName) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
            	return service.started;
            }
        }
        return false;
	}
	
	public static final void showAlertDialog(Context c, 
			String title, String message,
			DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setNeutralButton(c.getString(android.R.string.ok), listener);
        builder.show();
	}

	private static final Object mLock = new Object();
	private static boolean enableVpn = false;
	public static final boolean checkNetwork(final Context context) {
		/*
		ConnectivityManager connectivityManager = 
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo mobile = connectivityManager.getActiveNetworkInfo();
		if (mobile.getTypeName().equalsIgnoreCase("mobile") && mobile.isConnected()) {
			return true;
		}
		return false;
		*/
//		synchronized (mLock) {
//			return enableVpn;
//		}
		return VpnCtlTask.VpnStatus.enableVPN();
	}
	
	@Deprecated
	public static final void setEnableVpn(final boolean enableVpn) {

	}
	
	public static final String genTransactionId() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.KOREA);
		return dateFormat.format(calendar.getTime());
	}
	
	public static final String buildRequestPayload(JSONObject json) {
		AbstractPayloadBuilder requestPayload = new JsonPayloadBuilder(RPCEnumUtils.DataType.REQUEST);
		List<Object> lo = new ArrayList<Object>();
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("reqAusData", json.toString().replace("\\", ""));
		lo.add(map);
		requestPayload.setParams(lo);
		
		return requestPayload.build();
	}

	public static String[] checkPermission(Context context, String[] requiredPermissions) {
		 LogUtil.d(Utils.class, "CheckPermission : " + requiredPermissions.toString());
		 Set<String> set = new HashSet<String>();
		 for (String reqPermission : requiredPermissions) {
			 if (ActivityCompat.checkSelfPermission(context, reqPermission) == PackageManager.PERMISSION_DENIED) {
				 set.add(reqPermission);
			 }
		 }
		 
		 return set.isEmpty() ? null : set.toArray(new String[]{});
	}
	
	/*
	public static String getMD5(String data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5"); 
		md.update(data.getBytes()); 
		byte byteData[] = md.digest();
		StringBuffer sb = new StringBuffer(); 

		for(int i = 0 ; i < byteData.length ; i++){
			sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
		}

		return sb.toString();
	}
	*/
	
	public static String createTempFile(String prefix, String suffix, File directory, String data) throws IOException {
        File tempfile = File.createTempFile(prefix, suffix, directory);
        tempfile.deleteOnExit();
        BufferedWriter out = null;
        try {
        	out = new BufferedWriter(new FileWriter(tempfile));
        	out.write(data);
        	out.flush();
        } finally {
        	if (out != null) {
        		out.close();
        	}
        }
        
        return tempfile.getAbsolutePath();
	}
	
	public static final String decrypt(String lic, String data) {
		return new Aria(lic).Decrypt(data);
	}
}
