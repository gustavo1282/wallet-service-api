package com.guga.walletserviceapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.guga.walletserviceapi.model.DepositSender;

@Repository
public interface DepositSenderRepository extends JpaRepository<DepositSender, Long>  { 

}
