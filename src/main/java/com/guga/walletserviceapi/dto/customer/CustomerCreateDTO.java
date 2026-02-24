package com.guga.walletserviceapi.dto.customer;

import java.time.LocalDate;

import org.hibernate.validator.constraints.br.CPF;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerCreateDTO(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Email String email,
    @NotBlank @CPF String cpf,
    @NotBlank String phoneNumber,
    @NotBlank String documentId,
    @NotNull LocalDate birthDate
) {
}