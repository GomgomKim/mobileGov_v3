package kr.go.mobile.mobp.iff.http.parser;

import java.io.InputStream;
import java.io.IOException;

public class HTTPRequestParser extends HTTPParser {

    protected String requestType;
    protected String service;

    public HTTPRequestParser(InputStream is)
        throws IOException {
        super(is);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void parseHead(String line)
        throws IOException {
        int st = line.indexOf(" ");
        if (st == -1) {
            throw new IOException("Bad HTTP header");
        }
        requestType = line.substring(0, st);

        st++;
        int et = line.indexOf(" ", st);
        if (et == -1) {
            throw new IOException("Bad HTTP header");
        }
        service = line.substring(st, et);

        et++;
    }

}