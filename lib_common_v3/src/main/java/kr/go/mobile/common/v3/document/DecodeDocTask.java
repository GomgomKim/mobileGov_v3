package kr.go.mobile.common.v3.document;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DecodeDocTask implements Runnable {

    private final String TAG = DecodeDocTask.class.getSimpleName();

    private static final long SLEEP_TIME_MILLISECONDS = 500;
    private static final long MAX_RETRY = 3;

    public interface ITrimCacheSizeAction {
        void onTrim();
    }

    int pageDocImage;
    byte[] encodeDocImageData;
    ITrimCacheSizeAction action;
    Handler docConvertManagerHandler;
    int retryCount = 0;

    public static DecodeDocTask obtain(int page, byte[] responseBytes, ITrimCacheSizeAction action, Handler handler) {
        DecodeDocTask task = new DecodeDocTask(page, responseBytes, action, handler);
        return task;
    }

    private DecodeDocTask(int page, byte[] encodeImage, ITrimCacheSizeAction action, Handler handler) {
        this.pageDocImage = page;
        this.encodeDocImageData = encodeImage;
        this.action = action;
        this.docConvertManagerHandler = handler;
    }

    @Override
    public void run() {
        Bitmap decodeDocImage = null;
        try {
            do {
                if (retryCount >= MAX_RETRY) {
                    break;
                }
                decodeDocImage = simpleDecode(encodeDocImageData);
                retryCount++;
            } while (decodeDocImage == null);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (decodeDocImage == null) {
                // TODO 실패 메시지만 보내기
                docConvertManagerHandler.sendEmptyMessage(DocConvertManager.MANAGER_CONVERT_FAILED);
            } else {
                ConvertedDoc convertedDoc = new ConvertedDoc(pageDocImage, decodeDocImage);
                Message m = docConvertManagerHandler.obtainMessage(DocConvertManager.MANAGER_CONVERT_COMPLETED, convertedDoc);
                m.sendToTarget();
            }

        }
    }

    private Bitmap simpleDecode(byte[] imageBuffer) throws InterruptedException {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = false;

        try {
            Log.w(TAG, "in decode stage. (freeMemory = " + Runtime.getRuntime().freeMemory()
                    + ", totalMemory =  " + Runtime.getRuntime().totalMemory()
                    + ", maxMemory =  " + Runtime.getRuntime().maxMemory()
                    + ")");

            return BitmapFactory.decodeByteArray(imageBuffer, 0, imageBuffer.length, opts);
        } catch (Throwable T) {
            Log.e(TAG, "Out of memory in decode stage. (freeMemory = " + Runtime.getRuntime().freeMemory()
                    + ", totalMemory =  " + Runtime.getRuntime().totalMemory()
                    + ", maxMemory =  " + Runtime.getRuntime().maxMemory()
                    + ")");

            action.onTrim();

            try {
                Thread.sleep(SLEEP_TIME_MILLISECONDS);
            } catch (InterruptedException e) {
                throw e;
            }
        }

        return null;
    }
}
