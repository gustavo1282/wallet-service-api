package com.guga.walletserviceapi.service;

import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.repository.MovementTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class MovementTransferService {

    @Autowired
    private MovementTransferRepository movementTransferRepository;

    public MovementTransaction save(MovementTransaction movementTransaction) {

        MovementTransaction movementTransactionSaved = movementTransferRepository.save(movementTransaction);
        return movementTransactionSaved;
    }

}
