package com.guga.walletserviceapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guga.walletserviceapi.model.TransferMoneyReceived;

public interface TransferReceivedRepository extends JpaRepository<TransferMoneyReceived, Long> {
}
