package kr.go.mobile.mobp.iff.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import kr.go.mobile.iff.util.LogUtil;
//import kr.go.mobile.mobp.iff.MainActivity;
import kr.go.mobile.mobp.iff.NewProgressActivity;
import kr.go.mobile.mobp.iff.http.HttpManager;
//import kr.go.mobile.mobp.iff.service.LauncherAccessibilityService;
import kr.go.mobile.mobp.mff.lib.util.E2ESetting;

public class Utils {
	private static final String TAG = HttpManager.class.getSimpleName(); 
	private static final int LOW_DPI_STATUS_BAR_HEIGHT = 19;
	private static final int MEDIUM_DPI_STATUS_BAR_HEIGHT = 25;
	private static final int HIGH_DPI_STATUS_BAR_HEIGHT = 38;

	private static final String CATEGORY_MAIN  = "android.intent.action.MAIN";
    private static final String CATEGORY_IFF   = E2ESetting.IFF_CATEGORY;
    
    public static int nNotForegroundProcessCnt = 0;

    public static final int IO_BUFFER_SIZE = 8 * 1024;
    public final static boolean DEBUG = false;    
    
    public static boolean IS_PROGRESS_SHOWING = false;

    @Deprecated
	public static void clearMem(Context c) {
//		ActivityManager amgr = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
//		List<ActivityManager.RunningAppProcessInfo> list = amgr.getRunningAppProcesses();
//		if (list != null) {
//			for (int i = 0; i < list.size(); i++) {
//				ActivityManager.RunningAppProcessInfo apinfo = list.get(i);
//				String[] pkgList = apinfo.pkgList;
//				if (apinfo.processName.contains(c.getPackageName())) {
//					for (int j = 0; j < pkgList.length; j++) {
//
//						amgr.killBackgroundProcesses(pkgList[j]);
//					}
//				}
//			}
//		}
	}

    @Deprecated
	public static String getDeviceID(final Context c) {
		String deviceID = "";
//
//		deviceID = getAndroidID(c);
//		if (deviceID.length() > 0) {
//			return deviceID;
//		}
//		deviceID = getTelephonyDeviceID(c);
//		if (deviceID.length() > 0) {
//			return deviceID;
//		}
//
//		deviceID = getWifiMacAddress(c);
//
//		LogUtil.log_d(c, "getDeviceID :: " + deviceID);
		return deviceID;
	}

    @Deprecated
	public static String getAndroidID(Context c) {
		String androidID = "";
/*
		try {
			androidID = Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
			return androidID;
		} catch (Exception e) {
			LogUtil.errorLog(c, e);
		}*/
		return androidID;
	}

    @Deprecated
	public static String getTelephonyDeviceID(final Context c) {
		String deviceID = "";
//
//		try {
//			TelephonyManager manager = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
//			deviceID = manager.getDeviceId();
//			return deviceID;
//		} catch (Exception e) {
//			LogUtil.errorLog(c, e);
//		}
		return deviceID;
	}

	@Deprecated
    public static String getWifiMacAddress(final Context c) {
		String macAddr = "";
//
//		WifiManager wMng = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
//		WifiInfo info = wMng.getConnectionInfo();
//		macAddr = info.getMacAddress();

		return macAddr;
	}

	@Deprecated
	public static int getStatusBarSizeOnCreate(DisplayMetrics displayMetrics) {
		int statusBarHeight = 0;

//		switch (displayMetrics.densityDpi) {
//		case DisplayMetrics.DENSITY_HIGH:
//			statusBarHeight = HIGH_DPI_STATUS_BAR_HEIGHT;
//			break;
//		case DisplayMetrics.DENSITY_MEDIUM:
//			statusBarHeight = MEDIUM_DPI_STATUS_BAR_HEIGHT;
//			break;
//		case DisplayMetrics.DENSITY_LOW:
//			statusBarHeight = LOW_DPI_STATUS_BAR_HEIGHT;
//			break;
//		default:
//			statusBarHeight = MEDIUM_DPI_STATUS_BAR_HEIGHT;
//		}

		return statusBarHeight;
	}

