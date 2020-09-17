package kr.go.mobile.mobp.iff.http.parser;

import java.util.List;
import java.util.Map;

import kr.go.mobile.iff.util.LogUtil;
import kr.go.mobile.mobp.iff.http.connection.CustomUrlConnection;
import kr.go.mobile.mobp.iff.R;
import android.content.Context;
import android.util.Log;

/**
 * @Class Name : HttpHeader
 * @작성일 : 2015. 10. 12.
 * @작성자 : 조명수
 * @변경이력 :
 * @Class 설명 : urlConnection 으로부터 각 헤더필드에 해당하는 값들을 뽑아 저장... 기존 socket 사용시에는 line
 *        을 읽어 "startsWith" 등 String 클래스 메소드를 사용하여 값을 가져왔었음
 */
public class HttpHeader {

	private Context context;

	private CustomUrlConnection connection;

	// ////////////////////////
	private static final String CRLF = "\r\n";
	private static final String HTTP_VERSION = "HTTP/1.1";
	private static final String METHOD = "POST ";
	private static final String HOST = "Host: ";
	private static final String CONTENT_LENGTH = "Content-Length";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String USER_AGENT = "User-Agent";
	private static final String SERVER = "Server";
	private static final String CONNECTION = "Connection";
	private static final String CHUNKED = "Transfer-Encoding: chunked";
	private static final String MO_PAGECOUNT = "MO_PAGECOUNT";
	private static final String MO_CONVERTING = "MO_CONVERTING";
	
	
	private static final String MO_HASHCODE = "MO_HASHCODE";
	
	////
	private static final String MO_PAGEWIDTH = "MO_PAGEWIDTH";
	private static final String MO_PAGEHEIGHT = "MO_PAGEHEIGHT";
	////
	private static final String CONTENT_DISPOSITION = "Content-Disposition";

	private static final String CONNECTION_CLOSE = "Connection: close\r\n";
	private static final String CHUNKING = CHUNKED + CRLF;
	
	private static final String MO_ERRCODE = "MO_ERRCODE";
	private static final String MO_STATE = "MO_STATE";
	// ////////////////////////

	private String strServer = "";
	private String strHost = "";
	private String strContentType = "";
	private String strConnection = "";
	private String strMoPageCount = "";
	private String strMoConverting = "";
	////
	private String strMoPageWidth = "";
	private String strMoPageHeight = "";
	//
	private String strMoHashCode = "";
	////
	private String strContentDisposition = "";
	private String strChunked = "";	
	
	private String strMoErrCode = "";	
	private String strMoState = "";	

	private int nContentLength = -100;
	private boolean bChunked;
	private LineReader reader;
	private StringBuffer header;
	
	private String strResponseHeader = "";

	public HttpHeader(final Context c, final CustomUrlConnection conn) {
		context = c;
		connection = conn;
	}
	
	public String getResponseHeader(){
		return strResponseHeader;
	}

	public void getHeaderFields() {
		LogUtil.d(getClass(), "HttpHeader getHeaderFields 1");
		Map<String, List<String>> map = connection.getHeaderFields();
		if(map == null){
			return;
		}
		LogUtil.d(getClass(), "HttpHeader getHeaderFields map : " +map.toString());
		strResponseHeader = map.toString();
		
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			String strKey = entry.getKey();
			List<String> listValue = entry.getValue();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < listValue.size(); i++) {
				sb.append(listValue.get(i));
			}
			LogUtil.d(getClass(), "HttpHeader getHeaderFields Key : " + strKey + ", Value : " + sb.toString());

