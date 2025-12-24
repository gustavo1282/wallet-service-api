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

### Passo 1: Recompilar o JAR (Maven)

```bash
./mvnw clean package -DskipTests
```

---

### Passo 2: Atualizar apenas o container da aplicação

Este comando:
- Rebuilda a imagem
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