package kr.go.mobile.mobp.iff.http.data;

import java.io.IOException;

import kr.go.mobile.iff.util.LogUtil;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

import android.util.Log;

public class HttpResponseDataParser extends HttpDataParser {
	private static final String TAG = HttpResponseDataParser.class.getSimpleName(); 
	private ObjectMapper objectMapper;
	
	private JsonNode jsonNode;
	
	@Override
	public void setOriginalData(String data) {
		objectMapper = new ObjectMapper();
		try {
			jsonNode = objectMapper.readTree(data);
		} catch (JsonProcessingException e) {
			LogUtil.e(TAG, Log.getStackTraceString(e));
		} catch (Exception e) {
			LogUtil.e(TAG, Log.getStackTraceString(e));
		}

	}
	
	@Override
	public boolean hasValue(String fieldName) {
		if(jsonNode == null){
			return false;
		}
		
		if(fieldName == null || fieldName.length() <= 0){
			return false;
		}
		
		return hasValue(jsonNode, fieldName);
	}

	@Override
	public String getStringValue(String fieldName) {
		if(jsonNode == null){
			Log.d("kkk", "findString 2");
			return "";
		}
		
		if(fieldName == null || fieldName.length() <= 0){
			Log.d("kkk", "findString 3");
			return "";
		}

		return findString(jsonNode.toString(), fieldName);
	}

	@Override
	public int getIntValue(String fieldName) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean hasValue(JsonNode parent, final String fieldName) {
		boolean bHasValue = false;

		if (parent.has(fieldName) && parent.path(fieldName).toString().length() > 0) {			
			return true;
		}

		for (JsonNode child : parent) {
			bHasValue = hasValue(child, fieldName);
			if (bHasValue)
				break;

		}
		return bHasValue;
	}
	
	public String findString(String jsonStr, String fieldName) {
		String str = "";

		JsonFactory f = new MappingJsonFactory();
		try {
			JsonParser jp = f.createJsonParser(jsonStr);
			JsonToken current;

			current = jp.nextToken();
			if (current != JsonToken.START_OBJECT) {
				Log.d("kkk", "findString 1");
				return str;
			}
			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String currentName = jp.getCurrentName();							
				if (fieldName.equals(currentName)) {
					str = jp.nextTextValue();					
					break;
				}
			}


		} catch (JsonParseException e) {
			LogUtil.e(TAG, Log.getStackTraceString(e));
		} catch (IOException e) {
			LogUtil.e(TAG, Log.getStackTraceString(e));
		}

		return str;
	}


}
