package com.guga.walletserviceapi.dto.transaction;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TransactionTransferDTO(
    @NotNull Long walletIdReceived,
    @NotNull @DecimalMin("0.01") BigDecimal amount
) {}