package com.guga.walletserviceapi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_withdraw")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
public class WithdrawMoney extends Transaction {

    /* 
    @CsvBindByName(column = "movementId")
    @Column(name = "movement_id_fk", insertable = false, updatable = false)
    private Long movementId;
    */
}
