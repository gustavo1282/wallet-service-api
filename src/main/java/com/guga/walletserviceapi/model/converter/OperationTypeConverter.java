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
        if (value == null) {
            return null; 
        }

        // Agora é seguro chamar intValue() ou buscar a enumeração
        for (OperationType type : OperationType.values()) {
            if (value.equals(type.getValue())) { // Supondo que você tem um método getId() na sua enum
                return type;
            }
        }
        
        throw new IllegalArgumentException("OperationType ID inválido: " + value);
    }
}
