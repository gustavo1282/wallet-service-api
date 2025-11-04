package com.guga.walletserviceapi.model.enums;

import java.util.stream.Stream;

public enum ProcessTypeTransaction {
    SUCCESS(1),
    INSUFFICIENT_BALANCE(2),
    SAME_WALLET(3),
    INVALID(4),
    AMOUNT_DEPOSIT_INSUFFICIENT(5),
    WALLET_STATUS_INVALID(6),
    AMOUNT_TRANSFER_INVALID(7)
    ;

    private final Integer value;

    private ProcessTypeTransaction(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return this.value;
    }

    public static ProcessTypeTransaction fromValue(Integer dbValue) {
        return Stream.of(ProcessTypeTransaction.values())
                .filter(c -> c.getValue().equals(dbValue))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
