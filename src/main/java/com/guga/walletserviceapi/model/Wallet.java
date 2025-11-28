package com.guga.walletserviceapi.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.converter.LocalDateTimeCsvConverter;
import com.guga.walletserviceapi.model.converter.StatusConverter;
import com.guga.walletserviceapi.model.enums.Status;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//@Data

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
    @CsvBindByName(column = "walletId")
    @Column(name = "wallet_id", nullable = false, unique = true)
    private Long walletId;

    @CsvBindByName(column = "customerId", required = true)
    @NotNull(message = "Customer ID cannot be null")
    @Column(name = "customer_id_fk", insertable = true, updatable = true)
    private Long customerId;


    @Schema(description = "Customer associated with the wallet", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id_fk", referencedColumnName = "customer_id", nullable = false, insertable = false, updatable = false)
    private Customer customer;

    @CsvBindByName(column = "previousBalance")
    @Digits(integer = 14, fraction = 2, message = "Previous balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "previous_balance", nullable = false)
    private BigDecimal previousBalance;


    @CsvBindByName(column = "currentBalance")
    @Digits(integer = 14, fraction = 2, message = "Current balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;


    @CsvBindByName(column = "loginUser")
    @NotBlank(message = "Login user cannot be null or empty")
    @Column(name = "login_user", length = 20)
    private String loginUser;


    @CsvBindByName(column = "status")
    @Convert(converter = StatusConverter.class)
    @Column(name = "status", nullable = false, length = 2)
    private Status status;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @CsvCustomBindByName(column = "createdAt", converter = LocalDateTimeCsvConverter.class)
    @Column(name = "created_At", nullable = false)
    private LocalDateTime createdAt;


    @CsvCustomBindByName(column = "updatedAt", converter = LocalDateTimeCsvConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "updated_At", nullable = false)
    private LocalDateTime updatedAt;

}
