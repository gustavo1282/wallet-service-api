package com.guga.walletserviceapi.model.enums;

import java.util.stream.Stream;

public enum LoginAuthType {
    USER_NAME(0),
    CPF(1),
    E_MAIL(2);

    private final Integer value;

    LoginAuthType(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static LoginAuthType fromValue(Integer dbValue) {
        return Stream.of(LoginAuthType.values())
                .filter(c -> c.getValue().equals(dbValue))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
