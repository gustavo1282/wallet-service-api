# Architecture Decisions (ADR) — Wallet Service API

Este documento registra decisões técnicas relevantes tomadas ao longo da evolução do projeto.
Objetivo: preservar contexto, alternativas e trade-offs para manter coerência arquitetural e facilitar manutenção.

---

## ADR-001 — Stack base: Spring Boot 3.x + Java 21

**Data:** 2026-01-28  
**Status:** Aceita

### Contexto
O projeto visa estudo e consolidação de boas práticas modernas de backend, com foco em stack atual de mercado e compatibilidade com bibliotecas recentes.

### Decisão
Utilizar **Spring Boot 3.x** com **Java 21**.

### Alternativas consideradas
- Java 17 LTS
- Spring Boot 2.x (legado)

### Motivos
- Ecossistema atualizado e com suporte moderno
- Compatibilidade com bibliotecas atuais (security/observability)
- Redução de riscos com versões legadas

### Consequências
- Necessidade de ambiente com Java 21
- Algumas dependências antigas podem ser incompatíveis

---

## ADR-002 — Cobertura de testes: JaCoCo integrado ao lifecycle Maven

**Data:** 2026-01-28  
**Status:** Aceita

### Contexto
Necessidade de medir cobertura e reduzir risco de regressões, com relatório reproduzível e integrável ao CI/quality tools.

### Decisão
Adotar **JaCoCo** integrado ao Maven lifecycle, gerando relatório no `verify`.

### Alternativas consideradas
- Cobertura apenas via IDE
- Ferramentas alternativas (ex.: cobertura via plugin específico de IDE/CI)

### Motivos
- Padrão de mercado para Java
- Integração simples com CI e Sonar
- Relatório HTML para análise local

### Consequências
- Build pode ficar um pouco mais lento
- Necessidade de manter testes e excluir corretamente artefatos não relevantes (DTO/config)

---

## ADR-003 — Qualidade de código: Sonar como referência de visibilidade e gate

**Data:** 2026-02-01  
**Status:** Planejada

### Contexto
Precisamos consolidar qualidade como parte do fluxo, com feedback rápido em PR/branch e histórico evolutivo.

### Decisão
Usar **Sonar** como ferramenta central de:
- code smells
- bugs/vulnerabilidades
- coverage (via JaCoCo)
- quality gate

### Alternativas consideradas
- Apenas JaCoCo + revisão manual
- Linting isolado sem quality gate

### Motivos
- Visibilidade centralizada e histórica
- Padrão forte em times enterprise
- Automação de critérios mínimos de qualidade

### Consequências
- Exige configuração cuidadosa (exclusões/thresholds realistas)
- Pode gerar fricção se o gate for agressivo no início

---

## ADR-004 — Estratégia inicial de Quality Gate (baseline realista)

**Data:** 2026-02-01  
**Status:** Planejada

### Contexto
Cobertura atual é baixa em `service` e `security`. Um gate rígido bloquearia evolução.

### Decisão
Adotar **baseline realista** e aumentar gradualmente por ciclos:
- iniciar com thresholds mínimos viáveis
- focar principalmente em `service` e `security`
- evitar “cobertura artificial” em DTO/config

### Alternativas consideradas
- Gate alto (ex.: 80%+ desde o início)
- Sem gate (apenas visibilidade)

### Motivos
- Permite evolução incremental
- Evita “gaming” (testes fracos só para aumentar número)
- Foco no que reduz risco (branches e regras de negócio)

### Consequências
- Cobertura inicial não será “bonita”, mas será dirigida por valor
- Necessita disciplina para aumentar alvos aos poucos

---

## ADR-005 — CI/CD: pipeline com build + testes + quality (Sonar/JaCoCo)

**Data:** 2026-02-04  
**Status:** Planejada

### Contexto
Precisamos garantir repetibilidade e feedback rápido com validação automática.

### Decisão
Pipeline do Git deve executar:
1. `mvn clean verify`
2. gerar JaCoCo
3. publicar análise no Sonar
4. (quando aplicável) bloquear merge por Quality Gate

### Alternativas consideradas
- Executar quality apenas “on-demand”
- Executar testes sem quality no CI

### Motivos
- Segurança contra regressões
- Padronização do processo de entrega
- Qualidade como primeiro cidadão

### Consequências
- CI mais lento, porém mais confiável
- Necessidade de caching Maven para performance

---

## ADR-006 — Cache: Redis como cache de leitura com TTL e observabilidade

