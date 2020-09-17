package com.infrawaretech.docviewer;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;

class DocInfoFactory {

    public static class Option {

        protected String InServiceHeader;
        protected String InServiceId;
        protected String InTargetUrl;
        protected String InFileName;
        protected String InFileExt;
        protected String InCreated;
        protected String mHashCode;

        public String getServiceHeader() {
            return this.InServiceHeader;
        }

        public String genParamenter(int page) {
            return genParamenter(page, false);
        }

        public String genParamenter(int page, boolean justDocInfo) {

            if (TextUtils.isEmpty(mHashCode)) {
                mHashCode = "";
            }

            JSONObject json = new JSONObject();
            try {
                json.put("url", InTargetUrl);
                // 중계서버 개선 작업으로 인하여 문서뷰어에 대한 서비스 ID 변경 -- BEGIN
                // 2018-01-02 servicemethod -> sCode 변경 요청
                // json.put("sCode", InServiceId);
                // 중계서버 개선 작업으로 인하여 문서뷰어에 대한 서비스 ID 변경 -- END
                json.put("fileName", InFileName);
                json.put("page", page);
                json.put("requesthashcode", mHashCode);
                if (InCreated != null && !InCreated.equals("")) {
                    json.put("ext", InFileExt + "@" + InCreated);
                } else {
                    json.put("ext", InFileExt);
                }
                if (justDocInfo) {
                    json.put("action", "empty"); // 변환 상태만 요청.
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json.toString();
        }

        public void saveHashCode(String hashCode) {
            mHashCode = hashCode;
        }

        public void valid() throws FileNotFoundException {
            if (InServiceHeader == null || InServiceHeader.isEmpty()
                    || InTargetUrl == null || InTargetUrl.isEmpty()
                    || InFileExt == null || InFileExt.isEmpty()
                    || InFileName == null || InFileName.isEmpty()) {
                throw new FileNotFoundException();
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(Option.class.getSimpleName());
            sb.append(" [");
            sb.append("InServiceID=").append(InServiceId);
            sb.append("InTargetUrl=").append(InTargetUrl);
            sb.append("InFileName=").append(InFileName);
            sb.append("InFileExt=").append(InFileExt);
            sb.append("InCreated=").append(InCreated);
            sb.append("mHashCode=").append(mHashCode);
            sb.append(" ]");
            return sb.toString();
        }
    }
}
