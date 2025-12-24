# API Reference

Documentação completa de todos os endpoints disponíveis no Wallet Service API.

## 📋 Sumário

- [Authentication](#authentication)
- [Customers](#customers)
- [Wallets](#wallets)
- [Transactions](#transactions)
- [Wallet Operator](#wallet-operator)
- [Parameters](#parameters)
- [Status Codes](#status-codes)

---

## Authentication

Endpoints para autenticação e gerenciamento de tokens JWT.

### Login

**POST** `/api/auth/login`

Realiza login de um usuário e retorna tokens JWT (access e refresh).

**Request:**
```json
{
  "username": "usuario_exemplo",
  "password": "senha_segura"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "Invalid credentials",
  "message": "Username or password is incorrect"
}
```

---

### Register

**POST** `/api/auth/register`

Registra um novo usuário no sistema.

**Request:**
```json
{
  "username": "novo_usuario",
  "password": "senha_segura"
}
```

**Response (201 Created):**
```json
{
  "status": "User registration logic pending implementation in a Service class."
}
```

---

### Refresh Token

**POST** `/api/auth/refresh`

Renova um access token usando um refresh token válido.

**Query Parameters:**
- `refreshToken` (string, required): Token de renovação

**Example:**
```
POST /api/auth/refresh?refreshToken=eyJhbGc...
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Invalid or expired refresh token"
}
```

---

## Customers

Endpoints para gerenciamento de clientes.

### Create Customer

**POST** `/api/customers/customer`

Cria um novo cliente no sistema.

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "name": "João Silva",
  "email": "joao.silva@example.com",
  "phone": "11987654321",
  "cpf": "12345678901",
  "status": "ACTIVE"
}
```

**Response (201 Created):**
```json
{
  "customerId": 1,
  "name": "João Silva",
  "email": "joao.silva@example.com",
  "phone": "11987654321",
  "cpf": "12345678901",
  "status": "ACTIVE",
  "createdAt": "2024-12-08T10:30:00Z"
}
```

---

### Get Customer by ID

**GET** `/api/customers/{id}`

Recupera um cliente específico pelo ID.

**Path Parameters:**
- `id` (long, required): ID do cliente

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "customerId": 1,
  "name": "João Silva",
  "email": "joao.silva@example.com",
  "phone": "11987654321",
  "cpf": "12345678901",
  "status": "ACTIVE",
  "createdAt": "2024-12-08T10:30:00Z",
  "updatedAt": "2024-12-08T10:30:00Z"
}
```

**Error Response (404 Not Found):**
```json
{
  "error": "Customer not found",
  "id": 999
}
```

---

### Update Customer

**PUT** `/api/customers/{id}`

Atualiza um cliente existente.

**Path Parameters:**
- `id` (long, required): ID do cliente

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "name": "João Silva Santos",
  "email": "joao.santos@example.com",
  "phone": "11987654322",
  "status": "INACTIVE"
}
```

**Response (200 OK):**
```json
{
  "customerId": 1,
  "name": "João Silva Santos",
  "email": "joao.santos@example.com",
  "phone": "11987654322",
  "cpf": "12345678901",
  "status": "INACTIVE",
  "updatedAt": "2024-12-08T11:45:00Z"
}
```

---

### List Customers

**GET** `/api/customers/list`

Lista todos os clientes com suporte a paginação e filtros.

**Query Parameters:**
- `status` (string, optional): Filtrar por status (ACTIVE, INACTIVE)
- `page` (int, default: 0): Número da página

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "customerId": 1,
      "name": "João Silva",
      "email": "joao.silva@example.com",
      "status": "ACTIVE",
      "createdAt": "2024-12-08T10:30:00Z"
    },
    {
      "customerId": 2,
      "name": "Maria Santos",
      "email": "maria.santos@example.com",
      "status": "ACTIVE",
      "createdAt": "2024-12-08T10:35:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 25,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "number": 0,
  "size": 25,
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

---

## Wallets

Endpoints para gerenciamento de carteiras digitais.

### Create Wallet

**POST** `/api/wallets/wallet`

Cria uma nova carteira para um cliente.

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "customerId": 1,
  "walletType": "SAVINGS",
  "initialBalance": 1000.00,
  "status": "ACTIVE"
}
```

**Response (201 Created):**
```json
{
  "walletId": 1,
  "customerId": 1,
  "walletType": "SAVINGS",
  "balance": 1000.00,
  "status": "ACTIVE",
  "createdAt": "2024-12-08T10:30:00Z"
}
```

---

### Get Wallet by ID

**GET** `/api/wallets/{id}`

Recupera uma carteira específica pelo ID.

**Path Parameters:**
- `id` (long, required): ID da carteira

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "walletId": 1,
  "customerId": 1,
  "walletType": "SAVINGS",
  "balance": 1500.00,
  "status": "ACTIVE",
  "createdAt": "2024-12-08T10:30:00Z",
  "updatedAt": "2024-12-08T11:20:00Z"
}
```

---

### Update Wallet

**PUT** `/api/wallets/{id}`

Atualiza uma carteira existente.

**Path Parameters:**
- `id` (long, required): ID da carteira

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "status": "INACTIVE"
}
```

**Response (200 OK):**
```json
{
  "walletId": 1,
  "customerId": 1,
  "walletType": "SAVINGS",
  "balance": 1500.00,
  "status": "INACTIVE",
  "updatedAt": "2024-12-08T11:45:00Z"
}
```

---

### List All Wallets

**GET** `/api/wallets/list`

Lista todas as carteiras com paginação.

**Query Parameters:**
- `page` (int, default: 0): Número da página

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "walletId": 1,
      "customerId": 1,
      "walletType": "SAVINGS",
      "balance": 1500.00,
      "status": "ACTIVE"
    },
    {
      "walletId": 2,
      "customerId": 2,
      "walletType": "CHECKING",
      "balance": 5000.00,
      "status": "ACTIVE"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 25
  },
  "totalElements": 2,
  "totalPages": 1
}
```

---

### Get Wallets by Customer

**GET** `/api/wallets/search-by-customer`

Busca carteiras de um cliente específico.

**Query Parameters:**
- `customerId` (long, required): ID do cliente
- `page` (int, default: 0): Número da página

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "walletId": 1,
      "customerId": 1,
      "walletType": "SAVINGS",
      "balance": 1500.00,
      "status": "ACTIVE"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 25
  },
  "totalElements": 1,
  "totalPages": 1
}
```

---

## Transactions

Endpoints para gerenciamento de transações financeiras.

### Create Deposit Transaction

**POST** `/api/transactions/transaction?type=DEPOSIT`

Realiza um depósito em uma carteira.

**Query Parameters:**
- `walletId` (long, required): ID da carteira
- `amount` (BigDecimal, required): Valor do depósito
- `cpfSender` (string, required): CPF do remetente
- `terminalId` (string, required): ID do terminal
- `senderName` (string, required): Nome do remetente

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (201 Created):**
```json
{
  "transactionId": 1,
  "walletId": 1,
  "amount": 500.00,
  "type": "DEPOSIT",
  "status": "COMPLETED",
  "cpfSender": "12345678901",
  "senderName": "João Silva",
  "terminalId": "TERM001",
  "createdAt": "2024-12-08T10:30:00Z"
}
```

---

### Create Withdraw Transaction

**POST** `/api/transactions/transaction?type=WITHDRAW`

Realiza um saque de uma carteira.

**Query Parameters:**
- `walletId` (long, required): ID da carteira
- `amount` (BigDecimal, required): Valor do saque

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (201 Created):**
```json
{
  "transactionId": 2,
  "walletId": 1,
  "amount": 200.00,
  "type": "WITHDRAW",
  "status": "COMPLETED",
  "createdAt": "2024-12-08T10:35:00Z"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Insufficient balance",
  "available": 100.00,
  "requested": 200.00
}
```

---

### Create Transfer Transaction

**POST** `/api/transactions/transaction?type=TRANSFER_SEND`

Realiza uma transferência entre carteiras.

**Query Parameters:**
- `walletIdSend` (long, required): ID da carteira de origem
- `walletIdReceived` (long, required): ID da carteira de destino
- `amount` (BigDecimal, required): Valor da transferência

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (201 Created):**
```json
{
  "transactionId": 3,
  "walletIdSend": 1,
  "walletIdReceived": 2,
  "amount": 300.00,
  "type": "TRANSFER_SEND",
  "status": "COMPLETED",
  "createdAt": "2024-12-08T10:40:00Z"
}
```

---

### Get Transaction by ID

**GET** `/api/transactions/{transactionId}`

Recupera uma transação específica.

**Path Parameters:**
- `transactionId` (long, required): ID da transação

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "transactionId": 1,
  "walletId": 1,
  "amount": 500.00,
  "type": "DEPOSIT",
  "status": "COMPLETED",
  "createdAt": "2024-12-08T10:30:00Z",
  "updatedAt": "2024-12-08T10:30:00Z"
}
```

---

### Get Transactions by Wallet

**GET** `/api/transactions/search-wallet?walletId={walletId}`

Lista transações de uma carteira.

**Query Parameters:**
- `walletId` (long, required): ID da carteira
- `page` (int, default: 0): Número da página

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "transactionId": 1,
      "walletId": 1,
      "amount": 500.00,
      "type": "DEPOSIT",
      "status": "COMPLETED",
      "createdAt": "2024-12-08T10:30:00Z"
    },
    {
      "transactionId": 2,
      "walletId": 1,
      "amount": 200.00,
      "type": "WITHDRAW",
      "status": "COMPLETED",
      "createdAt": "2024-12-08T10:35:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 25
  },
  "totalElements": 2,
  "totalPages": 1
}
```

---

### List Transactions with Filter

**GET** `/api/transactions/list?walletId={walletId}&typeTransaction={type}`

Lista transações com filtro por tipo e carteira.

**Query Parameters:**
- `walletId` (long, required): ID da carteira
- `typeTransaction` (string, optional): Tipo (DEPOSIT, WITHDRAW, TRANSFER_SEND, TRANSFER_RECEIVED)
- `page` (int, default: 0): Número da página

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "transactionId": 1,
      "walletId": 1,
      "amount": 500.00,
      "type": "DEPOSIT",
      "status": "COMPLETED"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 25
  },
  "totalElements": 1,
  "totalPages": 1
}
```

---

## Wallet Operator

Endpoints administrativos para operações em lote.

### Get Transaction by ID (Operator)

**GET** `/api/wallet-operator/transaction/{transactionId}`

Recupera uma transação (uso interno).

**Path Parameters:**
- `transactionId` (long, required): ID da transação

**Response (200 OK):**
```json
{
  "transactionId": 1,
  "walletId": 1,
  "amount": 500.00,
  "type": "DEPOSIT",
  "status": "COMPLETED"
}
```

---

### Get Transactions by Wallet and Date

**GET** `/api/wallet-operator/transactions/{walletId}/{date}`

Filtra transações por carteira e data.

**Path Parameters:**
- `walletId` (long, required): ID da carteira
- `date` (LocalDate, required): Data (formato: YYYY-MM-DD)

**Response (200 OK):**
```json
[
  {
    "transactionId": 1,
    "walletId": 1,
    "amount": 500.00,
    "type": "DEPOSIT",
    "status": "COMPLETED",
    "createdAt": "2024-12-08T10:30:00Z"
  }
]
```

---

### Upload Customer Data (CSV)

**POST** `/api/wallet-operator/upload-customer`

Importa dados de clientes via arquivo CSV.

**Headers:**
```
Content-Type: multipart/form-data
```

**Form Data:**
- `file` (MultipartFile, required): Arquivo CSV

**CSV Format:**
```csv
name,email,phone,cpf,status
João Silva,joao@example.com,11987654321,12345678901,ACTIVE
Maria Santos,maria@example.com,11987654322,12345678902,ACTIVE
```

**Response (200 OK):**
```json
{
  "message": "Carga de clientes via CSV concluída com sucesso."
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "O arquivo de upload está vazio."
}
```

---

### Upload Wallet Data (CSV)

**POST** `/api/wallet-operator/upload-wallet`

Importa dados de carteiras via arquivo CSV.

**Headers:**
```
Content-Type: multipart/form-data
```

**Form Data:**
- `file` (MultipartFile, required): Arquivo CSV

**CSV Format:**
```csv
customerId,walletType,initialBalance,status
1,SAVINGS,1000.00,ACTIVE
2,CHECKING,5000.00,ACTIVE
```

**Response (200 OK):**
```json
{
  "message": "Carga de clientes via CSV concluída com sucesso."
}
```

---

### Upload Transactions Data (CSV)

**POST** `/api/wallet-operator/upload-transactions`

Importa dados de transações via arquivo CSV.

**Headers:**
```
Content-Type: multipart/form-data
```

**Form Data:**
- `file` (MultipartFile, required): Arquivo CSV

**Response (200 OK):**
```json
{
  "message": "Carga de Transações via CSV concluída com sucesso."
}
```

---

### Upload Movements Data (CSV)

**POST** `/api/wallet-operator/upload-movements`

Importa dados de movimentações via arquivo CSV.

**Headers:**
```
Content-Type: multipart/form-data
```

**Form Data:**
- `file` (MultipartFile, required): Arquivo CSV

**Response (200 OK):**
```json
{
  "message": "Carga de Transações via CSV concluída com sucesso."
}
```

---

### Upload Deposit Senders Data (CSV)

**POST** `/api/wallet-operator/upload-deposit-senders`

Importa dados de remetentes de depósitos via arquivo CSV.

**Headers:**
```
Content-Type: multipart/form-data
```

**Form Data:**
- `file` (MultipartFile, required): Arquivo CSV

**Response (200 OK):**
```json
{
  "message": "Carga de Transações via CSV concluída com sucesso."
}
```

---

## Parameters

Endpoints para gerenciamento de parâmetros da aplicação.

### Create Parameter

**POST** `/api/params`

Cria um novo parâmetro de configuração.

**Headers:**
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**Request:**
```json
{
  "paramName": "MAX_TRANSFER_AMOUNT",
  "paramValue": "50000.00",
  "description": "Valor máximo permitido para transferências"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "paramName": "MAX_TRANSFER_AMOUNT",
  "paramValue": "50000.00",
  "description": "Valor máximo permitido para transferências"
}
```

---

### Get All Parameters

**GET** `/api/params/list`

Lista todos os parâmetros.

**Query Parameters:**
- `page` (int, default: 0): Número da página

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "paramName": "MAX_TRANSFER_AMOUNT",
    "paramValue": "50000.00",
    "description": "Valor máximo permitido para transferências"
  },
  {
    "id": 2,
    "paramName": "MIN_BALANCE",
    "paramValue": "10.00",
    "description": "Saldo mínimo permitido"
  }
]
```

