package com.guga.walletserviceapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_deposit_money")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
public class DepositMoney extends Transaction {
    
    @Schema(description = "Deposit Sender associated with the transaction", accessMode = Schema.AccessMode.READ_WRITE)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_sender_id_fk", referencedColumnName = "deposit_sender_id", nullable = true, insertable = true, updatable = false)
    private DepositSender depositSender;

}
