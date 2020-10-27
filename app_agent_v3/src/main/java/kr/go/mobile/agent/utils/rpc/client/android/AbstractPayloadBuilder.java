package kr.go.mobile.agent.utils.rpc.client.android;
/*
 * Copyright 2009, Mahmood Ali.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of Mahmood Ali. nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import kr.go.mobile.agent.utils.rpc.util.RPCEnumUtils;
import kr.go.mobile.agent.utils.rpc.util.TransactionIdGenerator;


public abstract class AbstractPayloadBuilder {
	
	/* 헤더 필드 정의 */
	public static final String HEADER_E2E = "X-E2e";
	public static final String HEADER_MIFS_ID = "X-Mifs-Id";
	public static final String HEADER_USER_AGENT = "X-User-Agent";
	
    protected Map<String, Object> mapperWrap;
    protected Map<String, Object> root;
    
	protected Object data;
    protected List<Object> params;
    
    protected RPCEnumUtils.DataType builderDataType;

	private String id;
	private String userId;
    private String auth;
    private String service;
    private String methodName;
    private String result;
    private String msg;
    private String code;
    
    private Map<String, Object> methodCall;
    private Map<String, Object> methodResponse;
    
    private String serviceClass;
    
    protected void setMapperWrap(Map<String, Object> mapperWrap) {
		this.mapperWrap = mapperWrap;
		
		extractField(this.mapperWrap);
	}

	protected Map<String, Object> getRoot() {
		return root;
	}

	public Object getData() {
		return data;
	}

	public List<Object> getParams() {
		return params;
	}

	public String getId() {
		return id;
	}
	
	public String getUserId() {
		return userId;
	}

	public String getAuth() {
		return auth;
	}

	public String getService() {
		return service;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getResult() {
		return result;
	}

	public String getMsg() {
		return msg;
	}

	public String getCode() {
		return code;
	}
	
	public String getServiceClass() {
		if(serviceClass == null) {
	    	if(this.methodName != null && this.methodName.indexOf(".") > 0) {
	    		this.serviceClass = this.methodName.substring(0, this.methodName.lastIndexOf("."));
	    	}
		}
		return serviceClass;
	}

	protected void setServiceClass(String serviceClass) {
		this.serviceClass = serviceClass;
	}

	protected void setMethodCall(Map<String, Object> methodCall) {
		this.methodCall = methodCall;
	}

	protected void setMethodResponse(Map<String, Object> methodResponse) {
		this.methodResponse = methodResponse;
	}

	protected RPCEnumUtils.DataType getBuilderDataType() {
		return builderDataType;
	}

	public void setBuilderDataType(RPCEnumUtils.DataType builderDataType) {
		this.builderDataType = builderDataType;
	}
	
	AbstractPayloadBuilder () {
		this.mapperWrap = new HashMap<String, Object>();
        this.root = new HashMap<String, Object>();
        this.data = new HashMap<String, Object>();
        this.params = new ArrayList<Object>();
        
        this.methodCall = new HashMap<String, Object>();
        this.methodResponse = new HashMap<String, Object>();
	}

	AbstractPayloadBuilder(RPCEnumUtils.DataType dataType) {
    	this.builderDataType = dataType;
    	this.mapperWrap = new HashMap<String, Object>();
        this.root = new HashMap<String, Object>();
        this.data = new HashMap<String, Object>();
        this.params = new ArrayList<Object>();
        
        this.methodCall = new HashMap<String, Object>();
        this.methodResponse = new HashMap<String, Object>();
    }
    
    public AbstractPayloadBuilder setId(String id) {
    	this.id = id;
    	root.put("id", id);
    	return this;
    }
    
    public AbstractPayloadBuilder setUserId(String userId) {
    	this.userId = userId;
    	root.put("userId", userId);
    	return this;
    }
    
    public AbstractPayloadBuilder setAuth(String auth) {
    	this.auth = auth;
    	root.put("auth", auth);
    	return this;
    }
    
    public AbstractPayloadBuilder setService(String service) {
    	this.service = service;
    	root.put("service", service);
    	return this;
    }
    
    public AbstractPayloadBuilder setMethodName(String methodName) {
    	this.methodName = methodName;
    	root.put("methodName", methodName);
    	return this;
    }
    
    public AbstractPayloadBuilder setResult(String result) {
    	this.result = result;
    	root.put("result", result);
    	return this;
    }
    
    public AbstractPayloadBuilder setMsg(String msg) {
    	this.msg = msg;
    	root.put("msg", msg);
    	return this;
    }
    
    public AbstractPayloadBuilder setCode(String code) {
    	this.code = code;
    	root.put("code", code);
    	return this;
    }

    public AbstractPayloadBuilder setData(Object data) {
    	this.data = data;
        root.put("data", data);
        return this;
    }
    
    public AbstractPayloadBuilder setParams(List<Object> params) {
    	this.params = params;
        root.put("params", params);
        return this;
    }
    
    protected AbstractPayloadBuilder removeUserId() {
    	root.remove("userId");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeResult() {
    	root.remove("result");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeCode() {
    	root.remove("code");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeMsg() {
    	root.remove("msg");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeData() {
    	root.remove("data");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeAuth() {
    	root.remove("auth");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeService() {
    	root.remove("service");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeMethodName() {
    	root.remove("methodName");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeParams() {
    	root.remove("params");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeMethodCall() {
    	mapperWrap.remove("methodCall");
    	return this;
    }
    
    protected AbstractPayloadBuilder removeMethodResponse() {
    	mapperWrap.remove("methodResponse");
    	return this;
    }
    
    protected void arrangeField() {
    	
    	if(this.builderDataType == RPCEnumUtils.DataType.REQUEST) {
			removeResult();
			removeCode();
			removeMsg();
			removeData();
			mapperWrap.put("methodCall", root);
			if(StringUtils.isBlank(id)) {
				setId(TransactionIdGenerator.getInstance().nextKey());
			}
			removeMethodResponse();
		}
		else {
			removeUserId();
			removeAuth();
			removeService();
			removeMethodName();
			removeParams();
			mapperWrap.put("methodResponse", root);
			
			if(StringUtils.isBlank(id)) {
				setId(TransactionIdGenerator.getInstance().nextKey());
			}
			removeMethodCall();
			
		}
    }
    
    protected void extractField(Map<String, Object> mMapperWrap) {
    	if(this.builderDataType == RPCEnumUtils.DataType.REQUEST) {
    		if(mMapperWrap.containsKey("methodCall")) {
	    		methodCall = (Map<String, Object>) mMapperWrap.get("methodCall");
				if(methodCall.containsKey("id")) {
					this.setId(methodCall.get("id").toString());
				}
				if(methodCall.containsKey("userId")) {
					this.setUserId(methodCall.get("userId").toString());
				}
				if(methodCall.containsKey("auth")) {
					this.setAuth(methodCall.get("auth").toString());
				}
				if(methodCall.containsKey("service")) {
					this.setService(methodCall.get("service").toString());
				}
				if(methodCall.containsKey("methodName")) {
					this.setMethodName(methodCall.get("methodName").toString());
				}
				if(methodCall.containsKey("params")) {
					if(methodCall.get("params") instanceof ArrayList){
						this.setParams((List<Object>)methodCall.get("params"));
					}
					else {
						
						this.getParams().add(methodCall.get("params"));
					}
				}
    		}
    		mMapperWrap.remove("methodCall");
		}
		else {
			if(mMapperWrap.containsKey("methodResponse")) {
				methodResponse = (Map<String, Object>) mMapperWrap.get("methodResponse");
				if(methodResponse.containsKey("id")) {
					this.setId(methodResponse.get("id").toString());
				}
				if(methodResponse.containsKey("result")) {
					this.setResult(methodResponse.get("result").toString());
				}
				if(methodResponse.containsKey("code")) {
					this.setCode(methodResponse.get("code").toString());
				}
				if(methodResponse.containsKey("msg")) {
					this.setMsg(methodResponse.get("msg").toString());
				}
				if(methodResponse.containsKey("data")) {
					if(methodResponse.get("data") instanceof ArrayList){
						this.setData((List<Object>)methodResponse.get("data"));
					}
					else {
						this.setData(methodResponse.get("data"));
					}
				}
			}
		}
    }
    
    abstract public String build();

    abstract public void fromBuild(String source);

    @Override
    public String toString() {
        return this.build();
    }
    
    public String toRawString() {
    	ObjectMapper mapper = new ObjectMapper();
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

	public static AbstractPayloadBuilder builder(RPCEnumUtils.ContentType contentType, RPCEnumUtils.DataType dataType) {
		if(contentType == RPCEnumUtils.ContentType.APPLICATION_JSON) {
			return new JsonPayloadBuilder(dataType);
		}
		else {
			return new XmlPayloadBuilder(dataType);
		}
	}
}
