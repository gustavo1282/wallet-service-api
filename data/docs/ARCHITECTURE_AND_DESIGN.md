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

## 📚 Referências de Arquitetura

- Clean Architecture (Robert C. Martin)
- Domain-Driven Design (Eric Evans)
- Microservices Patterns (Sam Newman)
- Spring Best Practices Documentation