package kr.go.mobile.common.v3;

public enum GPKI_KEY {

    /**
     * 사용자 이름을 요청한다.
     */
    _NICKNAME("gov:nickname"),
    /**
     * 사용자 DN 값을 요청한다.
     */
    _DN("gov:dn"),
    /**
     * 사용자 ID 정보를 요청한다.
     */
    _CN("gov:cn"),
    /**
     * 사용자 부서 정보를 요청한다.
     */
    _OU("gov:ou"),
    /**
     * 사용자 부서 코드 값을 요청한다.
     */
    _OU_CODE("gov:oucode"),
    /**
     * 사용자 부서 (?) 정보를 요청한다.
     */
    _DEPARTMENT("gov:department"),
    /**
     * 사용자 부서 (?) 코드 값을 요청한다.
     */
    _DEPARTMENT_NUMBER("gov:departmentnumber");

    public static GPKI_KEY getInstance(String key) {
        for (GPKI_KEY gpkiKey : GPKI_KEY.values()) {
            if (gpkiKey.equals(key))
                return gpkiKey;
        }
        return null;
    }

    String key;
    GPKI_KEY(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public boolean equals(String strKey) {
        return this.key.equals(strKey);
    }

}
