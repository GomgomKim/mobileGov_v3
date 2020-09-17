package kr.go.mobile.iff.sample;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.infrawaretech.docviewer.DocViewerActivity;


public class DocViewerListAdapter extends BaseAdapter {

	List<DocViewerVO> DocInfoVO = new ArrayList<DocViewerVO>();
	
	@Override
	public int getCount() {
		return DocInfoVO.size();
	}

	@Override
	public Object getItem(int position) {
		return DocInfoVO.get(position);
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
		
		TextView txtServiceID = (TextView) view.findViewById(R.id.txtServiceId);
		TextView txtParmas = (TextView) view.findViewById(R.id.txtServiceParams);
		
		final DocViewerVO docViewerVO = (DocViewerVO) getItem(position);
		txtServiceID.setText(docViewerVO.getFileName());
		txtParmas.setText(docViewerVO.getFileURL());
		
		view.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				Intent i = new Intent(view.getContext(), DocViewerActivity.class);
				i.putExtra(DocViewerActivity.EXTRA_FILE_NAME, docViewerVO.getFileName());
				i.putExtra(DocViewerActivity.EXTRA_URL, docViewerVO.getFileURL());
				i.putExtra(DocViewerActivity.EXTRA_CREATED, docViewerVO.getFileCreatedDate());
				i.putExtra("LOG", true);
				view.getContext().startActivity(i);
			}
		});
		
		return view;
	}

	protected void addDocViewerVO(String fileName, String fileURL, String fileCreatedDate) {
		DocViewerVO vo = new DocViewerVO();
		vo.setFileName(fileName);
		vo.setFileURL(fileURL);
		vo.setFileCreatedDate(fileCreatedDate);
		DocInfoVO.add(vo);
	}
}
