# API Reference

Documentação dos endpoints disponíveis no Wallet Service API.

---

# 🧍 Customer

## POST /customers
Cria um novo cliente.

## GET /customers/{id}
Retorna cliente por ID.

## GET /customers?status=ACTIVE
Lista clientes filtrando por status.

## PUT /customers/{id}
Atualiza dados do cliente.

## PATCH /customers/{id}/status
Altera o status do cliente.

---

# 💼 Wallet

## GET /wallets/{customerId}
Retorna informações da wallet do cliente.

---

# 💰 Depósitos

## POST /deposits
Cria depósito.

---

# 🔄 Movements

## GET /movements?walletId=xxx
Lista movimentações.

---

# 🔁 Transferências

## POST /transfers
Realiza transferência entre contas.

---

# 🧾 Transactions

## GET /transactions?walletId=xxx
Lista transações.

---

# 🧪 Respostas Comuns

### 400  
Erros de validação.

### 404  
Registro não encontrado.

### 422  
Regra de negócio não atendida.

### 500  
Erro interno inesperado.

---
