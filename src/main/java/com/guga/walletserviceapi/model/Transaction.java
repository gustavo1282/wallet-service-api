package com.guga.walletserviceapi.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.guga.walletserviceapi.model.converter.OperationTypeConverter;
import com.guga.walletserviceapi.model.converter.StatusTransactionConverter;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_transaction")
@Inheritance(strategy = InheritanceType.JOINED)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "operationType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DepositMoney.class, name = "DEPOSIT"),
        @JsonSubTypes.Type(value = WithdrawMoney.class, name = "WITHDRAW"),
        @JsonSubTypes.Type(value = TransferMoneySend.class, name = "TRANSFER_SEND"),
        @JsonSubTypes.Type(value = TransferMoneyReceived.class, name = "TRANSFER_RECEIVED")
        }
)
@Getter
@Setter
@EqualsAndHashCode(of = "transactionId")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
//@RequiredArgsConstructor
//@Builder
public abstract class Transaction {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "login", nullable = false, length = 25)
    private String login;

    @NotNull(message = "Wallet ID cannot be null")
    @Column(name = "wallet_id", insertable = false, updatable = false)
    private Long walletId;

    @Schema(description = "Wallet associated with the transaction", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne
    @JoinColumn(name = "wallet_Id")
    private Wallet wallet;

    @Convert(converter = OperationTypeConverter.class)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Digits(integer = 14, fraction = 2, message = "Previous balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "previous_balance", nullable = false)
    private BigDecimal previousBalance;

    @Digits(integer = 14, fraction = 2, message = "Amount must have up to 14 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "current_Balance", nullable = false)
    private BigDecimal currentBalance;

    @Convert(converter = StatusTransactionConverter.class)
    @Column(name = "status_transaction", nullable = false, length = 2)
    StatusTransaction statusTransaction;

}
