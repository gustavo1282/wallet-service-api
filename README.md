# Wallet Service API

Uma API REST robusta construida com **Spring Boot 3.4.10** para gerenciar o ciclo de vida completo de um servico de carteira digital. Inclui autenticacao JWT, operacoes de transacoes, gerenciamento de clientes e carteiras, com suporte a multiplos bancos de dados e integracao com Prometheus para observabilidade.

## 🎯 Visão Geral

O Wallet Service API e um microsservico moderno que fornece:

- **Autenticacao e Autorizacao** via JWT (jjwt 0.12.6)
- **Gerenciamento de Clientes** com validacao e filtros por status
- **Gerenciamento de Carteiras** com operacoes de saldo
- **Transacoes Financeiras** (Deposito, Saque, Transferencia)
- **Upload de Dados** via CSV com processamento em lote
- **Documentacao Interativa** via Swagger/OpenAPI
- **Gestao de Segredos** com HashiCorp Vault
- **Observabilidade** com Prometheus e metricas
- **Suporte Multiplataforma** com Docker e Docker Compose

## 📋 Requisitos do Sistema

- **Java**: 21 (JDK 21+)
- **Maven**: 3.9.11+
- **Banco de Dados**: PostgreSQL 15.3+ ou H2 (desenvolvimento)
- **Docker**: 20.10+ (opcional, para containerizacao)
- **Git**: Para controle de versao
- **Newman**: 6+ (opcional, para testes E2E da collection Postman)
- **jq**: 1.6+ (opcional, para script de registro/login automatico)

## 🚀 Como Começar

### 1. Clonar o Repositório

```bash
git clone https://github.com/gustavo1282/wallet-service-api.git
cd wallet-service-api
git checkout feature/v0.2.4-autentication-jwt
```

### 2. Compilar o Projeto

```bash
# Usando Maven diretamente
./mvnw clean package -DskipTests

# Ou no Windows
mvnw.cmd clean package -DskipTests
```

### 3. Executar a Aplicação

O projeto suporta dois modos de execucao: **Local** (para desenvolvimento) e **Docker** (para ambientes completos).

#### 💻 Desenvolvimento Local (Contribuidores)

Ideal para desenvolvimento diario. Requer PostgreSQL rodando localmente na porta `5432`.

```bash
# Executar com perfil local
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### 4. Acessar a Aplicação

Apos subir os servicos com `docker-compose up -d`, acesse:

#### Aplicação Principal
- **API Principal**: [http://localhost:8080/wallet-service-api/api](http://localhost:8080/wallet-service-api/api)
- **Swagger UI**: [http://localhost:8080/wallet-service-api/swagger-ui.html](http://localhost:8080/wallet-service-api/swagger-ui.html)
- **Health Check**: [http://localhost:8080/wallet-service-api/actuator/health](http://localhost:8080/wallet-service-api/actuator/health)
- **Metricas Prometheus (da app)**: [http://localhost:8080/wallet-service-api/actuator/prometheus](http://localhost:8080/wallet-service-api/actuator/prometheus)

#### Observabilidade
- **Prometheus**: [http://localhost:9090](http://localhost:9090)
- **Grafana**: [http://localhost:3000](http://localhost:3000) (admin/admin)
- **Jaeger**: [http://localhost:16686](http://localhost:16686)
- **OpenTelemetry Collector**: [http://localhost:8889/metrics](http://localhost:8889/metrics)

#### Banco de Dados
- **PgAdmin**: [http://localhost:5050](http://localhost:5050) (admin@postgres.com / wallet_pass)

## 🏗️ Estrutura do Projeto

```text
wallet-service-api/
├── src/
│   ├── main/
│   │   ├── java/com/guga/walletserviceapi/
│   │   │   ├── WalletServiceApplication.java      # Entry point
│   │   │   ├── config/                            # Configuracoes Spring
│   │   │   ├── controller/                        # REST Controllers
│   │   │   ├── model/                             # Entidades JPA
│   │   │   ├── repository/                        # Data Access Layer
│   │   │   ├── service/                           # Business Logic
│   │   │   ├── security/                          # JWT e Seguranca
│   │   │   ├── exception/                         # Exception Handling
│   │   │   └── helpers/                           # Utilitarios
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── static/
│   └── test/
├── data/
│   ├── docs/                                      # Documentacao tecnica
│   ├── postman/                                   # Collection Postman e templates
│   ├── scripts/grafana/                           # Backup e utilitarios do Grafana
│   ├── scripts/newman/                            # Execucao Newman local/docker
│   │   ├── register_and_run.sh
│   │   ├── run_newman_docker.sh
│   │   └── reports/newman/
│   ├── schema-*.sql
│   └── seed/
├── prometheus/
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## 📦 Dependências Principais

| Tecnologia | Versao | Proposito |
|------------|--------|----------|
| Spring Boot | 3.4.10 | Framework principal |
| Spring Security | Latest | Autenticacao e autorizacao |
| jjwt | 0.12.6 | Geracao e validacao de JWT |
| Spring Data JPA | Latest | ORM e acesso a dados |
| PostgreSQL | 15.3+ | Banco de dados producao |
| Springdoc OpenAPI | 2.8.0 | Documentacao API |
| Micrometer/Prometheus | Latest | Metricas e observabilidade |
| Micrometer Tracing | Latest | Tracing automatico via Spring Boot |
| OpenTelemetry Collector | 0.99.0 | Coleta e processamento de traces/metricas |
| Jaeger | 1.60 | Visualizacao de traces distribuidos |
| Grafana | 10.2.3 | Dashboards de monitoramento |
| Lombok | 1.18.30 | Reducao de boilerplate |
| JaCoCo | 0.8.12 | Cobertura de testes |
| SonarQube | 3.10.0 | Analise de codigo |

