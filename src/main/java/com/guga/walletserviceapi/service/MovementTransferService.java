package com.guga.walletserviceapi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.repository.MovementTransferRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovementTransferService implements IWalletApiService {

    private static final Logger LOGGER = LogManager.getLogger(MovementTransferService.class);

    private final MovementTransferRepository movementTransferRepository;
    private final ParamAppService paramAppService;

    @Transactional(rollbackFor = Exception.class)
    public MovementTransaction save(MovementTransaction movementTransaction) {
        LOGGER.info("MOVEMENT_TRANSFER_SERVICE_SAVE_ENTRY");

        Long nextId = nextIdGenerate();
        movementTransaction.setMovementId(nextId);
        LOGGER.info("MOVEMENT_TRANSFER_SERVICE_SAVE_DECISION | generatedMovementId={}", nextId);

        MovementTransaction saved = movementTransferRepository.save(movementTransaction);
        LOGGER.info("MOVEMENT_TRANSFER_SERVICE_SAVE_SUCCESS | movementId={}", saved.getMovementId());
        return saved;
    }

    @Override
    public Long nextIdGenerate() {
        return paramAppService
            .getNextSequenceId(ParamApp.SEQ_MOVEMENT_TRN_ID)
            .getValueLong();
    }
}
