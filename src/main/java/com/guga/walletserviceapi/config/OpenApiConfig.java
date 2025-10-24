package com.guga.walletserviceapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 1. Nome interno do esquema de segurança
        final String securitySchemeName = "basicAuth";

        return new OpenAPI()
                .info(new Info().title("Wallet Service API")
                        .version("v1.0")
                        .description(
                          """
                          Documentação da API de Carteira de Clientes.
                          
                          **IMPORTANTE:** Todos os endpoints requerem autenticação **HTTP Basic**.
                          
                          1.  Clique no botão **"Authorize"** no topo da página.
                          2.  Use as credenciais de teste: **Usuário: `user`** | **Senha: `password`**
                          3.  Feche a janela e execute os métodos.
                          """
                        )
                )

                // 2. Adiciona o esquema de segurança aos componentes
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .in(SecurityScheme.In.HEADER)   // Onde a chave é enviada (Header, Query ou Cookie)

                                        .name(securitySchemeName)
                                        .scheme("basic") // O protocolo que você usa (.httpBasic())

                                        //.scheme("bearer")
                                        //.bearerFormat("JWT") // Informa que o token é um JWT

                                        //.type(SecurityScheme.Type.APIKEY)
                                        //.name("X-API-KEY") // O nome da chave no cabeçalho

//                                        new SecurityScheme()
//                                                .type(SecurityScheme.Type.OAUTH2)
//                                                .flows(new OAuthFlows()
//                                                        .implicit(new OAuthFlow()
//                                                                .authorizationUrl("http://authserver.com/oauth/authorize")
//                                                                .scopes(new Scopes().addString("read", "Permissão de leitura"))
//                                                        )
//                                                )


                        )
                )

                // 3. Aplica este esquema de segurança globalmente a TODOS os endpoints
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

}
