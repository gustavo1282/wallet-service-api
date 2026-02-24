package com.guga.walletserviceapi.dto.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.Status;

import jakarta.validation.constraints.NotNull;

public record WalletCreateDTO(
    @NotNull Long customerId,
    OperationType lastOperationType,
    BigDecimal previousBalance,
    BigDecimal currentBalance,
    Status status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) 
{ }