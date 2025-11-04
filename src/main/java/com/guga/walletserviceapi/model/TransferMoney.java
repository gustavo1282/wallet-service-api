package com.guga.walletserviceapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity(name = "tb_transfer_money")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
public class TransferMoney extends Transaction {

    @NotNull(message = "Transfer Money To cannot be null")
    @Column(name = "id_transfer_money_to", insertable = true, updatable = false)
    private Long idTransferMoneyTo;

    @Schema(description = "Transfer Money To associated with the wallet", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne
    @JoinColumn(name = "id_transfer_money_to", nullable = false)
    private TransferMoneyTo transferMoneyTo;

}
