package com.guga.walletserviceapi.model.converter;

import com.guga.walletserviceapi.model.enums.LoginAuthType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LoginAuthTypeConverter implements AttributeConverter<LoginAuthType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(LoginAuthType LoginAuthType) {
        if (LoginAuthType == null) {
            return null;
        }
        return LoginAuthType.getValue();
    }

    @Override
    public LoginAuthType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return LoginAuthType.fromValue(dbData);
    }
}
