# Build and CI/CD

Documentação sobre o processo de build, testes e integração contínua do Wallet Service API.

## 🏗️ Build Process

### Pré-requisitos

- **Java**: JDK 21+
- **Maven**: 3.9.11+ (ou use `./mvnw`)
- **Git**: Para controle de versão

### Compilação Local

#### 1. Limpar e Compilar

```bash
# Windows
mvnw.cmd clean package -DskipTests

# Linux/Mac
./mvnw clean package -DskipTests
```

**O que faz:**
- Remove artefatos anteriores (`clean`)
- Compila código-fonte
- Executa testes (se não usar `-DskipTests`)
- Gera JAR executável em `target/wallet-service-api-0.2.4-SNAPSHOT.jar`

#### 2. Com Testes Inclusos

```bash
./mvnw clean package
# Executa JUnit tests + JaCoCo coverage
```

#### 3. Apenas Compilar (sem packaging)

```bash
./mvnw clean compile
```

### Estrutura de Profiles Maven

**Arquivo:** `pom.xml`

```xml
<profiles>
    <profile>
        <id>local</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <spring.profiles.active>local</spring.profiles.active>
            <log.level>DEBUG</log.level>
        </properties>
    </profile>
    
    <profile>
        <id>cloud</id>
        <activation>
            <activeByDefault>false</activeByDefault>
        </activation>
        <properties>
            <spring.profiles.active>cloud</spring.profiles.active>
            <log.level>INFO</log.level>
            <sonar.organization>gustavo1282</sonar.organization>
        </properties>
    </profile>
</profiles>
```

**Usar profile específico:**
```bash
./mvnw clean package -Pcloud
```

## 🧪 Testes

### Estrutura de Testes

```
src/test/
├── java/com/guga/walletserviceapi/
│   ├── WalletServiceApplicationTests.java    # Testes de integração
│   ├── controller/                           # Testes dos controllers
│   ├── helpers/                              # Testes de utilidades
│   └── security/                             # Testes de segurança
└── resources/
    └── application-test.yml                  # Config de testes
```

### Executar Testes

#### Todos os testes
```bash
./mvnw test
```

#### Teste específico
```bash
./mvnw test -Dtest=CustomerControllerTest
```

#### Testes com padrão
```bash
./mvnw test -Dtest=*ControllerTest
```

#### Skip testes
```bash
./mvnw clean package -DskipTests
```

### Cobertura de Testes com JaCoCo

#### Gerar Relatório
```bash
./mvnw test jacoco:report
```

**Acessar relatório:**
```
target/site/jacoco/index.html
```

#### Verificar Cobertura Mínima
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <excludes>
                            <exclude>*Test</exclude>
                        </excludes>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Testes E2E com Newman

#### Visão Geral

A suíte E2E utiliza a collection Postman versionada no repositório para validar fluxos críticos da API.

**Arquivos envolvidos:**
- `data/postman/postman_wallet_collection.json`
- `data/postman/login_test_credentials.json`
- `data/scripts/newman/register_and_run.sh`
- `data/scripts/newman/run_newman_docker.sh`

#### Execução local (Newman instalado)

```bash
bash data/scripts/newman/register_and_run.sh http://localhost:8080/wallet-service-api
```

Esse script:
- Lê credenciais de `data/postman/login_test_credentials.json`
- Realiza `register` (best effort) e `login`
- Injeta `accessToken` na collection
- Gera relatórios JUnit XML por usuário em `data/scripts/newman/reports/newman`

#### Execução via Docker (sem instalar Newman local)

```bash
bash data/scripts/newman/run_newman_docker.sh \
  http://host.docker.internal:8080/wallet-service-api \
  postman_wallet_collection.json
```

No Linux/Mac, pode-se usar `http://localhost:8080/wallet-service-api`.

#### Relatórios

- Diretório: `data/scripts/newman/reports/newman`
- Formato: `JUnit XML`
- Exemplo: `result_<usuario>.xml`

### Script de Qualidade (`wallet_quality.sh`)

Automação para validação completa local de qualidade (build, testes, cobertura e análise estática).

**Arquivo:**
- `data/scripts/quality/wallet_quality.sh`

**Fluxo executado:**
1. `mvn -B clean verify` (build + testes + JaCoCo)
2. `mvn -B sonar:sonar` (quando `SKIP_SONAR=false`)
3. Abertura do relatório HTML do JaCoCo (se disponível)

**Uso básico:**

```bash
SONAR_TOKEN=xxxx bash data/scripts/quality/wallet_quality.sh
```

**Opções úteis:**

```bash
# Alterar host do Sonar
SONAR_HOST_URL=http://localhost:9000 SONAR_TOKEN=xxxx bash data/scripts/quality/wallet_quality.sh

# Rodar sem envio ao Sonar
SKIP_SONAR=true bash data/scripts/quality/wallet_quality.sh
```

