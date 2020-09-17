package com.sds.BizAppLauncher.gov.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sds.mobile.servicebrokerLib.ServiceBrokerLib;
import com.sds.mobile.servicebrokerLib.event.ResponseEvent;
import com.sds.mobile.servicebrokerLib.event.ResponseListener;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import kr.go.mobile.iff.util.LogUtil;

@Deprecated
public class GovFileUtil{
	private static final String TAG = "GovFileUtil";
	public static String uploadFile(Context context, String serverURL, String userID, String password, String attachID, String wsdlURL, String namespace, String src, String fileName){
		if ((wsdlURL == null) || (src == null)) {
			return "-1003";
		}

		String filePath = "";
		File uploadFile = null;

		filePath = src;
		uploadFile = new File(filePath);
		try
		{
			if ((uploadFile == null) || (!uploadFile.exists()) || (!uploadFile.isFile()))
				throw new FileNotFoundException();
		}
		catch (FileNotFoundException e) {
			LogUtil.e(TAG, e.getMessage());
			return "-1000";
		}

		String result = sendToServer(context, wsdlURL, uploadFile, null);
		LogUtil.d(TAG, "GovFile upload :: " + result);
		return result;
	}

	public static String uploadFile(Context context, String serverURL22, String userID22, String password22, String attachID22, String wsdlURL, String namespace22, byte[] raw, String fileName){
		if ((wsdlURL == null) || (raw == null) || (fileName == null)) {
			return "-1003";
		}
		String filePath = "";
		File uploadFile = null;

		filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "download" + File.separator;
		File tempFolder = new File(filePath);
		
		if (!tempFolder.exists()) {
			tempFolder.mkdir();
		}
		
		String retCode = "";
		FileOutputStream os = null;
		try
		{
			uploadFile = new File(filePath + fileName);
			checkExistAndDeleteFileAndCreate(uploadFile);

			os = new FileOutputStream(uploadFile, true);

			os.write(raw);
			os.flush();
		}
		catch (FileNotFoundException e) {
			checkExistAndDeleteFile(uploadFile);
			LogUtil.e(TAG, e.getMessage());
			retCode = "-1000";
		} catch (IOException e) {
			checkExistAndDeleteFile(uploadFile);
			LogUtil.e(TAG, e.getMessage());
			retCode = "-1004";
		} catch (Exception e) {
			checkExistAndDeleteFile(uploadFile);
			LogUtil.e(TAG, e.getMessage());
			retCode = "-1004";
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					LogUtil.e(TAG, e.getMessage());
				}
			}
		}
		
		if (retCode.length() > 0) {
			return retCode;
		}
		
		String result = sendToServer(context, wsdlURL, uploadFile, null);

		checkExistAndDeleteFile(uploadFile);

		return result;
	}
	
	private static synchronized void checkExistAndDeleteFileAndCreate(File file) throws IOException {
		if (file.exists()) {
			file.delete();
			file.createNewFile();
		} else {
			file.createNewFile();
		}
	}
	
	public static String uploadFile(Context context, String wsdlURL, String src, String parameter){
		if ((wsdlURL == null) || (src == null)) {
			return "-1003";
		}

		String filePath = "";
		File uploadFile = null;

		filePath = src;
		uploadFile = new File(filePath);
		try
		{
			if ((uploadFile == null) || (!uploadFile.exists()) || (!uploadFile.isFile()))
				throw new FileNotFoundException();
		}
		catch (FileNotFoundException e) {
			LogUtil.e(TAG, e.getMessage());
			return "-1000";
		}

		String result = sendToServer(context, wsdlURL, uploadFile, parameter);
		LogUtil.d(TAG, "GovFile upload :: " + result);
		return result;
	}

	public static String uploadFile(Context context, String wsdlURL, byte[] raw, String fileName, String parameter){
		if ((wsdlURL == null) || (raw == null) || (fileName == null)) {
			return "-1003";
		}
		
		String filePath = "";
		File uploadFile = null;

		filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "download" + File.separator;
		File tempFolder = new File(filePath);
		
		if (!tempFolder.exists()) {
			tempFolder.mkdir();
		}
		
		String retCode = "";
		FileOutputStream os = null;
		try
		{
			uploadFile = new File(filePath + fileName);
			checkExistAndDeleteFileAndCreate(uploadFile);

			os = new FileOutputStream(uploadFile, true);

			os.write(raw);
			os.flush();
		}
		catch (FileNotFoundException e) {
			checkExistAndDeleteFile(uploadFile);
			LogUtil.e(TAG, e.getMessage());
			retCode = "-1000";
		} catch (IOException e) {
			checkExistAndDeleteFile(uploadFile);
			LogUtil.e(TAG, e.getMessage());
			retCode = "-1004";
		} catch (Exception e) {
			checkExistAndDeleteFile(uploadFile);
			LogUtil.e(TAG, e.getMessage());
			retCode = "-1004";
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					LogUtil.e(TAG, e.getMessage());
				}
			}
		}
		
		if (retCode.length() > 0) {
			return retCode;
		}
		
		String result = sendToServer(context, wsdlURL, uploadFile, parameter);

		checkExistAndDeleteFile(uploadFile);

		return result;
	}
	
	private static synchronized void checkExistAndDeleteFile(File file) {
		if (file.exists()) {
			file.delete();
		}
	}
	
	private static String sendToServer(Context context, String wsdlURL, File uploadFile, String parameter) {
		//error 1004
		String result = "";
		
		ServiceBrokerLib service = new ServiceBrokerLib(context, new ResponseListener() {
			
			@Override
			public void receive(ResponseEvent responseEvent) {
				int resultCode = responseEvent.getResultCode();
                String resultMsg = responseEvent.getResultData();
                
                LogUtil.d(TAG, "resultCode :: " + resultCode);
                LogUtil.d(TAG, "resultMsg :: " + resultMsg);
			}
		});
		
		Intent intent = new Intent();
		intent.putExtra("dataType", "json");        
		intent.putExtra("sCode", "upload");
		intent.putExtra("filePath", uploadFile.getPath());
		if(parameter == null){
			intent.putExtra("parameter", "url="+wsdlURL+";");
		}else{
			intent.putExtra("parameter", "url="+wsdlURL+";"+parameter);
		}
		
		service.request(intent);
		
		result = service.waitToResponse();
		LogUtil.d(TAG, "GovFile upload :: " + result);
		return result;
	}
	
	public static void downloadFile(Context context, ResponseListener listener, String fileName, String filePath){
        ServiceBrokerLib lib = new ServiceBrokerLib(context, listener);

        Intent intent = new Intent();
        intent.putExtra("dataType", "json");        
        intent.putExtra("sCode", "download");
        intent.putExtra("fileName", fileName);
        intent.putExtra("filePath", filePath);
        
        lib.request(intent);
	}

}