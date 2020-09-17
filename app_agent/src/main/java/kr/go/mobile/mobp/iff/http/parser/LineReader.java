package kr.go.mobile.mobp.iff.http.parser;

import java.io.InputStream;
import java.io.IOException;
import java.io.FilterInputStream;

public class LineReader extends FilterInputStream {

    private static final int MAX_LEN = 16* 1024;

    protected int charsRead = 0;

    public LineReader(InputStream is) {
        super(is);
    }

    public InputStream getInputStream() {
        return in;
    }

    public int getCharsRead() {
        return charsRead;
    }

    public String readLine()
        throws IOException {
        return readLine(in);
    }

    /**
     * Read a line of text from the given Stream and return it
     * as a String.  Assumes lines end in CRLF.
     * @param in a connected stream which contains the entire
     * message being sen.
     * @exception IOException if a connection fails or abnormal connection
     * termination.
     * @return the next line read from the stream.
     */
    protected String readLine(InputStream in)
        throws IOException {
        StringBuffer buf = new StringBuffer();
        int c, length = 0;

        while(true) {
            c = in.read();
            if (c == -1 || c == '\n' || length > MAX_LEN) {
                charsRead++;
                break;
            } else if (c == '\r') {
                in.read();
                charsRead+=2;
                break;
            } else {
                buf.append((char)c);
                length++;
            }
        }
        charsRead += length;
        return buf.toString();
    }
}