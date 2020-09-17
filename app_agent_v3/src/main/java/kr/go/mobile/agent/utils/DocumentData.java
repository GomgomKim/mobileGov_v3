package kr.go.mobile.agent.utils;

import org.jetbrains.annotations.NotNull;

public class DocumentData {

    final String code;
    final String msg;

    private DocumentData(@NotNull String code, @NotNull String msg) {
        this.code = code;
        this.msg = msg;
    }

    public DocumentData getInstance() {
        return this;
    }

    public String getCode(){
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }

}
