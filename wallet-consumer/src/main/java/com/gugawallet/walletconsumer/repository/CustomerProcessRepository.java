package com.gugawallet.walletconsumer.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.gugawallet.walletconsumer.model.CustomerProcess;

public interface CustomerProcessRepository extends MongoRepository<CustomerProcess, String> {
    Optional<CustomerProcess> findByCustomerId(Long customerId);
}