	/**
	 * @Method Name : showProgressActivity
	 * @?�성??: 2015. 10. 16.
	 * @?�성??: 조명??
	 * @변경이??:
	 * @Method ?�명 : ?�플?��? ?�비?�앱?�서 ?�이브러리�? ?�해 HttpService �??�용???�신?�때 ?�말 ?�면???�로그래?��?
	 *         보여주기 ?�함 (그냥 ?�로그래?��? ?�용?�면 ?�보?�서..)
	 */
//	private static CustomDialog customDialog;
//	
//	public static void showProgressActivity(final Context context , final String title) {
//		if(customDialog == null){
//			customDialog = new CustomDialog(context, title);
//		}else{
//			customDialog.dismiss();
//		}
//		customDialog.show();
//	}
//	
//	public static void showProgressActivity(final Context context) {
//		if(customDialog == null){
//			customDialog = new CustomDialog(context, "");
//		}else{
//			customDialog.dismiss();
//		}
//		customDialog.show();
//	}
//
//	public static void dismissProgressActivity(final Context context) {
//		if(customDialog != null){
//			customDialog.dismiss();
//		}
//	}
	
	@Deprecated
	public static void showProgressActivity(final Context context , final String title) {
		if (IS_PROGRESS_SHOWING == false) {
			Intent intent = new Intent(context, NewProgressActivity.class);
			intent.putExtra(NewProgressActivity.EXTRA_TITLE, title);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
			
			IS_PROGRESS_SHOWING = true;
		}
	}
	
