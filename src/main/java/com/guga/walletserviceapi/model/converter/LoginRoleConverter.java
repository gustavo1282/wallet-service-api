package com.guga.walletserviceapi.model.converter;

import com.guga.walletserviceapi.model.enums.LoginRole;

import jakarta.persistence.AttributeConverter;

public class LoginRoleConverter implements AttributeConverter<LoginRole, Integer> {

    @Override
    public Integer convertToDatabaseColumn(LoginRole role) {
        if (role == null) {
            return null;
        }
        return role.getValue();
    }

    @Override
    public LoginRole convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return LoginRole.fromValue(dbData);
    }
}
