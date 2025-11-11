package com.guga.walletserviceapi.model.enums;

import java.util.stream.Stream;

public enum StatusTransaction {
    SUCCESS(1),
    INSUFFICIENT_BALANCE(2),
    SAME_WALLET(3),
    INVALID(4),
    AMOUNT_DEPOSIT_INSUFFICIENT(5),
    WALLET_STATUS_INVALID(6),
    AMOUNT_TRANSFER_INVALID(7),
    CUSTOMER_STATUS_INVALID(8),
    WALLET_INVALID(9),
    CUSTOMER_INVALID(10)
    ;

    private final Integer value;

    private StatusTransaction(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return this.value;
    }

    public static StatusTransaction fromValue(Integer dbValue) {
        return Stream.of(StatusTransaction.values())
                .filter(c -> c.getValue().equals(dbValue))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
