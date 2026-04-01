package com.guga.walletserviceapi.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.repository.MovementTransactionRepository;
import com.guga.walletserviceapi.service.common.DataPersistenceService;
import com.guga.walletserviceapi.service.common.PersistenceSummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovementTransactionService {

    private static final Logger LOGGER = LogManager.getLogger(MovementTransactionService.class);

    private final MovementTransactionRepository moveTrnRepository;
    private final DataPersistenceService importService;

    @Transactional(rollbackFor = Exception.class)
    public PersistenceSummary importMovementTransactions(MultipartFile file) {
        LOGGER.info("MOVEMENT_TRANSACTION_SERVICE_IMPORT_ENTRY | file={}", file.getOriginalFilename());
        PersistenceSummary result = importService.importJsonFromUpload(
            file,
            new TypeReference<List<MovementTransaction>>() {},
            moveTrnRepository
        );
        LOGGER.info("MOVEMENT_TRANSACTION_SERVICE_IMPORT_SUCCESS | file={}", file.getOriginalFilename());
        return result;
    }
}
