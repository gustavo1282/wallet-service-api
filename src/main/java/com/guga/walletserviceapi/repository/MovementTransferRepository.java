package com.guga.walletserviceapi.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.guga.walletserviceapi.model.MovementTransaction;

public interface MovementTransferRepository extends
        JpaRepository<MovementTransaction, Long> {

}
