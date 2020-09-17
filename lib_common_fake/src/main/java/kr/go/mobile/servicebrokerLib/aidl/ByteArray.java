package com.sds.mobile.servicebrokerLib.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class ByteArray implements Parcelable {
  private final byte[] bytes;

  public ByteArray(byte[] bytes) {
        this.bytes = bytes;
  }


  public byte[] getBytes() {
    return bytes;
  }


  public int describeContents() {
    return 0;
  }


  public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(bytes.length);
        dest.writeByteArray(bytes);
  }


  public static final Parcelable.Creator<ByteArray> CREATOR =
      new Parcelable.Creator<ByteArray>() {
        public ByteArray createFromParcel(Parcel source) {
                int length = source.readInt();
                byte[] bytes = new byte[length];
                source.readByteArray(bytes);
                
          return new ByteArray(bytes);
        }


        public ByteArray[] newArray(int size) {
          return new ByteArray[size];
        }
      };
}