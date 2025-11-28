package com.guga.walletserviceapi.model.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.guga.walletserviceapi.model.enums.OperationType;

public class OperationTypeDeserializer extends JsonDeserializer<OperationType> {

    @Override
    public OperationType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();

        // 1. Tenta por nome do enum
        try {
            return OperationType.valueOf(value.toUpperCase());
        } catch (Exception ignored) {}

        // 2. Tenta por código numérico
        try {
            int code = Integer.parseInt(value);
            return OperationType.fromCode(code);
        } catch (Exception ignored) {}

        throw new InvalidFormatException(p, 
            "Invalid operationType: " + value,
            value,
            OperationType.class
        );
    }
    
}
