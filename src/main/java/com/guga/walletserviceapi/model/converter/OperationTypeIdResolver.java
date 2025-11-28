package com.guga.walletserviceapi.model.converter;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.TransferMoneyReceived;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.WithdrawMoney;
import com.guga.walletserviceapi.model.enums.OperationType;

public class OperationTypeIdResolver extends TypeIdResolverBase {

    @Override
    public void init(JavaType bt) {}

    @Override
    public String idFromValue(Object value) {
        return ((OperationType) value).name();
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return idFromValue(value);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        OperationType type;

        if (id.matches("\\d+")) {
            type = OperationType.fromCode(Integer.parseInt(id));
        } else {
            type = OperationType.valueOf(id.toUpperCase());
        }

        Class<?> clazz = switch (type) {
            case DEPOSIT -> DepositMoney.class;
            case WITHDRAW -> WithdrawMoney.class;
            case TRANSFER_SEND -> TransferMoneySend.class;
            case TRANSFER_RECEIVED -> TransferMoneyReceived.class;
        };

        return context.constructType(clazz);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
    
}
