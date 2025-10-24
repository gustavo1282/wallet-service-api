package com.guga.walletserviceapi.model.enums;

import java.util.stream.Stream;

public enum TransactionType {
    WITHDRAW(1),
    DEPOSIT(2),
    TRANSFER(3);

    private final Integer value;

    private TransactionType(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return this.value;
    }

    public static TransactionType fromValue(Integer dbValue) {
        return Stream.of(TransactionType.values())
                .filter(c -> c.getValue().equals(dbValue))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
