package com.guga.walletserviceapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DepositMoney extends Transaction {

    @Column(name = "depositId", nullable = false)
    private Long depositId;

    @Schema(description = "Deposit Terminal associated with operation Money Deposit Transaction",
            accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne
    @JoinColumn(name = "depositId")
    private DepositTerminal depositTerminal;

}
