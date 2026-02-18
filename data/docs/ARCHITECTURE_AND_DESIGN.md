# Architecture and Design

Documentação da arquitetura, padrões de design e decisões técnicas do Wallet Service API.

## 📐 Visão Geral da Arquitetura

O Wallet Service API segue uma arquitetura em camadas (Layered Architecture) com separação clara de responsabilidades:

```
┌─────────────────────────────────────────────────────────────┐
│                    REST Controllers                          │
│   (AuthController, CustomerController, TransactionController)│
└────────────────────┬────────────────────────────────────────┘
                     │
┌─────────────────────▼────────────────────────────────────────┐
│              Service Layer (Business Logic)                   │
│ (CustomerService, WalletService, TransactionService)          │
└────────────────────┬────────────────────────────────────────┘
                     │
┌─────────────────────▼────────────────────────────────────────┐
│          Repository Layer (Data Access)                       │
│  (Spring Data JPA - CustomerRepository, WalletRepository)     │
└────────────────────┬────────────────────────────────────────┘
                     │
┌─────────────────────▼────────────────────────────────────────┐
│           Database Layer (PostgreSQL/H2)                      │
│              (Persistence & Storage)                          │
└──────────────────────────────────────────────────────────────┘
                     │
┌─────────────────────▼────────────────────────────────────────┐
│           Infrastructure & Security                           │
│      (HashiCorp Vault, OTel Collector, Jaeger, Tempo)         │
└──────────────────────────────────────────────────────────────┘
```

## 🏗️ Componentes Principais

### 1. **Controller Layer** (Apresentação)

Responsável por:
- Processar requisições HTTP
- Validar entrada de dados
- Serializar respostas JSON
- Gerenciar autenticação/autorização

**Controllers Principais:**

| Controller | Funcionalidade |
|-----------|---------------|
| `AuthController` | Autenticação JWT (login, register, refresh) |
| `CustomerController` | CRUD de clientes |
| `WalletController` | CRUD de carteiras |
| `TransactionController` | Operações financeiras |
| `WalletOperatorController` | Operações em lote (uploads CSV) |
| `ParamAppController` | Gerenciamento de parâmetros |

**Padrão: MVC com REST**
- Anotações: `@RestController`, `@RequestMapping`, `@GetMapping`, etc.
- Validação: `@Valid` com Jakarta Bean Validation
- Serialização: Jackson com `@JsonProperty`

### 2. **Service Layer** (Lógica de Negócio)

Responsável por:
- Implementar regras de negócio
- Coordenar operações entre repositórios
- Validações complexas
- Tratamento de erros

**Services Principais:**

| Service | Responsabilidades |
|---------|------------------|
| `CustomerService` | Criar, atualizar, listar, filtrar clientes |
| `WalletService` | Gerenciar carteiras, saldos, importar CSV |
| `TransactionService` | Processar depósitos, saques, transferências |
| `ParamAppService` | Gerenciar parâmetros de configuração |
| `JwtService` | Gerar, validar e gerenciar tokens JWT |
| `DepositSenderService` | Gerenciar remetentes de depósitos |
| `DataPersistenceService` | Orquestração de importação/exportação de dados (CSV/JSON) |

**Padrão: Dependency Injection**
```java
@Service
@RequiredArgsConstructor  // Lombok - injeta constructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    
    public Customer saveCustomer(Customer customer) {
        // validação
        return customerRepository.save(customer);
    }
}
```

### 3. **Repository Layer** (Acesso a Dados)

Responsável por:
- Operações CRUD no banco de dados
- Consultas customizadas (Query Methods)
- Abstração do banco de dados

**Repositories:**

```java
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCpf(String cpf);
    Page<Customer> findByStatus(Status status, Pageable pageable);
}
```

**Uso de Spring Data JPA:**
- Herança de `JpaRepository<T, ID>`
- Métodos derivados automáticos
- Suporte a paginação e ordenação

### 4. **Model Layer** (Entidades)

Representam dados persistidos e transferências entre camadas.

**Entidades Principais:**

```
Customer
├── id
├── name
├── email
├── cpf (PK)
├── status (ACTIVE, INACTIVE)
└── timestamps

Wallet
├── id
├── customerId (FK)
├── balance
├── walletType
├── status
└── timestamps

Transaction (Base)
├── id
├── walletId
├── amount
├── type
├── status
├── timestamps
└── Subtypes:
    ├── DepositMoney
    ├── WithdrawMoney
    ├── TransferMoneySend
    └── TransferMoneyReceived
```

