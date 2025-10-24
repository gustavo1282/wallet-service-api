package com.guga.walletserviceapi.model.converter;

import com.guga.walletserviceapi.model.enums.Status;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Status, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Status status) {
        if (status == null) {
            return null;
        }
        return status.getValue();
    }

    @Override
    public Status convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return Status.fromValue(dbData);
    }
}
