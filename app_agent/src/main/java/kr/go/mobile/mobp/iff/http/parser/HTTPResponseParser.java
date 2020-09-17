package kr.go.mobile.mobp.iff.http.parser;

import java.io.InputStream;
import java.io.IOException;

public class HTTPResponseParser extends HTTPParser {

    protected String httpType;
    protected int httpCode;
    protected String httpMsg;

    public HTTPResponseParser(InputStream is)
        throws IOException {
        super(is);
    }

    public String getMessage() {
        return httpMsg;
    }

    public int getStatusCode() {
        return httpCode;
    }

    public boolean isOK() {
        return (httpCode == 200);
    }

    public void parseHead(String line)
        throws IOException {
        int st = line.indexOf(" ");
        if (st == -1) {
            throw new IOException("Bad HTTP header");
        }
        httpType = line.substring(0, st);

        st++;
        int et = line.indexOf(" ", st);
        if (et == -1) {
            throw new IOException("Bad HTTP header");
        }

        try {
            httpCode = Integer.parseInt(line.substring(st, et).trim());
        } catch(NullPointerException e) {
        	throw new IOException("Bad HTTP header");
        } catch(IndexOutOfBoundsException e) {
        	throw new IOException("Bad HTTP header");
        } catch(NumberFormatException e) {
            throw new IOException("Bad HTTP header");
        }

        et++;
        httpMsg = line.substring(et);
    }

}