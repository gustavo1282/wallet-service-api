package com.guga.walletserviceapi.model;

import com.guga.walletserviceapi.model.converter.TransactionTypeConverter;
import com.guga.walletserviceapi.model.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "tb_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "transactionId")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Convert(converter = TransactionTypeConverter.class)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Digits(integer = 14, fraction = 2, message = "Previous balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "previous_balance", nullable = false)
    private BigDecimal previousBalance;

    @Digits(integer = 14, fraction = 2, message = "Amount must have up to 14 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "current_Balance", nullable = false)
    private BigDecimal currentBalance;

}
