package com.guga.walletserviceapi.model.enums;

import java.util.stream.Stream;

public enum Status {
    ACTIVE(1),
    INACTIVE(2),
    BLOCKED(3),
    PENDING(4),
    REVIEW(5),
    WAITING_VERIFICATION(6),
    ARCHIVED(7);

    private final Integer value;

    Status(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static Status fromValue(Integer dbValue) {
        return Stream.of(Status.values())
                .filter(c -> c.getValue().equals(dbValue))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
