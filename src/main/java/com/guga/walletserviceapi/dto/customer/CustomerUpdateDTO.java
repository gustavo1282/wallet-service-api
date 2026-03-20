package com.guga.walletserviceapi.dto.customer;

import java.time.LocalDate;

import com.guga.walletserviceapi.model.enums.Status;

public record CustomerUpdateDTO(
    //String firstName,
    //String lastName,
    String email,
    String phoneNumber,
    //String documentId,
    LocalDate birthDate,
    Status status
) {
}