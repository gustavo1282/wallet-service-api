package com.guga.walletserviceapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guga.walletserviceapi.model.WithdrawMoney;

public interface WithdrawMoneyRepository extends JpaRepository<WithdrawMoney, Long> {
}