**Padrão: JPA Entity**
```java
@Entity
@Table(name = "customers")
@Data  // Lombok: gera getters, setters, toString, etc
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;
    
    @Column(unique = true, nullable = false)
    private String cpf;
    
    @Enumerated(EnumType.STRING)
    private Status status;
}
```

### 5. **Security Layer** (Autenticação/Autorização)

Responsável por:
- Geração e validação de JWT
- Configuração de Spring Security
- Proteção de endpoints

**Componentes:**

| Componente | Responsabilidade |
|-----------|-----------------|
| `JwtService` | Gerar tokens, validar, extrair claims |
| `SpringSecurityConfig` | Configurar filtros, AuthenticationManager |
| `JwtAuthenticationFilter` | Interceptor para validação de tokens |
| `SecurityMatchers` | Configuração centralizada de rotas públicas/privadas via properties |

**Fluxo de Autenticação:**
```
1. POST /api/auth/login
2. AuthenticationManager.authenticate(username, password)
3. JwtService.generateAccessToken(username)
4. Retorna {accessToken, refreshToken}
5. Cliente envia Authorization: Bearer {token}
6. JwtAuthenticationFilter valida e autentica
```

### 6. **Exception Handling**

Tratamento centralizado de erros com `GlobalExceptionHandler`.

**Exceções Customizadas:**

| Exceção | Descrição | HTTP Status |
|---------|-----------|-------------|
| `ResourceNotFoundException` | Recurso não encontrado | 404 |
| `ResourceBadRequestException` | Requisição inválida | 400 |
| `InsufficientBalanceException` | Saldo insuficiente | 400 |
| `CustomerException` | Erro específico de cliente | 400 |
| `TransactionException` | Erro em transação | 400 |
| `WalletException` | Erro de carteira | 400 |

**Uso:**
```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(
    ResourceNotFoundException ex, 
    HttpServletRequest request) {
    return ResponseEntity.status(404)
        .body(new ErrorResponse("Resource not found", ex.getMessage()));
}
```

## 🔄 Fluxos de Negócio Principais

### Fluxo 1: Autenticação de Usuário

```
┌─────────────────┐
│  Client Request │
│  username/pwd   │
└────────┬────────┘
         │ POST /api/auth/login
         ▼
┌─────────────────────────────────────┐
│  AuthController.login()             │
└─────────────────┬───────────────────┘
                  │
         ┌────────▼──────────┐
         │ AuthenticationMgr │
         │ .authenticate()   │
         └────────┬──────────┘
                  │ (válida credenciais)
         ┌────────▼──────────┐
         │   JwtService      │
         │ generateAccessToken│
         └────────┬──────────┘
                  │
         ┌────────▼──────────┐
         │  Return Tokens    │
         │ {access, refresh} │
         └───────────────────┘
```

### Fluxo 2: Criar Transação de Depósito

```
┌─────────────────────────────────────┐
│ POST /api/transactions/transaction  │
│   ?type=DEPOSIT                     │
└────────────┬────────────────────────┘
             │
┌────────────▼──────────────────────┐
│ TransactionController             │
│ .createNewDepositMoneyTransaction │
└────────────┬─────────────────────┘
             │
┌────────────▼──────────────────────┐
│ TransactionService                │
│ .saveDepositMoney()               │
│ - Validar wallet                  │
│ - Validar amount                  │
│ - Atualizar balance               │
└────────────┬─────────────────────┘
             │
┌────────────▼──────────────────────┐
│ WalletRepository.save()           │
│ DepositMoneyRepository.save()      │
└────────────┬─────────────────────┘
             │
┌────────────▼──────────────────────┐
│ Database Persistence              │
│ (INSERT INTO deposits, wallets)   │
└──────────────────────────────────┘
```

### Fluxo 3: Transferência Entre Carteiras

```
┌──────────────────────────────────┐
│ POST /api/transactions/transaction│
│   ?type=TRANSFER_SEND             │
└────────────┬─────────────────────┘
             │
┌────────────▼──────────────────────┐
│ TransactionService                │
│ .saveTransferMoneySend()          │
│ 1. Validar wallets (send/receive) │
│ 2. Validar saldo (send wallet)    │
│ 3. Criar TransferMoneySend        │
│ 4. Criar TransferMoneyReceived    │
│ 5. Atualizar saldos (ambas)       │
└────────────┬─────────────────────┘
             │
┌────────────▼──────────────────────┐
│ Repository Operations             │
│ - WalletRepository.save() x2      │
│ - TransferRepository.save() x2    │
└────────────┬─────────────────────┘
             │
┌────────────▼──────────────────────┐
│ Database (ACID Transaction)       │
│ All or Nothing (rollback on error)│
└──────────────────────────────────┘
```

