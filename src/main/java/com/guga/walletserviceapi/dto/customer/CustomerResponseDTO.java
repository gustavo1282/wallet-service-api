package com.guga.walletserviceapi.dto.customer;

import java.time.LocalDate;

import com.guga.walletserviceapi.model.enums.Status;

public record CustomerResponseDTO(
    Long customerId,
    String firstName,
    String lastName,
    String fullName,
    String email,
    String cpf,
    String phoneNumber,
    String documentId,
    LocalDate birthDate,
    Status status
) {
}