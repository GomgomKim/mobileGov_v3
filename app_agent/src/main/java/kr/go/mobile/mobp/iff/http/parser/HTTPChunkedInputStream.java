package kr.go.mobile.mobp.iff.http.parser;

import java.io.InputStream;
import java.io.IOException;

public class HTTPChunkedInputStream extends InputStream {

    protected byte[] buf;
    protected int index;
    protected int max;
    protected boolean eof;
    protected InputStream in;

    public HTTPChunkedInputStream(InputStream in) {
        this.in = in;
        // initial buf size - will adjust automatically
        buf = new byte[2048];
        index = 0;
        max = 0;
        eof = false;
    }

    /* only called when the buffer is empty */
    private void readChunk()
        throws IOException {

        String line = readLine(in).trim();
        int length  = Integer.parseInt(line, 16);

        if (length > 0) {

            // make sure the chunk will fit into the buffer

            if (length > buf.length) {
                buf = new byte[length];
            }

            int bytesLeft = length;
            int reqBytes = 0;
            int off = 0;
            int read = 0;

            /*  multiple reads might be necessary to load
                the entire chunk */
            while (bytesLeft != 0) {
                reqBytes = bytesLeft;
                read = in.read(buf, off, reqBytes);
                if (read == -1) break;
                bytesLeft -= read;
                off += read;
            }

            max = off;
            index = 0;

        } else {
            // end of data indicated
            eof = true;
        }

        in.read(); // skip CR
        in.read(); // skip LF

    }

    /**
     * Read a line of text from the given Stream and return it
     * as a String.  Assumes lines end in CRLF.
     */
    private String readLine(InputStream in) throws IOException {
        StringBuffer buf = new StringBuffer();
        int c, length = 0;

        while(true) {
            c = in.read();
            if (c == -1 || c == '\n' || length > 512) {
                break;
            } else if (c == '\r') {
                in.read();
                return buf.toString();
            } else {
                buf.append((char)c);
                length++;
            }

        }
        return buf.toString();
    }

    public int read(byte [] buffer, int off, int len)
        throws IOException {
        if (eof) return -1;
        if (max == index) readChunk();

        if (index + len <= max) {
            // that's easy
            System.arraycopy(buf, index, buffer, off, len);
            index += len;
            return len;
        } else {
            int maximum = max - index;
            System.arraycopy(buf, index, buffer, off, maximum);
            index += maximum;
            int read = read(buffer, off+maximum, len-maximum);
            if (read == -1) {
                return maximum;
            } else {
                return maximum + read;
            }
        }
    }

    public int read()
        throws IOException {
        if (eof) return -1;
        if (max == index) readChunk();
        return buf[index++] & 0xff;
    }

    public int available()
        throws IOException {
        return in.available();
    }

    public void close()
        throws IOException {
        in.close();
    }

}