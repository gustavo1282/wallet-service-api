package com.guga.walletserviceapi.model.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepositRequest {

    private Long walletId; 
    private BigDecimal amount; 
    private String cpfSender;
    private String terminalId; 
    private String senderName;

}
