# Data Model

Estrutura do banco de dados utilizada pelo Wallet Service API.

---

# 🧍 Tabela: customer
- id
- full_name
- cpf
- status
- created_at

---

# 💼 Tabela: wallet
- wallet_id
- customer_id
- balance

---

# 💰 Tabela: deposit_sender
- id
- full_name
- cpf
- amount

---

# 🔄 Tabela: movement
- id
- transaction_id
- wallet_id
- type
- amount
- created_at

---

# 🧾 Tabela: transaction
- id
- created_at
- description
