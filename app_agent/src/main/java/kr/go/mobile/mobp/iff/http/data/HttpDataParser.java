package kr.go.mobile.mobp.iff.http.data;

public abstract class HttpDataParser {

	public abstract void setOriginalData(final String data);
	
	public abstract String getStringValue(final String fieldName);
	
	public abstract int getIntValue(final String fieldName);
	
	public abstract boolean hasValue(final String fieldName);
	
}
