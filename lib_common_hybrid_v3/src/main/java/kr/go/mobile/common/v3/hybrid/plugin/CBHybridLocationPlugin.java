package kr.go.mobile.common.v3.hybrid.plugin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Timer;
import java.util.TimerTask;

import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.common.v3.hybrid.CBHybridActivity;
import kr.go.mobile.common.v3.hybrid.CBHybridException;


/**
 * Created by ChangBum Hong on 2020-08-03.
 * cloverm@infrawareglobal.com
 */
public class CBHybridLocationPlugin extends CBHybridPlugin {

    private static final String TAG = CBHybridLocationPlugin.class.getSimpleName();
    private static final int REQ_PERMISSION_FINE_LOCATION = 2001;
    private static final String JSON_PARAM_ACCURACY = "accuracy";
    private static final String JSON_PARAM_TIMEOUT = "timeout";

    private static final long LOCATION_UPDATE_MIN_TIME = 100;
    private static final int FIND_DEFAULT_TIMEOUT = 30000;
    private static final int FIND_DEFAULT_MIN_DISTANCE = 100;

    private boolean isWaitReqPerFineLocation = true;
    private CBHybridActivity mActivity;
    private LocationManager mLocationManager;
    private Timer mLocationUpdateTimer;

    private CBHybridActivity.IRequestPermissionListener mRequestPermissionListener = new CBHybridActivity.IRequestPermissionListener() {
        @Override
        public void onResult(int reqCode, boolean result) {

            switch (reqCode) {
                case REQ_PERMISSION_FINE_LOCATION:
                    isWaitReqPerFineLocation = false;
                    break;
            }
        }
    };


    public CBHybridLocationPlugin() {

    }

    @Override
    public void init(Context context) {
        mActivity = (CBHybridActivity) context;
        mActivity.addRequestPermissionListener(REQ_PERMISSION_FINE_LOCATION, mRequestPermissionListener);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }


    /**
     * 위치정보 정확도 관련 거리 설정값 변환
     **/
    private int changeMinDistance(int distance) {
        switch (distance) {
            case 1:
                return 1000;
            case 2:
                return 500;
            case 3:
                return 100;
            case 4:
                return 50;
            case 5:
                return 10;
            default:
                return 3;
        }
    }

    /**
     * Javascript에서 사용하는 JSON Location 정보 형태로 변환
     *
     * @param location Location
     * @return JSON String
     * @throws JSONException
     */
    private final String generateJSONStr(Location location) throws JSONException {
        JSONStringer jsonStringer = new JSONStringer();
        jsonStringer.object();
        jsonStringer.key("timestamp").value(location.getTime());
        jsonStringer.key("coords");
        jsonStringer.object();
        jsonStringer.key("latitude").value(location.getLatitude());
        jsonStringer.key("longitude").value(location.getLongitude());
        jsonStringer.key("altitude").value(location.getAltitude());
        jsonStringer.key("accuracy").value((double) location.getAccuracy());
        jsonStringer.key("bearing").value((double) location.getBearing());
        jsonStringer.key("speed").value((double) location.getSpeed());
        jsonStringer.endObject();
        jsonStringer.endObject();
        return jsonStringer.toString();
    }

