package kr.go.mobile.agent.service.monitor;

import android.os.Parcel;
import android.os.Parcelable;

public class SecureNetworkData implements Parcelable {

    protected SecureNetworkData(Parcel in) {
        loginId = in.readString();
        loginPw = in.readString();
        int cmdValue = in.readInt();
        int statusValue = in.readInt();
        command = CMD.valueOf(cmdValue);
        status = STATUS.valueOf(statusValue);
        failMessage = in.readString();
    }

    public static final Creator<SecureNetworkData> CREATOR = new Creator<SecureNetworkData>() {
        @Override
        public SecureNetworkData createFromParcel(Parcel in) {
            return new SecureNetworkData(in);
        }

        @Override
        public SecureNetworkData[] newArray(int size) {
            return new SecureNetworkData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(loginId);
        dest.writeString(loginPw);
        dest.writeInt(command.value);
        dest.writeInt(status.value);
        dest.writeString(failMessage);
    }

    public enum CMD {
        _START(1),
        _STOP(2),
        _DESTROY(3);
        int value;
        CMD(int value) {
            this.value = value;
        }
        static CMD valueOf(int value) {
            for (CMD c : CMD.values()) {
                if (c.value == value) {
                    return c;
                }
            }
            throw new IllegalStateException("Unexpected value: " + value);
        }
    }

    public enum STATUS {
        _CONNECTED(1),
        _CONNECTING(2),
        _DISCONNECTED(3);
        int value;
        STATUS(int value) {
            this.value = value;
        }
        static STATUS valueOf(int value) {
            for (STATUS s : STATUS.values()) {
                if (s.value == value) {
                    return s;
                }
            }
            throw new IllegalStateException("Unexpected value: " + value);
        }
    }

    final String loginId;
    final String loginPw;
    String failMessage = "";
    CMD command = CMD._START;
    STATUS status = STATUS._DISCONNECTED;

    public SecureNetworkData(String loginId, String loginPW) {
        this.loginId = loginId;
        this.loginPw = loginPW;
    }

    public void changeStatus(STATUS s) {
        this.status = s;
    }

    public String getFailMessage() { return this.failMessage; }

    public CMD getCommand() {
        return this.command;
    }

    public STATUS getStatus() {
        return this.status;
    }

    public boolean retryConnection() {
        if (status == STATUS._DISCONNECTED && command == CMD._START) {
            return true;
        }
        return false;
    }

    public String getLoginID() {
        return this.loginId;
    }

    public String getLoginPw() {
        return this.loginPw;
    }
}
