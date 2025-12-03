package com.guga.walletserviceapi.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.converter.LocalDateTimeCsvConverter;
import com.guga.walletserviceapi.model.converter.OperationTypeConverter;
import com.guga.walletserviceapi.model.converter.StatusTransactionConverter;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonPropertyOrder({
    "movementId", "createdAt", "operationType", "amount", "statusTransaction", "transactionId", "walletId", "transactionReferenceId", "walletReferenceId"
})

@Entity()
@Table(name = "tb_movement_transfer")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
public class MovementTransaction {

    @Id // Chave primária simples, campo único
    @CsvBindByName(column = "movementId")
    @Column(name = "movement_id", nullable = false, unique = true)
    @EqualsAndHashCode.Include
    private Long movementId;


    @CsvBindByName(column = "transactionId")
    @Column(name = "transaction_id", nullable = false, insertable = true, updatable = false)
    private Long transactionId;


    @CsvBindByName(column = "walletId")
    @Column(name = "wallet_id", nullable = false, insertable = true, updatable = false)
    private Long walletId;


    @CsvBindByName(column = "amount")
    @Digits(integer = 14, fraction = 2, message = "Amount must have up to 14 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;


    @CsvBindByName(column = "transactionReferenceId")
    @Column(name = "transaction_reference_id", nullable = true, insertable = true, updatable = false)
    private Long transactionReferenceId;


    @CsvBindByName(column = "walletReferenceId")
    @Column(name = "wallet_reference_id", nullable = true, insertable = true, updatable = false)
    private Long walletReferenceId;


    @CsvBindByName(column = "operationType")
    @Convert(converter = OperationTypeConverter.class)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    
    @Convert(converter = StatusTransactionConverter.class)
    @Column(name = "status_transaction", nullable = false, length = 2)
    StatusTransaction statusTransaction;


    @CsvCustomBindByName(column = "createdAt", converter = LocalDateTimeCsvConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}