**Entradas de ambiente:**
- `SONAR_HOST_URL` (default: `http://localhost:9000`)
- `SONAR_TOKEN` (obrigatório quando `SKIP_SONAR=false`)
- `SKIP_SONAR` (default: `false`)

**Saídas esperadas:**
- `target/site/jacoco/jacoco.xml`
- `target/site/jacoco/index.html`

## 🐳 Docker & Docker Compose

### Ambiente Multi-Container

**Arquivo:** `docker-compose.yml`

```yaml
version: '3.8'

services:
  wallet-service-api:
    build:
      context: .
      dockerfile: Dockerfile
    image: wallet_service:latest
    container_name: cont-wallet-service-api
    ports:
      - "8080:8080"    # API
      - "5005:5005"    # Debug
    environment:
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/wallet_db
      SPRING_DATASOURCE_USERNAME: wallet_user
      SPRING_DATASOURCE_PASSWORD: wallet_pass
    networks:
      - wallet_network
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: postgres:15.3-alpine
    container_name: cont-wallet-postgres
    environment:
      POSTGRES_DB: wallet_db
      POSTGRES_USER: wallet_user
      POSTGRES_PASSWORD: wallet_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wallet_user"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - wallet_network

volumes:
  postgres_data:

networks:
  wallet_network:
    driver: bridge
```

### Build da Imagem Docker

**Arquivo:** `Dockerfile` (Multi-stage)

```dockerfile
# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
```

### Comandos Docker

```bash
# Build manual (sem compose)
docker build -t wallet_service:latest .

# Executar aplicação e banco
docker-compose up -d

# Ver logs
docker-compose logs -f wallet-service-api

# Parar containers
docker-compose down

# Remover volumes (limpar dados)
docker-compose down -v

# Rebuildar imagem
docker-compose up -d --build

# Acessar container
docker exec -it cont-wallet-service-api bash

# Ver status
docker-compose ps
```

### Otimizações de Imagem

**Layer Caching:**
```dockerfile
# ✅ BOM: Dependências primeiro (cache reutilizável)
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package

# ❌ RUIM: Tudo junto (cache invalidado com qualquer mudança)
COPY . .
RUN mvn package
```

**Tamanho da Imagem:**
- Stage 1 (build): ~1.5GB
- Stage 2 (runtime): ~130MB ✅

## 🔄 CI/CD Pipeline

### GitHub Actions (Exemplo)

**Arquivo:** `.github/workflows/build.yml`

```yaml
name: Build & Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15.3-alpine
        env:
          POSTGRES_PASSWORD: wallet_pass
          POSTGRES_DB: wallet_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build with Maven
      run: ./mvnw clean package
    
    - name: Run Tests
      run: ./mvnw test
    
    - name: Upload Coverage
      uses: codecov/codecov-action@v3
      with:
        files: ./target/site/jacoco/jacoco.xml
    
    - name: SonarQube Analysis
      run: ./mvnw sonar:sonar \
        -Dsonar.projectKey=com.gugawallet:wallet-service-api \
        -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
        -Dsonar.login=${{ secrets.SONAR_TOKEN }}

    - name: Install Newman + jq
      run: |
        sudo apt-get update
        sudo apt-get install -y jq
        npm install -g newman

    - name: Start services
      run: docker compose up -d postgres wallet-service-api

    - name: Wait API health
      run: |
        for i in {1..30}; do
          curl -fsS http://localhost:8080/actuator/health && exit 0
          sleep 5
        done
        exit 1

    - name: Run Newman E2E
      run: bash data/scripts/newman/register_and_run.sh http://localhost:8080/wallet-service-api

    - name: Upload Newman reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: newman-reports
        path: data/scripts/newman/reports/newman/*.xml
```

### Jenkins Pipeline (Exemplo)

**Arquivo:** `Jenkinsfile`

```groovy
pipeline {
    agent any
    
    environment {
        JAVA_HOME = "/usr/lib/jvm/java-21-temurin"
        PATH = "${JAVA_HOME}/bin:${PATH}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'develop',
                    url: 'https://github.com/gustavo1282/wallet-service-api.git'
            }
        }
        
        stage('Build') {
            steps {
                sh './mvnw clean package -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                sh './mvnw test'
            }
        }
        
        stage('SonarQube') {
            steps {
                sh '''./mvnw sonar:sonar \
                  -Dsonar.projectKey=com.gugawallet:wallet-service-api \
                  -Dsonar.host.url=http://sonarqube:9000 \
                  -Dsonar.login=${SONAR_TOKEN}'''
            }
        }
        
        stage('Docker Build') {
            steps {
                sh 'docker build -t wallet_service:${BUILD_NUMBER} .'
            }
        }
        
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                sh 'docker-compose up -d'
            }
        }
    }
    
    post {
        always {
            junit 'target/surefire-reports/**/*.xml'
            publishHTML([
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html',
                reportName: 'JaCoCo Coverage'
            ])
        }
        failure {
            emailext(
                to: '${BUILD_LOG_REGEX_MATCH}',
                subject: 'Build Failed: ${JOB_NAME}',
                body: 'Build failed. Check logs: ${BUILD_URL}'
            )
        }
    }
}
```

