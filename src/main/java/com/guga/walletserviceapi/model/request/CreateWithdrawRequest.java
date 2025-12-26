package com.guga.walletserviceapi.model.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class CreateWithdrawRequest {
    private Long walletId;
    private BigDecimal amount;
}
