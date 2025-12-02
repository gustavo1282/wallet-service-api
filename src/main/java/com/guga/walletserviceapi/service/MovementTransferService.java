package com.guga.walletserviceapi.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.repository.MovementTransferRepository;

public class MovementTransferService {

    @Autowired
    private MovementTransferRepository movementTransferRepository;

    public MovementTransaction save(MovementTransaction movementTransaction) {

        MovementTransaction movementTransactionSaved = movementTransferRepository.save(movementTransaction);
        return movementTransactionSaved;
    }

}