## 🎯 Padrões de Design Utilizados

### 1. **Dependency Injection (DI)**

Framework: Spring Framework
```java
@Service
@RequiredArgsConstructor  // Lombok
public class CustomerService {
    private final CustomerRepository repository;
    // Constructor injetado automaticamente
}
```

### 2. **Repository Pattern**

Abstração do acesso a dados
```java
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCpf(String cpf);
    List<Customer> findByStatus(Status status);
}
```

### 3. **Service Layer Pattern**

Lógica de negócio centralizada
```java
@Service
public class CustomerService {
    public Customer saveCustomer(Customer customer) {
        validate(customer);
        return repository.save(customer);
    }
}
```

### 4. **Strategy Pattern**

Diferentes tipos de transações
```java
public abstract class Transaction {
    // Base class
}

public class DepositMoney extends Transaction {
    // Implementação específica
}

public class WithdrawMoney extends Transaction {
    // Implementação específica
}
```

### 5. **DTO Pattern** (Data Transfer Object)

Separação entre persistência e API
```java
@Data
public class CustomerDTO {
    private Long id;
    private String name;
    // Sem dependências JPA
}
```

### 6. **Singleton Pattern**

Services e Repositories são singletons Spring
```java
@Service  // Criado uma única vez por contexto
public class CustomerService { ... }
```

### 7. **Builder Pattern**

Construção de objetos complexos
```java
Customer customer = Customer.builder()
    .name("João")
    .cpf("12345678901")
    .email("joao@example.com")
    .build();
```

## 🗄️ Design de Banco de Dados

### Normalização

- **Forma Normal: 3NF** (Third Normal Form)
- Minimizar redundância
- Integridade referencial

### Principais Tabelas

**customers**
```sql
CREATE TABLE customers (
    customer_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**wallets**
```sql
CREATE TABLE wallets (
    wallet_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    wallet_type VARCHAR(50),
    balance DECIMAL(18,2) DEFAULT 0,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

CREATE INDEX idx_wallets_customer_id ON wallets(customer_id);
CREATE INDEX idx_wallets_status ON wallets(status);
```

**transactions** (Herança de tipo única)
```sql
CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dtype VARCHAR(50),  -- Discriminator para JPA
    FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id)
);

CREATE INDEX idx_transactions_wallet_id ON transactions(wallet_id);
CREATE INDEX idx_transactions_date ON transactions(created_at);
```

### Relacionamentos

```
┌──────────────┐
│  Customers   │ 1 ─── * Wallets
└──────────────┘
       │
       └─ Unique: cpf, email
       
┌──────────────┐
│   Wallets    │ 1 ─── * Transactions
└──────────────┘
       │
       └─ Multiple: customer_id, status
       
┌──────────────┐
│ Transactions │
└──────────────┘
       │
       ├─ DepositMoney
       ├─ WithdrawMoney
       ├─ TransferMoneySend
       └─ TransferMoneyReceived
```

## 🔒 Segurança (Defense in Depth)

### Camadas de Proteção

1. **Autenticação JWT**
   - Token gerado no login
   - Validação em cada requisição
   - TTL configurável

2. **Spring Security**
   - Proteção CSRF (por padrão)
   - Password encoding (BCrypt)
   - Controle de acesso por role

3. **HTTPS/TLS**
   - Criptografia de transporte (production)

4. **Validação de Input**
   - Jakarta Bean Validation
   - Escapar especiais (XSS)

5. **SQL Injection Prevention**
   - Prepared Statements (JPA)
   - Parameterized Queries

6. **Gestão de Segredos (Vault)**
   - Centralização de credenciais (Banco, JWT)
   - Rotação de segredos dinâmica
   - Políticas de acesso granular

## 📊 Padrões de Escalabilidade

### Caching

```java
@Cacheable(value = "customers", key = "#customerId")
public Customer getCustomerById(Long customerId) { ... }
```

Configuração em `application.yml`:
```yaml
spring:
  cache:
    type: simple  # ConcurrentHashMap em memória
```

Potencial upgrade para Redis em produção:
```yaml
spring:
  cache:
    type: redis
```

### Paginação

Todas as listas retornam dados paginados:
```java
Page<Customer> customers = service.list(
    PageRequest.of(0, 25, Sort.by("createdAt"))
);
```

### Batch Processing

Upload de CSV em lote:
```yaml
spring:
  jpa:
    properties:
      jdbc.batch_size: 1000
      order_inserts: true
      order_updates: true
