package com.sds.mobile.servicebrokerLib.event;

import android.os.Parcelable;

public interface ResponseListener {
    public abstract void receive(ResponseEvent responseevent);
}