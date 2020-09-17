package com.infrawaretech.docviewer.ui;

public class LocalConstants {
	
	public static class Broadcast {
		public static final String ACTION_DOC_CONVERT_STATE = "com.infrawaretech.docview.ACTION_CONVERT_STATE";
		public static final String EXTRA_STATE = "doc_convert_state"; 
		public static final String EXTRA_TOTAL_PAGE = "doc_total_page";
		public static final String EXTRA_HASHCODE = "doc_hash_code";
		
		public static final String ACTION_DOC_CONVERT_STATUS = "com.infrawaretech.docview.ACTION_CONVERT_STATUS";
		public static final String EXTRA_STATUS = "doc_convert_status"; // 네트워크 에러 / 변환상태조회
		public static final String EXTRA_ERROR_MESSAGE = "doc_error_message";
		
		public static final String ACTION_DOC_VIEW_EVENT = "com.infrawaretech.docview.ACTION_VIEW_EVENT";
		public static final String EXTRA_EVENT = "doc_convert_event";
		public static final String EXTRA_PAGE = "doc_page";
	}
	
	public static class DocConvertState {
		public static final boolean CONVERT_FINISH = true;
		public static final boolean CONVERT_ING = false;
	}
	
	public static class DocViewStatus {
		public static final int DECODE_FAILED= -2;
		public static final int VPN_DISCONNECTION= -1;
		public static final int REQUEST_FAILED= 0;
		public static final int REQUEST_BEGIN = 1;
		public static final int DOWNLOAD_COMPLETE = 2;
		public static final int REQUEST_COMPLETE = 3;
	}
	
	public static class DocViewEvent {
		public static final int DECODE_FAILED = 0;
		public static final int BEGIN = 1;
		public static final int SHOW = 2;
		public static final int REMOVE = 3;
	}
	
	public static final int STATUS_CONVERTTING = 0;
	public static final int STATUS_CONVERTED = 1;
	public static final int STATUS_ERRNO_FINISH_DOC_VIEWER = 100;
}
