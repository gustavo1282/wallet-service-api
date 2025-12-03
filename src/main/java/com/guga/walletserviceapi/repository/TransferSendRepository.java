package com.guga.walletserviceapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guga.walletserviceapi.model.TransferMoneySend;

public interface TransferSendRepository extends JpaRepository<TransferMoneySend, Long> {
}
