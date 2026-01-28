# Roadmap Técnico — Wallet Service API

Este documento descreve a direção técnica do projeto, prioridades e ciclos de entrega.
O foco é evoluir o sistema por entregas integradas, bem testadas, documentadas e persistidas no repositório.

---

## Princípios de execução

- Trabalhar por **ciclos de 3 dias**, com entregas completas ao final de cada ciclo.
- Cada ciclo deve gerar:
  - código integrado (main/develop)
  - testes atualizados
  - documentação mínima (README/docs)
  - validação no CI (build + tests + quality)
- Prioridade para evolução com alto impacto em confiabilidade, qualidade e manutenção.

---

## Definição de pronto (DoD) do ciclo

Um ciclo é considerado concluído quando:

- `mvn clean verify` passa localmente
- CI executa build + testes com sucesso
- (quando aplicável) Sonar atualizado com Quality Gate visível
- documentação atualizada (docs/ + README quando necessário)
- mudanças observáveis e verificáveis (ex.: logs/métricas/health, endpoints, dashboards)

---

## 🎯 Ciclos planejados (3 dias)

### Ciclo 1 — Reconfigurar Sonar + Integrar JaCoCo
**Objetivo:** consolidar qualidade como “primeira classe” no projeto.

**Entrega**
- Reconfigurar Sonar (projeto, properties, exclusões e padrões)
- Integrar JaCoCo ao Sonar (leitura do coverage report)
- Definir baseline e regras iniciais de Quality Gate (realistas)
- Documentar como rodar local + como validar no CI

**Critérios de aceite**
- CI rodando: build + testes + análise Sonar
- Sonar exibindo coverage (JaCoCo) corretamente
- Documentação mínima em `docs/TESTING.md` ou `docs/QUALITY.md`

---

### Ciclo 2 — Atualizar CI/CD Git para Sonar + JaCoCo
**Objetivo:** pipeline repetível e confiável, com feedback rápido.

**Entrega**
- Ajustar pipeline do Git (GitHub Actions/GitLab CI) para:
  - `mvn clean verify`
  - gerar relatório JaCoCo
  - publicar análise no Sonar
- Cache de dependências Maven
- Separar jobs (build/test/quality) se fizer sentido

**Critérios de aceite**
- Pipeline determinístico (2 execuções seguidas com mesmo resultado)
- Logs claros do job (passo a passo)
- Falha do Quality Gate bloqueando PR/merge (se aplicável)

---

### Ciclo 3 — Incrementar Redis (cache) com foco em valor
**Objetivo:** usar Redis de forma consciente (não “colocar Redis por colocar”).

**Entrega**
- Definir caso(s) de uso:
  - cache de leitura (ex.: customer/wallet)
  - cache de autenticação (quando aplicável)
- Configuração por ambiente (`application-*.yml`)
- Estratégia de TTL e invalidação
- Métricas/observabilidade do cache (hit/miss) quando possível
- Testes (unitários e/ou integração com Testcontainers/embedded)

**Critérios de aceite**
- Cache funcionando com fallback correto (sem Redis, não quebra)
- Logs/métricas mínimas para validação
- Documentação em `docs/CACHE.md` (ou sessão no README)

---

### Ciclo 4 — Incrementar fila Kafka (eventos)
**Objetivo:** evoluir o projeto para eventos assíncronos com clareza de domínio.

**Entrega**
- Definir 1–2 eventos de negócio iniciais (ex.: `TransactionCreated`, `WalletUpdated`)
- Produção de eventos nos pontos certos (service)
- Consumidor simples com idempotência mínima (se aplicável)
- Contratos de evento versionados (schema/JSON)
- Testes (foco em serialização e integração básica)

**Critérios de aceite**
- Evento publicado e consumido em ambiente local (docker compose)
- Contrato do evento documentado e versionado
- Observabilidade mínima (logs + métricas de consumo)

---

### Ciclo 5 — Incrementar fila MQ (mensageria alternativa)
**Objetivo:** suportar/estudar fila tradicional (RabbitMQ/ActiveMQ) e comparar padrões.

**Entrega**
- Escolher a tecnologia (RabbitMQ recomendado para estudo)
- Implementar um fluxo simples (ex.: envio de notificação/auditoria)
- Padronizar retry/dead-letter (mínimo viável)
- Testes e documentação

**Critérios de aceite**
- Mensagens trafegando em ambiente local
- Retry/DLQ básico documentado
- Guia em `docs/MESSAGING.md`

---

## 🔜 Próximos focos após os ciclos

- Consolidar testes de regra de negócio (branches subindo continuamente)
- Padronizar contrato de erros da API (handlers + responses)
- Melhorar robustez em segurança (roles/claims/refresh)
- Revisar organização por módulos/pacotes conforme crescimento

---

## ❌ Fora de escopo (por enquanto)

- Cobertura de DTO/model simples como meta principal
- Otimizações prematuras de performance
- Complexidade de orquestração além do necessário para o aprendizado

---

## Atualização do roadmap

Este roadmap deve ser atualizado ao final de cada ciclo com:
- status (Done / In progress / Next)
- aprendizados
- ajustes de prioridade
