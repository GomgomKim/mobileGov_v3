package kr.go.mobile.agent.service.broker;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class Utils {

    static String replace(String text, String searchString, String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    static String replace(String text, String searchString, String replacement, int max) {
        if (!isEmpty(text) && !isEmpty(searchString) && replacement != null && max != 0) {
            int start = 0;
            int end = text.indexOf(searchString, start);
            if (end == -1) {
                return text;
            } else {
                int replLength = searchString.length();
                int increase = replacement.length() - replLength;
                increase = increase < 0 ? 0 : increase;
                increase *= max < 0 ? 16 : (max > 64 ? 64 : max);

                StringBuffer buf;
                for(buf = new StringBuffer(text.length() + increase); end != -1; end = text.indexOf(searchString, start)) {
                    buf.append(text.substring(start, end)).append(replacement);
                    start = end + replLength;
                    --max;
                    if (max == 0) {
                        break;
                    }
                }

                buf.append(text.substring(start));
                return buf.toString();
            }
        } else {
            return text;
        }
    }

    static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }


    static byte[] readParcelFile(ParcelFileDescriptor pfd) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = null;
        InputStream inputStream = null;
        try {
            inputStream = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            byteArrayOutputStream = new ByteArrayOutputStream();

            int readCount;
            byte[] readBuffer = new byte[10224 * 10];

            while((readCount = inputStream.read(readBuffer, 0, readBuffer.length)) != -1) {
                byteArrayOutputStream.write(readBuffer, 0, readCount);
            }

            return byteArrayOutputStream.toByteArray();
        } finally {
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException ignored) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }

        }
    }

    static OutputStream getOutputStream(ParcelFileDescriptor pfd) {
        return new ParcelFileDescriptor.AutoCloseOutputStream(pfd);
    }

    static ParcelFileDescriptor pipeTo(OutputStream outputStream) throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        new TransferWork(new ParcelFileDescriptor.AutoCloseInputStream(readSide), outputStream).start();
        return writeSide;
    }

    static ParcelFileDescriptor pipeFrom(InputStream inputStream) throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        new TransferWork(inputStream, new ParcelFileDescriptor.AutoCloseOutputStream(writeSide)).start();

        return readSide;
    }

    static class TransferWork extends Thread {
        final InputStream mIn;
        final OutputStream mOut;

        TransferWork(InputStream in, OutputStream out) {
            super("MOBP-ParcelFileDescriptor Transfer Thread");
            mIn = in;
            mOut = out;
            setDaemon(true);
        }

        @Override
        public synchronized void run() {
            byte[] buf = new byte[4096];
            int len;

            try {
                while ((len = mIn.read(buf)) > 0) {
                    mOut.write(buf, 0, len);
                }
                mOut.flush();
            } catch (IOException e) {
                Log.e("ERRRR", "DADDSDD", e);
            } finally {
                try {
                    mIn.close();
                } catch (IOException e) {
                    Log.e("ERRRR", "DADDSDD", e);
                }
                try {
                    mOut.close();
                } catch (IOException e) {
                    Log.e("ERRRR", "DADDSDD", e);
                }
            }
        }
    }
}
