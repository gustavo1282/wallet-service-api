package com.guga.walletserviceapi.service.common;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.GlobalHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataImportService {

    // Método Genérico <T> representa a Entidade
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> ImportSummary importJson(MultipartFile file, TypeReference<List<T>> typeRef, JpaRepository<T, ?> repository) {
        try {
            ObjectMapper mapper = FileUtils.instanceObjectMapper();
            List<T> data = mapper.readValue(file.getInputStream(), typeRef);

            int batchSize = GlobalHelper.BATCH_SIZE;

            for (int i = 0; i < data.size(); i += batchSize) {
                int end = Math.min(data.size(), i + batchSize);
                List<T> lote = data.subList(i, end);
                repository.saveAll(lote);
            }

            return new ImportSummary(data.size(), "SUCCESS", "Dados importados com sucesso");
        } catch (Exception e) {
            throw new ResourceBadRequestException("Erro ao processar importação: " + e.getMessage());
        }
    }
}