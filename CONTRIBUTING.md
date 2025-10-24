# 🤝 Contribuindo com o Wallet Service

Obrigado por contribuir com o projeto **Wallet Service**!  
Abaixo estão as diretrizes para rodar localmente, abrir PRs e manter a consistência do código.

---

## 💻 Rodando Localmente (Modo Dev)

1. Clone o repositório:
   ```bash
   git clone https://github.com/gustavo1282/wallet-service.git
   cd wallet-service
   ```
2. Suba os serviços com Docker:
   ```bash
   ./start_dev_app.sh
   ```
3. Ou rode localmente via Maven:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 🧪 Testes

Executar todos os testes:
```bash
./mvnw test
```

- **Testes Unitários:** verificam regras de negócio.
- **Testes de Integração:** validam persistência e endpoints.
- **Testcontainers:** simula o ambiente real com Postgres.

---

## 🌿 Convenções de Branch e Commit

| Tipo             | Exemplo                            | Branch Base   | Descrição                                                                                       |
|:-----------------|:-----------------------------------|:--------------|:------------------------------------------------------------------------------------------------|
| 🟢 **main**      | `main`                             | `N/A`         | Contém o código estável e homologado. Cada merge aqui representa uma nova versão em produção.   |
| 🏗️ **develop**  | `develop`                          | `main`        | É a branch base de desenvolvimento. Todas as novas features partem e voltam para ela.           |
| ✨ **feature/**   | `feature/transferencia-wallet`     | `develop`     | Implementação de novas funcionalidades.                                                         |
| 🐛 **fix/**      | `fix/ajuste-calculo-saldo`         | `develop`     | Correções de bugs e falhas (não críticas) encontradas durante o desenvolvimento.                |
| 🔥 **hotfix/**   | `hotfix/corrige-deposito-negativo` | `main`        | Correções urgentes em produção. **(Deve ser mergeado para `main` e `develop`)**                 |
| 🚀 **release/**  | `release/v1.0.0`                   | `develop`     | Preparação final de uma nova versão de produção (testes de aceite, ajustes de última hora).     |
| 🔨 **refactor/** | `refactor/service-transacao`       | `develop`     | Refatorações e melhorias internas de código **sem** mudar o comportamento da aplicação.         |
| 📚 **docs/**     | `docs/atualizar-readme`            | `develop`     | Usado para arquivos `.md`, documentação, wiki, comentários e ajustes não-código.                |


Formato de commit:
```
tipo(escopo): descrição curta
ex: feature(transacao): adiciona validação de saldo
```

## 🧩 Como contribuir passo a passo

1. **Clone o repositório oficial**
   ```bash
   git clone https://github.com/gustavo1282/wallet-service.git
   cd wallet-service

2. **Crie uma branch a partir de develop**
    ```bash
    git checkout develop
    git pull origin develop
    git checkout -b feature/nome-da-feature

3. **Implemente suas alterações e faça commit**
    ```bash
    git add .
    git commit -m "feature(wallet): adiciona validação de saldo"

4. **Envie sua branch para o repositório remoto**
    ```bash
   git push origin feature/nome-da-feature

5. **Abra um Pull Request (PR)**
   - Base: *develop*
   - Compare: *feature/nome-da-feature*
   - Adicione descrição e prints, se aplicável.

**Aprovação obrigatória**
- PRs só podem ser mergeados após revisão de pelo menos 1 responsável técnico.

- O merge deve ser feito via Pull Request, nunca direto na branch main.


### 🧐 Permissões e Revisões
- Apenas usuários com permissão de maintainer podem aprovar merges.
- Commits diretos na branch main são bloqueados.
- Todas as alterações passam por code review.
- Branches inativas podem ser arquivadas após 30 dias.

---
## 🧩 Fluxo visual simplificado

    main  ←── release/*  ←── develop  ←── feature/*
      ↑          ↑             ↑              ↑
      │          │             └── fix/*      └── docs/
      │          └── hotfix/*                 
      └───────────────────────────────→ produção

---

## 🧱 Exemplo prático

````bash
#Criação de nova funcionalidade

    git checkout develop
    git pull origin develop
    git checkout -b feature/novo-endpoint-saque

# Implementação, commit e push
    git add .
    git commit -m "feature(saque): cria endpoint para saque de valores"
    git push origin feature/novo-endpoint-saque

# Depois abre um Pull Request para develop
````

----

## 🧱 Boas Práticas de Código

- Respeitar camadas: `controller → service → repository`
- Evitar lógica de negócio em controllers
- Adicionar logs significativos
- Usar `@Valid` em DTOs
- Cobrir métodos com testes unitários

---

## 🚀 Pull Requests

1. Atualize seu branch com `main`.
2. Garanta que todos os testes passam.
3. Crie o PR com descrição clara e prints se necessário.
4. Aguarde revisão.

---

## 🪄 Observabilidade e Monitoramento

Após subir via Docker, acesse:
- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3000](http://localhost:3000)
- API: [http://localhost:8080/api](http://localhost:8080/api)

---

## 💬 Contato

Dúvidas ou sugestões?  
Abra uma issue no GitHub ou envie um e-mail para [gustavo1282@gmail.com](mailto:gustavo1282@gmail.com).
