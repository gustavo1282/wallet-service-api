package com.guga.walletserviceapi.repository;

import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.DepositMoney;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositMoneyRepository extends JpaRepository<DepositMoney, Long> {

}
