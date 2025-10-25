# 🚀 Integração Contínua — Análise SonarQube

Este pacote adiciona ao projeto **Wallet Service API** o workflow do **GitHub Actions**  
para realizar automaticamente a análise de qualidade de código via **SonarQube**, tanto em ambiente **local (Docker)** quanto na **nuvem (SonarCloud)**.

---

## 🧱 Estrutura adicionada

```
.github/
└── workflows/
    └── sonar.yml
ci.md
```

---

## ⚙️ Configuração dos Ambientes

### 🔹 1. Ambiente Local (SonarQube em Docker)

Use esta configuração quando quiser rodar o SonarQube localmente em sua máquina.

#### 🧩 Pré-requisitos

- Docker instalado
- Maven configurado (ou wrapper `./mvnw`)
- SonarQube rodando localmente

#### ▶️ Subindo o SonarQube Local

```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts
```

Verifique se está rodando:
```bash
docker ps | grep sonarqube
```

Acesse em: [http://localhost:9000](http://localhost:9000)  
Login padrão: `admin` / `admin`

#### ▶️ Executando a análise localmente

```bash
mvn clean verify sonar:sonar -DskipTests \
  -Dsonar.login=admin \
  -Dsonar.password=admin123 \
  -Dsonar.host.url=http://localhost:9000 \
  -P desenv
```

Isso executará a análise com o perfil `desenv` e enviará os resultados para seu Sonar local.

---

### 🔹 2. Ambiente Cloud (GitHub Actions + SonarCloud)

Use este modo para que o **GitHub Actions** execute a análise e envie os resultados para o **SonarCloud** automaticamente.

#### 🧩 Configuração dos Secrets

No repositório do GitHub:

1. Vá em **Settings → Secrets and variables → Actions**
2. Clique em **New repository secret**
3. Adicione os seguintes valores:

| Nome | Valor | Descrição |
|------|--------|------------|
| `SONAR_HOST_URL` | `https://sonarcloud.io` | URL da instância do Sonar na nuvem |
| `SONAR_TOKEN` | `seu_token_sonarcloud` | Token gerado no painel do SonarCloud |
| `SONAR_ORGANIZATION` | `sua_organizacao` | (opcional) Nome da organização no SonarCloud |
| `SONAR_PROJECT_KEY` | `wallet-service-api` | Chave do projeto cadastrada no SonarCloud |

---

## 🧩 Arquivo `.github/workflows/sonar.yml`

```yaml
name: 🧠 SonarQube Analysis

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
  workflow_dispatch:

jobs:
  sonar:
    name: 🔍 SonarQube Quality Scan
    runs-on: ubuntu-latest

    strategy:
      matrix:
        profile: [ desenv, cloud ]

    steps:
      - name: 🛒 Checkout code
        uses: actions/checkout@v4

      - name: ☕ Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: 17
          cache: 'maven'

      - name: 🔨 Build and analyze
        run: |
          if [ "${{ matrix.profile }}" == "desenv" ]; then
            ./mvnw clean verify sonar:sonar -DskipTests \
              -Dsonar.login=admin \
              -Dsonar.password=admin123 \
              -Dsonar.host.url=http://localhost:9000 \
              -P desenv
          else
            ./mvnw clean verify sonar:sonar -DskipTests \
              -Dsonar.projectKey=${{ secrets.SONAR_PROJECT_KEY }} \
              -Dsonar.organization=${{ secrets.SONAR_ORGANIZATION }} \
              -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
              -Dsonar.login=${{ secrets.SONAR_TOKEN }} \
              -P cloud
          fi
```

---

## 🧪 Execução Manual

1. Vá até a aba **Actions** do repositório no GitHub
2. Selecione o workflow **SonarQube Analysis**
3. Clique em **Run workflow**
4. Escolha o **profile** desejado (local ou cloud, conforme configurado)
5. Acompanhe os logs da execução

---

## ✅ Checklist de Verificação

| Item | Verifique se... | Status |
|------|------------------|--------|
| Pasta `.github/workflows` existe | ✅ | |
| Secrets foram criados no GitHub | ✅ | |
| SonarQube local está rodando (porta 9000) | ✅ | |
| SonarCloud possui o projeto configurado | ✅ | |
| Token está válido e com permissões corretas | ✅ | |

---

## 🎯 Resumo

| Modo | Execução | Destino | Autenticação |
|------|-----------|----------|---------------|
| **Local (Docker)** | `mvn sonar:sonar -P desenv` | `http://localhost:9000` | admin / admin123 |
| **Cloud (GitHub)** | GitHub Actions (CI) | `https://sonarcloud.io` | Token via Secrets |

---

Com este setup, seu projeto **Wallet Service API** está preparado para análise de código tanto **localmente** quanto **em integração contínua via GitHub Actions**, com total flexibilidade para alternar entre perfis de execução. 🚀
