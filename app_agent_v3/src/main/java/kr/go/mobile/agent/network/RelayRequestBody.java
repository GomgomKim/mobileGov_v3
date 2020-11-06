package kr.go.mobile.agent.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class RelayRequestBody {

    static Calendar CALENDAR = Calendar.getInstance();
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSSSSS", Locale.KOREA);

    static class TransactionIdGenerator {
        private static TransactionIdGenerator generator;

        private long currTimeMillis;
        private long currSeq;
        private TransactionIdGenerator() {
            currTimeMillis = System.currentTimeMillis();
            currSeq = 0;
        }

        public static TransactionIdGenerator getInstance() {
            if (generator == null) {
                generator = new TransactionIdGenerator();
            }

            synchronized(generator){
                return generator;
            }
        }

        public String nextKey() {
            synchronized (generator) {
                long tempTimeMillis = System.nanoTime();
                if (currTimeMillis == tempTimeMillis) {
                    currSeq++;
                } else {
                    currTimeMillis = tempTimeMillis;
                    currSeq = 1;
                }
            }
            return makeTransactionKey(currTimeMillis, currSeq);
        }

        private String makeTransactionKey(long millis, long seq) {
            String sSeq = "00000" + Long.toString(seq);
            sSeq = millis + sSeq;
            if (sSeq.length() > 12) {
                sSeq = sSeq.substring(sSeq.length() - 12, sSeq.length());
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA);

            return sdf.format(Calendar.getInstance().getTime()) + "-" + sSeq;
        }
    }

    private static String buildMethodCall(JSONObject o) throws JSONException {
        // STEP 3. 요청 파라미터 목록 생성.
        JSONArray reqParamArray = new JSONArray();
        reqParamArray.put(o);

        // STEP 4. 요청 파라미터 목록 값 등록
        JSONObject reqParams = new JSONObject();
        reqParams.put("id", TransactionIdGenerator.getInstance().nextKey());
        reqParams.put("params", reqParamArray);

        JSONObject reqMethodCall = new JSONObject();
        reqMethodCall.put("methodCall", reqParams);
        // 중계 서버 요청 포맷
        // {
        //   "methodCall": {
        //      "id":"20201028154607-124327000001",
        //      "params":[
        //           __데이터__
        //      ]
        //   }
        // }
        return reqMethodCall.toString();
    }

    public static class AuthBuilder {
        String signed;

        AuthBuilder setSigned(String base64) {
            this.signed = base64;
            return this;
        }

        RequestBody build() throws JSONException, UnsupportedEncodingException {
            JSONObject reqValues = new JSONObject();
            reqValues.put("transactionId", DATE_FORMAT.format(CALENDAR.getTime()));
            reqValues.put("reqType", "1");
            reqValues.put("signedData", signed);

            JSONObject reqParam = new JSONObject();
            reqParam.put("reqAusData", reqValues.toString());

            String encAuthBody = URLEncoder.encode(buildMethodCall(reqParam), "UTF-8");

            byte[] byte_body = encAuthBody.getBytes(StandardCharsets.UTF_8);
            return RequestBody.create(byte_body, MediaType.parse("application/octet-stream"));
        }
    }

    public static class DefaultBuilder {
        String params;
        DefaultBuilder setServiceParams(String serviceParams) {
            this.params = serviceParams;
            return this;
        }

        RequestBody build() {
            byte[] byte_body = params.getBytes(StandardCharsets.UTF_8);
            return RequestBody.create(byte_body, MediaType.parse("application/octet-stream"));
        }
    }

    public static class MultiPartBuilder {
        MultipartBody.Builder builder;
        boolean existUrl;
        String boundaryId;
        MultiPartBuilder(String boundaryId) {
            this.existUrl = false;
            this.boundaryId = boundaryId;
            builder = new MultipartBody.Builder(boundaryId)
                    .setType(MultipartBody.FORM);
        }

        MultiPartBuilder setUploadURL(String relayUrl) {
            if (existUrl) return this;

            existUrl = true;
            return addParam("url", relayUrl);
        }

        MultiPartBuilder addFile(String fileName, byte[] uploadBytes) {
            RequestBody rqFile = RequestBody.create(uploadBytes, MediaType.parse("application/octet-stream"));
            builder.addFormDataPart("file", fileName, rqFile);
            return this;
        }

        MultiPartBuilder addParam(String key, String value) {
            builder.addFormDataPart(key, value);
            return this;
        }

        MultiPartBuilder addParam(String value) {
            builder.addFormDataPart(value, "");
            return this;
        }

        MultipartBody build() {
            return builder.build();
        }

        String getContentType() {
            return "multipart/form-data; charset=UTF-8; boundary=" + this.boundaryId;
        }
    }

    public static class DownloadBuilder extends DefaultBuilder {
        boolean existUrl = false;
        String relayUrl = "url=";

        DownloadBuilder setDownloadURL(String relayUrl) {
            if (existUrl) return this;
            existUrl = true;
            this.relayUrl += relayUrl;
            return this;
        }

        RequestBody build() {
            if (params != null && !params.isEmpty()) {
                params = String.format("%s&%s", relayUrl, params);
            } else {
                params = relayUrl;
            }
            return super.build();
        }
    }

    public static class ReportBuilder {

        private String fileName;
        private String fileExt;
        private int fileSize;
        private long beginTime;
        private long endTime;

        ReportBuilder setFileName(String fileName) {
            this.fileName = fileName;
            this.fileExt = fileName.substring(fileName.lastIndexOf(".") + 1);
            return this;
        }

        ReportBuilder setFileSize(int fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        ReportBuilder setBeginTime(long beginTime) {
            this.beginTime = beginTime;
            return this;
        }

        ReportBuilder setEndTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public RequestBody build() throws JSONException {
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.KOREA);
            String beginDate = timeFormat.format(new Date(beginTime));
            String endDate = timeFormat.format(new Date(endTime));
            long estimateTime = endTime - beginTime;

            JSONObject reqValues = new JSONObject();
            reqValues.put("fileName", fileName);
            reqValues.put("ext", fileExt);
            reqValues.put("size", fileSize);
            reqValues.put("estimateTime", estimateTime);
            reqValues.put("startTime", beginDate);
            reqValues.put("endTime", endDate);

            String uploadReportBody = buildMethodCall(reqValues);
            byte[] byte_body = uploadReportBody.getBytes(StandardCharsets.UTF_8);
            /*
            {"methodCall": {
                    "id":"20201028180659-784488000001",
                    "params":[{"":"jpg","":"tmp.jpg","":21706,"":839,"":"20201028180658310","":"20201028180659149"}]
                }
            }
            */
            return RequestBody.create(byte_body, MediaType.parse("application/octet-stream"));
        }
    }
}
