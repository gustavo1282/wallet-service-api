# 💼 Wallet Service

Serviço de carteira digital desenvolvido em **Java 17 + Spring Boot 3.4.10**, responsável por gerenciar saldos e transações de usuários (depósito, saque e transferência), com rastreabilidade completa e observabilidade integrada.

---

## 👤 Autor

**Gustavo Souza (Guga)**  
📧 [gustavo1282@gmail.com](mailto:gustavo1282@gmail.com)  
🔗 [LinkedIn](https://www.linkedin.com/in/gustavo-souza-68b34335/) | [GitHub](https://github.com/gustavo1282)

---

## 🧩 Objetivo Geral

Criar um **serviço de carteira digital (wallet service)** que permita:
- Gerenciar saldos e movimentações financeiras entre usuários;
- Garantir **transações ACID**, **integridade dos dados** e **rastreabilidade total**;
- Disponibilizar métricas e logs estruturados para auditoria e observabilidade.

---

## ⚙️ Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 3.4.10**
- **Spring Data JPA (Hibernate)**
- **PostgreSQL 15.3-alpine**
- **Prometheus v2.51.0**
- **Grafana 10.2.3**
- **Docker & Docker Compose**

---

## 📋 Requisitos (Resumo)

> Detalhamento completo em [ARCHITECTURE_AND_DESIGN.md](./ARCHITECTURE_AND_DESIGN.md#requisitos).

### Requisitos Funcionais
- Criar e consultar carteiras;
- Consultar saldo atual e histórico;
- Depositar, sacar e transferir fundos.

### Requisitos Não Funcionais
- Alta disponibilidade;
- Auditoria completa de transações;
- Persistência e consistência de dados.

---

## 🔒 Boas Práticas e Considerações Técnicas

- Uso de **transações atômicas (@Transactional)**;
- **Lock pessimista** para evitar concorrência no saldo;
- **Logs de auditoria** completos (tabela `Transaction`);
- **Testes unitários e de integração** com JUnit + Testcontainers;
- **DTOs e Services** para separação de responsabilidades.

---

## ⚙️ Configurações Importantes

Arquivo `application.yml` (trecho):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://host.docker.internal:5432/wallet_db
    username: wallet_user
    password: wallet_pass
```

Prometheus coleta métricas em:
```
http://host.docker.internal:8080/actuator/prometheus
```

---

## 🔗 Endpoints Principais

> Caminho base: `/api`

| Método   | Endpoint                                       | Descrição                        |
|:---------|:-----------------------------------------------|:---------------------------------|
| POST     | `/wallets`                                     | Criar carteira para um cliente   |
| GET      | `/wallets/{id}`                                | Consultar carteira               |
| GET      | `/wallets/{id}/balance`                        | Consultar saldo atual            |
| GET      | `/wallets/{id}/balance/history?at={timestamp}` | Consultar saldo histórico        |
| POST     | `/wallets/{id}/deposit`                        | Depositar fundos                 |
| POST     | `/wallets/{id}/withdraw`                       | Sacar fundos                     |
| POST     | `/wallets/transfer`                            | Transferir entre carteiras       |
| CRUD     | `/customers`                                   | CRUD de clientes                 |
| CRUD     | `/transactions`                                | Histórico de transações          |

📘 **Swagger / OpenAPI:**  
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 🧪 Testes e Integração Contínua

Executar todos os testes:
```bash
./mvnw test
```

### Stack de Testes
- **Testes Unitários:** JUnit 5 + Mockito
- **Testes de Integração:** Spring Boot Test + Testcontainers (Postgres)
- **Pipeline CI (sugestão):**
    - `mvnw clean verify`
    - build da imagem Docker
    - push para o registry (se aplicável)

---

## 🗄️ Banco de Dados e Migrations

- Recomendado: **Flyway** ou **Liquibase** para versionar o schema.
- Em desenvolvimento: `spring.jpa.hibernate.ddl-auto=update` (não recomendado para produção).
- Tabelas principais:
    - `customer`
    - `wallet`
    - `transaction`

---

## 📊 Observabilidade

Métricas e healthchecks expostos via Spring Boot Actuator:

| Componente      | Endpoint / Porta                                                                       | Descrição                    |
|-----------------|----------------------------------------------------------------------------------------|------------------------------|
| **Prometheus**  | [http://localhost:9090](http://localhost:9090)                                         | Coleta métricas da aplicação |
| **Grafana**     | [http://localhost:3000](http://localhost:3000)                                         | Dashboards e alertas         |
| **API Metrics** | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | Métricas da aplicação        |

Mais detalhes técnicos em [ARCHITECTURE_AND_DESIGN.md](./ARCHITECTURE_AND_DESIGN.md#observabilidade).

---

## 🚀 Quickstart

Subir todo o ambiente (Postgres + API + Observabilidade):
```bash
./start_dev_app.sh
```

## ⚙️ Perfis de Execução (Profiles)

O projeto utiliza perfis Spring Boot para gerenciar ambientes:

| Cenário                         | Forma Mais Clara/Usual        | Onde Configurar                     | Exemplo                            |
|:--------------------------------|:------------------------------|:------------------------------------|:-----------------------------------|
| **Testes (JUnit)**              | Anotação Java                 | Na classe de teste                  | `@ActiveProfiles("test")`          |
| **Desenvolvimento Local (IDE)** | VM Options                    | Run Configuration do IDE.           | `-Dspring.profiles.active=desenv`  |
| **Validação via Docker**        | Variável de Ambiente (S.O.)   | No Dockerfile/docker-compose ou K8s | `SPRING_PROFILES_ACTIVE=homolog`   |
| **Containers (Docker/Prod)**    | Variável de Ambiente (S.O.)   | No Dockerfile/docker-compose ou K8s | `SPRING_PROFILES_ACTIVE=homolog`   |


### Como ativar um profile

#### Via terminal
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=desenv


Rodar apenas o serviço localmente:
```bash
./mvnw spring-boot:run
```

#### Via Docker Compose
```bash
environment:
- SPRING_PROFILES_ACTIVE=homolog
```

#### Via variável de ambiente (produção)
```bash
export SPRING_PROFILES_ACTIVE=prod
```

### Cada profile possui um arquivo de configuração específico:
- application-desenv.yml
- application-homolog.yml
- application-prod.yml

---

## 📚 Documentação Complementar

| Documento                                                  | Descrição                               |
|------------------------------------------------------------|-----------------------------------------|
| [ARCHITECTURE_AND_DESIGN.md](./ARCHITECTURE_AND_DESIGN.md) | Design técnico e decisões arquiteturais |
| [CONTRIBUTING.md](./CONTRIBUTING.md)                       | Como contribuir e rodar localmente      |
