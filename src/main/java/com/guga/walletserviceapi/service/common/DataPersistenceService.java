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
import com.guga.walletserviceapi.helpers.GlobalHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataPersistenceService {

    private final ObjectMapper mapper;

    /**
     * MÉTODO CORE: O "motor" de conversão. 
     * Aceita InputStream, o que o torna compatível com arquivos locais e uploads.
     */
    public <T> List<T> parseInputStreamToList(java.io.InputStream is, TypeReference<List<T>> typeRef) {
        try {
            return mapper.readValue(is, typeRef);
        } catch (Exception e) {
            throw new ResourceBadRequestException("Erro na análise dos dados: " + e.getMessage());
        }
    }

    public <T> List<T> importJson(String filePath, TypeReference<List<T>> typeRef) {
        try {
            java.io.File file = new java.io.File(filePath);
            
            if (!file.exists()) {
                throw new RuntimeException("Arquivo não encontrado no sistema de arquivos: " + filePath);
            }

            java.io.InputStream is = new java.io.FileInputStream(file);

            if (is == null) throw new RuntimeException("Arquivo não encontrado: " + filePath);

            return mapper.readValue(is, typeRef);
            
        } catch (Exception e) {
            throw new ResourceBadRequestException("Erro na análise dos dados: " + e.getMessage());
        }
    }    

    /**
     * MÉTODO PARA UPLOAD: Utilizado pela Controller (Produção).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> PersistenceSummary importJsonFromUpload(MultipartFile file, TypeReference<List<T>> typeRef, JpaRepository<T, ?> repository) {
        try {
            List<T> data = parseInputStreamToList(file.getInputStream(), typeRef);
            return executeBatchSave(data, repository);
        } catch (Exception e) {
            throw new ResourceBadRequestException("Erro ao processar MultipartFile: " + e.getMessage());
        }
    }


    /**
     * MÉTODO PARA ARQUIVOS LOCAIS: Utilizado pelos Testes e Cargas Internas.
     * Recebe o caminho do arquivo (diretório + nome).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> PersistenceSummary importAndBachSaveFromPath(String filePath, TypeReference<List<T>> typeRef, JpaRepository<T, ?> repository) {
        try {
            // Utiliza o ClassLoader para ler do diretório de resources
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
            if (is == null) throw new RuntimeException("Arquivo não encontrado: " + filePath);
            
            List<T> data = parseInputStreamToList(is, typeRef);
            return executeBatchSave(data, repository);
        } catch (Exception e) {
            throw new ResourceBadRequestException("Erro ao processar arquivo local: " + e.getMessage());
        }
    }

    // Lógica de salvamento em lote extraída para evitar duplicidade
    private <T> PersistenceSummary executeBatchSave(List<T> data, JpaRepository<T, ?> repository) {
        int batchSize = GlobalHelper.BATCH_SIZE;
        for (int i = 0; i < data.size(); i += batchSize) {
            int end = Math.min(data.size(), i + batchSize);
            repository.saveAll(data.subList(i, end));
        }
        return PersistenceSummary.success("Dados processados com sucesso", data, data.size());
    }

    /**
     * MÉTODO DE EXPORTAÇÃO: Grava a lista de objetos no caminho fornecido.
     * Aceita caminhos relativos (./data/seed/...) ou absolutos.
     */
    public <T> void exportToJson(List<T> data, String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            
            // Cria a estrutura de pastas (ex: ./data/seed) se ela não existir
            java.io.File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Grava os dados com formatação identada (Pretty Print)
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
            
        } catch (Exception e) {
            throw new ResourceBadRequestException("Erro ao exportar dados para [" + filePath + "]: " + e.getMessage());
        }
    }

}