---

### Get Parameter by ID

**GET** `/api/params/{id}`

Recupera um parâmetro específico.

**Path Parameters:**
- `id` (long, required): ID do parâmetro

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "paramName": "MAX_TRANSFER_AMOUNT",
  "paramValue": "50000.00",
  "description": "Valor máximo permitido para transferências"
}
```

---

### Delete Parameter

**DELETE** `/api/params/{id}`

Remove um parâmetro.

**Path Parameters:**
- `id` (long, required): ID do parâmetro

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (204 No Content)**

---

## Status Codes

| Código | Descrição |
|--------|-----------|
| **200** | OK - Requisição bem-sucedida |
| **201** | Created - Recurso criado com sucesso |
| **204** | No Content - Requisição bem-sucedida sem retorno |
| **400** | Bad Request - Requisição inválida |
| **401** | Unauthorized - Autenticação necessária |
| **403** | Forbidden - Acesso proibido |
| **404** | Not Found - Recurso não encontrado |
| **409** | Conflict - Conflito (ex: ID duplicado) |
| **500** | Internal Server Error - Erro no servidor |
| **502** | Bad Gateway - Serviço indisponível |
| **503** | Service Unavailable - Serviço temporariamente indisponível |

---

## Headers Comuns

### Request Headers Obrigatórios

Para endpoints protegidos:
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### Response Headers

```
Content-Type: application/json; charset=UTF-8
X-Request-ID: {uuid}
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
```

---

## Exemplos com cURL

### Login
```bash
curl -X POST http://localhost:8080/wallet-service-api/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "usuario",
    "password": "senha"
  }'
```

### Criar Cliente
```bash
curl -X POST http://localhost:8080/wallet-service-api/api/customers/customer \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@example.com",
    "cpf": "12345678901"
  }'
```

### Fazer Depósito
```bash
curl -X POST "http://localhost:8080/wallet-service-api/api/transactions/transaction?type=DEPOSIT&walletId=1&amount=500.00&cpfSender=12345678901&terminalId=TERM001&senderName=João" \
  -H "Authorization: Bearer {accessToken}"
```

### Listar Carteiras
```bash
curl -X GET http://localhost:8080/wallet-service-api/api/wallets/list?page=0 \
  -H "Authorization: Bearer {accessToken}"
```