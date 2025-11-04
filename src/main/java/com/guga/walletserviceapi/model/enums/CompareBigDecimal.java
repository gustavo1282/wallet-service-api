package com.guga.walletserviceapi.model.enums;

public enum CompareBigDecimal {
    LESS_THAN(-1),
    EQUAL(0),
    GREATER_THAN(1);

    private final int value;

    CompareBigDecimal(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // Método auxiliar para converter o int retornado pelo compareTo em um Enum
    public static CompareBigDecimal fromInt(int intValue) {
        for (CompareBigDecimal result : CompareBigDecimal.values()) {
            if (result.value == intValue) {
                return result;
            }
        }
        // Isso nunca deve acontecer se intValue for -1, 0 ou 1
        throw new IllegalArgumentException("Valor de comparação inválido: " + intValue);
    }

}
