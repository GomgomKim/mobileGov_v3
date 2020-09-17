package com.infrawaretech.docviewer;

/**
 * 문서 변환 상태 클래스
 */
public class DocConvertStatus {

    /**
     * 문서 요청 실패 상태
     */
    public static final int STATUS_REQ_FAILED = -1;

    /**
     * 문서 Bitmap 변환 실패
     */
    public static final int STATUS_DECODE_FAILED = -2;
    /**
     * 문서 변환 상태 요청
     */
    public static final int STATUS_CONVERT = 1;

    /**
     * 문서 변환 요청 성공 상태
     */
    public static final int STATUS_REQ_COMPLETE = 2;

    private int totalPage;
    private int convertedPage;
    private String hashCode;
    private boolean isConverted;
    private int status;
    private String message;

    DocConvertStatus(int status, int totalPage, int convertedPage, String hashCode, boolean isConverted, String message) {
        this.status = status;
        this.totalPage = totalPage;
        this.convertedPage = convertedPage;
        this.hashCode = hashCode;
        this.isConverted = isConverted;
        this.message = message;
    }

    /**
     * 상태값
     * @return 상태값
     */
    public int getStatus() {
        return status;
    }

    /**
     * 총 페이지 수
     * @return 문서 총 페이지수
     */
    public int getTotalPage() {
        return totalPage;
    }

    /**
     * 변환된 페이지 번호
     * @return 변환된 페이지 번호
     */
    public int getConvertedPage() {
        return convertedPage;
    }

    /**
     * 문서 고유 Hash 값
     * @return 문서 고유 Hash값
     */
    public String getHashCode() {
        return hashCode;
    }

    /**
     * 문서 변환 여부
     * @return 문서 변환 여부
     */
    public boolean isConverted() {
        return isConverted;
    }

    /**
     * 에러 발생시 상태 메시지
     * @return 상태 메시지
     */
    public String getErrMsg() {
        return message;
    }
}
