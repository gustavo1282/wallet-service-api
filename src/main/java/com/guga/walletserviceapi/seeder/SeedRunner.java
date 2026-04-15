package com.guga.walletserviceapi.seeder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.guga.walletserviceapi.logging.LogMarkers;

@Service
public class SeedRunner implements CommandLineRunner {
    private static final Logger LOGGER = LogManager.getLogger(SeedRunner.class);

    private SeedExecutor executor;
    private SeedOrderConfig config;

    @Value("${app.seeder.enabled:false}")
    private boolean seederEnabled;

    // O construtor é atualizado para injetar a nova propriedade do application.yml
    public SeedRunner(SeedExecutor executor, SeedOrderConfig config) {
        this.executor = executor;
        this.config = config;
    }

    @Override
    public void run(String... args) throws Exception {
        if (seederEnabled) {
            LOGGER.info(LogMarkers.LOG, "Executando seeder de dados via CommandLineRunner (app.seeder.enabled=true)");
            runSeed();
        } else {
            LOGGER.info(LogMarkers.LOG, "Seeder na inicialização está desabilitado (app.seeder.enabled=false).");
        }
    }
    
    public void runSeed() {
        if (seederEnabled) {
            LOGGER.info(LogMarkers.LOG, "Executando seeder de dados via CommandLineRunner (app.seeder.enabled=true)");

            LOGGER.info(LogMarkers.LOG, "Iniciando execução do seeder...");
            for (SeedDefinition seed : config.orderedSeeds()) {
                executor.loadJSONAndSaveRepository(seed.fileName(), seed.entityClass());
                LOGGER.info(LogMarkers.LOG, "✔ Seed carregado: " + seed.fileName());
            }
            LOGGER.info(LogMarkers.LOG, "Execução do seeder finalizada.");
        }
        else {
            LOGGER.info(LogMarkers.LOG, "Seeder na inicialização está desabilitado (app.seeder.enabled=false).");
        }
    }

}
