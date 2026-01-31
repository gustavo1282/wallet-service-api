package com.guga.walletserviceapi.service.outbox;

import java.time.LocalDateTime;

import com.guga.walletserviceapi.model.enums.Status;

public record CustomerRegisteredPayload(
    Long customerId,
    String documentId,
    String cpf,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String phoneNumber,
    Status status,
    LocalDateTime createdAt
) {
}