	@Deprecated
	public static void showProgressActivity(final Context context) {
		if (IS_PROGRESS_SHOWING == false) {
			Intent intent = new Intent(context, NewProgressActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
			
			IS_PROGRESS_SHOWING = true;
		}
	}

	@Deprecated
	public static void dismissProgressActivity(final Context context) {
		
		if (NewProgressActivity.progressActivity != null) {
			NewProgressActivity.progressActivity.finish();
			NewProgressActivity.progressActivity = null;
			
			IS_PROGRESS_SHOWING = false;
		}
	}


	/**
	* @Method Name	:	isTablet
	* @?�성??			:	2015. 10. 22. 
	* @?�성??			:	조명??
	* @변경이??			:
	* @Method ?�명 		:  ?�이?�웃???�러�?만들지?�기?�해......
	*/
	@Deprecated
	public static boolean isTablet(final Context context) {
		// ?�블�? ??구분
		if(context == null || context.getResources() == null){
			return false;
		}
		
		int portrait_width_pixel = Math.min(context.getResources().getDisplayMetrics().widthPixels, context.getResources().getDisplayMetrics().heightPixels);
		int dots_per_virtual_inch = context.getResources().getDisplayMetrics().densityDpi;
		float virutal_width_inch = portrait_width_pixel / dots_per_virtual_inch;
		if (virutal_width_inch <= 2) {
			// is phone
			return false;
		} else {
			// is tablet
			return true;
		}
	}
	
	@Deprecated
	public static int getScreenOrientation(final Context context) {

		if(context == null){
			return Configuration.ORIENTATION_PORTRAIT;
		}
		Configuration config = context.getResources().getConfiguration();
		return config.orientation;
	}
	
//	public static Map setParameter(final Context c,final String parameter){
//		//svcXml={"Param":[{"CAR_NO":"30�?790"}],"ServiceNm":"SQL.CONFIG.NGAVL_VL00001"}]}
//
//		//parameter 추�?
//		Map map = new HashMap();
//
//		if(parameter.indexOf("&") != -1){
//        	String[] paramArray = parameter.split("&");
//        	for(String param : paramArray){
//        		int idx = param.indexOf("=");
//        		String key = param.substring(0, idx);
//        		String value = param.substring(idx+1, param.length());
//            	//String[] keyValue = param.split("=");
//            	map.put(key, value);
//            }
//        }else if(parameter.indexOf(";") != -1){
//        	String[] paramArray = parameter.split(";");
//            for(String param : paramArray){
//            	String[] keyValue = param.split("=");
//            	if(keyValue.length > 1){
//            		String key = keyValue[0];
//            		String value = keyValue[1];
//            		map.put(key, value);
//            	}else if(keyValue.length > 0){
//            		String key = keyValue[0];
//            		map.put(key, "");
//            	}
//            }
//        }else if(parameter.indexOf("=") != -1){
//        	//구분???�이 ?�나???�라메테 ??경우
//        	String[] keyValue = parameter.split("=");
//        	if(keyValue.length > 1){
//        		String key = keyValue[0];
//        		String value = keyValue[1];
//        		map.put(key, value);
//        	}else if(keyValue.length > 0){
//        		String key = keyValue[0];
//        		map.put(key, "");
//        	}
//        }
//		
//		LogUtil.log_e(c, "SSO DATA param ::: " + map.toString());
//		
//		return map;
//	}
	
	public static ArrayList<String> setParameter(final Context c,final String parameter) {
		
		ArrayList<String> list = new ArrayList<String>();

		if (parameter.indexOf(";") != -1) {
			String[] paramArray = parameter.split(";");
			for (String param : paramArray) {
				String[] paramArray2 = param.split("&");
				for (String param2 : paramArray2) {
					String[] keyValue = param2.split("=");
					if (keyValue.length > 1) {
						String key = keyValue[0];
						String value = keyValue[1];
						list.add(key + "=" + value);
					} else if (keyValue.length > 0) {
						String key = keyValue[0];
						list.add(key + "=" + "");
					}
				}
			}
		} else if (parameter.indexOf("&") != -1) {
			String[] paramArray = parameter.split("&");
			for (String param : paramArray) {
				String[] keyValue = param.split("=");
				if (keyValue.length > 1) {
					String key = keyValue[0];
					String value = keyValue[1];
					list.add(key + "=" + value);
				} else if (keyValue.length > 0) {
					String key = keyValue[0];
					list.add(key + "=" + "");
				}
			}
		} else if (parameter.indexOf("=") != -1) {
			// 구분???�이 ?�나???�라메테 ??경우
			String[] keyValue = parameter.split("=");
			if (keyValue.length > 1) {
				String key = keyValue[0];
				String value = keyValue[1];
				list.add(key + "=" + value);
			} else if (keyValue.length > 0) {
				String key = keyValue[0];
				list.add(key + "=" + "");
			}
		}

		for (int i = 0; i < list.size(); i++) {
			LogUtil.d(Utils.class, "SSO DATA get ::: " + list.get(i));
		}
		
		return list;
	}
	
	@Deprecated
	public static int PixelToDp(Context context, int pixel) {

		DisplayMetrics metrics = context.getResources().getDisplayMetrics();

		float dp = pixel / (metrics.densityDpi / 160f);

		return (int) dp;

	}
	
	@Deprecated
	public static int DpToPixel(final Context c, int dp) {
		int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics());
		return px;
	}
	
	
	/**
	* @Method Name	:	getNumColumnsGrid
	* @?�성??			:	2015. 11. 27. 
	* @?�성??			:	조명??
	* @변경이??			:
	* @Method ?�명 		: 	?�블�?모바?�일???�라 ?�한 ?�면 방향???�라 그리??컬럼 �?���?지?�해 반환?�다.
	*/
	@Deprecated
	public static int getNumColumnsGrid(final Context c){
		if (isTablet(c)) {
			if (getScreenOrientation(c) == Configuration.ORIENTATION_LANDSCAPE) {
				return 8;
			} else {
				return 5;
			}
		} else {
			if (Utils.getScreenOrientation(c) == Configuration.ORIENTATION_LANDSCAPE) {
				return 6;
			} else {
				return 3;
			}
		}
	}
	
//	public static boolean homeCheck(final String packageName, final String topActivityPkg) {
//		boolean isPackage = false;
//
//		if (topActivityPkg.equals(packageName)) {
//			isPackage = true;
//		} else if (topActivityPkg.equals("android")) {
//			isPackage = true;
//		} else if (topActivityPkg.equals("com.android.packageinstaller")) {
//			isPackage = true;
//		} else if (topActivityPkg.equals("net.secuwiz.SecuwaySSL.mobileservice")) {
//			isPackage = true;
//		} else if (topActivityPkg.equals("com.HybridPlatformExample")) {
//			isPackage = true;
//		} else {
//			List<Map<String,Object>> list = MainActivity.appList;
//
//			if (list != null) {
//				for (int i = 0; i < list.size(); i++) {
//					Map<String,Object> map = (Map<String,Object>) list.get(i);
//					Object objPackageID = map.get("packageId");
//					if (objPackageID != null) {
//						String packageId = objPackageID.toString();
//						if (topActivityPkg.equals(packageId)) {
//							isPackage = true;
//						}
//					} else {
//						isPackage = false;
//					}
//				}
//			}
//		}
//
//		return isPackage;
//	}
	
//	public static boolean isRelatedPackage(final List<String> foregroundList) {
//		boolean isPackage = false;
//		try {
//			if (foregroundList.contains("android")) {
//				isPackage = true;
//			} else if (foregroundList.contains("com.android.packageinstaller")) {
//				isPackage = true;
//			} 
////			else if (foregroundList.contains("net.secuwiz.SecuwaySSL.mobileservice")) {
////				isPackage = true;
////			} 
//			else if (foregroundList.contains("com.HybridPlatformExample")) {
//				isPackage = true;
//			} 
////			else if (foregroundList.contains("com.google.process")) { // 5.1.1 ?�말?�데 ?�꾸 ?�서..?�외..
////				isPackage = true;
////			} else if (foregroundList.contains("com.google.android.gms.persistent")) { // 5.1.1 ?�말?�데 ?�꾸 ?�서..?�외..
////				isPackage = true;
////			} else if (foregroundList.contains("android.process")) { // 5.1.1 ?�말?�데 ?�꾸 ?�서..?�외..
////				isPackage = true;
////			} 
////			else if (foregroundList.contains("com.android.settings")) { // 5.1.1 ?�말?�데 ?�꾸 ?�서..?�외..
////				isPackage = true;
////			}
////			else if (foregroundList.contains("com.samsung.hs20provider")) { // 5.1.1 ?�말?�데 ?�꾸 ?�서..?�외..
////				isPackage = true;
////			} 
//			else {
//				List<Map<String, Object>> list = MainActivity.appList;
//
//				if (list != null) {
//					for (int i = 0; i < list.size(); i++) {
//						Map<String, Object> map = (Map<String, Object>) list.get(i);
//						Object objPackageID = map.get("packageId");
//						if (objPackageID != null) {
//							String packageId = objPackageID.toString();
//							Log.d("jms", "SessionCheck packageId : " + packageId);
//							if (foregroundList.contains(packageId)) {
//								isPackage = true;
//							}
//						} else {
//							isPackage = false;
//						}
//					}
//				}
//			}
//
//			return isPackage;
//		} catch (NullPointerException e) {
//			e.printStackTrace();
//		}
//
//		return isPackage;
//	}
	
