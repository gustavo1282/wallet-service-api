# Data Model

Documentação do modelo de dados, schema e relacionamentos do Wallet Service API.

## 🗄️ Visão Geral do Banco de Dados

**SGBD Suportado:** PostgreSQL 15.3+ (Produção) | H2 (Desenvolvimento)

**Tipo de Herança JPA:** Single Table Inheritance (Discriminator)

## 📊 Tabelas e Entidades

### 1. CUSTOMERS (Clientes)

Armazena informações dos clientes do sistema.

**Tabela SQL:**
```sql
CREATE TABLE customers (
    customer_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    cpf VARCHAR(11) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_customers_cpf ON customers(cpf);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_status ON customers(status);
```

**Entidade JPA:**
```java
@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(unique = true, nullable = false, length = 255)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(unique = true, nullable = false, length = 11)
    private String cpf;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Wallet> wallets;
}
```

**Campos:**

| Campo | Tipo | Restrições | Descrição |
|-------|------|-----------|----------|
| `customer_id` | BIGINT | PK, AUTO | Identificador único |
| `name` | VARCHAR(255) | NOT NULL | Nome do cliente |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Email (único) |
| `phone` | VARCHAR(20) | - | Telefone de contato |
| `cpf` | VARCHAR(11) | UNIQUE, NOT NULL | CPF (único) |
| `status` | ENUM | NOT NULL, DEFAULT='ACTIVE' | ACTIVE ou INACTIVE |
| `created_at` | TIMESTAMP | NOT NULL | Data de criação |
| `updated_at` | TIMESTAMP | NOT NULL | Data de atualização |

**Enumeração Status:**
```java
public enum Status {
    ACTIVE("Ativo"),
    INACTIVE("Inativo");
}
```

---

### 2. WALLETS (Carteiras)

Armazena carteiras digitais dos clientes.

**Tabela SQL:**
```sql
CREATE TABLE wallets (
    wallet_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    wallet_type VARCHAR(50),
    balance DECIMAL(18,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT chk_balance CHECK (balance >= 0),
    CONSTRAINT chk_wallet_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_wallets_customer_id ON wallets(customer_id);
CREATE INDEX idx_wallets_status ON wallets(status);
CREATE INDEX idx_wallets_created_at ON wallets(created_at);
```

**Entidade JPA:**
```java
@Entity
@Table(name = "wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(name = "customer_id", insertable = false, updatable = false)
    private Long customerId;
    
    @Column(length = 50)
    private String walletType;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL)
    private List<Transaction> transactions;
}
```

**Campos:**

| Campo | Tipo | Restrições | Descrição |
|-------|------|-----------|----------|
| `wallet_id` | BIGINT | PK, AUTO | Identificador único |
| `customer_id` | BIGINT | FK, NOT NULL | Referência ao cliente |
| `wallet_type` | VARCHAR(50) | - | Tipo: SAVINGS, CHECKING, etc |
| `balance` | DECIMAL(18,2) | NOT NULL, >= 0 | Saldo atual |
| `status` | ENUM | NOT NULL | ACTIVE, INACTIVE, SUSPENDED |
| `created_at` | TIMESTAMP | NOT NULL | Data de criação |
| `updated_at` | TIMESTAMP | NOT NULL | Data de atualização |

**Relacionamento:**
```
Customer 1 ---> * Wallet
```
- Cascata de delete: Ao deletar customer, todas wallets são deletadas
- Fetch type: LAZY (carrega sob demanda)

---

### 3. TRANSACTIONS (Transações)

Armazena transações financeiras (Depósito, Saque, Transferência).

**Estratégia de Herança:** Single Table Inheritance

**Tabela SQL:**
```sql
CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Colunas para diferentes tipos
    cpf_sender VARCHAR(11),
    sender_name VARCHAR(255),
    terminal_id VARCHAR(100),
    wallet_id_send BIGINT,
    wallet_id_received BIGINT,
    
    -- Discriminator para JPA
    dtype VARCHAR(50) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id) ON DELETE CASCADE,
    CONSTRAINT chk_transaction_status CHECK (
        status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT chk_amount CHECK (amount > 0)
);

CREATE INDEX idx_transactions_wallet_id ON transactions(wallet_id);
CREATE INDEX idx_transactions_type ON transactions(dtype);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_status ON transactions(status);
```

