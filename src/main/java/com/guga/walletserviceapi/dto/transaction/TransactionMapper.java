package com.guga.walletserviceapi.dto.transaction;

import org.springframework.stereotype.Component;

import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneyReceived;
import com.guga.walletserviceapi.model.TransferMoneySend;

@Component
public class TransactionMapper {

    public TransactionResponseDTO toDto(Transaction entity) {
        if (entity == null) return null;

        String senderName = null;
        String cpfSender = null;

        if (entity instanceof DepositMoney d) {
            // Mantive seu comportamento (se depositSender for null -> null)
            // Se preferir “devolver DTO sem sender”, é só mudar.
            if (d.getDepositSender() != null) {
                senderName = d.getDepositSender().getFullName();
                cpfSender = d.getDepositSender().getCpf();
            }
        }

        Long relatedWalletId = null;
        if (entity instanceof TransferMoneySend t) {
            // ⚠️ Você comentou que queria .getWalletIdReceived()
            // Mantive exatamente como estava: t.getWalletId()
            relatedWalletId = t.getWalletId();
        } else if (entity instanceof TransferMoneyReceived r) {
            relatedWalletId = r.getWalletId();
        }

        return new TransactionResponseDTO(
            entity.getTransactionId(),
            entity.getWalletId(),
            entity.getAmount(),
            entity.getOperationType(),
            entity.getStatusTransaction(),
            entity.getCreatedAt(),
            senderName,
            cpfSender,
            relatedWalletId
        );
    }
}