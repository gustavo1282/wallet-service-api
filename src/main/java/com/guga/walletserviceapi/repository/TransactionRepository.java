package com.guga.walletserviceapi.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.guga.walletserviceapi.model.Transaction;

@Repository
public interface TransactionRepository extends
        JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    /***
     * Searches for transactions by wallet ID and within a time period
     * @param walletId
     * @param start
     * @param end
     * @param pageable
     * @return List<Transaction>
     */
    Page<Transaction> findByWallet_WalletIdOrderByCreatedAtDesc(Long walletId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Transaction> findTop500ByWallet_WalletId(Long walletId, Pageable pageable);

    Optional<Page<Transaction>> findByWallet_WalletId(Long walletId, Pageable pageable);
    
// Usamos Wallet (o objeto) + WalletId (o campo dentro do objeto)
    Optional<Page<Transaction>> findByWalletWalletIdAndCreatedAtBetween(
        Long walletId, 
        LocalDateTime start, 
        LocalDateTime end, 
        Pageable pageable
    );

    // 2. Para a última transação (corrigindo o filtro e a ordenação)
    Optional<Transaction> findFirstByWalletWalletIdOrderByTransactionIdDesc(Long walletId);

}
