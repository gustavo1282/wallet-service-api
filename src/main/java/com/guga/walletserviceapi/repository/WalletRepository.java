package com.guga.walletserviceapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.guga.walletserviceapi.model.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Page<Wallet> findByCustomer_CustomerId(Long customerId, Pageable pageable);
    
    //List<Wallet> findByCustomer_CustomerId(Long customerId);

}
