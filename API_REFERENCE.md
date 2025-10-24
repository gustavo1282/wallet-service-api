
---

## 🧭 API_REFERENCE.md

---

## Endpoints principais (resumo)

### Base path: /wallet-services/api (conforme application.yml).

### application

- POST /wallets — Criar wallet para um customer.

- GET /wallets/{id} — Recuperar dados da wallet.

- GET /wallets/{id}/balance — Saldo atual.

- GET /wallets/{id}/balance/history?at={timestamp} — Saldo em um timestamp passado.

- POST /wallets/{id}/deposit — Depositar valor.

- POST /wallets/{id}/withdraw — Sacar valor.

- POST /wallets/transfer — Transferir entre wallets.

- CRUD Customers: /customers endpoints (list/create/get/update/delete).

- CRUD Transactions: /transactions endpoints (list/get).

---

