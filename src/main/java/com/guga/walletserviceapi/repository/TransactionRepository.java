package com.guga.walletserviceapi.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.enums.OperationType;

@Repository
public interface TransactionRepository extends
        JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findByWalletId(
        Long walletId, 
        Pageable pageable);

    Page<Transaction> findByWalletIdAndCreatedAtBetween(
        Long walletId,
        LocalDateTime createdAtStart, 
        LocalDateTime createdAtEnd,
        Pageable pageable
    );

    Page<Transaction> findByWalletIdAndOperationType(
        Long walletId, 
        OperationType operationType, 
        Pageable pageable);

}