    /**
     * 구라이브러리 지원
     *
     * @param callbackID
     * @param jsonArgs
     * @throws CBHybridException
     */
    @Deprecated
    public void mdhGetCurrentPositionAsync(final String callbackID, String jsonArgs) throws CBHybridException {
        Log.d(TAG, "mdhGetCurrentPositionAsync callbackID=" + callbackID + "/json=" + jsonArgs);
        getCurrentPosition(callbackID, jsonArgs, new LocationListener() {
            @Override
            @SuppressLint("MissingPermission")
            public void onLocationChanged(Location location) {

                CBHybridPluginResult cbHybridPluginResult = null;

                if (location == null) {
                    cbHybridPluginResult = new CBHybridPluginResult("GPS 를 사용할 수 없습니다.");
                    cbHybridPluginResult.setStatus(CommonBasedConstants.HYBRID_ERROR_GPS_IS_NOT_AVAILABLE);
                } else {
                    try {
                        cbHybridPluginResult = new CBHybridPluginResult(generateJSONStr(location), true);
                    } catch (JSONException e) {
                        cbHybridPluginResult = new CBHybridPluginResult("위치 정보를 생성할 수 없습니다. (cause = " + e.getMessage() +")");
                        cbHybridPluginResult.setStatus(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR);
                    }
                }

                mLocationUpdateTimer.cancel();
                mLocationManager.removeUpdates(this);
                if (!mActivity.isDestroyed()) {
                    mActivity.sendAsyncResult(callbackID, cbHybridPluginResult);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        }, new LocationTimerTask(callbackID, true));
    }

    /**
     * @param callbackID
     * @param jsonArgs
     * @throws CBHybridException
     */
    public void getCurrentPositionAsync(final String callbackID, String jsonArgs) throws CBHybridException {
        Log.d(TAG, "getCurrentPositionAsync callbackID=" + callbackID + "/json=" + jsonArgs);
        getCurrentPosition(callbackID, jsonArgs, new LocationListener() {
            @Override
            @SuppressLint("MissingPermission")
            public void onLocationChanged(Location location) {

                CBHybridPluginResult cbHybridPluginResult = null;

                if (location == null) {
                    cbHybridPluginResult = new CBHybridPluginResult("GPS 를 사용할 수 없습니다.");
                    cbHybridPluginResult.setStatus(CommonBasedConstants.HYBRID_ERROR_GPS_IS_NOT_AVAILABLE);
                } else {
                    try {
                        cbHybridPluginResult = new CBHybridPluginResult(generateJSONStr(location), true);
                    } catch (JSONException e) {
                        cbHybridPluginResult = new CBHybridPluginResult("위치 정보를 생성할 수 없습니다. (cause = " + e.getMessage() +")");
                        cbHybridPluginResult.setStatus(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR);
                    }
                }

                mLocationUpdateTimer.cancel();
                mLocationManager.removeUpdates(this);
                if (!mActivity.isDestroyed()) {
                    mActivity.sendAsyncResult(callbackID, cbHybridPluginResult);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        }, new LocationTimerTask(callbackID));
    }

    /**
     * @param callbackID
     * @param jsonArgs
     * @param locationListener
     * @throws CBHybridException
     */
    @SuppressLint("MissingPermission")
    public void getCurrentPosition(String callbackID, String jsonArgs, LocationListener locationListener, TimerTask timerTask) throws CBHybridException {
        Log.d(TAG, "getCurrentPosition callbackID=" + callbackID + "/json=" + jsonArgs);
        if (!LocationManagerCompat.isLocationEnabled(mLocationManager)) {
            showLocationSettingDialog();
        } else {

            int minDistance = FIND_DEFAULT_MIN_DISTANCE;
            int timeout = FIND_DEFAULT_TIMEOUT;

            try {
                JSONObject jsonObject = new JSONObject(jsonArgs);
                if (jsonObject.has(JSON_PARAM_ACCURACY)) {
                    minDistance = jsonObject.getInt(JSON_PARAM_ACCURACY);
                    minDistance = changeMinDistance(minDistance);
                }

                if (jsonObject.has(JSON_PARAM_TIMEOUT)) {
                    timeout = jsonObject.getInt(JSON_PARAM_TIMEOUT);
                }

            } catch (JSONException e) {
                throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR, "");
            }


            if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_PERMISSION_DENIED, "위치정보에 대한 권한이 허용되지 않았습니다.");

                } else {

                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQ_PERMISSION_FINE_LOCATION);

                    //퍼미션 허용/거부 대기
                    while (isWaitReqPerFineLocation) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_PERMISSION_DENIED, "위치정보에 대한 권한이 허용되지 않았습니다.");
                        }
                    }

                    isWaitReqPerFineLocation = true;

                    if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        throw new CBHybridException(CommonBasedConstants.HYBRID_ERROR_PERMISSION_DENIED, "위치정보에 대한 권한이 허용되지 않았습니다.");
                    }
                }
            }

            if (mLocationUpdateTimer != null) {
                mLocationUpdateTimer.cancel();
            }

            mLocationUpdateTimer = new Timer();
            mLocationUpdateTimer.schedule(timerTask, timeout);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_MIN_TIME, minDistance, locationListener);
        }
    }


    /**
     * 위치정보 설정화면 이동 팝업 실행 및 이동 처리
     * (구라이브러리는 영어로 메세지가 뜸)
     */
    private void showLocationSettingDialog() {
        Log.d(TAG, "showLocationSettingDialog");
        AlertDialog alertDialog = null;
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity);
        dialogBuilder.setMessage("위치정보가 사용안함 상태입니다.\n위치정보 설정을 변경하시겠습니까?");
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("설정화면으로 이동", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent gpsSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mActivity.startActivity(gpsSettingIntent);
            }
        });
        dialogBuilder.setNegativeButton("변경안함", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    /**
     * LocationTimerTask
     * 제한된 시간동안 GPS Location 정보가 올라오지 않을 경우
     * Network Provider에 Location 정보를 Javascript로 올림
     */
    class LocationTimerTask extends TimerTask {

        private String callbackID; //콜백 ID
        private boolean isMDHCall = false;

        public LocationTimerTask(String callbackID) {
            this.callbackID = callbackID;
        }

        //구라이브러리 지원
        @Deprecated
        public LocationTimerTask(String callbackID, boolean isMDHCall) {
            this.callbackID = callbackID;
            this.isMDHCall = isMDHCall;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            CBHybridPluginResult cbHybridPluginResult;

            if (location == null) {
                cbHybridPluginResult = new CBHybridPluginResult("GPS 정보 획득에 실패하였습니다. (타임아웃)");
                cbHybridPluginResult.setStatus(CommonBasedConstants.HYBRID_ERROR_GPS_TIMEOUT);
            } else {
                try {
                    cbHybridPluginResult = new CBHybridPluginResult(generateJSONStr(location), true);
                } catch (JSONException e) {
                    cbHybridPluginResult = new CBHybridPluginResult("위치 정보를 생성할 수 없습니다. (cause = " + e.getMessage() +")");
                    cbHybridPluginResult.setStatus(CommonBasedConstants.HYBRID_ERROR_JSON_EXPR);
                }
            }

            //지정된 에러 코드 올리기
            if (!mActivity.isDestroyed()) {
                mActivity.sendAsyncResult(this.callbackID, cbHybridPluginResult);
            }
        }
    }

}