## 🔐 Autenticação

A API utiliza **JWT (JSON Web Tokens)** para autenticacao.

### Fluxo de Login

1. **POST** `/api/auth/login`
2. Recebe `accessToken` e `refreshToken`
3. Envia no header:
   `Authorization: Bearer {accessToken}`

### Endpoints de Autenticação

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| POST | `/api/auth/login` | Autenticar usuario |
| POST | `/api/auth/register` | Registrar novo usuario |
| POST | `/api/auth/refresh` | Renovar access token |

## 📚 Documentação Técnica

Documentacao detalhada disponivel em `data/docs/`:

- **[API_REFERENCE.md](data/docs/API_REFERENCE.md)**
- **[ARCHITECTURE_AND_DESIGN.md](data/docs/ARCHITECTURE_AND_DESIGN.md)**
- **[ARQUITECTURE_DECISIONS-ADR.md](data/docs/ARQUITECTURE_DECISIONS-ADR.md)**
- **[DATA_MODEL.md](data/docs/DATA_MODEL.md)**
- **[BUILD_AND_CI.md](data/docs/BUILD_AND_CI.md)**
- **[GUIDE_DOCKER.md](data/docs/GUIDE_DOCKER.md)**
- **[SECURITY.md](data/docs/SECURITY.md)**
- **[OBSERVABILITY.md](data/docs/OBSERVABILITY.md)** - inclui backup do Grafana (`backup_grafana.sh`)
- **[CONTRIBUTING.md](data/docs/CONTRIBUTING.md)**
- **[ROADMAP.md](data/docs/ROADMAP.md)**

## 🧪 Testes

### Executar Testes Unitários

```bash
./mvnw test
```

### Gerar Relatório de Cobertura

```bash
./mvnw test jacoco:report
# Relatorio em: target/site/jacoco/index.html
```

### Análise com SonarQube

```bash
./mvnw clean package sonar:sonar \
  -Dsonar.projectKey=com.gugawallet:wallet-service-api \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=wallet-service-api-sonar-token
```

### Testes E2E com Newman (Postman Collection)

A automacao E2E usa a collection versionada em:

- `data/postman/postman_wallet_collection.json`

#### Opção A: Execução local (Newman instalado)

```bash
bash data/scripts/newman/register_and_run.sh http://localhost:8080/wallet-service-api
```

Esse script:
- Le credenciais em `data/postman/login_test_credentials.json`
- Realiza `register` (best effort) e `login`
- Injeta `accessToken` no Newman
- Gera relatorio JUnit XML por usuario em `data/scripts/newman/reports/newman`

#### Opção B: Execução via Docker (sem newman local)

```bash
bash data/scripts/newman/run_newman_docker.sh http://host.docker.internal:8080/wallet-service-api postman_wallet_collection.json
```

No Linux/Mac, pode usar `http://localhost:8080/wallet-service-api`.

## 🐳 Docker

### Build e Deploy

```bash
docker build -t wallet_service:latest .
docker-compose up -d
docker-compose logs -f wallet-service-api
docker-compose down
```

### Configurações de Ambiente

Exemplo em `docker-compose.yml`:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: local
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wallet_db
  SPRING_DATASOURCE_USERNAME: wallet_user
  SPRING_DATASOURCE_PASSWORD: wallet_pass
  SPRING_JPA_HIBERNATE_DDL_AUTO: update
```

## 📊 Observabilidade

### Endpoints de Actuator

```text
GET  /actuator/health
GET  /actuator/info
GET  /actuator/metrics
GET  /actuator/prometheus
```

## 🤝 Contribuindo

Leia [CONTRIBUTING.md](data/docs/CONTRIBUTING.md).

## 📝 Versionamento

Este projeto segue [Semantic Versioning](https://semver.org/).

Versao atual: **0.2.4-SNAPSHOT**

## 📄 Licença

Este projeto esta sob licenca [MIT](LICENSE).

## 👨‍💻 Autor

**Gustavo1282**  
[GitHub](https://github.com/gustavo1282) | [LinkedIn](https://www.linkedin.com/in/gustavo-souza-68b34335/)

## 📞 Suporte

1. Abra uma [Issue](https://github.com/gustavo1282/wallet-service-api/issues)
2. Consulte a documentacao em `data/docs/`
3. Verifique logs e metricas no Prometheus

## ✅ Changelog

Veja [CHANGELOG.md](CHANGELOG.md).

## 🗺️ Roadmap

- [ ] Implementar refresh token com expiracao configuravel
- [ ] Autenticacao OAuth2 com Google/GitHub
- [ ] Criptografia de dados sensiveis
- [ ] Cache distribuido (Redis)
- [ ] Message broker (RabbitMQ)
- [ ] Testes de integracao E2E
- [ ] Dashboard de administracao
