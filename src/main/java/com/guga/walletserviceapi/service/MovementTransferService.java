package com.guga.walletserviceapi.service;

import org.springframework.stereotype.Service;

import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.repository.MovementTransferRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovementTransferService implements IWalletApiService {

    private final MovementTransferRepository movementTransferRepository;
    private final ParamAppService paramAppService;

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
