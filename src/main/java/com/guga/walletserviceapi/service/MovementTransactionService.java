package com.guga.walletserviceapi.service;

import java.util.List;

import org.springframework.stereotype.Service;
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

    private final MovementTransactionRepository moveTrnRepository;

    private final DataPersistenceService importService;

    public PersistenceSummary importMovementTransactions(MultipartFile file) {
        return importService.importJsonFromUpload(file, new TypeReference<List<MovementTransaction>>() {}, moveTrnRepository);
    }

}