## 🔍 Análise de Código

### SonarQube

#### Executar Análise

```bash
./mvnw clean package sonar:sonar \
  -Dsonar.projectKey=com.gugawallet:wallet-service-api \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=wallet-service-api-sonar-token
```

#### Configuração em `pom.xml`

```xml
<properties>
    <sonar.projectKey>com.gugawallet:wallet-service-api</sonar.projectKey>
    <sonar.host.url>http://localhost:9000</sonar.host.url>
    <sonar.token>wallet-service-api-sonar-token</sonar.token>
    <sonar.coverage.jacoco.xmlReportPaths>
        ${project.build.directory}/site/jacoco/jacoco.xml
    </sonar.coverage.jacoco.xmlReportPaths>
</properties>

<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>
```

#### Painel SonarQube

Acessar: `http://localhost:9000`

Métricas analisadas:
- ✅ Cobertura de código
- ✅ Vulnerabilidades de segurança
- ✅ Code smells
- ✅ Bugs detectados
- ✅ Duplicação de código
- ✅ Complexidade ciclomática

## 🚀 Deploy & Release

### Versionamento Semântico

Versão atual: **0.2.4-SNAPSHOT**

Formato: `MAJOR.MINOR.PATCH[-QUALIFIER]`

**Exemplo de incremento:**

```bash
# Release (remove SNAPSHOT)
mvn versions:set -DnewVersion=0.2.4
mvn versions:commit

# Nova versão de desenvolvimento
mvn versions:set -DnewVersion=0.2.5-SNAPSHOT
mvn versions:commit

# Deploy
mvn clean deploy
```

### Deploy em Produção

#### 1. Preparar Release

```bash
git checkout main
git pull origin main
./mvnw clean package
```

#### 2. Executar com Docker

```bash
# Build
docker build -t wallet_service:0.2.4 .

# Tag
docker tag wallet_service:0.2.4 myregistry.azurecr.io/wallet_service:0.2.4

# Push
docker push myregistry.azurecr.io/wallet_service:0.2.4

# Deploy (Kubernetes ou Docker Swarm)
kubectl apply -f k8s-deployment.yaml
```

#### 3. Health Check Pós-Deploy

```bash
# Esperar aplicação iniciar
sleep 30

# Health check
curl -f http://localhost:8080/wallet-service-api/actuator/health \
  || exit 1

# Smoke test
curl -X GET http://localhost:8080/wallet-service-api/actuator/info
```

### Blue-Green Deployment

```yaml
# Blue (versão atual)
deployment-blue:
  image: wallet_service:0.2.3

# Green (versão nova)
deployment-green:
  image: wallet_service:0.2.4

# Após testes bem-sucedidos:
# 1. Green recebe tráfego
# 2. Blue permanece como rollback
# 3. Blue é desligado após confirmação
```

## 📊 Métricas de Build

### Tempo de Build

```bash
./mvnw clean package -DskipTests \
  | grep -E "BUILD|Total"
```

Tempo esperado:
- `clean package -DskipTests`: ~30-45s
- `clean package` (com testes): ~2-3 minutos
- `clean deploy`: ~5 minutos

### Tamanho de Artefatos

```bash
# JAR da aplicação
ls -lh target/wallet-service-api-*.jar

# Esperado: ~50-80 MB
```

## 🔧 Troubleshooting

### Erro: "Maven command not found"

```bash
# Windows
set PATH=%PATH%;C:\Users\seu_usuario\AppData\Local\Programs\Git\cmd

# Linux/Mac
export PATH=$PATH:~/maven/apache-maven-3.9.11/bin
```

### Erro: "Java version mismatch"

```bash
# Verificar versão Java
java -version

# Deve ser 21+

# Configurar JAVA_HOME
set JAVA_HOME=C:\Program Files\Java\jdk-21  # Windows
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk  # Linux
```

### Build falha em testes

```bash
# Executar com debug
./mvnw test -X

# Apenas um teste
./mvnw test -Dtest=WalletServiceTest
```

### Docker não encontra PostgreSQL

```bash
# Verificar se containers estão rodando
docker-compose ps

# Verificar logs
docker-compose logs postgres

# Reiniciar
docker-compose restart postgres
```

## 📚 Recursos Adicionais

- [Maven Documentation](https://maven.apache.org/guides/)
- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Build](https://spring.io/guides/gs/maven/)
- [JaCoCo Coverage](https://www.eclemma.org/jacoco/)
- [SonarQube Documentation](https://docs.sonarqube.org/)
