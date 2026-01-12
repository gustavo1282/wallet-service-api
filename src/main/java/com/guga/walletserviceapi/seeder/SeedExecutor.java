package com.guga.walletserviceapi.seeder;

import java.beans.Introspector;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;

import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.logging.LogMarkers;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SeedExecutor {

    private static final Logger LOGGER = LogManager.getLogger(SeedExecutor.class);

    private final ApplicationContext context;

    public <T> void loadJSONAndSaveRepository(String filePath, Class<T> clazz) {
        try {
            List<T> items = FileUtils.loadJSONToListObject(filePath, clazz);
            
            String repositoryBeanName = Introspector.decapitalize(clazz.getSimpleName()) + "Repository";
            
            JpaRepository<T, ?> repository = (JpaRepository<T, ?>)context.getBean(repositoryBeanName);
            
             if (repository.count() == 0) {                
                repository.saveAll(items);
            }

        } catch (TransactionSystemException ex) {
            // Tentativa de obter a mensagem da causa raiz
            String message = ex.getRootCause().getMessage();

            LOGGER.error(LogMarkers.LOG, "System Exception ao gravar dados do arquivo : {} {}", 
                filePath, message
            );

        } catch (Exception e) {

            String message = e.getMessage();

            LOGGER.error(LogMarkers.LOG, "Exception ao gravar dados do arquivo : {} {}", 
                filePath, message
            );            

        }
    }
}
