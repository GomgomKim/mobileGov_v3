package com.infrawaretech.docviewer.plugin;

import kr.go.mobile.mobp.mff.lib.plugins.NewMDHPlugin;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Intent;
import android.util.Log;

import com.infrawaretech.docviewer.DocViewerActivity;

import com.infrawaretech.docviewer.ui.R;
import com.infrawaretech.docviewer.utils.LibraryTool;
import com.sds.mobiledesk.mdhybrid.MDHPlugin.MDHPluginResult;
import com.sds.mobiledesk.mdhybrid.common.MDHCommonError;
import com.sds.mobiledesk.mdhybrid.common.MDHCommonException;

public class DOCViewPlugin extends NewMDHPlugin {
	
	private static final String TAG = DOCViewPlugin.class.getSimpleName();
	private static final int DOC_VIEW_REQUEST_CODE = 9001;
	
	private final String JAVASCRIPT_UNDEFINED = "undefined";
	private final String KEY_SERVICEID = "serviceID";
	private final String KEY_ATTACHID = "attachID";
	private final String KEY_FILE_DATA = "file_name";
	private final String PREFIX_URL = "URL";
	private final String SPLIT_RAGULAR = ";";
	private final int INDEX_REAL_URL= 5;
	private final int INDEX_FILENAME = 0;
	private final int INDEX_FILE_CREATED_DATE = 1;
	
	public MDHPluginResult load(String reqParams) {
		int error_code = MDHCommonError.MDH_UNKNOWN_ERROR;
		
		try {
			if (reqParams == "" || reqParams.equals(JAVASCRIPT_UNDEFINED)) {
				throw new MDHCommonException(MDHCommonError.MDH_INVALID_ARGUMENT);
			}
			
			JSONObject jsonParams = (JSONObject)new JSONTokener(reqParams).nextValue();
			
			if (!jsonParams.has(KEY_ATTACHID)) { 
				throw new MDHCommonException(MDHCommonError.MDH_INVALID_ARGUMENT);
			}
			
			if (!jsonParams.has(KEY_SERVICEID) && false) { 
			  throw new MDHCommonException(MDHCommonError.MDH_INVALID_ARGUMENT);
			}
			
			String str = jsonParams.getString(KEY_ATTACHID);
			
			if ((str == null) || (str.length() < 1) 
			    || (str == " ")){
				throw new MDHCommonException(MDHCommonError.MDH_INVALID_ARGUMENT);
			}
			
			if (str.startsWith("http") && false) {
			  throw new MDHCommonException(MDHCommonError.MDH_INVALID_ARGUMENT);
			}
			
			String attach_id = jsonParams.getString(KEY_ATTACHID);
			String file_data =jsonParams.getString(KEY_FILE_DATA);
			String serviceId =false ? jsonParams.getString(KEY_SERVICEID) : "" ;
			
			String fileTargetUrl = attach_id;
			String fileName = "";
			String fileCreated = "";
			String fileExt = "";
			if (attach_id.startsWith(PREFIX_URL)) {
				fileTargetUrl = attach_id.substring(INDEX_REAL_URL);
			}
			
			String[] fileData = file_data.split(SPLIT_RAGULAR);
			try {
				fileName = fileData[INDEX_FILENAME];
			} catch (ArrayIndexOutOfBoundsException e) {}
			try {
				fileCreated = fileData[INDEX_FILE_CREATED_DATE];
			} catch (ArrayIndexOutOfBoundsException e) {}
			
			int indexExt = fileName.lastIndexOf(".");
			if(indexExt < 0){
				throw new MDHCommonException(MDHCommonError.MDH_INVALID_ARGUMENT);
			}
			
			fileExt = fileName.substring(indexExt + 1, fileName.length()); 
			
			Log.d(TAG, "[RequestData] URL=" + fileTargetUrl + ", FILE_NAME=" + fileName + ", FILE_EXT=" + fileExt + ", FILE_CREATED=" + fileCreated);
			
			// 문서뷰어 화면 호출
			Intent intent = new Intent(getContext(), DocViewerActivity.class);
			
			intent.putExtra(DocViewerActivity.EXTRA_SERVICE_ID, serviceId);
			intent.putExtra(DocViewerActivity.EXTRA_URL, fileTargetUrl);
			intent.putExtra(DocViewerActivity.EXTRA_FILE_NAME, fileName);
			intent.putExtra(DocViewerActivity.EXTRA_CREATED, fileCreated);
			
			intent.putExtra("LOG", true); //debug option
	
			startActivityForResult(this, intent, DOC_VIEW_REQUEST_CODE);
			error_code = MDHCommonError.MDH_START_ACTIVITY_FOR_RESULT;
		} catch (MDHCommonException e) {
			error_code = e.getErrorCode();
		} catch (JSONException e) {
		  e.printStackTrace();
			error_code = MDHCommonError.MDH_JSON_EXP_ERROR;
		}
		
		return new MDHPluginResult(error_code, getErrorMessage(error_code));
	}

	public MDHPluginResult getVersions() {
		return new MDHPluginResult(0, "1.0");
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case DOC_VIEW_REQUEST_CODE:
			sendAsyncResult(new MDHPluginResult(0));
			break;

		default:
			// TODO 
			break;
		}
	}

	@Override
	public void onDestroy() {
		DocViewerActivity.THIS.finish();
	}

	@Override
	public void onPause() {

	}

	@Override
	public void onResume() {

	}

	private String getErrorMessage(int code) {
		int res_id = -1;
		switch (code) {
		
		case MDHCommonError.MDH_START_ACTIVITY_FOR_RESULT:
			return "";
		case MDHCommonError.MDH_INVALID_ARGUMENT:
			res_id = R.string.it_dv_unkown_error_msg;
			break;
		case MDHCommonError.MDH_JSON_EXP_ERROR:
			res_id = R.string.it_dv_json_exp_error_msg;
			break;
		default:
			res_id = R.string.it_dv_unkown_error_msg;
			break;
		}
		
		if (res_id > 0) {
			return getContext().getString(res_id);
		} else {
			return "";
		}
	}
}
