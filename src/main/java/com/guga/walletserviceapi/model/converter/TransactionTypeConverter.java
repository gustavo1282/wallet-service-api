package com.guga.walletserviceapi.model.converter;

import com.guga.walletserviceapi.model.enums.TransactionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionTypeConverter  implements AttributeConverter<TransactionType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TransactionType transactionType) {
        if (transactionType == null) {
            return null;
        }
        return transactionType.getValue();
    }

    @Override
    public TransactionType convertToEntityAttribute(Integer value) {
        if (value == 0) {
            return null;
        }
        return TransactionType.fromValue(value);
    }
}
