

---

# **📄 2. ARCHITECTURE_AND_DESIGN.md**

```markdown
# Architecture and Design

Este documento apresenta a arquitetura do **Wallet Service API**, seus componentes principais, padrões adotados e as decisões técnicas que suportam o funcionamento do sistema.

---

# 📐 Visão Geral da Arquitetura

A aplicação segue princípios de:

- Clean Architecture (adaptado)
- Domain-Driven Design (conceptual)
- Separação clara de responsabilidades
- Modularidade e observabilidade

---

# 🏗️ Camadas

Controller → DTO/Record → Service → Domain → Repository → Database


### **Controller**
- Recebe requisições
- Valida dados
- Orquestra chamadas aos services

### **Service**
- Coração do domínio
- Implementa regras financeiras
- Controla transações
- Registra eventos e logs

### **Repository**
- Persistência via Spring Data JPA
- Queries por métodos e JPQL

### **Entity**
- Modelo do banco
- Entidades normalizadas

### **Mapper**
- Tradução entre Entity ↔ DTO

### **Domain**
- Enums
- Regras
- Tipos específicos do negócio

---

# ⚙️ Fluxos Operacionais do Sistema

## 1. 📌 Cadastro do Cliente
1. Cliente é criado
2. Wallet é criada automaticamente
3. Status inicial é ACTIVE
4. Dados são persistidos

---

## 2. 💰 Depósito
1. DepositSender é registrado
2. Depósito é lançado como **credit movement**
3. Uma Transaction é criada
4. Wallet tem seu saldo atualizado
5. Operação é registrada de forma auditável

---

## 3. 🔄 Movimentação (Movement)
- Toda alteração financeira gera um Movement
- Tipos:
  - CREDIT
  - DEBIT
- Movements compõem Transactions
- Saldo é atualizado a partir deles

---

## 4. 🔁 Transferência
1. Origem → débito
2. Destino → crédito
3. Transações relacionadas
4. Operação atômica

---

# 📊 Decisões Arquiteturais Importantes

### ✔️ Entities isoladas do domínio
O domínio define comportamento; a entity define persistência.

### ✔️ MapStruct para mapeamentos
Simples, limpo, performático.

### ✔️ Uso de Services focados e pequenos
Evita "serviços gigantes".

### ✔️ Exceptions customizadas
Melhor controle de erros.

### ✔️ Registro consistente de Movements
Suporta auditoria completa.

---

# 📦 Estrutura de Pacotes (Explicada)

- **controller**: entrada da API  
- **service**: lógica  
- **repository**: operações DB  
- **entity**: tabelas  
- **domain**: enums e regras  
- **record/dto/model**: transporte de dados  
- **exception/handler**: tratamento global  
- **seeder**: dados iniciais

---

# 🧭 Diagrama de Contexto (Simplificado)

[ Cliente ]
|
v
[ Controller ] → [ Service ] → [ Repository ] → [ Database ]
|
+→ [ Movements ]
+→ [ Transactions ]
+→ [ Transfer Logic ]


---

# 🔮 Possíveis Evoluções Futuras
- Saga Pattern para transações distribuídas  
- Autenticação JWT ou OIDC  
- Mensageria para auditoria externa  
- Cache  
- Rate limiting  

---




- [README.md](./../../README.md)
- [CONTRIBUTING.md](./CONTRIBUTING.md)
