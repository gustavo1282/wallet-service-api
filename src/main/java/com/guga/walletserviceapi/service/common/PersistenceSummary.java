package com.guga.walletserviceapi.service.common;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classe de retorno padronizada para operações de persistência e análise.
 * Substitui o Record para garantir flexibilidade e compatibilidade com Genéricos.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersistenceSummary<T> {

    private int recordsProcessed;
    private String status;
    private String message;
    private LocalDateTime timestamp;
    
    // O campo result armazena o objeto ou lista que foi processado
    private T result; 

    /**
     * Factory method para simplificar a criação do sucesso.
     */
    public static PersistenceSummary<?> success(String message, Object result, int count) {
        return PersistenceSummary.builder()
                .recordsProcessed(count)
                .status("SUCCESS")
                .message(message)
                .timestamp(LocalDateTime.now())
                .result(result)
                .build();
    }
}