**Entidade Base:**
```java
@Entity
@Table(name = "transactions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
    
    @Column(name = "wallet_id", insertable = false, updatable = false)
    private Long walletId;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTransaction status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

#### 3.1 DEPOSIT_MONEY (Depósito)

```java
@Entity
@DiscriminatorValue("DEPOSIT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositMoney extends Transaction {
    
    @Column(length = 11)
    private String cpfSender;
    
    @Column(length = 255)
    private String senderName;
    
    @Column(length = 100)
    private String terminalId;
}
```

**Exemplo SQL:**
```sql
INSERT INTO transactions (
    wallet_id, amount, transaction_type, status,
    cpf_sender, sender_name, terminal_id, dtype
) VALUES (
    1, 500.00, 'DEPOSIT', 'COMPLETED',
    '12345678901', 'João Silva', 'TERM001', 'DEPOSIT'
);
```

#### 3.2 WITHDRAW_MONEY (Saque)

```java
@Entity
@DiscriminatorValue("WITHDRAW")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawMoney extends Transaction {
    // Sem campos adicionais
}
```

#### 3.3 TRANSFER_MONEY_SEND (Transferência - Envio)

```java
@Entity
@DiscriminatorValue("TRANSFER_SEND")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferMoneySend extends Transaction {
    
    @Column(name = "wallet_id_received")
    private Long walletIdReceived;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id_received", insertable = false, updatable = false)
    private Wallet walletReceived;
}
```

#### 3.4 TRANSFER_MONEY_RECEIVED (Transferência - Recebimento)

```java
@Entity
@DiscriminatorValue("TRANSFER_RECEIVED")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferMoneyReceived extends Transaction {
    
    @Column(name = "wallet_id_send")
    private Long walletIdSend;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id_send", insertable = false, updatable = false)
    private Wallet walletSend;
}
```

**Enumeração StatusTransaction:**
```java
public enum StatusTransaction {
    PENDING("Pendente"),
    COMPLETED("Concluído"),
    FAILED("Falha"),
    CANCELLED("Cancelado");
}
```

---

### 4. MOVEMENT_TRANSACTIONS (Movimentações)

Armazena histórico detalhado de movimentações.

**Tabela SQL:**
```sql
CREATE TABLE movement_transactions (
    movement_id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    previous_balance DECIMAL(18,2) NOT NULL,
    new_balance DECIMAL(18,2) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE,
    FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id) ON DELETE CASCADE
);

