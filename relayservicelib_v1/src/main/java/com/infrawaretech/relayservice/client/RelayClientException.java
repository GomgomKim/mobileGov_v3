package com.infrawaretech.relayservice.client;

public class RelayClientException extends Exception {

	/**
	 * 알수없는 에러
	 */
	public static final int ERRNO_UNKNOWN = 1;
	/**
	 * 서버와 연결할 수 없음
	 */
	public static final int ERRNO_NOT_CONNECTION = 2;
	/**
	 * output 에러
	 */
	public static final int ERRNO_CAN_NOT_WRTIE = 3;
	/**
	 * input 에러
	 */
	public static final int ERRNO_CAN_NOT_READ = 4;
	/**
	 * 사용자에 의한 정지
	 */
	public static final int ERRNO_INTERRUPTED = 5;
	/**
	 * 인코딩 미지원
	 */
	public static final int ERRNO_UNSUPPORTED_ENCODING = 6;
	
	private static final long serialVersionUID = 1L;
	private int code = -1;
	
	public RelayClientException(int code, Throwable t) {
		super(t);
		this.code = code;
	}
	
	public RelayClientException(String message, Throwable t) {
		super(message, t);
	}
	
	public String getMessage() {
		if (code > 0) {
			switch (code) {
			case ERRNO_NOT_CONNECTION:
				return "서버와 연결을 할 수 없습니다.";
			case ERRNO_CAN_NOT_READ:
				return "데이터를 읽을 수 없습니다.";
			case ERRNO_CAN_NOT_WRTIE:
				return "서버로 요청할 수 없습니다.";
			case ERRNO_INTERRUPTED:
				return "사용자에 의하여 중지되었습니다";
			case ERRNO_UNSUPPORTED_ENCODING:
				return "지원하지 않는 인코딩 방식입니다.";
			default:
				return "데이터 처리 중 에러가 발생하였습니다. 다시 시도하주시기 바랍니다. (-1)";
			}
		} else {
			return super.getMessage();
		}
	}
	
}
