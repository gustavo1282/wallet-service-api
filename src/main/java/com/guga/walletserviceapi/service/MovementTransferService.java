package com.guga.walletserviceapi.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.repository.MovementTransferRepository;

public class MovementTransferService implements IWalletApiService {

    @Autowired
    private MovementTransferRepository movementTransferRepository;

    @Autowired
    private ParamAppService paramAppService;

    public MovementTransaction save(MovementTransaction movementTransaction) {
        movementTransaction.setMovementId(nextIdGenerate());
        MovementTransaction movementTransactionSaved = movementTransferRepository.save(movementTransaction);
        return movementTransactionSaved;
    }

    @Override
    public Long nextIdGenerate() {
        return paramAppService
            .getNextSequenceId(ParamApp.SEQ_MOVEMENT_TRN_ID)
            .getValueLong();
    }

}
