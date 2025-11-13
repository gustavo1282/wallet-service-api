package com.guga.walletserviceapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_transfer_send")
@Data
@NoArgsConstructor // ESSA é crucial para o Jackson/Spring
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
public class TransferMoneySend extends Transaction {

    @Schema(description = "Transfer Money associated with the wallet", accessMode = Schema.AccessMode.READ_ONLY)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "movement_id", nullable = false)
    private MovementTransaction movementTransaction;

}