	@Deprecated
	public static boolean checkRelatedPackageOnForeGround(Context c) {
		boolean isPackage = false;
/*
		try {
			List<Map<String, Object>> list = MainActivity.appList;

//			List<String> listForegroundPackage = ProcessManager.getRunningForegroundAppsStringList(c);
//			for(String process : listForegroundPackage){
////				Log.d("jms", "checkRelatedPackageOnForeGround : " + process);								
//			}
			
//			List<String> listForegroundPackage = new ArrayList<String>();
//			listForegroundPackage.add(ProcessManager.getForegroundApp(c));
//			
			List<String> listApps = new ArrayList<String>();
			
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					Map<String, Object> map = (Map<String, Object>) list.get(i);
					Object objPackageID = map.get("packageId");
					if (objPackageID != null) {
						String packageId = objPackageID.toString();
						listApps.add(packageId);
					} 
				}	
			}
			
			// ?�외처리(?�처???�한 ?�키지�?취급) ???�키지???�기??추�?.
			listApps.add("com.google.android.packageinstaller");
			listApps.add("com.android.packageinstaller");
			listApps.add("com.android.vending");
			listApps.add("net.secuwiz.SecuwaySSL.mobileservice");
			listApps.add("com.google.android.gms.persistent");
			listApps.add("com.sds.mobile.mdm.client.MDMManager");
			listApps.add(c.getPackageName());
			
			String strForeGroundApp = ProcessManager.getForegroundApp(c);
			if(strForeGroundApp != null){
				strForeGroundApp = strForeGroundApp.trim();
			}

			
			String curPkg = LauncherAccessibilityService.getCurPackage();
			LogUtil.log_d(c, "LauncherAccessibilityService.getCurPackage=>" + curPkg);
			
		
			if(listApps.contains(strForeGroundApp) || strForeGroundApp == null){
				nNotForegroundProcessCnt = 0;
				isPackage = true;	
			}else{
				if(nNotForegroundProcessCnt >= 5){
//					nNotForegroundProcessCnt = 0;
					isPackage = false;
				}else{
					nNotForegroundProcessCnt++;
					isPackage = true;
				}
					
			}
			
			if(curPkg != null) {
				if (!isPackage || strForeGroundApp == null) {
					if(curPkg != null && listApps.contains(curPkg)) {
						isPackage = true;
					} else {
						isPackage = false;
					}
				}
			}

			return isPackage;
			
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
*/
		return isPackage;
	}
	
	@Deprecated
	public static boolean isMyProcessInTheForeground() {
		return false;
	}

	@Deprecated
	public static boolean checkRelatedPackageOnForeGroundOrg(Context c) {
		return false;
	}
	
	@Deprecated
	private static String read(String path) throws IOException {
		return null;
	}
	
	@Deprecated
	public static boolean isAppInstalled(Context context, String packageName) {
		return false;
	}
	
