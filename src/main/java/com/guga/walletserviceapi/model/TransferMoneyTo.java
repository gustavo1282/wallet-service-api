package com.guga.walletserviceapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity(name = "tb_transfer_money_to")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
public class TransferMoneyTo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transfer_money_to", nullable = false)
    @EqualsAndHashCode.Include
    private Long idTransferMoneyTo;

    @Column(name = "transaction_id", insertable = false, updatable = false)
    private Long transactionId;

    @NotNull(message = "Wallet ID cannot be null")
    @Column(name = "wallet_id", insertable = false, updatable = false)
    private Long walletId;

    @Schema(description = "Wallet associated with the transaction", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne
    @JoinColumn(name = "wallet_Id")
    private Wallet wallet;

    @Digits(integer = 14, fraction = 2, message = "Amount must have up to 14 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

}
