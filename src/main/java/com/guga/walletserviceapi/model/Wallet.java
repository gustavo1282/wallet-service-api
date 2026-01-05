package com.guga.walletserviceapi.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.converter.OperationTypeConverter;
import com.guga.walletserviceapi.model.converter.StatusConverter;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonPropertyOrder({
    "walletId", "status", "customerId", "lastOperationType", "previousBalance", "currentBalance", "createdAt", "updatedAt"
})
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "walletId")
@Entity
@Table(name = "tb_wallet")
public class Wallet {

    @Id // Chave primária simples, campo único
    @Column(name = "wallet_id", nullable = false, unique = true)
    private Long walletId;


    @NotNull(message = "Customer ID cannot be null")
    @Column(name = "customer_id_fk", insertable = true, updatable = true)
    private Long customerId;


    @Schema(description = "Customer associated with the wallet", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id_fk", referencedColumnName = "customer_id", nullable = false, insertable = false, updatable = false)
    private Customer customer;


    @Convert(converter = OperationTypeConverter.class)
    @JsonProperty("lastOperationType")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "last_operation_type", nullable = true, length = 2, insertable = true, updatable = true)
    private OperationType lastOperationType;

    
    @Digits(integer = 14, fraction = 2, message = "Previous balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "previous_balance", nullable = false)
    private BigDecimal previousBalance;



    @Digits(integer = 14, fraction = 2, message = "Current balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;


    @Convert(converter = StatusConverter.class)
    @Column(name = "status", nullable = false, length = 2, insertable = true, updatable = true)
    private Status status;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "created_At", nullable = false)
    private LocalDateTime createdAt;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "updated_At", nullable = false)
    private LocalDateTime updatedAt;

}
