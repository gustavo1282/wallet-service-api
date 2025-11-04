package com.guga.walletserviceapi.model;

import com.guga.walletserviceapi.model.converter.StatusConverter;
import com.guga.walletserviceapi.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "tb_wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "walletId")
@Builder(toBuilder = true)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_Id", nullable = false)
    private Long walletId;

    @NotNull(message = "Customer ID cannot be null")
    @Column(name = "customer_id", insertable = false, updatable = false)
    private Long customerId;

    @Schema(description = "Customer associated with the wallet", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Convert(converter = StatusConverter.class)
    @Column(name = "status", nullable = false, length = 2)
    private Status status;

    @Digits(integer = 14, fraction = 2, message = "Current balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "current_Balance", nullable = false)
    private BigDecimal currentBalance;

    @Digits(integer = 14, fraction = 2, message = "Current balance must have up to 14 integer digits and 2 decimal places")
    @Column(name = "previous_Balance", nullable = false)
    private BigDecimal previousBalance;

    @NotNull(message = "Login user cannot be null")
    @Column(name = "login_User", nullable = false, length = 20)
    private String loginUser;

    @Column(name = "created_At", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_At", nullable = false)
    @CreationTimestamp
    private LocalDateTime updatedAt;

}
