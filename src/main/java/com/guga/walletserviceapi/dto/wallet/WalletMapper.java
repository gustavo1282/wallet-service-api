package com.guga.walletserviceapi.dto.wallet;

import org.springframework.stereotype.Component;

import com.guga.walletserviceapi.model.Wallet;

@Component
public class WalletMapper {

    public WalletResponseDTO toDto(Wallet entity) {
        if (entity == null) return null;

        return new WalletResponseDTO(
            entity.getWalletId(),
            entity.getCustomerId(),
            entity.getLastOperationType(),
            entity.getPreviousBalance(),
            entity.getCurrentBalance(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Create: não seta walletId (normalmente é gerado no banco/serviço).
     */
    public Wallet toEntity(WalletCreateDTO dto) {
        if (dto == null) return null;

        Wallet wallet = new Wallet();
        wallet.setCustomerId(dto.customerId());
        wallet.setLastOperationType(dto.lastOperationType());
        wallet.setPreviousBalance(dto.previousBalance());
        wallet.setCurrentBalance(dto.currentBalance());
        wallet.setStatus(dto.status());
        wallet.setCreatedAt(dto.createdAt());
        wallet.setUpdatedAt(dto.updatedAt());
        return wallet;
    }

    /**
     * Update: aplica somente campos permitidos (mantém o resto).
     * Ideal quando você faz PUT/PATCH e não quer sobrescrever campos sensíveis.
     */
    public void updateEntityFromDto(WalletUpdateDTO dto, Wallet target) {
        if (dto == null || target == null) return;

        // hoje seu WalletUpdateDTO só tem status, então fica simples:
        if (dto.status() != null) {
            target.setStatus(dto.status());
        }
    }

    /**
     * Converte WalletUpdateDTO em entidade parcial para update.
     * Apenas campos permitidos são populados.
     */
    public Wallet toEntity(WalletUpdateDTO dto) {
        if (dto == null) return null;

        Wallet wallet = new Wallet();

        // ⚠️ Apenas campos atualizáveis
        if (dto.status() != null) {
            wallet.setStatus(dto.status());
        }

        return wallet;
    }

}