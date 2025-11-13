package com.guga.walletserviceapi.repository;


import com.guga.walletserviceapi.model.MovementTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementTransferRepository extends
        JpaRepository<MovementTransaction, Long> {

}
