package com.guga.walletserviceapi.repository;

import com.guga.walletserviceapi.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

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
    Page<Transaction> findByWalletWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Transaction> findTop200ByWalletId(Long walletId, Pageable pageable);

}
