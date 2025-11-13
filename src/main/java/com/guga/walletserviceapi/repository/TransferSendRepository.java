package com.guga.walletserviceapi.repository;

import com.guga.walletserviceapi.model.TransferMoneySend;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferSendRepository extends JpaRepository<TransferMoneySend, Long> {
}
