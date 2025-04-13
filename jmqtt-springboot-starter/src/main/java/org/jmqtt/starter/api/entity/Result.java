package org.jmqtt.starter.api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class Result<T> {

    private int code;

    private String msg;

    private T data;

    public Result(T data) {
        this.code = 200;
        this.msg = "success";
        this.data = data;
    }

    private Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static <T> Result<T> ok() {
        return new Result<>();
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(400, msg);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message);
    }

}