	@Deprecated
	public static boolean isIFFPackage(final Context context, final String packageName){
		boolean bIFFPackage = true;
//		Intent intent = new Intent(CATEGORY_MAIN);
//		intent.addCategory(CATEGORY_IFF);
//
//		PackageManager manager = context.getPackageManager();
//		List<ResolveInfo> iffGroupApps = manager.queryIntentActivities(intent, PackageManager.GET_INTENT_FILTERS);
//		ArrayList<String> listGroupAppsPackage = new ArrayList<String>();
//		// list contains 메소?�로 ?�함?�어?�는지 비교�??�하�??�려�?..
//		for (int a = 0; a < iffGroupApps.size(); a++) {
//			listGroupAppsPackage.add(iffGroupApps.get(a).activityInfo.applicationInfo.packageName);
//		}
//		
//		if(listGroupAppsPackage.contains(packageName)){
//			bIFFPackage = true;
//		}else if(isAppInstalled(context, packageName) == false){
//			bIFFPackage = true;
//		}else{
//			bIFFPackage = false;
//		}
		return bIFFPackage;
	}


	/**
     * Workaround for bug pre-Froyo, see here for more info:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
	@Deprecated
    public static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    /**
     * Get the size in bytes of a bitmap.
     * 
     * @param bitmap
     * @return size in bytes
     */
    @SuppressLint("NewApi")
    @Deprecated
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * Check if external storage is built-in or removable.
     * 
     * @return True if external storage is removable (like an SD card), false
     *         otherwise.
     */
    @SuppressLint("NewApi")
    @Deprecated
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     * 
     * @param context The context to use
     * @return The external cache dir
     */
    @SuppressLint("NewApi")
    @Deprecated
    public static File getExternalCacheDir(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    /**
     * Check how much usable space is available at a given path.
     * 
     * @param path The path to check
     * @return The space available in bytes
     */
    @SuppressLint("NewApi")
    @Deprecated
    public static long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * Get the memory class of this device (approx. per-app memory limit)
     * 
     * @param context
     * @return
     */
    @Deprecated
    public static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();
    }
    
	// public static HashMap<String, String> convertToHashMap(String jsonString)
	// {
	// HashMap<String, Integer> myHashMap = new HashMap<String, Integer>();
	// try {
	// JSONArray jArray = new JSONArray(jsonString);
	// JSONObject jObject = null;
	// String keyString = null;
	// for (int i = 0; i < jArray.length(); i++) {
	// jObject = jArray.getJSONObject(i);
	// // beacuse you have only one key-value pair in each object so I
	// // have used index 0
	// keyString = (String) jObject.names().get(0);
	// myHashMap.put(keyString, jObject.getInt(keyString));
	// }
	// } catch (JSONException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// return myHashMap;
	// }
    
    @Deprecated
	public static String getStrResource(String strResName, Context context)
			throws NameNotFoundException {

		Context resContext = context.createPackageContext(context.getPackageName(), 0);
		Resources res = resContext.getResources();

		int id = res.getIdentifier(strResName, "string", context.getPackageName());
		if (id == 0) {
			return "";
		} else {
			return res.getString(id);
		}
	}
	
    @Deprecated
	public static void stringTofile(String filename, String str) {
		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + filename);
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write(str.getBytes());
		} catch (FileNotFoundException e) {
			LogUtil.e(TAG, Log.getStackTraceString(e));
		} catch (IOException e) {
			LogUtil.e(TAG, Log.getStackTraceString(e));
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				LogUtil.e(TAG, Log.getStackTraceString(e));
			}
		}
		
	}

    @Deprecated
    public static String getURLEncode(String content, String encodingName){
        try {
            return URLEncoder.encode(content, encodingName);
        } catch (UnsupportedEncodingException e) {
        	LogUtil.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }
     
    @Deprecated
    public static String getURLDecode(String content, String encodingName){
        try {
            return URLDecoder.decode(content, encodingName);
        } catch (UnsupportedEncodingException e) {
        	LogUtil.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }	
    @Deprecated
    public static String getTimeStampStr() {
        java.util.Date date= new java.util.Date();
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS")
                             .format(date.getTime());
		
        return timeStamp;
	}
    
    @Deprecated
	public static boolean isEmail(String email) {
		if (email == null)
			return false;
		boolean b = Pattern.matches("[\\w\\~\\-\\.]+@[\\w\\~\\-]+(\\.[\\w\\~\\-]+)+", email.trim());
		return b;
	}
}
