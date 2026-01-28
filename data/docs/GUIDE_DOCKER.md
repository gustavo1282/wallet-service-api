# 🐳 Guia Prático – Docker Compose (2025)

Este guia reúne os **comandos essenciais do Docker Compose** para manipular seu ambiente consolidado de desenvolvimento em **2025**, facilitando o dia a dia do time.

---

## 1️⃣ Como iniciar ou parar o ambiente

O comando `up` lê o arquivo `docker-compose.yml` e sobe todos os serviços definidos.

### ▶️ Subir tudo (em segundo plano)

```bash
docker-compose up -d
```

---

### ⏸️ Parar tudo (mantendo os dados dos volumes)

```bash
docker-compose stop
```

---

### ▶️ Subir novamente (após um `stop`)

```bash
docker-compose start
```

---

### 🧨 Derrubar tudo (remove containers, mantém volumes)

```bash
docker-compose down
```

---

## 2️⃣ Alteração no código – Como atualizar a aplicação?

Como o projeto utiliza:

```yaml
build:
  context: .
```

o Docker precisa **recriar a imagem** sempre que o código Java é alterado.

> ⚠️ Importante: **não é necessário derrubar o banco de dados** para atualizar apenas a aplicação.

---

### Atualizar apenas o container da aplicação

Este comando:
- **Compila o projeto** (dentro do Docker, via Multistage build)
- Recria a imagem
- Reinicia **somente** a aplicação
- Mantém Postgres, Jaeger e outros serviços ativos

```bash
docker-compose up -d --build wallet-service-api
```

---

## 3️⃣ Como remover tudo (Containers + Imagens + Volumes)

Use estes comandos quando quiser **limpar completamente o ambiente**.

---

### 🧹 Remover containers e volumes (apaga dados do banco)

```bash
docker-compose down -v
```

---

### 🔥 Remover tudo, inclusive as imagens

```bash
docker-compose down -v --rmi all
```

---

### 🧼 Limpeza profunda (cache + imagens órfãs)

```bash
docker system prune -a
```

> ⚠️ Atenção: este comando remove **todas as imagens não utilizadas** no Docker.

---

## 4️⃣ Comandos de manipulação e inspeção (`docker exec`)

Para interagir diretamente com containers em execução.

---

### 📄 Ver logs em tempo real

Fundamental para depuração de **OpenTelemetry**, **Spring Boot** e falhas de integração.

```bash
docker-compose logs -f wallet-service-api
```

---

### 🐚 Entrar dentro do container (Shell)

Útil para:
- Navegar em diretórios
- Inspecionar arquivos
- Validar variáveis de ambiente

```bash
docker exec -it cont-wallet-service-api /bin/sh
```

Ou, no caso do runner ACT:

```bash
docker exec -it cont-wallet-runner-act /bin/bash
```

---

### 📂 Ler um arquivo dentro do container (sem entrar nele)

```bash
docker exec cont-wallet-service-api cat /var/lib/wallet_service/data/log-especifico.txt
```

---

### 📊 Ver consumo de CPU e memória (Docker Stats – 2025)

```bash
docker stats
```

---

## 4️⃣ Serviços de Observabilidade

A stack inclui serviços para monitoramento e tracing:

- **Prometheus** (porta 9090): Métricas em http://localhost:9090
- **Grafana** (porta 3000): Dashboards em http://localhost:3000 (login: admin/admin)
- **Jaeger** (porta 16686): Traces em http://localhost:16686
- **Tempo** (porta 3200): Backend de tracing (acessível via Grafana)
- **OpenTelemetry Collector** (portas 4317/4318/8889): Recebe traces e gera métricas
- **Vault** (porta 8200): Gestão de segredos

### ▶️ Subir apenas observabilidade
```bash
docker-compose up -d prometheus grafana jaeger 
```

### 🧹 Limpar apenas observabilidade
```bash
docker-compose down prometheus grafana jaeger otel-collector
```

---

## 💡 Dica de Ouro (2025)

Sempre que ocorrer:

- ❌ Erro **403 no Swagger**
- ❌ Falha no **OpenTelemetry**
- ❌ Problemas de tracing

👉 **Use imediatamente:**

```bash
docker-compose logs -f wallet-service-api
```

Se aparecer:

```
Connection refused
```

para o Jaeger:

1. Execute:
   ```bash
   docker ps
   ```
2. Verifique se o container `cont-wallet-jaeger` está com status **Up**

---

## 📚 Referência Oficial

Para documentação completa e sempre atualizada, consulte:

👉 **Guia oficia
