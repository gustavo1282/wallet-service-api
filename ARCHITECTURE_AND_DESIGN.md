# 🏗️ Wallet Service — Arquitetura e Design

---

## 📋 Visão Geral

O **Wallet Service** é um microserviço de carteira digital responsável por gerenciar saldos e transações financeiras (depósito, saque e transferência).  
Garante **consistência de dados, auditabilidade** e **observabilidade** por meio do **Prometheus**, **Grafana** e **Spring Boot Actuator**.

---

## 🧩 Modelo de Domínio

### Entidades

#### Customer
| Campo     | Tipo          | Descrição           |
|-----------|---------------|---------------------|
| id        | Long          | Identificador único |
| name      | String        | Nome do cliente     |
| email     | String        | E-mail do cliente   |
| createdAt | LocalDateTime | Data de criação     |

#### Wallet
| Campo                 | Tipo          | Descrição                       |
|-----------------------|---------------|---------------------------------|
| id                    | Long          | Identificador da carteira       |
| customerId            | Long          | Chave estrangeira para Customer |
| balance               | BigDecimal    | Saldo atual                     |
| previousBalance       | BigDecimal    | Saldo anterior                  |
| createdAt / updatedAt | LocalDateTime | Datas de criação e atualização  |

#### Transaction
| Campo        | Tipo          | Descrição                                         |
|--------------|---------------|---------------------------------------------------|
| id           | Long          | ID da transação                                   |
| walletId     | Long          | Carteira relacionada                              |
| type         | Enum          | DEPOSIT / WITHDRAWAL / TRANSFER_IN / TRANSFER_OUT |
| amount       | BigDecimal    | Valor da transação                                |
| balanceAfter | BigDecimal    | Saldo após a transação                            |
| referenceId  | String        | ID compartilhado em transferências                |
| timestamp    | LocalDateTime | Momento da execução                               |

---

## ⚙️ Arquitetura

O serviço segue o padrão de **arquitetura em camadas** usando **Spring Boot**:

```
[Controller]  →  [Service]  →  [Repository]  →  [Database]
                     ↓
              [Event / Audit Log]
```

| Camada | Responsabilidade |
|--------|------------------|
| **Controller** | Gerencia endpoints REST (requisições HTTP JSON). |
| **Service** | Contém a lógica de negócio e as regras transacionais. |
| **Repository** | Persiste entidades via Spring Data JPA. |
| **Database** | Armazenamento relacional durável (PostgreSQL). |

A API é stateless e escalável horizontalmente.

---

## 🔄 Fluxo de Transação — Exemplo (Transferência)

1. Valida as carteiras de origem e destino.
2. Inicia uma **transação de banco** (`@Transactional`).
3. Aplica **lock pessimista** nas duas carteiras.
4. Verifica saldo suficiente.
5. Deduz do remetente e adiciona ao destinatário.
6. Registra duas transações (`TRANSFER_OUT` e `TRANSFER_IN`).
7. Comita a transação.

Garante **atomicidade**, **consistência** e **auditabilidade**.

---

## ⚖️ Concorrência e Transações

- **@Transactional** garante atomicidade.
- **@Lock(LockModeType.PESSIMISTIC_WRITE)** evita race conditions.
- **Isolation:** `READ_COMMITTED`.
- **Rollback:** exceções geram rollback automático.
- **Idempotência:** `referenceId` previne duplicações.

---

## 🧮 Banco de Dados

```
Customer (1) ─── (1) Wallet ─── (N) Transaction
```

- ORM: **Hibernate / JPA**
- Banco: **PostgreSQL 15.3-alpine**
- Versionamento: **Flyway**

---

## 📊 Observabilidade

### Métricas e Monitoramento

- Endpoints expostos via **Spring Actuator**:
    - `/actuator/health`
    - `/actuator/info`
    - `/actuator/metrics`
    - `/actuator/prometheus`

### Prometheus
```yaml
scrape_configs:
  - job_name: 'wallet-service'
    scrape_interval: 5s
    static_configs:
      - targets: ['wallet-service:8080']
```

### Grafana
- Fonte de dados: Prometheus (`http://prometheus:9090`)
- Dashboards: latência de API, erros 5xx, uso de memória JVM, conexões DB

### Logs
- Estruturados (SLF4J + Logback), com `traceId`
- Futuro: integração com **OpenTelemetry**

---

## 🔐 Segurança e Boas Práticas

| Área                    | Descrição                            |
|-------------------------|--------------------------------------|
| **Autenticação**        | Pronto para OAuth2 / Keycloak        |
| **Segredos**            | Usar variáveis de ambiente           |
| **Validação**           | DTOs com `@Valid`                    |
| **Tratamento de Erros** | Centralizado com `@ControllerAdvice` |
| **Logs**                | Sem dados sensíveis                  |
| **Transações**          | Atômicas e auditáveis                |

---

## 🧪 Estratégia de Testes

| Tipo           | Ferramenta                        | Objetivo                       |
|----------------|-----------------------------------|--------------------------------|
| **Unitário**   | JUnit 5 + Mockito                 | Testar regras de negócio       |
| **Integração** | Spring Boot Test + Testcontainers | Validar persistência e API     |
| **Smoke**      | Testes pós-deploy                 | Verificar endpoints principais |

---

## 🚀 Deploy e Dockerização

- **Docker Compose** sobe:
    - `wallet-service`
    - `postgres`
    - `prometheus`
    - `grafana`
- Script principal: `./start_dev_app.sh`

---

## 🧠 Melhorias Futuras

| Área                | Atual                | Futuro                        |
|---------------------|----------------------|-------------------------------|
| **Concorrência**    | Lock pessimista      | Otimista / Event sourcing     |
| **Persistência**    | Relacional           | CQRS / NoSQL híbrido          |
| **Observabilidade** | Prometheus + Grafana | OpenTelemetry                 |
| **Segurança**       | Básica               | OAuth2 / JWT                  |
| **Escalabilidade**  | Stateless            | Fila de mensagens assíncronas |

---

## 📚 Documentos Relacionados

- [README.md](./README.md)
- [CONTRIBUTING.md](./CONTRIBUTING.md)
