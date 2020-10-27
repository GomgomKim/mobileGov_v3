package kr.go.mobile.common.v3.hybrid;

/**
 * Created by ChangBum Hong on 2020-07-23.
 * cloverm@infrawareglobal.com
 * 예외 처리 코드 및 설명
 */
public class CBHybridException extends Exception {
/*
    @Deprecated
    public enum Type {
        UNKNOWN(999, "알수없는 에러"),
        CALL_PARAMETER(601, "호출 파라미터 에러"),
        PLUGIN_NOT_FOUND(602, "미등록 플러그인 에러"),
        NEW_INSTANCE(603, "호출 객체 생성 에러"),
        METHOD_CALL(604, "함수 호출 에러"),
        JSON_PARSING(605, "JSON 파싱 에러"),
        INVALID_PARAMETER(606, "유효하지 않은 파라미터 에러"),
        CALL_PHONE_PERMISSION(607, "전화 걸기 퍼미션 에러"),
        SEND_SMS_PERMISSION(608, "문자 보내기 퍼미션 에러"),
        LOCATION_PERMISSION(609, "위치 정보 퍼미션 에러"),
        AIRPLANE_MODE(610, "비행기 모드 에러"),
        NO_SIM(611, "NO SIM 에러"),

        //필요한 건지 검토 필요
        SMS_GENERIC_FAILURE(701, "SMS GENERIC FAILURE"),
        SMS_NO_SERVICE(702, "SMS NO SERVICE"),
        SMS_NULL_PDU(703, "SMS NULL PDU"),
        SMS_RADIO_OFF(704, "SMS RADIO OFF"),

        LOCATION_NO_DATA(801, "위치정보 없음"),
        LOCATION_TIMEOUT(802, "위치정보 수신 타임아웃");


        final int code;
        final String description;

        Type(int code, String des) {
            this.code = code;
            this.description = des;
        }

        public int getCode() {
            return this.code;
        }

        public String getDes() {
            return this.description;
        }
    }

 */

    private int code;

    public CBHybridException(int code, String des) {
        super(des);

        this.code = code;
    }


    public int getCode() {
        return this.code;
    }

}
