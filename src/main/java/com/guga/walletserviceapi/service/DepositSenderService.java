package com.guga.walletserviceapi.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.repository.DepositSenderRepository;
import com.guga.walletserviceapi.service.common.DataImportService;
import com.guga.walletserviceapi.service.common.ImportSummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepositSenderService implements IWalletApiService{

    private final DepositSenderRepository depositSenderRepository;

    private final ParamAppService paramAppService;

    private final DataImportService importService;


    public DepositSender saveDepositSender(DepositSender depositSender) {
        depositSender.setSenderId(nextIdGenerate());
        DepositSender depositSenderSaved = depositSenderRepository.save(depositSender);
        return depositSenderSaved;
    }

    @Override
    public Long nextIdGenerate() {
        return paramAppService
            .getNextSequenceId(ParamApp.SEQ_DEPOSIT_SENDER_ID)
            .getValueLong();
    }


    public ImportSummary importDeposits(MultipartFile file) {
        return importService.importJson(file, new TypeReference<List<DepositSender>>() {}, depositSenderRepository);
    }
    
}
