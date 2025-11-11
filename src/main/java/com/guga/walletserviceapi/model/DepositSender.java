package com.guga.walletserviceapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table( name = "deposit_sender" )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "senderId")
@SuperBuilder
public class DepositSender {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_sender_id", nullable = false)
    private Long senderId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @NotNull(message = "Wallet ID cannot be null")
    @Column(name = "wallet_id", insertable = false, updatable = false)
    private Long walletId;

    @Column(name = "terminalId", length = 12)
    private String terminalId;

    @Column(name = "senderName", length = 40)
    private String fullName;

    @Column(name = "cpf", length = 18)
    private String cpf;

    @Digits(integer = 14, fraction = 2, message = "Amount must have up to 14 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = true)
    private BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

}
