package com.guga.walletserviceapi.dto.transaction;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.br.CPF;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionDepositDTO(
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank @CPF String cpfSender,
    @NotBlank String terminalId,
    @NotBlank String senderName
) {}