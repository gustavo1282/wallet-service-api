package com.guga.walletserviceapi.repository;

import com.guga.walletserviceapi.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /***
     * Find wallets by customer id
     * @param customerId
     * @return
     ***/
    @Query(value = "SELECT * FROM tb_wallet WHERE customer_id = :customerId Order By created_at DESC LIMIT 1" ,
            nativeQuery = true)
    Optional<Wallet> findWalletByCustomerId(@Param("customerId") Long customerId);

}
