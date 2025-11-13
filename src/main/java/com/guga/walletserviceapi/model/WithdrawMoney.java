package com.guga.walletserviceapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

//@NoArgsConstructor
//@AllArgsConstructor
//@RequiredArgsConstructor
@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
public class WithdrawMoney extends Transaction {

    @Schema(description = "Transfer Money associated with the wallet", accessMode = Schema.AccessMode.READ_ONLY)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "movement_id", nullable = false)
    private MovementTransaction movementTransaction;

}
