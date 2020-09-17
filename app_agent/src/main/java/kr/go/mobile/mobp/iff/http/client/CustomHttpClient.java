package kr.go.mobile.mobp.iff.http.client;

import kr.go.mobile.mobp.iff.http.HttpRequest;
import kr.go.mobile.mobp.iff.http.HttpResponse;
import android.content.Context;


public abstract class CustomHttpClient {

	abstract protected Object createHttpClient(final Context c, final int timeout);
	
	abstract public void close();
	
	abstract public HttpResponse excute(HttpRequest request) throws Exception;
	
	abstract public HttpResponse excuteUpload(HttpRequest request) throws Exception;

	public final Object createConnection(final Context c, final int timeout) throws Exception{
		return createHttpClient(c, timeout);

	}

}
