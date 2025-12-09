package com.guga.walletserviceapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.Status;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Page<Wallet> findByCustomerId(Long customerId, Pageable pageable);

    Page<Wallet> findByStatus(Status status, Pageable pageable);

}
