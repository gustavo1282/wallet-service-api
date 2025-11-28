package com.guga.walletserviceapi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "tb_transfer_received")
@Data
@NoArgsConstructor // ESSA é crucial para o Jackson/Spring
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder
public class TransferMoneyReceived extends Transaction {

    //@CsvBindByName(column = "movementId")
    //@Column(name = "movement_id_fk", insertable = false, updatable = false)
    //private Long movementId;

}
