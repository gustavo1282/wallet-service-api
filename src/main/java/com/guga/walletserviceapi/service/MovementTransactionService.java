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
import com.guga.walletserviceapi.model.MovementTransaction;
import com.guga.walletserviceapi.repository.MovementTransactionRepository;

@Service
public class MovementTransactionService {

    @Autowired
    private MovementTransactionRepository movementTransactionRepository;

    /***
     * Função responsável por realizar carga inicial de dados para a tabela correspondente a partir de um 
     * arquivo no formato JSON
     * @param file
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadCsvAndSave(MultipartFile file) throws Exception {
        long regId = 0;
        try {
            ObjectMapper mapper = FileUtils.instanceObjectMapper();

            TypeReference<List<MovementTransaction>> movementTypeRef = new TypeReference<List<MovementTransaction>>() { };

            List<MovementTransaction> movements = mapper.readValue(file.getInputStream(), movementTypeRef);

            for (int i = 0; i < movements.size(); i += GlobalHelper.BATCH_SIZE) {

                int end = Math.min(movements.size(), i + GlobalHelper.BATCH_SIZE);
                
                List<MovementTransaction> lote = movements.subList(i, end);

                movementTransactionRepository.saveAll(lote);

            }
        } catch (Exception e) {
            throw new ResourceBadRequestException(e.getMessage());
        }
    }

}
