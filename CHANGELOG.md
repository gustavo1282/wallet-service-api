# Changelog

Todas as alterações notáveis neste projeto serão documentadas neste arquivo.

## [0.2.8-SNAPSHOT] - 2026-01-18

### ⚙️ Infraestrutura & DevOps
- **Dockerização Completa:**
  - Adicionado `Dockerfile` multistage (Build com Maven/Java 21 + Runtime Alpine).
  - Atualizado `docker-compose.yml` para realizar o build local da imagem (`build: .`).
  - Configuração de variáveis de ambiente para `JWT_SECRET` e `SPRING_CLOUD_VAULT_ENABLED` no contexto do container.
- **Configuração do Spring Boot:**
  - Correção do erro `InvalidConfigDataPropertyException` removendo a auto-ativação de perfis em `application-local.yml` e `application-docker.yml`.
  - Atualização dos endpoints do Actuator: substituição de `httptrace` (depreciado) por `httpexchanges`.
  - Configuração do `launch.json` (VS Code) para execução local com perfil correto.

### 🛡️ Segurança
- **JWT & Auth:**
  - Refatoração do `JwtAuthenticationFilter` e `JwtService` para melhor tratamento de exceções e validação.
  - Ajuste em `SecurityMatchers` para carregar corretamente as listas de permissão via `@ConfigurationProperties`.
  - Remoção de `CryptoUtils` (cleanup).

### ♻️ Refatoração
- **Persistência de Dados:**
  - Substituição do `DataImportService` pelo novo `DataPersistenceService` (melhor separação de responsabilidades na importação de dados).
  - Adicionado `PhoneNumberDeserializer` para normalização de telefones nos DTOs/Models.
  - Limpeza de testes de integração antigos (`SecurityIntegrationTest`).

### 📊 Observabilidade
- Ajustes nas configurações do `otel-collector-config.yml` e `prometheus.yml` para garantir a coleta de métricas no ambiente Docker.
