# syntax=docker/dockerfile:1
# Estágio 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Configurações de log do Maven para o seu acompanhamento de milissegundos
ENV MAVEN_OPTS="-Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss.SSS"

# 2. Copia apenas a estrutura de gerenciamento de dependências
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw

# 3. Em vez de go-offline, fazemos um "verify" apenas do pom para aquecer o cache.
# O mount=type=cache garante que o que for baixado aqui sobreviva ao próximo comando.
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:resolve-plugins -B

# 4. Copia o código fonte
COPY src ./src

# 5. Build Final (SEM o modo -o para evitar erros de Parent POM ausente)
# O uso do cache mount aqui faz com que o Maven perceba que 99% das libs já estão lá
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests -B

# Estágio 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

# Boas práticas para Java 21+
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]