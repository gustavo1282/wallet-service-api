package com.gugawallet.walletconsumer.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerRegisteredEvent {

    @NotNull
    private Long customerId;

    private String documentId;

    private String cpf;

    private String firstName;

    private String lastName;

    private String fullName;

    private String email;

    private String phoneNumber;

    private CustomerStatus status;

    private Integer score;

    private String processId;

    private LocalDateTime createdAt;
}
