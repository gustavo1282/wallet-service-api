package com.guga.walletserviceapi.model.enums;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OperationType {
    WITHDRAW(1),
    DEPOSIT(2),
    TRANSFER_SEND(3),
    TRANSFER_RECEIVED(4)
    ;

    private final Integer value;

    OperationType(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    // ---- Converte de código numérico (BD)
    public static OperationType fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(t -> t.getValue().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid code for OperationType: " + code));
    }

    // ---- Converte de nome (JSON: "DEPOSIT", "withdraw", etc.)
    public static OperationType fromName(String name) {
        try {
            return OperationType.valueOf(name.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid name for OperationType: " + name);
        }
    }

    // ---- Permite aceitar tanto números quanto textos (opcional)
    @JsonCreator
    public static OperationType fromJson(String value) {
        if (value == null) return null;

        // Tenta converter como código numérico
        try {
            return fromCode(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {}

        // Tenta converter como nome
        return fromName(value);
    }

}
