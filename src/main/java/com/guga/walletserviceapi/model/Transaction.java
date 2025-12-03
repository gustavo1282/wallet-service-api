package com.guga.walletserviceapi.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.converter.LocalDateTimeCsvConverter;
import com.guga.walletserviceapi.model.converter.OperationTypeConverter;
import com.guga.walletserviceapi.model.converter.StatusTransactionConverter;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Inheritance(strategy = InheritanceType.JOINED)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "operationType",
    visible = true
    )
@JsonSubTypes({
    @JsonSubTypes.Type(value = DepositMoney.class, name = "DEPOSIT"),
    @JsonSubTypes.Type(value = WithdrawMoney.class, name = "WITHDRAW"),
    @JsonSubTypes.Type(value = TransferMoneySend.class, name = "TRANSFER_SEND"),
    @JsonSubTypes.Type(value = TransferMoneyReceived.class, name = "TRANSFER_RECEIVED")
})
@JsonPropertyOrder({
    "transactionId", "operationType", "walletId", "login", "previousBalance", "amount", "currentBalance", "statusTransaction", "movementId", "createdAt"
})


@Entity
@Table(name = "tb_transaction")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "transactionId")
public abstract class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;

    @Column(name = "login", nullable = false, length = 25)
    private String login;

    @CsvBindByName(column = "walletId", required = true)
    @NotNull(message = "Wallet ID cannot be null")
    @Column(name = "wallet_id", insertable = true, updatable = true)
    private Long walletId;

    @JsonIgnore
    @Schema(description = "Wallet associated with the transaction", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id_fk", referencedColumnName = "wallet_Id", nullable = true, insertable = false, updatable = false)
    private Wallet wallet;

    @Convert(converter = OperationTypeConverter.class)
    @Column(name = "operation_type", nullable = false, length = 2)
    @JsonProperty("operationType")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OperationType operationType;

    //@CsvBindByName(column = "previousBalance")
    @Digits(integer = 14, fraction = 2, message = "Previous balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "previous_Balance", nullable = false)
    private BigDecimal previousBalance;

    //@CsvBindByName(column = "amount")
    @Digits(integer = 14, fraction = 2, message = "Amount must have up to 14 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    //@CsvBindByName(column = "currentBalance")
    @Digits(integer = 14, fraction = 2, message = "Current balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "current_Balance", nullable = false)
    private BigDecimal currentBalance;

    //@CsvBindByName(column = "statusTransaction")
    @Convert(converter = StatusTransactionConverter.class)
    @Column(name = "status_transaction", nullable = false, length = 2)
    StatusTransaction statusTransaction;

    @CsvCustomBindByName(column = "created_at", converter = LocalDateTimeCsvConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @CsvBindByName(column = "movementId", required = true)
    @Column(name = "movementId", insertable = true, updatable = false)
    private Long movementId;

    @JsonIgnore
    @Schema(description = "Transfer Money associated with the moviment", accessMode = Schema.AccessMode.READ_WRITE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movement_id_fk", referencedColumnName = "movement_id", nullable = true, insertable = false, updatable = false)
    private MovementTransaction movement;

}
