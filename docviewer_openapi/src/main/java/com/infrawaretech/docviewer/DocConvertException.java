package com.infrawaretech.docviewer;

/**
 * 문서 변환 요청 예외처리 관련 클래스
 */
public class DocConvertException extends Exception {

    public static final int CODE_NOT_INIT = 101;
    public static final int CODE_NOT_SET = 102;
    public static final int CODE_INVALID_PARAM = 103;

    static final String MSG_NOT_INIT = "모듈이 초기화 되지 않았습니다.";
    static final String MSG_NOT_SET= "문서 URL이 입력되지 않았거나 정상적으로 초기화 되지 않았습니다.";
    static final String MSG_INVALID_PARAM = "잘못된 아큐먼트 값이 입력되었습니다.";

    private int exceptionCode;

    DocConvertException(int code, String msg){
        super(msg);
        exceptionCode = code;
    }

    public int getErrorCode() {
        return exceptionCode;
    }
}
