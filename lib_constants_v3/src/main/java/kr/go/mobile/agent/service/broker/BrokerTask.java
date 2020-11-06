package kr.go.mobile.agent.service.broker;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class BrokerTask implements Parcelable {

    // TODO 라이브러리에서 uid 를 넘기고 보안 에이전트에서는 uid 를 이용하여 package name 을 획득하여 사용.
    int requestUid;

    public String serviceId;
    public String serviceParam;

    String fileName;
    String targetRelayUrl;
    ParcelFileDescriptor pfd;
    IBrokerServiceCallback serviceCallback;

    public static BrokerTask obtain(String serviceId) {
        // TODO 재사용 로직으로 구현 필요
        return new BrokerTask(serviceId);
    }

    BrokerTask(String serviceId) {
        this.requestUid = Process.myUid();
        this.serviceId = serviceId;
    }

    protected BrokerTask(Parcel in) {
        requestUid = in.readInt();
        serviceId = in.readString();
        serviceParam = in.readString();
        fileName = in.readString();
        targetRelayUrl = in.readString();
        pfd = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
    }

    @Deprecated
    public int getContentsLength() {
        return serviceParam.length();
    }

    public static final Creator<BrokerTask> CREATOR = new Creator<BrokerTask>() {
        @Override
        public BrokerTask createFromParcel(Parcel in) {
            return new BrokerTask(in);
        }

        @Override
        public BrokerTask[] newArray(int size) {
            return new BrokerTask[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(requestUid);
        dest.writeString(serviceId);
        dest.writeString(serviceParam);
        dest.writeString(fileName == null ? "" : fileName);
        dest.writeString(targetRelayUrl);
        dest.writeParcelable(pfd, flags);
    }

    public String getServiceId() { return serviceId; }

    public String getTargetRelayUrl() {
        return targetRelayUrl;
    }

    public String getOriginalServiceParam() {
        return serviceParam;
    }

    // 중계 서버와 사전에 정의된 내용으로 value 값만 URL 인코딩(UTF-8)이 필요함.
    public String getServiceParam() throws UnsupportedEncodingException {
        StringBuilder ret = new StringBuilder();
        String[] params = serviceParam.split("[&]");

        for (String param : params) {
            if (ret.length() > 0) {
                ret.append("&");
            }
            param = param.trim();
            String[] splitData = param.split("=");
            String value = "";
            if (splitData.length == 2) {
                value = URLEncoder.encode(splitData[1], "UTF-8");
                value = Utils.replace(value, "+", "%20");
                ret.append(splitData[0]).append("=").append(value);
            } else if (splitData.length == 1) {
                ret.append(splitData[0]);
            } else if (splitData.length > 2) {
                value = URLEncoder.encode(param.substring(param.indexOf("=") + 1), "UTF-8");
                value = Utils.replace(value, "+", "%20");
                ret.append(param.substring(0, param.indexOf("="))).append("=").append(value);
            }
        }
        return ret.toString();
    }

    public void validUriForWrite(Context context, Uri localUri) throws IOException {
        try {
            this.pfd = context.getContentResolver().openFileDescriptor(localUri, "w", null);
        } catch (FileNotFoundException e) {
            // Uri 로 접근할 수 없는 경로일 경우
            File downloadFile = new File(localUri.getPath());
            if (!downloadFile.exists()) {
                downloadFile.createNewFile();
            }
            FileOutputStream fo = new FileOutputStream(downloadFile);
            this.pfd = Utils.pipeTo(fo);
        }
        validFileControlOpt(localUri.getLastPathSegment());
    }

    public void validUriForRead(Context context, Uri localUri, String fileName) throws IOException {
        try {
            this.pfd = context.getContentResolver().openFileDescriptor(localUri, "r", null);
        } catch (FileNotFoundException e) {
            // Uri 로 접근할 수 없는 경로일 경우
            FileInputStream fi = new FileInputStream(localUri.getPath());
            this.pfd = Utils.pipeFrom(fi);
        }
        validFileControlOpt(fileName);
    }

    private void validFileControlOpt(String fileName) {
        this.fileName = fileName;
        StringBuilder newServiceParams = new StringBuilder();
        String[] tmp = serviceParam.split("[&]");
        for (String bodyParams : tmp) {
            boolean append = false;
            String[] splitData = bodyParams.trim().split("[=]");
            if (splitData.length == 1) {
                append = true;
            } else if ("url".equals(splitData[0])){
                this.targetRelayUrl = splitData[1];
            } else {
                append = true;
            }
            if (append) {
                if (newServiceParams.length() > 1) {
                    newServiceParams.append("&");
                }
                newServiceParams.append(bodyParams);
            }
        }
        this.serviceParam = newServiceParams.toString();
    }

    public String getFileName() {
        return this.fileName;
    }

    public byte[] getFileBytes() throws IOException {
        return Utils.readParcelFile(pfd);
    }

    public OutputStream getOutputStream() {
        return Utils.getOutputStream(pfd);
    }

    public void clear() {
        if (pfd != null) {
            try {
                pfd.close();
            } catch (IOException ignored) {
            }
        }
    }


}
