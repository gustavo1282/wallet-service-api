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

    //private final SeedLoader seedLoader;
    //private final JsonSeedLoader jsonSeedLoader; 
    private final ApplicationContext context;

    ///public SeedExecutor(SeedLoader seedLoader, ApplicationContext context) {
    //    this.seedLoader = seedLoader;
    //    this.context = context;
    //}

    public <T> void loadJSONAndSaveRepository(String filePath, Class<T> clazz) {
        List<T> items;
        
        try {
            items = FileUtils.loadJSONToListObject(filePath, clazz); //jsonSeedLoader.loadList(filePath, clazz);
        } catch (Exception e) {
            System.out.println("Problemas ao carregar: " + filePath + "\\n" + e.getMessage());
            return;
        }

        String repositoryBeanName = Introspector.decapitalize(clazz.getSimpleName()) + "Repository";

        // 2. Descobre o repository correspondente automaticamente
        JpaRepository<T, ?> repository = (JpaRepository<T, ?>)
                context.getBean(repositoryBeanName);


        if (repository.count() == 0) {
            // 3. Salva tudo    
            repository.saveAll(items);
        }
    }
}
