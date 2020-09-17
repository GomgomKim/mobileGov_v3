package kr.go.mobile.mobp.iff.mobp.rpc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RPCEnumUtils {

	/**
	 * JSON-RPC envelope type.
	 * 
	 * @author Daniel Stefaniuk
	 */
	public static enum Version {
	
	    UNDEFINED(""),
	    MOBP_RPC_1_0("MOBP-RPC-1.0");
	
	    private final String name;
	
	    Version(String name) {
	
	        this.name = name;
	    }
	
	    @Override
	    public String toString() {
	
	        return this.name;
	    }
	}

	/**
	 * JSON-RPC response content type.
	 * 
	 * @author Daniel Stefaniuk
	 */
	public static enum ContentType {
	
		UNDEFINED(""),
	    APPLICATION_JSON("application/json; charset=UTF-8"),
	    APPLICATION_XML("application/xml; charset=UTF-8");
	
	    private final String name;
	
	    ContentType(String name) {
	
	        this.name = name;
	    }
	
	    @Override
	    public String toString() {
	
	        return this.name;
	    }
	}

	/**
	 * RPC supported data types.
	 * 
	 * @author Daniel Stefaniuk
	 */
	public static enum ValueType {
	
	    ARRAY(new Class<?>[] { ArrayList.class }),
	    LIST(new Class<?>[] { List.class }),
	    MAP(new Class<?>[] { Map.class }),
	    HASHMAP(new Class<?>[] { HashMap.class }),
	    OBJECT(new Class<?>[] { LinkedHashMap.class }),
	    NUMBER(new Class<?>[] { Float.class, Double.class }),
	    INTEGER(new Class<?>[] { Integer.class, Long.class }),
	    BOOLEAN(new Class<?>[] { Boolean.class }),
	    STRING(new Class<?>[] { String.class });
	
	    private final Class<?>[] classes;
	
	    private ValueType(Class<?>[] classes) {
	
	        this.classes = classes;
	    }
	
	    public static String getName(Class<?> clazz) {
	
	        // check all data types
	        for(ValueType type: ValueType.values()) {
	            for(Class<?> c: type.classes) {
	                if(clazz.equals(c)) {
	                    return type.toString();
	                }
	            }
	        }
	
	        // non of the above, so this must be POJO object
	        return ValueType.OBJECT.toString();
	    }
	
	    @Override
	    public String toString() {
	
	        return name().toLowerCase();
	    }
	}

	/**
	 * JSON-RPC transport type.
	 * 
	 * @author Daniel Stefaniuk
	 */
	public static enum Transport {
	
	    UNDEFINED(""),
	    POST("POST");
	
	    private final String name;
	
	    Transport(String name) {
	
	        this.name = name;
	    }
	
	    @Override
	    public String toString() {
	
	        return this.name;
	    }
	}

	public static enum Result {
	
	    TRUE("1"),
	    FALSE("0");
	
	    private final String name;
	
	    Result(String name) {
	
	        this.name = name;
	    }
	
	    @Override
	    public String toString() {
	
	        return this.name;
	    }
	}
	
	public static enum HeaderE2E {
	    USE("1"),
	    NOT_USE("0");
	
	    private final String name;
	
	    HeaderE2E(String name) {
	        this.name = name;
	    }
	    
	    @Override
	    public String toString() {
	        return this.name;
	    }
	}
	
	public static enum DataType {
		REQUEST, RESPONSE
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}