```

## 🚀 Performance Optimization

### Database Queries

**Lazy Loading vs Eager Loading**
```java
@OneToMany(fetch = FetchType.LAZY)  // Carrega sob demanda
private List<Wallet> wallets;
```

**Query Projections**
```java
@Query("SELECT w.walletId, w.balance FROM Wallet w")
List<WalletDTO> findAllOptimized();
```

**Índices**
- PK: `id`
- FK: `customer_id`, `wallet_id`
- Busca: `status`, `cpf`, `email`
- Range: `created_at`, `updated_at`

### HTTP Caching

```java
// ETag/Last-Modified (implementar se necessário)
response.setHeader("Cache-Control", "max-age=300");  // 5 minutos
```

## 📈 Monitoramento e Observabilidade

### Logs Estruturados

```java
log.info("Customer created", Map.of(
    "customerId", customer.getId(),
    "cpf", customer.getCpf()
));
```

### Métricas (Prometheus)

```java
@Timed(value = "transaction.process")
public Transaction processTransaction(...) { ... }
```

### Health Checks

```java
GET /actuator/health
```

## 🔄 Versionamento de API

Estratégia: Versionamento por URL
```
/api/v1/customers
/api/v2/customers (mudança incompatível)
```

Ou por header:
```
Accept: application/vnd.wallet.v1+json
```


## 📝 Auditoria e Rastreamento

### Conceito
A aplicação passa a contar com um **módulo dedicado de auditoria**, responsável por registrar o contexto das operações realizadas.  
Esse módulo garante transparência e rastreabilidade em transações críticas, como movimentações de carteira e operações financeiras.

### Componentes
| Componente            | Responsabilidade |
|-----------------------|-----------------|
| `AuditContextFactory` | Criação e propagação do contexto de auditoria |
| `AuditEvent`          | Representação de eventos auditáveis |
| `AuditModule`         | Centralização das regras de auditoria |

### Fluxo de Auditoria
```
┌───────────────────────────────┐
│ Controller recebe requisição  │
└───────────────┬───────────────┘
│
┌───────────────▼───────────────┐
│ AuditContextFactory cria      │
│ contexto de auditoria         │
└───────────────┬───────────────┘
│
┌───────────────▼───────────────┐
│ Operação registrada com       │
│ usuário, roles e traceId      │
└───────────────┬───────────────┘
│
┌───────────────▼───────────────┐
│ Evento auditável persistido   │
│ ou publicado                  │
└───────────────────────────────┘
```

---

## 🔍 Observabilidade e TraceId

### Conceito
Cada requisição agora recebe um **TraceId** único, permitindo correlação entre logs, auditoria e respostas da API.  
Esse identificador é fundamental para rastreabilidade ponta a ponta em ambientes distribuídos.

### Componentes
| Componente          | Responsabilidade |
|---------------------|-----------------|
| `TraceIdInjector`   | Injeção de identificador único em cada requisição |
| `OpenTelemetryConfig` | Configuração para rastreabilidade distribuída |
| `StructuredLogger`  | Logs estruturados com traceId |

### Fluxo de Observabilidade
```
┌───────────────────────────────┐
│ Requisição recebe TraceId     │
└───────────────┬───────────────┘
│
┌───────────────▼───────────────┐
│ TraceId propagado em          │
│ controllers, services, repos  │
└───────────────┬───────────────┘
│
┌───────────────▼───────────────┐
│ Logs e auditoria incluem      │
│ TraceId                       │
└───────────────┬───────────────┘
│
┌───────────────▼───────────────┐
│ OpenTelemetry correlaciona    │
│ dados distribuídos            │
└───────────────────────────────┘
```

---

## 🔐 Atualização da Security Layer

### Fluxo JWT
O fluxo de autenticação JWT foi redesenhado para maior clareza e consistência:
- `JwtAuthenticationDetails` como fonte única do contexto autenticado  
- `JwtAuthenticatedUserProvider` padroniza acesso ao usuário  
- `@PreAuthorize` avaliado após autenticação JWT  
- Revisão dos filtros e providers, com configuração ajustada dos `SecurityMatchers`

### Novo Fluxo

1. POST /api/auth/login
2. AuthenticationManager.authenticate(username, password)
3. JwtService.generateAccessToken(username)
4. JwtAuthenticationDetails armazena contexto autenticado
5. JwtAuthenticatedUserProvider fornece usuário às camadas superiores
6. @PreAuthorize avalia permissões


---

## 🔄 Fluxo 4: Operação de Carteira com Auditoria e TraceId

```
┌───────────────────────────────────────────┐
│ POST /api/wallet/deposit                  │
│ {walletId, amount}                        │
└───────────────────┬───────────────────────┘
│
┌───────────────────▼───────────────────────┐
│ WalletController                          │
│ .deposit()                                │
│ - Recebe requisição autenticada           │
│ - Injeta TraceId                          │
└───────────────────┬───────────────────────┘
│
┌───────────────────▼───────────────────────┐
│ AuditContextFactory                       │
│ - Cria contexto de auditoria              │
│ - Associa usuário, roles e TraceId        │
└───────────────────┬───────────────────────┘
│
┌───────────────────▼───────────────────────┐
│ WalletService                             │
│ - Valida wallet e saldo                   │
│ - Executa operação de depósito            │
│ - Propaga contexto de auditoria           │
└───────────────────┬───────────────────────┘
│
┌───────────────────▼───────────────────────┐
│ TransactionRepository                     │
│ - Persiste transação                      │
│ - Inclui TraceId e dados de auditoria     │
└───────────────────┬───────────────────────┘
│
┌───────────────────▼───────────────────────┐
│ Auditoria & Logs                          │
│ - Evento auditável registrado             │
│ - Log estruturado com TraceId             │
│ - OpenTelemetry correlaciona operação     │
└───────────────────────────────────────────┘
```

---

## 🔄 Fluxo 5: Transferência Entre Carteiras com Auditoria e TraceId
```
┌───────────────────────────────────────────┐
│ POST /api/wallet/transfer                 │
│ {sourceWalletId, targetWalletId, amount}  │
└───────────────────┬───────────────────────┘
│
┌───────────────────▼───────────────────────┐
│ WalletController                          │
│ .transfer()                               │
│ - Recebe requisição autenticada           │
│ - Injeta TraceId                          │
└───────────────────┬───────────────────────┘
│
┌───────────────────▼───────────────────────┐
│ AuditContextFactory                       │
│ - Cria contexto de auditoria              │
│ - Associa usuário, roles e TraceId        │
└───────────────────┬───────────────────────┘
```

## � Novo Fluxo de Segurança e Autenticação (v0.2.7)

**Características:**
- Refatoração completa do fluxo JWT com `JwtAuthenticationDetails`.
- Integração nativa entre autenticação e auditoria.
- Padronização do acesso ao contexto do usuário autenticado.

**Fluxo Atualizado:**

```
HTTP Request → JwtAuthenticationFilter → JwtAuthenticationProvider → JwtAuthenticationDetails → SecurityContext
    ↓
