package com.guga.walletserviceapi.model.converter;

import com.guga.walletserviceapi.model.enums.OperationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OperationTypeConverter implements AttributeConverter<OperationType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(OperationType operationType) {
        if (operationType == null) {
            return null;
        }
        return operationType.getValue();
    }

    @Override
    public OperationType convertToEntityAttribute(Integer value) {
        if (value == 0) {
            return null;
        }
        return OperationType.fromValue(value);
    }
}
