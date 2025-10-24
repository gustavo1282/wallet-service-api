# 🚀 Integração Contínua — Análise SonarQube

Este pacote adiciona ao projeto **Wallet Service API** o workflow do **GitHub Actions**
para realizar automaticamente a análise de qualidade de código via **SonarQube**.

---

## 🧱 Estrutura adicionada

```
.github/
└── workflows/
    └── sonar.yml
README_CI.md
```

---

## ⚙️ Passos para Configuração

### 1️⃣ Criar os Secrets no GitHub

1. Acesse o repositório no GitHub:  
   👉 https://github.com/gustavo1282/wallet-service-api

2. Vá em **Settings → Secrets and variables → Actions**  
   (ou **Settings → Actions → Secrets and variables** em versões mais novas).

3. Clique em **New repository secret** e adicione:

| Nome | Valor | Descrição |
|------|--------|------------|
| `SONAR_HOST_URL` | `http://localhost:9000` | Endereço do seu servidor SonarQube |
| `SONAR_TOKEN` | `SEU_TOKEN_GERADO_NO_SONAR` | Token pessoal gerado no painel do SonarQube |

---

### 2️⃣ Criar o Workflow localmente

Se a pasta `.github/workflows` ainda não existir, crie-a na raiz do projeto:
```
mkdir -p .github/workflows
```

Coloque o arquivo `sonar.yml` dentro dela.

---

### 3️⃣ Commitar e enviar ao repositório

```bash
git add .github/workflows/sonar.yml
git commit -m "ci(sonar): adiciona workflow de análise SonarQube"
git push origin feature/unit-tests-sonar
```

---

### 4️⃣ Executar o Workflow

Após o push:
1. Acesse a aba **Actions** do repositório no GitHub.  
2. Clique em **SonarQube Analysis**.  
3. Clique em **Run workflow** (graças ao `workflow_dispatch`).  
4. Acompanhe os logs da execução.

---

## 🧠 Detalhes Técnicos

- **Pipeline base:** Ubuntu runner (Linux)
- **Java:** versão 17 (Temurin)
- **Trigger:** Push, Pull Request e manual (`workflow_dispatch`)
- **Ferramentas:** Maven Wrapper (`./mvnw`) + Sonar Scanner Plugin
- **Autenticação:** via Secrets (`SONAR_HOST_URL` e `SONAR_TOKEN`)

---

## 🧩 Resultados da Análise

Ao final da execução:
- O GitHub Actions enviará o relatório ao servidor SonarQube.
- Você poderá visualizar em:  
  👉 `http://localhost:9000/projects`

---

## ✅ Verificação

| Item | Verifique se... |
|------|------------------|
| Pasta `.github/workflows` existe | ✅ |
| Secrets foram criados no GitHub | ✅ |
| SonarQube está rodando (porta 9000) | ✅ |
| Token foi gerado e válido | ✅ |

---

Pronto 🎯  
Com isso, seu projeto **Wallet Service API** está integrado ao **SonarQube** via **GitHub Actions** com execução automática e manual.
