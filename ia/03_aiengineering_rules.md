# AI Engineering Rules

Este arquivo define as regras de colaboração entre o desenvolvedor e assistentes de IA durante o desenvolvimento de software.

Objetivo:
Garantir respostas técnicas precisas, evitar sugestões genéricas e manter alinhamento com a arquitetura existente do projeto.

---

## 1. Trabalho Iterativo

O desenvolvimento deve ocorrer de forma iterativa.

Cada resposta da IA deve:
- permitir análise
- permitir validação
- permitir decisão antes da próxima etapa

A IA deve apresentar sugestões e aguardar aprovação antes de aplicar mudanças estruturais.

---

## 2. Comunicação Técnica Objetiva

Evitar respostas genéricas.

Caso não exista contexto suficiente para responder com precisão, a IA deve solicitar informações adicionais antes de propor qualquer solução.

Exemplos de contexto que podem ser solicitados:

- estrutura do projeto
- arquivos relevantes
- dependências
- arquitetura atual
- stack tecnológica

---

## 3. Implementação Sempre Contextualizada

A IA nunca deve sugerir alterações sem indicar claramente:

Arquivo:
(caminho completo do arquivo)

Ação:
criar | editar | refatorar

Local da alteração:
classe | método | configuração

Código sugerido:
(trecho de código)

Evitar respostas como:

"adicione este código"

Sem informar onde implementar.

---

## 4. Registro de Iteração

Cada interação técnica deve incluir um log de atividade.

Formato obrigatório:

LOG_AT:
DDDD - YYYY-MM-DD HH:mm:ss.SSS

Exemplo:

LOG_AT:
SATURDAY - 2026-03-08 10:41:23.153

---

## 5. Respeito à Estrutura Existente

Antes de sugerir novas implementações, a IA deve verificar:

- se já existe estrutura semelhante
- se há padrões já definidos no projeto
- se a solução pode evoluir código existente

Evitar criar estruturas paralelas desnecessárias.

---

## 6. Padrões do Projeto

Se o projeto possuir:

- padrões arquiteturais
- padrões de documentação
- padrões de código

A IA deve seguir os padrões existentes.

---

## 7. Solicitação de Esclarecimento

Caso qualquer ação não esteja clara, a IA deve solicitar mais detalhes antes de implementar ou sugerir mudanças.

Isso evita interpretações incorretas do contexto do projeto.

---

## Objetivo Final

Garantir:

- colaboração técnica eficiente
- evolução controlada do projeto
- respostas precisas e implementáveis
- alinhamento com arquitetura e padrões existentes
