package com.guga.walletserviceapi.model.converter;

import com.guga.walletserviceapi.model.enums.StatusTransaction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusTransactionConverter implements AttributeConverter<StatusTransaction, Integer> {

    @Override
    public Integer convertToDatabaseColumn(StatusTransaction status) {
        if (status == null) {
            return null;
        }
        return status.getValue();
    }

    @Override
    public StatusTransaction convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return StatusTransaction.fromValue(dbData);
    }
}
