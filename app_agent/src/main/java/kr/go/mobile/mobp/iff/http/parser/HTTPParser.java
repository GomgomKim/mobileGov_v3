package kr.go.mobile.mobp.iff.http.parser;

import java.io.IOException;
import java.io.InputStream;


public abstract class HTTPParser {


    protected String server;
    protected String host;
    protected String contentType;
    protected String connection;
    protected String moPageCount;
    protected String moConverting;
    protected String contentDisposition;
    protected int    contentLength;
    protected boolean chunked;
    protected LineReader reader;
    protected StringBuffer header;

    public HTTPParser(InputStream is)
        throws IOException {
        contentLength = -1;
        chunked = false;
        header = new StringBuffer();
        setInputStream(is);
        parse();
    }
    
    public String getContentType() {
        return contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    public boolean isChunked() {
        return chunked;
    }

    public LineReader getReader() {
        return reader;
    }

    public void setInputStream(InputStream in) {
        reader = new LineReader(in);
    }
    
    public StringBuffer getHTTPHeader() {
        return header;
    }
    
    public String getMoPage() {
		return moPageCount;
	}
    
    public String getMoConverting() {
		return moConverting;
	}
    
    public String getContentDisposition() {
		return contentDisposition;
	}
    
    public abstract void parseHead(String line)
        throws IOException;

    /**
     * Parses the typical HTTP header.
     * @exception IOException if a connection fails or bad/incomplete request
     */
    protected void parse()
        throws IOException {

        String line;
        line = reader.readLine();
        
        parseHead(line);
        header.append(line + "\n");
        
        while ( (line = reader.readLine()).length() != 0 ) {

        	header.append(line + "\n");	
            if (line.startsWith(HTTPProtocol.CONNECTION)) {
                connection = getRest(line, HTTPProtocol.CONNECTION.length());
            } else if (line.startsWith(HTTPProtocol.SERVER)) {
                server = getRest(line, HTTPProtocol.SERVER.length());
            } else if (line.startsWith(HTTPProtocol.CONTENT_TYPE)) {
                contentType = getRest(line, HTTPProtocol.CONTENT_TYPE.length());
            } else if (line.startsWith(HTTPProtocol.CONTENT_LENGTH)) {
                contentLength = Integer.parseInt(getRest(line, HTTPProtocol.CONTENT_LENGTH.length()));
            } else if (line.startsWith(HTTPProtocol.HOST)){
                host = getRest(line, HTTPProtocol.HOST.length());
            } else if (line.startsWith(HTTPProtocol.CHUNKED)) {
                chunked = true;
            } else if (line.startsWith(HTTPProtocol.MO_PAGECOUNT)) {
            	moPageCount = getRest(line, HTTPProtocol.MO_PAGECOUNT.length());
            } else if (line.startsWith(HTTPProtocol.MO_CONVERTING)) {
            	moConverting = getRest(line, HTTPProtocol.MO_CONVERTING.length());
            } else if (line.startsWith(HTTPProtocol.CONTENT_DISPOSITION)) {
            	contentDisposition = getRest(line, HTTPProtocol.CONTENT_DISPOSITION.length());
            }

        }
    }

    protected static final String getRest(String line, int index) {
        return line.substring(index).trim();
    }

}