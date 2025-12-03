# Build & CI

Documentação simples referente ao build do projeto.

---

# 🔧 Build

```bash
mvn clean install
```

# 🐳 Docker
```bash
docker-compose up --build
```


# 🤖 CI (Sugestão)

- Build

- Testes
- Verificação de estilo
- Deploy automatizado opcional


---

# **📄 7. DOMAIN_MODEL.md**

```markdown
# Domain Model

Este documento descreve o domínio do sistema.

---

# 🧍 Customer
- Identificação
- Status
- Possui 1 Wallet

---

# 💼 Wallet
- Saldo
- Movimentações
- Transações
- Vinculada a um Customer

---

# 💰 DepositSender
- Origem do depósito
- CPF
- Nome completo

---

# 🔄 Movement
- Tipo: CREDIT ou DEBIT
- Valor
- Data
- Relacionado a uma Transaction

---

# 🧾 Transaction
- Conjunto de Movements
- Operação financeira completa

---

# 🔁 Transfer
- Operação entre duas Wallets
- 1 débito + 1 crédito

---
