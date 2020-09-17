package kr.go.mobile.mobp.iff.mobp.rpc.client.android;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import kr.go.mobile.mobp.iff.mobp.rpc.util.RPCEnumUtils;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.fasterxml.jackson.xml.XmlMapper;

public class XmlPayloadBuilder extends AbstractPayloadBuilder {
	
	public XmlPayloadBuilder() {
		
	}

	public XmlPayloadBuilder(RPCEnumUtils.DataType dataType) {
		super(dataType);
	}

	@Override
	public String build() {
		ObjectMapper mapper = new XmlMapper();
		mapper.configure(Feature.UNWRAP_ROOT_VALUE, false);
		mapper.configure(Feature.WRAP_EXCEPTIONS, false);
		arrangeField();
		String sPayload = null;
		try {
			sPayload = mapper.writeValueAsString(mapperWrap);
			sPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + StringUtils.substringBetween(sPayload, "<HashMap xmlns=\"\">" ,"</HashMap>");
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
		ObjectMapper mapper = new XmlMapper();
		mapper.configure(Feature.WRAP_EXCEPTIONS, false);
		try {
			source = "<HashMap xmlns=\"\">"+StringUtils.remove(source,"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>")+"</HashMap>";
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