			if (strKey != null) {
				if (strKey.compareToIgnoreCase(CONNECTION) == 0) {
					strConnection = sb.toString();
				} else if (strKey.compareToIgnoreCase(SERVER) == 0) {
					strServer = sb.toString();
				} else if (strKey.compareToIgnoreCase(CONTENT_TYPE) == 0) {
					strContentType = sb.toString();
				} else if (strKey.compareToIgnoreCase(CONTENT_LENGTH) == 0) {
					nContentLength = Integer.valueOf(sb.toString());
				} else if (strKey.compareToIgnoreCase(HOST) == 0) {
					strHost = sb.toString();
				} else if (strKey.compareToIgnoreCase(CHUNKED) == 0) {
					String strChunked = "";
					strChunked = sb.toString();
					if (strChunked != null && strChunked.length() > 0) {
						bChunked = true;
					} else {
						bChunked = false;
					}
				} else if (strKey.compareToIgnoreCase(MO_PAGECOUNT) == 0) {
					strMoPageCount = sb.toString();
				} else if (strKey.compareToIgnoreCase(MO_CONVERTING) == 0) {
					strMoConverting = sb.toString();
				} else if (strKey.compareToIgnoreCase(MO_PAGEWIDTH) == 0) {
					strMoPageWidth = sb.toString();
				} else if (strKey.compareToIgnoreCase(MO_PAGEHEIGHT) == 0) {
					strMoPageHeight = sb.toString();
				}else if (strKey.compareToIgnoreCase(CONTENT_DISPOSITION) == 0) {
					strContentDisposition = sb.toString();
				}else if (strKey.compareToIgnoreCase(MO_ERRCODE) == 0) {
					strMoErrCode = sb.toString();
				}else if (strKey.compareToIgnoreCase(MO_STATE) == 0) {
					strMoState = sb.toString();
				} 
				if(context.getResources().getBoolean(R.bool.UseDocumentHash)){
					if (strKey.compareToIgnoreCase(MO_HASHCODE) == 0) {
						Log.d("jms", "MO_HASHCODE : " + sb.toString());
						strMoHashCode = sb.toString();
					} 
				}
			}
		}
	}

	public String getConnection() {
		if (connection != null) {
			if (strConnection != null && strConnection.length() > 0) {
				return strConnection;
			}
			strConnection = connection.getHeaderField(CONNECTION);
		}

		LogUtil.d(getClass(), "HttpHeader getConnection :: " + strConnection);

		return strConnection;
	}

	public String getServer() {
		if (connection != null) {
			if (strServer != null && strServer.length() > 0) {
				return strConnection;
			}
			strServer = connection.getHeaderField(SERVER);
		}

		LogUtil.d(getClass(), "HttpHeader getServer :: " + strServer);

		return strServer;
	}

	public String getContentType() {
		if (connection != null) {
			if (strContentType != null && strContentType.length() > 0) {
				return strContentType;
			}
			strContentType = connection.getHeaderField(CONTENT_TYPE);
		}

		LogUtil.d(getClass(), "HttpHeader getContentType :: " + strContentType);

		return strContentType;
	}

	public String getHost() {
		if (connection != null) {
			if (strHost != null && strHost.length() > 0) {
				return strHost;
			}
			strHost = connection.getHeaderField(HOST);
		}

		LogUtil.d(getClass(), "HttpHeader getHost :: " + strHost);

		return strHost;
	}

	public String getMOPageCount() {
		if (connection != null) {
			if (strMoPageCount != null && strMoPageCount.length() > 0) {
				return strMoPageCount;
			}
			strMoPageCount = connection.getHeaderField(MO_PAGECOUNT);
		}

		LogUtil.d(getClass(), "HttpHeader getMOPageCount :: " + strMoPageCount);

		return strMoPageCount;
	}

	public String getMOConverting() {
		if (connection != null) {
			if (strMoConverting != null && strMoConverting.length() > 0) {
				return strMoConverting;
			}
			strMoConverting = connection.getHeaderField(MO_CONVERTING);
		}

		LogUtil.d(getClass(), "HttpHeader getMOConverting :: " + strMoConverting);

		return strMoConverting;
	}
	
	/**
	* @Method Name	:	getMOPageWidth
	* @작성일				:	2015. 10. 15. 
	* @작성자				:	조명수
	* @변경이력				:	현재 문서 변환의 경우에만 사용됨
	* @Method 설명 		: 	문서변환 요청시 변환된 문서의 가로...
	* 								해당 가로사이즈로 DocumentActivity 에서 이미지 뷰의 가로사이즈를 결정
	*/
	public String getMOPageWidth() {
		if (connection != null) {
			if (strMoPageWidth != null && strMoPageWidth.length() > 0) {
//				LogUtil.d(getClass(), "HttpHeader getMOPageWidth1 :: " + strMoPageWidth);
				return strMoPageWidth;
			}
			strMoPageWidth = connection.getHeaderField(MO_PAGEWIDTH);
		}

		LogUtil.d(getClass(), "HttpHeader getMOPageWidth2 :: " + strMoPageWidth);

		return strMoPageWidth;
	}
	
	/**
	* @Method Name	:	getMOPageHeight
	* @작성일				:	2015. 10. 15. 
	* @작성자				:	조명수
	* @변경이력				:   현재 문서 변환의 경우에만 사용됨
	* @Method 설명 		:   문서변환 요청시 변환된 문서의 세로...
	* 								해당 세로사이즈로 DocumentActivity 에서 이미지 뷰의 세로사이즈를 결정
	*/	
	public String getMOPageHeight() {
		if (connection != null) {
			if (strMoPageHeight != null && strMoPageHeight.length() > 0) {
				return strMoPageHeight;
			}
			strMoPageHeight = connection.getHeaderField(MO_PAGEHEIGHT);
		}

		LogUtil.d(getClass(), "HttpHeader getMOPageHeight :: " + strMoPageHeight);

		return strMoPageHeight;
	}
	
	public String getMOHashCode() {
		if (connection != null) {
			if (strMoHashCode != null && strMoHashCode.length() > 0) {
				return strMoHashCode;
			}
			strMoHashCode = connection.getHeaderField(MO_HASHCODE);
		}

		return strMoHashCode;
	}

	public String getContentDisposition() {
		if (connection != null) {
			if (strContentDisposition != null && strContentDisposition.length() > 0) {
				return strContentDisposition;
			}
			strContentDisposition = connection.getHeaderField(CONTENT_DISPOSITION);
		}

//		LogUtil.d(getClass(), "HttpHeader Connection :: " + connection.getHeaderField("Connection"));
//		LogUtil.d(getClass(), "HttpHeader getContentDisposition :: " + strContentDisposition);

		return strContentDisposition;
	}

	public boolean isChunked() {
		if (connection != null) {
			if (strChunked != null && strChunked.length() > 0) {
				bChunked = true;
				return bChunked;
			}
			strChunked = connection.getHeaderField(CHUNKED);
		}

		LogUtil.d(getClass(), "HttpHeader getChunked :: " + strChunked);

		if (strChunked != null && strChunked.length() > 0) {
			bChunked = true;
		} else {
			bChunked = false;
		}

		return bChunked;
	}

	public int getContentLength() {
		if (connection != null) {
			if (nContentLength != -100) {
				return nContentLength;
			}
			nContentLength = Integer.parseInt(connection.getHeaderField(CONTENT_LENGTH));
		}

		LogUtil.d(getClass(), "HttpHeader getContentLength :: " + nContentLength);

		return nContentLength;
	}
	
	public String getMOErrCode() {
		if (connection != null) {
			if (strMoErrCode != null && strMoErrCode.length() > 0) {
				return strMoErrCode;
			}
			strMoErrCode = connection.getHeaderField(MO_ERRCODE);
		}

		return strMoErrCode;
	}
	
	public String getMOState() {
		if (connection != null) {
			if (strMoState != null && strMoState.length() > 0) {
				return strMoState;
			}
			strMoState = connection.getHeaderField(MO_STATE);
		}

		return strMoState;
	}
}
