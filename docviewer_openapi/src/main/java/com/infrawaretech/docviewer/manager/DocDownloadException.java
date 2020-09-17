package com.infrawaretech.docviewer.manager;

class DocDownloadException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public static final int ERROR_MALFORMED_URL = 1;
	public static final int ERROR_DATA_PROC = 2;
	public static final int ERROR_NOT_READY_DOC_CONVERT = 3;
	public static final int ERROR_DOC_CONVERT_SERVER_FAILED= 4;
	public static final int ERROR_NO_DATA = 5;
	public static final int ERROR_MALFORMED_CONTENT_TYPE = 6;
	
	private int code = -1;
	
	public DocDownloadException(String errorMessage) {
		super(errorMessage);
	}
	
	public DocDownloadException(String errorMessage, Throwable t) {
		super(errorMessage, t);
	}
	
	public DocDownloadException(int code) {
		this.code = code;
	}
	
	public DocDownloadException(int code, Throwable t) {
		super(t);
		this.code = code;
	}
	
	public String getMessage() {
		if (this.code > 0) {
			switch (this.code) {
			case ERROR_DOC_CONVERT_SERVER_FAILED:
				return "문서변환서버와 통신을 할 수 없습니다.. 잠시 후 다시 시도해주시기 바랍니다.";
			case ERROR_NOT_READY_DOC_CONVERT:
				return "서버의 문서변환 작업이 준비되지 않았습니다. 잠시 후 다시 시도해주시기 바랍니다.";
			case ERROR_MALFORMED_URL:
				return "URL 데이터가 잘못되었습니다.";
			case ERROR_NO_DATA:
				return "문서 데이터가 존재하지 않습니다.";
			case ERROR_DATA_PROC:
				return "데이터 처리 중 에러가 발생하였습니다.";
			case ERROR_MALFORMED_CONTENT_TYPE:
				return "변환된 문서 타입이 일치하지 않습니다.";
			default:
				return "문서 다운로드 중 에러가 발생하였습니다. 다시 시도하주시기 바랍니다. (-1)";
			}
		} else {
			return super.getMessage();
		}
	}
}
