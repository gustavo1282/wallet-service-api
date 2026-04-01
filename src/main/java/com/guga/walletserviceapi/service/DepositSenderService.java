package com.guga.walletserviceapi.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.repository.DepositSenderRepository;
import com.guga.walletserviceapi.service.common.DataPersistenceService;
import com.guga.walletserviceapi.service.common.PersistenceSummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepositSenderService implements IWalletApiService {

    private static final Logger LOGGER = LogManager.getLogger(DepositSenderService.class);

    private final DepositSenderRepository depositSenderRepository;
    private final ParamAppService paramAppService;
    private final DataPersistenceService importService;

    @Transactional(rollbackFor = Exception.class)
    public DepositSender saveDepositSender(DepositSender depositSender) {
        LOGGER.info("DEPOSIT_SENDER_SERVICE_SAVE_ENTRY");

        Long nextId = nextIdGenerate();
        depositSender.setSenderId(nextId);
        LOGGER.info("DEPOSIT_SENDER_SERVICE_SAVE_DECISION | generatedSenderId={}", nextId);

        DepositSender saved = depositSenderRepository.save(depositSender);
        LOGGER.info("DEPOSIT_SENDER_SERVICE_SAVE_SUCCESS | senderId={}", saved.getSenderId());
        return saved;
    }

    @Override
    public Long nextIdGenerate() {
        return paramAppService
            .getNextSequenceId(ParamApp.SEQ_DEPOSIT_SENDER_ID)
            .getValueLong();
    }

    @Transactional(rollbackFor = Exception.class)
    public PersistenceSummary importDeposits(MultipartFile file) {
        LOGGER.info("DEPOSIT_SENDER_SERVICE_IMPORT_ENTRY | file={}", file.getOriginalFilename());
        PersistenceSummary result = importService.importJsonFromUpload(
            file,
            new TypeReference<List<DepositSender>>() {},
            depositSenderRepository
        );
        LOGGER.info("DEPOSIT_SENDER_SERVICE_IMPORT_SUCCESS | file={}", file.getOriginalFilename());
        return result;
    }
}
