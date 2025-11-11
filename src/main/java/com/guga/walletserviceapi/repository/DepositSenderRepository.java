package com.guga.walletserviceapi.repository;

import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositSender;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositSenderRepository extends JpaRepository<DepositSender, Long> {
}
