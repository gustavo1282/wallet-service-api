package com.guga.walletserviceapi.model.converter;

import com.guga.walletserviceapi.model.enums.ProcessTypeTransaction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProcessTypeTransactionConverter implements AttributeConverter<ProcessTypeTransaction, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ProcessTypeTransaction status) {
        if (status == null) {
            return null;
        }
        return status.getValue();
    }

    @Override
    public ProcessTypeTransaction convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return ProcessTypeTransaction.fromValue(dbData);
    }
}
