package com.guga.walletserviceapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

import com.guga.walletserviceapi.logging.LogMarkers;

@EnableCaching
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
//@ComponentScan(basePackages = "com.guga.walletserviceapi")
public class WalletServiceApplication {

    private static final Logger LOGGER = LogManager.getLogger(WalletServiceApplication.class);

	public static void main(String[] args) {
		LOGGER.info(LogMarkers.LOG, "Applicação Wallet Service API inicializada com sucesso.");
		
		SpringApplication.run(WalletServiceApplication.class, args);
	}
    
}
