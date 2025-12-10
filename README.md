
## 👨‍💻 Autor

**Gustavo1282**  
[GitHub](https://github.com/gustavo1282) | [LinkedIn](https://www.linkedin.com/in/gustavo-souza-68b34335/)


# Wallet Service API

A **Wallet Service** API é um serviço pensado para facilitar a criação e operação de uma carteira digital.
Com autenticação segura, controle de clientes e processamento de transações, a API fornece uma base sólida para produtos financeiros modernos.

## 🎯 Visão Geral

O Wallet Service API é um microserviço moderno que fornece:

- **Autenticação e Autorização** via JWT (jjwt 0.12.6)
- **Gerenciamento de Clientes** com validação e filtros por status
- **Gerenciamento de Carteiras** com operações de saldo
- **Transações Financeiras** (Depósito, Saque, Transferência)
- **Upload de Dados** via CSV com processamento em lote
- **Documentação Interativa** via Swagger/OpenAPI
- **Observabilidade** com Prometheus e métricas
- **Suporte Multiplataforma** com Docker e Docker Compose

## 📋 Requisitos do Sistema

- **Java**: 21 (JDK 21+)
- **Maven**: 3.9.11+
- **Banco de Dados**: PostgreSQL 15.3+ ou H2 (desenvolvimento)
- **Docker**: 20.10+ (opcional, para containerização)
- **Git**: Para controle de versão

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

#### Opção A: Com Docker e Vault (Recomendado)

Inicia a aplicação e o servidor Hashicorp Vault para carregar as configurações.

```bash
# Para garantir que o ambiente seja recriado com as configurações do Vault
docker compose up -d --build --force-recreate
```

#### Opção B: Localmente

Roda com o perfil local (sem Vault), usando as configurações do application-local.yml.

```bash
# Certifique-se de que PostgreSQL está rodando na porta 5432
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Ou no Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### 4. Acessar a Aplicação

- **API Principal**: [http://localhost:8080/wallet-services-api/api](http://localhost:8080/wallet-services-api/api)
- **Swagger UI**: [http://localhost:8080/wallet-services-api/swagger-ui.html](http://localhost:8080/wallet-services-api/swagger-ui.html)
- **Health Check**: [http://localhost:8080/wallet-services-api/actuator/health](http://localhost:8080/wallet-services-api/actuator/health)
- **Métricas Prometheus**: [http://localhost:8080/wallet-services-api/actuator/prometheus](http://localhost:8080/wallet-services-api/actuator/prometheus)


## ⚙️ Configuração e Teste de CI/CD Local

Para validar o pipeline de CI/CD (.github/workflows/ci-cd.yml) localmente, utilizamos a ferramenta act, que simula o runner do GitHub Actions usando Docker.

### 1. Instalação do act (Windows)
O act deve ser instalado usando o gerenciador de pacotes Scoop, pois ele garante a correta configuração do $PATH.

### A. Instalar o Scoop (Se necessário)
Execute os comandos a seguir em um PowerShell (Terminal) sem privilégios de Administrador:

```bash
#PowerShell >>

Set-ExecutionPolicy RemoteSigned -Scope CurrentUser 
irm get.scoop.sh | iex
```

### B. Instalar o act
Após a instalação do Scoop, instale o act:

```bash
#PowerShell >>

scoop install act
```

### 2. Integração com VS Code (Opcional, mas Recomendado)

Para uma melhor experiência de depuração, instale a extensão no Visual Studio Code:

- Plugin VSC: GitHub Actions Runner (Permite rodar jobs do act diretamente na interface do VS Code).

### 3. Validação e Execução do Pipeline Local

Para simular o pipeline build_and_test usando seu código local, execute o comando no terminal (no diretório raiz do projeto):

```bash
# Roda o job 'build_and_test' simulando um evento push na branch atual
act push -j build_and_test
```


## 🐳 Docker e Orquestração

Comandos de Gestão

| Comando | Descrição |
| :--- | :--- |
| `docker compose up -d` | Inicia todos os serviços definidos no `docker-compose.yml` em segundo plano (detached mode). |
| `docker compose up -d --build --force-recreate` | **Atualiza:** Para, remove, reconstrói (se houver alteração no Dockerfile) e recria todos os serviços com as configurações mais recentes do `docker-compose.yml`. |
| `docker compose up -d [NOME_DO_SERVIÇO]` | Inicia apenas um serviço específico e suas dependências necessárias. |
| `docker compose down` | Para e remove todos os containers, redes e volumes criados pelo Compose (limpeza total). |
| `docker compose stop` | Apenas para os containers em execução, mantendo-os no disco. |
| `docker compose stop [NOME_DO_SERVIÇO]` | Para um serviço específico. |
| `docker compose ps` | Lista o status atual de todos os serviços definidos. |


### Configurações de Ambiente

As variáveis de ambiente podem ser definidas em docker-compose.yml ou em arquivos .env. O perfil de execução é definido pela variável SPRING_PROFILES_ACTIVE.

## 📦 Dependências Principais

| Tecnologia | Versão | Propósito |
|------------|--------|----------|
| Spring Boot | 3.4.10 | Framework principal |
| Spring Security | Latest | Autenticação e autorização |
| jjwt | 0.12.6 | Geração e validação de JWT |
| Spring Data JPA | Latest | ORM e acesso a dados |
| PostgreSQL | 15.3+ | Banco de dados produção |
| Springdoc OpenAPI | 2.8.0 | Documentação API |
| Micrometer/Prometheus | Latest | Métricas e observabilidade |
| Lombok | 1.18.30 | Redução de boilerplate |
| JaCoCo | 0.8.12 | Cobertura de testes |
| SonarQube | 3.10.0 | Análise de código |


## 🏗️ Estrutura do Projeto

```
wallet-service-api/
├── src/
│   ├── main/
│   │   ├── java/com/guga/walletserviceapi/
│   │   │   ├── WalletServiceApplication.java      # Entry point
│   │   │   ├── config/                            # Configurações Spring
│   │   │   ├── controller/                        # REST Controllers
│   │   │   ├── model/                             # Entidades JPA
│   │   │   ├── repository/                        # Data Access Layer
│   │   │   ├── service/                           # Business Logic
│   │   │   ├── security/                          # JWT e Segurança
│   │   │   ├── exception/                         # Exception Handling
│   │   │   └── helpers/                           # Utilitários
│   │   └── resources/
│   │       ├── application.yml                    # Configuração padrão
│   │       ├── application-local.yml              # Config local
│   │       └── static/                            # Assets estáticos
│   └── test/                                      # Testes unitários
├── data/
│   ├── docs/                                      # Documentação técnica
│   ├── schema-*.sql                               # Scripts DDL
│   └── seed/                                      # Dados de seed
├── prometheus/                                    # Configurações Prometheus
├── Dockerfile                                     # Build multi-stage
├── docker-compose.yml                            # Orquestração
└── pom.xml                                        # Dependências Maven

```

## 🔐 Autenticação

A API utiliza **JWT (JSON Web Tokens)** para autenticação. 

### Fluxo de Login

1. **POST** `/api/auth/login`
   ```json
   {
     "username": "usuario",
     "password": "senha"
   }
   ```

2. Resposta com tokens:
   ```json
   {
     "accessToken": "eyJhbGc...",
     "refreshToken": "eyJhbGc..."
   }
   ```

3. Usar `accessToken` no header:
   ```
   Authorization: Bearer {accessToken}
   ```

### Endpoints de Autenticação

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/login` | Autenticar usuário |
| POST | `/api/auth/register` | Registrar novo usuário |
| POST | `/api/auth/refresh` | Renovar access token |

## 📚 Documentação Técnica

Documentação detalhada está disponível em `data/docs/`:

- **[API_REFERENCE.md](data/docs/API_REFERENCE.md)** - Endpoints e exemplos de uso
- **[ARCHITECTURE_AND_DESIGN.md](data/docs/ARCHITECTURE_AND_DESIGN.md)** - Padrões e arquitetura
- **[DATA_MODEL.md](data/docs/DATA_MODEL.md)** - Schema e relacionamentos
- **[BUILD_AND_CI.md](data/docs/BUILD_AND_CI.md)** - Pipeline e deployment
- **[SECURITY.md](data/docs/SECURITY.md)** - Segurança e compliance
- **[OBSERVABILITY.md](data/docs/OBSERVABILITY.md)** - Logs, métricas e health checks
- **[CONTRIBUTING.md](data/docs/CONTRIBUTING.md)** - Guia de contribuição

## 🧪 Testes

### Executar Testes Unitários

```bash
./mvnw test
```

### Gerar Relatório de Cobertura

```bash
./mvnw test jacoco:report
# Relatório em: target/site/jacoco/index.html
```

### Análise com SonarQube

```bash
./mvnw clean package sonar:sonar \
  -Dsonar.projectKey=com.gugawallet:wallet-service-api \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=wallet-service-api-sonar-token
```

## 🐳 Docker

### Build e Deploy

```bash
# Build da imagem (automaticamente via docker-compose)
docker build -t wallet_service:latest .

# Executar container
docker-compose up -d

# Logs da aplicação
docker-compose logs -f wallet-service-api

# Parar containers
docker-compose down
```

### Configurações de Ambiente

As variáveis de ambiente podem ser definidas em `docker-compose.yml`:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: local  # Perfil: dev, local, cloud
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wallet_db
  SPRING_DATASOURCE_USERNAME: wallet_user
  SPRING_DATASOURCE_PASSWORD: wallet_pass
  SPRING_JPA_HIBERNATE_DDL_AUTO: update  # create-drop, validate, update
```

## 📊 Observabilidade

### Endpoints de Actuator

```
GET  /actuator/health           # Status da aplicação
GET  /actuator/info             # Informações da app
GET  /actuator/metrics          # Lista de métricas
GET  /actuator/prometheus       # Métricas em formato Prometheus
```

### Métricas Disponíveis

- `http_requests_total` - Total de requisições HTTP
- `http_request_duration_seconds` - Duração das requisições
- `db_connections_active` - Conexões ativas no banco
- `jvm_memory_used_bytes` - Memória JVM utilizada

### Prometheus

Arquivo de configuração: `prometheus/prometheus.yml`

```bash
# Acessar Prometheus (se rodando)
http://localhost:9090
```

## 🤝 Contribuindo

Leia [CONTRIBUTING.md](data/docs/CONTRIBUTING.md) para diretrizes de contribuição.

### Processo de Contribuição

1. Crie uma branch feature: `git checkout -b feature/meu-recurso`
2. Commit suas mudanças: `git commit -am 'Adiciona novo recurso'`
3. Push para a branch: `git push origin feature/meu-recurso`
4. Abra um Pull Request

## 📝 Versionamento

Este projeto segue [Semantic Versioning](https://semver.org/):

- **Major** (X.0.0): Mudanças incompatíveis na API
- **Minor** (0.X.0): Novas funcionalidades compatíveis
- **Patch** (0.0.X): Correções de bugs

Versão atual: **0.2.4-SNAPSHOT**

## 📄 Licença

Este projeto está sob licença [MIT](LICENSE). Veja o arquivo LICENSE para detalhes.

## 📞 Suporte

Para problemas, dúvidas ou sugestões:

1. Abra uma [Issue](https://github.com/gustavo1282/wallet-service-api/issues)
2. Consulte a [Documentação Técnica](data/docs/)
3. Verifique logs e métricas no Prometheus

## ✅ Changelog

Veja [CHANGELOG.md](CHANGELOG.md) para histórico de versões.

## 🗺️ Roadmap

- [ ] Implementar refresh token com expiração configurável
- [ ] Autenticação OAuth2 com Google/GitHub
- [ ] Criptografia de dados sensíveis
- [ ] Cache distribuído (Redis)
- [ ] Message broker (RabbitMQ)
- [ ] Testes de integração E2E
- [ ] Dashboard de administração