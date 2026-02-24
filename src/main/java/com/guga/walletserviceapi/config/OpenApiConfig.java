package com.guga.walletserviceapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Value("${app.api-prefix:}")
    private String servletPath;

    @Value("${springdoc.info.title:Wallet Service API}")
    private String apiTitle;

    @Value("${springdoc.info.description:Descrição da API}")
    private String apiDescription;

    @Value("${springdoc.info.version:1.0.0}")
    private String apiVersion;

    private String packageController = "com.guga.walletserviceapi.controller";


    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(servletPath, HandlerTypePredicate.forBasePackage(packageController)
        );
    }

    @Bean
    public OpenAPI walletOpenAPI() {
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title(apiTitle)
                .description(apiDescription)
                .version(apiVersion))
            .components(
                new Components().addSecuritySchemes("bearerAuth",
                    new SecurityScheme().name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            );
    }
    
}