AuditContextFactory (contexto de auditoria)
    ↓
Service Layer (regras de negócio)
    ↓
Repository Layer (persistência)
```

## �📚 Referências de Arquitetura

- Clean Architecture (Robert C. Martin)
- Domain-Driven Design (Eric Evans)
- Microservices Patterns (Sam Newman)
- Spring Best Practices Documentation

---

## Atualizacao (fev/2026) - Estrutura modular do `src` e suporte operacional em `data/`

### Estrutura principal de codigo (`src/main/java/com/guga/walletserviceapi`)

O projeto evoluiu para uma organizacao mais modular por responsabilidade:

- `audit/publisher`
- `config` e `config/web`
- `controller`
- `exception`
- `handler`
- `helpers`
- `infrastructure/data`
- `logging`
- `model` (`converter`, `dto`, `enums`, `request`, `serializers`)
- `repository`
- `security` (`auth`, `context`, `filter`, `handler`, `jwt`)
- `seeder`
- `service/common`

Essa segmentacao reduz acoplamento entre camadas, melhora rastreabilidade dos fluxos e facilita manutencao incremental de funcionalidades.

### Organizacao de artefatos de apoio (`data/`)

Foi consolidado como padrao de projeto manter artefatos de apoio em `data/`, incluindo:

- `data/docs` (documentacao tecnica)
- `data/postman` (collections, templates e credenciais de teste)
- `data/scripts` (scripts utilitarios e automacao)
- `data/grafana` (dashboards/provisionamento versionado)

Elementos de infraestrutura de runtime permanecem na raiz (ex.: `vault/`, `grafana-tempo/`, `prometheus/`, `docker-compose.yml`), mantendo separacao clara entre codigo de aplicacao, apoio de engenharia e orquestracao.

### Newman na arquitetura de qualidade

A validacao E2E via Newman integra a este desenho como camada de qualidade funcional:

- Collection: `data/postman/postman_wallet_collection.json`
- Credenciais de teste: `data/postman/login_test_credentials.json`
- Execucao local: `data/scripts/newman/register_and_run.sh`
- Execucao via Docker: `data/scripts/newman/run_newman_docker.sh`
- Relatorios: `data/scripts/newman/reports/newman/*.xml` (JUnit)

Com isso, os testes de API ficam versionados, reproduziveis e prontos para publicacao no CI/CD.
