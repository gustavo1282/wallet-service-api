package com.guga.walletserviceapi.model.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTransferRequest {
    private Long walletIdSend; 
    private Long walletIdReceived; 
    private BigDecimal amount;
}
