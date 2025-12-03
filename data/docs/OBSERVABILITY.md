# Observability

Este documento descreve abordagens de logs, métricas e rastreamento do Wallet Service API.

---

# 📝 Logs

- Log centralizado em todas as operações financeiras
- Identificação única por transação
- Logs estruturados
- Registro de erros com stacktrace

---

# 🔍 Tracing

- Cada request recebe um identificador
- Movimentações e transações são rastreáveis

---

# 📈 Métricas Recomendadas (não implementadas, mas sugeridas)

- Total de depósitos por hora
- Total de transferências
- Saldo agregado por dia
- Movimentações por wallet
- Erros por tipo

---

# 🚨 Alertas Recomendados

- Wallet com saldo negativo inesperado
- Falha recorrente de transferências
- Operações lentas
