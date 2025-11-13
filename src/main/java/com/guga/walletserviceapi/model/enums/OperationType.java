package com.guga.walletserviceapi.model.enums;

import java.util.stream.Stream;

public enum OperationType {
    WITHDRAW(1),
    DEPOSIT(2),
    TRANSFER_SEND(3),
    TRANSFER_RECEIVED(4)
    ;

    private final Integer value;

    private OperationType(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return this.value;
    }

    public static OperationType fromValue(Integer dbValue) {
        return Stream.of(OperationType.values())
                .filter(c -> c.getValue().equals(dbValue))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
