package com.guga.walletserviceapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opencsv.bean.CsvBindByName;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    
    @CsvBindByName(column = "deposit_sender_id", required = true)
    @Column(name = "deposit_sender_id", insertable = true, updatable = false)
    private Long depositSenderId;

    
    @JsonIgnore
    @Schema(description = "Deposit Sender associated with the transaction", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_sender_id_fk", referencedColumnName = "deposit_sender_id", nullable = true, insertable = false, updatable = false)
    private DepositSender depositSender;

}
