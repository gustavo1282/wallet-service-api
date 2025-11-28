package com.guga.walletserviceapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.guga.walletserviceapi.model.MovementTransaction;

@Repository
public interface MovementTransactionRepository extends JpaRepository<MovementTransaction, Long>  { 
    
}
