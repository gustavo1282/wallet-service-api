package com.guga.walletserviceapi.seeder;

import java.beans.Introspector;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;

import com.guga.walletserviceapi.helpers.FileUtils;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SeedExecutor {

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
            String errorMessage = ex.getRootCause().getMessage();
            
            System.out.println("Erro de Salvamento: " + errorMessage);

        } catch (Exception e) {
            // Captura quaisquer outras exceções
            e.printStackTrace();
        }
    }
}
