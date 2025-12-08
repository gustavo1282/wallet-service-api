package com.guga.walletserviceapi.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.DepositSender;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.repository.DepositSenderRepository;

@Service
public class DepositSenderService implements IWalletApiService{

    @Autowired
    private DepositSenderRepository depositSenderRepository;

    @Autowired
    private ParamAppService paramAppService;


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


    /***
     * Função responsável por realizar carga inicial de dados para a tabela correspondente a partir de um 
     * arquivo no formato JSON
     * @param file
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadCsvAndSave(MultipartFile file) throws Exception {
        try {
            ObjectMapper mapper = FileUtils.instanceObjectMapper();

            TypeReference<List<DepositSender>> depositSenderTypeRef = new TypeReference<List<DepositSender>>() { };

            List<DepositSender> depositSenders = mapper.readValue(file.getInputStream(), depositSenderTypeRef);

            for (int i = 0; i < depositSenders.size(); i += GlobalHelper.BATCH_SIZE) {

                int end = Math.min(depositSenders.size(), i + GlobalHelper.BATCH_SIZE);
                
                List<DepositSender> lote = depositSenders.subList(i, end);

                depositSenderRepository.saveAll(lote);

            }
        } catch (Exception e) {
            throw new ResourceBadRequestException(e.getMessage());
        }
    }

}
