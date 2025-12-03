package com.guga.walletserviceapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guga.walletserviceapi.model.DepositMoney;

public interface DepositMoneyRepository extends JpaRepository<DepositMoney, Long> {

}
