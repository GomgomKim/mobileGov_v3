package kr.go.mobile.agent.service.broker;

import android.os.Parcel;

import org.json.JSONObject;

import java.util.Objects;

public class Document extends MethodResponse {
    public String errorCode;
    public String hash;
    public String pageWidth;
    public String pageHeight;
    public String pageCount;
    public String state;
    public String converted;
    public String contentType;
    public String contentLength;
    public String contentDisposition;
    public byte[] byteImage;

    public Document() {
        super();
    }

    protected Document(Parcel in) {
        super(in);
        errorCode = in.readString();
        hash = in.readString();
        pageWidth = in.readString();
        pageHeight = in.readString();
        pageCount = in.readString();
        state = in.readString();
        converted = in.readString();
        contentType = in.readString();
        contentLength = in.readString();
        contentDisposition = in.readString();
        byteImage = new byte[in.readInt()];
        in.readByteArray(byteImage);
    }

    @Override
    public boolean relayServerOK() {
        return Objects.equals(state, "ERROR_NONE");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(errorCode);
        dest.writeString(hash);
        dest.writeString(pageWidth);
        dest.writeString(pageHeight);
        dest.writeString(pageCount);
        dest.writeString(state);
        dest.writeString(converted);
        dest.writeString(contentType);
        dest.writeString(contentLength);
        dest.writeString(contentDisposition);
        dest.writeInt(byteImage.length);
        dest.writeByteArray(byteImage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Document> CREATOR = new Creator<Document>() {
        @Override
        public Document createFromParcel(Parcel in) {
            return new Document(in);
        }

        @Override
        public Document[] newArray(int size) {
            return new Document[size];
        }
    };

}
