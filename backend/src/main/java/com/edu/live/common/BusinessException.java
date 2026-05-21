package com.edu.live.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final HttpStatus status;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        HttpStatus resolvedStatus = HttpStatus.resolve(code);
        this.status = resolvedStatus == null ? HttpStatus.BAD_REQUEST : resolvedStatus;
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.code = status.value();
        this.status = status;
    }
}
