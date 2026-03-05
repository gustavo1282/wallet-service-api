package com.guga.walletserviceapi.model.enums;

public enum ErrorCode {
    BAD_REQUEST("GENERIC_400"),
    UNAUTHORIZED("SECURITY_401"),
    FORBIDDEN("SECURITY_403"),
    NOT_FOUND("GENERIC_404"),
    CONFLICT("GENERIC_409"),
    INTERNAL_ERROR("GENERIC_500");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}