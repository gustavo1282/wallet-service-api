package com.guga.walletserviceapi.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.StatusTransaction;

public record TransactionResponseDTO(
    Long transactionId,
    Long walletId,
    BigDecimal amount,
    OperationType operationType,
    StatusTransaction status,
    LocalDateTime createdAt,
    String senderName,      // Específico para Depósito
    String cpfSender,       // Específico para Depósito
    Long relatedWalletId    // Específico para Transferência (Origem ou Destino)
) {}