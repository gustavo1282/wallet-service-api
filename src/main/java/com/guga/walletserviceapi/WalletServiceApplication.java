package com.guga.walletserviceapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
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

/**
     * 🚨 BEAN TEMPORÁRIO DE DEPURAÇÃO
     * Remove este método antes do commit final.
     */
    @Bean
    public CommandLineRunner imprimirTesteConexaoVault(
            // Tenta injetar a chave. Se não achar, imprime "FALHA_NA_LEITURA"
            @Value("${jwt.secret:falha.jwt.secret}") String vaultJtwSecret,
			@Value("${spring.datasource.url:falha.spring.datasource.url}")	String vaultSpringDatasourceUrl,
			@Value("${spring.datasource.username:falha.spring.datasource.username}")	String vaultSpringDatasourceUsername,
			@Value("${spring.datasource.password:falha.spring.datasource.password}")	String vaultSpringDatasourcePassword
    ) {
        return args -> {
            System.out.println("##################################################");
            System.out.println("# 🕵️ DEBUG VAULT (HOMOLOGAÇÃO)");
            System.out.println("# Chave 'vaultJtwSecret': " + vaultJtwSecret);
			System.out.println("# Chave 'vaultSpringDatasourceUrl': " + vaultSpringDatasourceUrl);
			System.out.println("# Chave 'vaultSpringDatasourceUsername': " + vaultSpringDatasourceUsername);
			System.out.println("# Chave 'vaultSpringDatasourcePassword': " + vaultSpringDatasourcePassword);
            System.out.println("##################################################");
        };
    }
}
