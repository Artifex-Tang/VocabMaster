package com.vocabmaster.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.slf4j.MDC;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private int code;
    private String msg;
    private T data;
    private String requestId;

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.requestId = MDC.get("requestId");
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> ok() {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    public static <T> R<T> fail(ErrorCode ec) {
        return new R<>(ec.getCode(), ec.getMsg(), null);
    }

    public static <T> R<T> fail(ErrorCode ec, String customMsg) {
        return new R<>(ec.getCode(), customMsg != null ? customMsg : ec.getMsg(), null);
    }
}
