package com.guga.walletserviceapi.seeder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.guga.walletserviceapi.logging.LogMarkers;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class SeedRunner implements CommandLineRunner {
    private static final Logger LOGGER = LogManager.getLogger(SeedRunner.class);

    private final SeedExecutor executor;
    private final SeedOrderConfig config;

    @Override
    public void run(String... args) throws Exception {

        for (SeedDefinition seed : config.orderedSeeds()) {
            executor.loadJSONAndSaveRepository(seed.fileName(), seed.entityClass());
            LOGGER.info(LogMarkers.LOG, "✔ Seed carregado: " + seed.fileName());
        }
    }

}
