package com.guga.walletserviceapi.repository;

import com.guga.walletserviceapi.model.WithdrawMoney;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawMoneyRepository extends JpaRepository<WithdrawMoney, Long> {
}
