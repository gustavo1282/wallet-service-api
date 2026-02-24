package com.guga.walletserviceapi.dto.wallet;

import java.time.LocalDateTime;

import com.guga.walletserviceapi.model.enums.Status;

public record WalletUpdateDTO(
    Status status,
    LocalDateTime updatedAt
    //OperationType lastOperationType,
    //BigDecimal previousBalance,
    //BigDecimal currentBalance,
    //LocalDateTime createdAt,
) 
{ }