
## 👤 Autor

**Gustavo Souza (Guga)**  
📧 [gustavo1282@gmail.com](mailto:gustavo1282@gmail.com)  
🔗 [LinkedIn](https://www.linkedin.com/in/gustavo-souza-68b34335/) | [GitHub](https://github.com/gustavo1282)

---

# Wallet Service API  
Sistema de carteira digital para gerenciamento de clientes, contas, depósitos, transferências e movimentações financeiras.

---

## 📌 Visão Geral

O **Wallet Service API** é um sistema completo de carteira digital desenvolvido com foco em:

- Arquitetura limpa
- Escalabilidade
- Observabilidade
- Segurança
- Boas práticas de engenharia
- Domínio claro do problema financeiro

O projeto evoluiu a partir de um estudo avançado de Java + Spring Boot, mas ganhou maturidade e agora se comporta como um **serviço real**, servindo como base para aprendizado, referência arquitetural e demonstração técnica.

---

## 🚀 Objetivos do Projeto

- Demonstrar uma arquitetura sólida e modular  
- Criar um sistema de operações financeiras consistente  
- Aderência às boas práticas do mercado  
- Possibilitar evolução por novos colaboradores  
- Documentação clara e completa  
- Base para futuras pesquisas, melhorias e experimentações

---

## 🧱 Estrutura do Projeto

``` bash
├───src
│   ├───main
│   │   ├───java
│   │   │   └───com
│   │   │       └───guga
│   │   │           └───walletserviceapi
│   │   │               ├───components
│   │   │               ├───config
│   │   │               ├───controller
│   │   │               ├───exception
│   │   │               ├───handler
│   │   │               ├───helpers
│   │   │               ├───model
│   │   │               │   ├───converter
│   │   │               │   ├───enums
│   │   │               │   ├───mapping
│   │   │               │   ├───mongodb
│   │   │               │   └───serializers
│   │   │               ├───repository
│   │   │               │   └───mongodb
│   │   │               ├───seeder
│   │   │               └───service
```

Cada camada possui responsabilidades bem definidas, seguindo princípios como:

- Coesão
- Baixo acoplamento
- Separação de preocupações
- Código modular e testável

---

## 🧩 Principais Funcionalidades

### ✔️ Customer
- Cadastro
- Atualização
- Consulta por ID e por Status
- Alteração de status
- Seed inicial opcional

### ✔️ Wallet
- Criação automática vinculada ao cliente
- Consulta de saldo
- Limites operacionais

### ✔️ Depósitos
- Entrada de valores com origem (DepositSender)
- Associação automática com Wallet e Transação

### ✔️ Movimentações (Movements)
- Crédito
- Débito
- Registro auditável
- Controle transacional

### ✔️ Transferências
- Transferências entre contas internas
- Operação atômica com compensação
- Registro completo da transação

### ✔️ Transações
- Histórico de eventos financeiros
- Rastreabilidade completa
- Auditoria técnica

---

## 🏛️ Arquitetura (Resumo)

- Controllers → entrada da API e validações
- Services → regras de negócio
- Repositories → persistência com JPA
- Entity → modelo de banco
- Domain → enums, regras específicas de domínio
- DTO/Record → transporte de dados
- Exception Handler global
- Seeders → carga inicial opcional de dados

O detalhamento técnico está disponível em **ARCHITECTURE_AND_DESIGN.md**.

---

## 🔧 Tecnologias Utilizadas

- Java 21  
- Spring Boot  
- JPA / Hibernate  
- PostgreSQL  
- Docker Compose  
- MapStruct  
- Lombok  
- JUnit / Mockito  
- Maven  

---

## ▶️ Como Executar

### Docker
```bash
docker-compose up --build
```

### Localmente
```
mvn clean install
mvn spring-boot:run
```


### Testes
```
mvn test
```

📚 Documentos Complementares

- [ARCHITECTURE_AND_DESIGN.md](data/docs/ARCHITECTURE_AND_DESIGN.md)
- [API_REFERENCE.md](data/docs/API_REFERENCE.md)
- [OBSERVABILITY.md](data/docs/OBSERVABILITY.md)
- [CONTRIBUTING.md](data/docs/CONTRIBUTING.md)
- [DATA_MODEL.md](data/docs/DATA_MODEL.md)
- [DOMAIN_MODEL.md](data/docs/DOMAIN_MODEL.md)
- [SECURITY.md](data/docs/SECURITY.md)
- [BUILD_AND_CI.md](data/docs/BUILD_AND_CI.md)

