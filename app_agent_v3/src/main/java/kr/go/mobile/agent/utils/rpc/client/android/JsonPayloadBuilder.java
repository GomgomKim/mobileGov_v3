package kr.go.mobile.agent.utils.rpc.client.android;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;


import kr.go.mobile.agent.utils.rpc.util.RPCEnumUtils;

public class JsonPayloadBuilder extends AbstractPayloadBuilder {
	
	public JsonPayloadBuilder() {
		
	}

	public JsonPayloadBuilder(RPCEnumUtils.DataType dataType) {
		super(dataType);
	}

	@Override
	public String build() {
		ObjectMapper mapper = new ObjectMapper();
		
		arrangeField();
		
		String sPayload = null;
		
		try {
			sPayload = mapper.writeValueAsString(mapperWrap);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return sPayload;
	}


	@Override
	public void fromBuild(String source) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Map<String, Object> mWrapper = mapper.readValue(source, HashMap.class);
			setMapperWrap(mWrapper);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}