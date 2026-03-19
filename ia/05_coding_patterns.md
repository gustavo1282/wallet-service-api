# Coding Patterns

Este documento define padrões de código do projeto.

---

## Linguagem

Java

Framework principal:

Spring Boot

---

## Estrutura de pacotes

controller  
service  
repository  
model  
dto  
config  
exception

---

## Convenções de código

Classes

PascalCase

Exemplo:

WalletService

Métodos

camelCase

Exemplo:

createWallet

---

## Padrão Controller

Controllers devem:

- ser responsáveis apenas por entrada e saída de dados
- delegar lógica para Services

---

## Padrão Service

Services devem:

- conter regras de negócio
- realizar validações
- coordenar chamadas de repositório

---

## Padrão Repository

Repositories devem:

- acessar banco de dados
- conter consultas específicas
- não conter lógica de negócio

---

## Tratamento de erros

Usar exceções customizadas.

Exemplo:

WalletNotFoundException

---

## Logs

Utilizar logging estruturado.

Evitar logs genéricos.

Preferir logs com contexto de operação.