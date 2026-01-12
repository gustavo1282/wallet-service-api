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

		System.out.println("JWT_SECRET=" + System.getenv("JWT_SECRET"));
		System.out.println("VAULT_URI=" + System.getenv("SPRING_CLOUD_VAULT_URI"));


		SpringApplication.run(WalletServiceApplication.class, args);
	}

}
