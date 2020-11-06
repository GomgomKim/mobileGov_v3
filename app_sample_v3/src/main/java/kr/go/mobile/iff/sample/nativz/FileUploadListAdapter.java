package kr.go.mobile.iff.sample.nativz;

import android.content.Context;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import kr.go.mobile.common.v3.CommonBasedAPI;
import kr.go.mobile.common.v3.CommonBasedConstants;
import kr.go.mobile.common.v3.RestrictedAPI;
import kr.go.mobile.common.v3.broker.Response;
import kr.go.mobile.iff.sample.R;


public class FileUploadListAdapter extends BaseAdapter {

	List<FileUploadVO> fileUploadVOS = new ArrayList<>();
	
	@Override
	public int getCount() {
		return fileUploadVOS.size();
	}

	@Override
	public Object getItem(int position) {
		return fileUploadVOS.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		final Context context = parent.getContext();
		
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.serviceid_list, parent, false);
		}
		
		TextView txtServiceID = view.findViewById(R.id.txtServiceId);
		TextView txtParmas = view.findViewById(R.id.txtServiceParams);
		
		final FileUploadVO fileUploadVO = (FileUploadVO) getItem(position);
		txtServiceID.setText(fileUploadVO.path);
		txtParmas.setText(fileUploadVO.url);

		view.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				String absolutePath = fileUploadVO.path;
				String relayUrl = fileUploadVO.url;
				String params = fileUploadVO.params;
				if (absolutePath.isEmpty()) {
					Message m = NativeActivity.handler.obtainMessage(NativeActivity.PICK_FROM_ALBUM, fileUploadVO);
					m.sendToTarget();
					return;
				}
				String fileName = new File(absolutePath).getName();
				try {
					Response resp = RestrictedAPI.executeUpload(fileName, absolutePath, relayUrl, params);
					int code = resp.getErrorCode();
					if (code == CommonBasedConstants.BROKER_ERROR_NONE) {
						String message = resp.getResponseString();
						StringBuilder sb = new StringBuilder();
						sb.append("Result :: ")
								.append("code = ").append(code)
								.append(", result = ").append(message);
						Log.d("Result : ", sb.toString());
						Toast.makeText(context, sb.toString(), Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(context, "실패 - " + resp.getErrorMessage(), Toast.LENGTH_LONG).show();
					}
				} catch (CommonBasedAPI.CommonBaseAPIException e) {
					Log.e("@@@", e.getMessage(), e);
					Toast.makeText(context, "요청 실패 : " + e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		return view;
	}

	protected void addFileUploadVO(String absolutePath, String relayUrl, String extraParams) {
		FileUploadVO vo = new FileUploadVO();
		vo.setAbsolutePath(absolutePath);
		vo.setRelayURL(relayUrl);
		vo.setExtraParams(extraParams);
		fileUploadVOS.add(vo);
	}
}
