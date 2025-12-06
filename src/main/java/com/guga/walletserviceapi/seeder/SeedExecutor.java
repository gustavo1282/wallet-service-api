package com.guga.walletserviceapi.seeder;

import java.beans.Introspector;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.guga.walletserviceapi.helpers.FileUtils;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SeedExecutor {

    private final ApplicationContext context;

    public <T> void loadJSONAndSaveRepository(String filePath, Class<T> clazz) {

        try {
            List<T> items = FileUtils.loadJSONToListObject(filePath, clazz); //jsonSeedLoader.loadList(filePath, clazz);
            String repositoryBeanName = Introspector.decapitalize(clazz.getSimpleName()) + "Repository";
            
            JpaRepository<T, ?> repository = (JpaRepository<T, ?>)
                context.getBean(repositoryBeanName);
                
            repository.saveAll(items);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