CREATE INDEX idx_movements_transaction_id ON movement_transactions(transaction_id);
CREATE INDEX idx_movements_wallet_id ON movement_transactions(wallet_id);
```

**Entidade JPA:**
```java
@Entity
@Table(name = "movement_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long movementId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal previousBalance;
    
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal newBalance;
    
    @Column(length = 50, nullable = false)
    private String operationType;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

### 5. DEPOSIT_SENDERS (Remetentes de Depósitos)

Armazena informações de remetentes de depósitos.

**Tabela SQL:**
```sql
CREATE TABLE deposit_senders (
    sender_id BIGSERIAL PRIMARY KEY,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sender_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_deposit_senders_cpf ON deposit_senders(cpf);
```

**Entidade JPA:**
```java
@Entity
@Table(name = "deposit_senders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositSender {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long senderId;
    
    @Column(unique = true, nullable = false, length = 11)
    private String cpf;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

---

### 6. PARAM_APP (Parâmetros da Aplicação)

Armazena parâmetros de configuração.

**Tabela SQL:**
```sql
CREATE TABLE param_app (
    id BIGSERIAL PRIMARY KEY,
    param_name VARCHAR(100) UNIQUE NOT NULL,
    param_value VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_param_app_name ON param_app(param_name);
```

**Entidade JPA:**
```java
@Entity
@Table(name = "param_app")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParamApp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String paramName;
    
    @Column(nullable = false, length = 255)
    private String paramValue;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

---

### 7. LOGIN_AUTH (Autenticação)

Armazena informações de login e autenticação.

**Tabela SQL:**
```sql
CREATE TABLE login_auth (
    login_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_auth_username ON login_auth(username);
```

**Entidade JPA:**
```java
@Entity
@Table(name = "login_auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAuth {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loginId;
    
    @Column(unique = true, nullable = false, length = 100)
    private String username;
    
    @Column(nullable = false, length = 255)
    private String password;  // BCrypt encoded
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

---

## 🔗 Relacionamentos (ER Diagram)

```
┌──────────────┐
│  CUSTOMERS   │
├──────────────┤
│ customer_id  │ PK
│ name         │
│ email        │ UNIQUE
│ cpf          │ UNIQUE
│ status       │
│ created_at   │
└──────┬───────┘
       │ 1
       │
       │ *
┌──────▼──────────┐
│    WALLETS      │
├─────────────────┤
│ wallet_id       │ PK
│ customer_id     │ FK
│ balance         │
│ status          │
│ created_at      │
└──────┬──────────┘
       │ 1
       │
       │ *
┌──────▼──────────────────┐
│   TRANSACTIONS          │
├─────────────────────────┤
│ transaction_id          │ PK
│ wallet_id               │ FK
│ amount                  │
│ dtype (Discriminator)   │
│ status                  │
│ [cols específicas]      │
└─────────────────────────┘
       │ 1
       │
       │ *
┌──────▼──────────────────┐
│ MOVEMENT_TRANSACTIONS   │
├─────────────────────────┤
│ movement_id             │ PK
│ transaction_id          │ FK
│ wallet_id               │ FK
│ previous_balance        │
│ new_balance             │
└─────────────────────────┘

┌──────────────────────┐
│  DEPOSIT_SENDERS     │
├──────────────────────┤
│ sender_id            │ PK
│ cpf                  │ UNIQUE
│ name                 │
│ status               │
└──────────────────────┘

┌──────────────────────┐
│    PARAM_APP         │
├──────────────────────┤
│ id                   │ PK
│ param_name           │ UNIQUE
│ param_value          │
│ description          │
└──────────────────────┘

┌──────────────────────┐
│    LOGIN_AUTH        │
├──────────────────────┤
│ login_id             │ PK
│ username             │ UNIQUE
│ password (BCrypt)    │
│ status               │
└──────────────────────┘
```

---

## 📐 Constraints e Validações

### Constraints no Banco

| Constraint | Descrição |
|-----------|-----------|
| PK | Primary Key em IDs |
| FK | Foreign Keys para relacionamentos |
| UNIQUE | Campos únicos (email, cpf, username) |
| NOT NULL | Campos obrigatórios |
| CHECK | Validações de valores (status, amount > 0) |
| ON DELETE CASCADE | Ao deletar customer, deleta wallets |

### Validações em Código (Jakarta Validation)

```java
@Entity
public class Customer {
    
    @NotNull(message = "CPF não pode ser nulo")
    @Size(min = 11, max = 11, message = "CPF deve ter 11 dígitos")
    @Column(unique = true)
    private String cpf;
    
    @Email(message = "Email deve ser válido")
    @NotBlank
    private String email;
    
    @Min(value = 0, message = "Saldo não pode ser negativo")
    private BigDecimal balance;
}
```

---

## 🔄 Transações Bancárias

### Transação de Depósito

```
ANTES:
Wallet(id=1) balance = 1000.00

OPERAÇÃO:
INSERT INTO transactions (...) VALUES (..., 'DEPOSIT', ..., 500.00)
UPDATE wallets SET balance = 1500.00 WHERE wallet_id = 1
INSERT INTO movement_transactions (...) VALUES (..., 1000.00, 1500.00)

DEPOIS:
Wallet(id=1) balance = 1500.00
```

### Transação de Transferência

```
ANTES:
Wallet A: 1000.00
Wallet B: 2000.00

OPERAÇÃO:
INSERT INTO transactions (...) 'TRANSFER_SEND' ... (300.00, wallet_a)
INSERT INTO transactions (...) 'TRANSFER_RECEIVED' ... (300.00, wallet_b)
UPDATE wallets SET balance = 700.00 WHERE wallet_id = A
UPDATE wallets SET balance = 2300.00 WHERE wallet_id = B
INSERT INTO movement_transactions x2

DEPOIS (ou ROLLBACK se erro):
Wallet A: 700.00
Wallet B: 2300.00
```

---

## 📊 Índices para Performance

```sql
-- Busca por CPF
CREATE INDEX idx_customers_cpf ON customers(cpf);

-- Busca por email
CREATE INDEX idx_customers_email ON customers(email);

-- Busca por status
CREATE INDEX idx_customers_status ON customers(status);

-- Carteiras por cliente
CREATE INDEX idx_wallets_customer_id ON wallets(customer_id);

-- Transações por carteira
CREATE INDEX idx_transactions_wallet_id ON transactions(wallet_id);

-- Transações por data (range queries)
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

-- Composite index (comum)
CREATE INDEX idx_transactions_wallet_date ON transactions(wallet_id, created_at);
```

---

## 🛡️ Segurança de Dados

### Campos Sensíveis

- `cpf`: Criptografar em produção (usar extensões PostgreSQL)
- `password`: Always BCrypt/Argon2 (nunca plaintext)
- `email`: Validar e sanitizar

### GDPR Compliance

```java
// Deletar cliente (GDPR Right to Erasure)
@Transactional
public void deleteCustomer(Long customerId) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    
    // Wallets são deletadas via cascade
    customerRepository.delete(customer);
    
    // Log para auditoria
    auditLog("CUSTOMER_DELETED", customerId);
}
```

---

## 📖 Referências e Ferramentas

- **PostgreSQL Docs**: https://www.postgresql.org/docs/
- **JPA/Hibernate**: https://docs.jboss.org/hibernate/stable/orm/
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **Tool Visual**: DBeaver, pgAdmin
- **Gerador ER**: DbVisualizer, Lucidchart