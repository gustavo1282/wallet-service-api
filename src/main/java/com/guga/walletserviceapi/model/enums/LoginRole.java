package com.guga.walletserviceapi.model.enums;

import java.util.stream.Stream;

public enum LoginRole {
    ADMIN(1),
    USER(2),
    SUPPORT(3),
    SYSTEM(4),
    MONITOR(5)
    ;

    private final Integer value;

    LoginRole(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static LoginRole fromValue(Integer dbValue) {
        return Stream.of(LoginRole.values())
                .filter(c -> c.getValue().equals(dbValue))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
