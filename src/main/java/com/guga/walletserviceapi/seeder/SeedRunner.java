package com.guga.walletserviceapi.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class SeedRunner implements CommandLineRunner {

    private final SeedExecutor executor;
    private final SeedOrderConfig config;

//    public SeedRunner(SeedExecutor executor, SeedOrderConfig config) {
 //       this.executor = executor;
 //       this.config = config;
 //   }

    @Override
    public void run(String... args) throws Exception {

        for (SeedDefinition seed : config.orderedSeeds()) {
            executor.loadJSONAndSaveRepository(seed.fileName(), seed.entityClass());
            System.out.println("✔ Seed carregado: " + seed.fileName());
        }
    }

}
