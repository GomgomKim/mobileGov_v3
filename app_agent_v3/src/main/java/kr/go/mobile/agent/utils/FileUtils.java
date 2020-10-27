package kr.go.mobile.agent.utils;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {

    final static String TAG = FileUtils.class.getSimpleName();

    // TODO multipart map 구현 필요
    /*
    private Map<String, RequestBody> setMultiPartEntry(HttpRequest request){
        Map<String, RequestBody> partMap = new HashMap<>();
        partMap.put("file", fileBody);

        MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
        multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        multipartEntity.setCharset(Charset.forName("UTF-8"));


        FileBody fileBody = new FileBody(new File(request.getAttachFilePath()));
        multipartEntity.addPart("file", fileBody);
        try {

            if(request.getBodyList() != null && request.getBodyList().size() > 0){
                for(int i=0 ; i<request.getBodyList().size() ; i++){

                    String data = request.getBodyList().get(i);

                    Log.d(TAG,  "setMultiPartEntry data :: " + data);
                    String[] splitData = data.trim().split("=");

                    Log.d(TAG,  "setMultiPartEntry data 0 :: " + splitData[0]);
                    if(splitData.length == 2){
                        Log.d(TAG,  "setMultiPartEntry data 1 :: " + splitData[1]);
                        partMap.put(splitData[0], splitData[1]);
                    }

                    if(splitData.length == 1){
                        partMap.put(splitData[0], "");
                    }

                }
            }

        } catch (NullPointerException e) {
            Log.e(TAG, "", e);
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }

        return multipartEntity.build();
    }
    */

    public static String makeFileRespData(final String fileName, final String ext, final String size, final long startTime, final long endTime) {
        long estimateTime = endTime - startTime;
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.KOREA);
        String start = timeFormat.format(new Date(startTime));
        String end = timeFormat.format(new Date(endTime));

        Log.d(TAG, "fileName :: " + fileName);
        Log.d(TAG, "ext :: " + ext);
        Log.d(TAG, "size :: " + size);
        Log.d(TAG, "startTime :: " + start);
        Log.d(TAG, "endTime :: " + end);
        Log.d(TAG, "estimateTime :: " + estimateTime);

        String data = "";
//        AbstractPayloadBuilder requestPayload = new JsonPayloadBuilder(RPCEnumUtils.DataType.REQUEST);
//        List<Object> lo = new ArrayList<Object>();
//        Map map = new HashMap();
//        map.put("fileName", fileName);
//        map.put("ext", ext);
//        map.put("size", size);
//        map.put("startTime", start);
//        map.put("endTime", end);
//        map.put("estimateTime", estimateTime + "");
//        lo.add(map);
//        requestPayload.setParams(lo);
//        data = requestPayload.build();

        return data;
    }

    /*
    파일 확장자
     */
    public static String getExtension(String filePath){
        String fileExtension = filePath.substring(filePath.lastIndexOf(".")+1);
        return TextUtils.isEmpty(fileExtension) ? null : fileExtension;
    }

    /*
    파일 이름
     */
    public static String getFileName(String filePath){
        String fileName = null;
        fileName = filePath.substring(filePath.lastIndexOf("/"),filePath.lastIndexOf("."));
        return fileName;
    }
}
