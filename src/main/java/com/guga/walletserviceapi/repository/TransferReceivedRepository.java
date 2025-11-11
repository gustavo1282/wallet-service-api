package com.guga.walletserviceapi.repository;

import com.guga.walletserviceapi.model.TransferMoneyReceived;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferReceivedRepository extends JpaRepository<TransferMoneyReceived, Long> {
}
