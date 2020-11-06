package kr.go.mobile.common.v3;

import android.net.Uri;

import java.util.Objects;

import kr.go.mobile.common.v3.broker.Response;

public class RestrictedAPI {

    static String key;

    public static void setKey(String key) {
        RestrictedAPI.key = key;
    }

    public static Response executeUpload(String fileName, String absolutePath, String relayUrl, String extraParams) throws CommonBasedAPI.CommonBaseAPIException {
        return executeUpload(fileName, Uri.parse(absolutePath), relayUrl, extraParams);
    }

    public static Response executeUpload(String fileName, Uri targetUri, String relayUrl, String extraParams) throws CommonBasedAPI.CommonBaseAPIException {
        if (Objects.equals(RestrictedAPI.key, null)) {
            throw new CommonBasedAPI.CommonBaseAPIException("제한된 기능으로 사용할 수 없습니다.");
        }
        return CommonBasedAPI.executeUpload(fileName, targetUri, relayUrl, extraParams);
    }

    public static Response executeDownload(String relayUrl, String absolutePath) throws CommonBasedAPI.CommonBaseAPIException {
        if (Objects.equals(RestrictedAPI.key, null)) {
            throw new CommonBasedAPI.CommonBaseAPIException("제한된 기능으로 사용할 수 없습니다.");
        }
        return CommonBasedAPI.executeDownload(relayUrl, absolutePath);
    }

}
