package com.guga.walletserviceapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

@EnableCaching
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
//@ComponentScan(basePackages = "com.guga.walletserviceapi")
public class WalletServiceApplication {

	public static void main(String[] args) {

		System.out.println("Applicação Wallet Service API inicializada com sucesso.");

		SpringApplication.run(WalletServiceApplication.class, args);
	}
    
}