**Data:** 2026-02-07  
**Status:** Planejada

### Contexto
Desejo de evoluir performance e reduzir carga em leituras repetidas, sem comprometer consistência e simplicidade.

### Decisão
Adotar **Redis** inicialmente como **cache de leitura** (read-through/cache-aside), com:
- TTL definido por domínio
- invalidação simples quando houver update
- métricas/observabilidade mínima (hit/miss quando possível)

### Alternativas consideradas
- Cache em memória (Caffeine/ConcurrentMap)
- Sem cache
- Redis como state principal (não aplicável)

### Motivos
- Redis é padrão de mercado
- Ajuda em cenários de performance e escalabilidade
- Boa base para estudo e evolução

### Consequências
- Complexidade de invalidar/atualizar cache
- Dependência de infraestrutura (docker compose/local/prod)
- Necessidade de fallback (sem Redis, sistema continua)

---

## ADR-007 — Eventos: Kafka para evolução assíncrona orientada a eventos

**Data:** 2026-02-10  
**Status:** Planejada

### Contexto
Desejo de adicionar comunicação assíncrona e eventos de domínio, com observabilidade e contratos claros.

### Decisão
Adotar **Kafka** para eventos (ex.: `TransactionCreated`, `WalletUpdated`), com:
- contratos versionados
- consumidor com idempotência mínima
- observabilidade mínima (logs + métricas básicas)

### Alternativas consideradas
- RabbitMQ como padrão único
- Comunicação síncrona (REST) apenas

### Motivos
- Kafka é amplamente usado para event streaming
- Ajuda a estudar padrões de eventos e consumo
- Boa aderência para pipelines de eventos

### Consequências
- Operação local (docker compose) mais pesada
- Necessidade de gestão de contratos e evolução de schema
- Necessidade de considerar ordem/reprocessamento

---

## ADR-008 — MQ: fila tradicional para comparação e cenários de comando/worker

**Data:** 2026-02-13  
**Status:** Planejada

### Contexto
Desejo de suportar mensageria tradicional e comparar com Kafka, aplicando padrões como retry e DLQ.

### Decisão
Adotar uma fila MQ (recomendado **RabbitMQ** para estudo), com:
- um fluxo simples (ex.: auditoria/notify)
- retry básico
- DLQ (dead-letter) mínima
- documentação do contrato de mensagem

### Alternativas consideradas
- Apenas Kafka para tudo
- ActiveMQ
- SQS/serviço gerenciado (fora do escopo no momento)

### Motivos
- RabbitMQ é padrão em cenários de comando e worker
- Complementa o aprendizado com Kafka
- Facilita estudo de retry/DLQ e padrões de consumo

### Consequências
- Mais um componente de infra local
- Mais contratos e configurações para manter
- Necessidade de boas práticas de idempotência

---

## ADR-009 — Cadência de entregas: ciclos de 3 dias com DoD

**Data:** 2026-01-28  
**Status:** Aceita

### Contexto
Necessidade de cadência consistente, com entregas integradas e bem definidas.

### Decisão
Trabalhar por **ciclos de 3 dias**, concluindo cada ciclo com:
- código integrado
- testes e build passando
- documentação mínima atualizada
- validação no CI

### Alternativas consideradas
- Roadmap por “quarter”
- Trabalho por tarefas soltas sem marco de entrega

### Motivos
- Entregas frequentes e mensuráveis
- Reduz contexto perdido
- Ajuda a manter padrão de qualidade e disciplina

### Consequências
- Requer escopo bem controlado por ciclo
- Exige priorização clara e corte do “nice to have”

---

## ADR-010 — Versionamento: Integração SemVer entre Maven, Git e Docker

**Data:** 2026-02-22
**Status:** Aceita

### Contexto
Necessidade de garantir que a versão do código (Java) seja a mesma da imagem (Docker) e da documentação (OpenAPI), evitando desincronia e facilitando rastreabilidade em produção.

### Decisão
Centralizar a versão no `pom.xml` e propagá-la automaticamente:
- **Build:** Script `wallet.sh` extrai versão do Maven e injeta como tag Docker.
- **Runtime:** Spring Boot injeta `@project.version@` no `application.yml` para uso no Swagger e Actuator.
- **Git:** Plugin `git-commit-id` gera metadados do commit no artefato.

### Consequências
- `pom.xml` torna-se a fonte única da verdade para versão.
- Imagens Docker de desenvolvimento deixam de ser apenas `latest`, facilitando rollback e debug.
