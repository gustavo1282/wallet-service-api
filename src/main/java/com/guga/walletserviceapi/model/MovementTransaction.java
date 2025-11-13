package com.guga.walletserviceapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.guga.walletserviceapi.model.converter.OperationTypeConverter;
import com.guga.walletserviceapi.model.enums.OperationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity()
@Table(name = "tb_movement_transfer")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
public class MovementTransaction {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long movementId;

    @Column(name = "transaction_id", insertable = false, updatable = false)
    private Long transactionId;

    @NotNull(message = "Wallet ID cannot be null")
    @Column(name = "wallet_id", insertable = false, updatable = false)
    private Long walletId;

    @Digits(integer = 14, fraction = 2, message = "Amount must have up to 14 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "transaction_to_id", insertable = false, updatable = false)
    private Long transactionToId;

    @NotNull(message = "WalletTo ID cannot be null")
    @Column(name = "wallet_to_id", insertable = false, updatable = false)
    private Long walletToId;

    @Convert(converter = OperationTypeConverter.class)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

}
