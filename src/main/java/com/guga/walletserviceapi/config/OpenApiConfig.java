package com.guga.walletserviceapi.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    final String securitySchemeName = "basicAuth";

    @Bean
    public OpenAPI walletOpenAPI() {

        return new OpenAPI()
            .info(new Info().title("Wallet Service API")
                .version("v1.0")
                .description(
                    """
                    Documentação da API de Carteira de Clientes.

                    ---

                    ### 🔑 Autenticação (A partir da v0.2.4)

                    A autenticação agora utiliza **JSON Web Tokens (JWT)**. Para acessar os endpoints protegidos, siga os passos abaixo:

                    1.  **Obter o Token:**
                        * Execute o endpoint de login: **`POST /api/auth/login`**.
                        * Use suas credenciais de usuário e senha no corpo da requisição (Request Body).

                    2.  **Autorizar o Swagger (OpenAPI):**
                        * Copie o **token JWT** retornado no campo `access_token`.
                        * Clique no botão **"Authorize"** (Autorizar) no topo da página.
                        * Cole o token no campo de autenticação (Geralmente no formato **`Bearer SeuTokenAqui`**).

                    3.  **Executar Métodos:**
                        * Com o token configurado no Swagger, você pode executar todos os métodos protegidos.

                    ---
                    
                    **Credenciais de Teste:**
                    * **Usuário:** `user`
                    * **Senha:** `password`
                    
                    """
                )
            )

            .components(
                new Components()
                    .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                            .name("bearerAuth")
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )

            // ADICIONE ESTA LINHA PARA ATIVAR O TOKEN NO SWAGGER
            //.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))            

            ;        
    }

    @Bean
    public GroupedOpenApi apiV1() {
        return GroupedOpenApi.builder()
            .group("v1")
            .pathsToMatch("/**")  // Alterar de "/api/v1/**" para "/**" para incluir todos os paths
            .addOpenApiCustomizer(openApi -> openApi.info(new Info().title("Wallet API - V1")))
            .build();
    }

}
